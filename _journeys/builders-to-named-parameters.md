---
title: Builders to Named Parameters
layout: default
published: false
---
# {{page.title}}

A recent newcomer to Java might be struck by the way libraries provide "builders" to help programmers construct objects.
What do Java programmers use builders for? And what does Kotlin do differently for those use cases?

## Builders in Java

The authors of <<GHJV_DPEOROOS_1994,_Design Patterns: Elements of Reusable Object-Oriented Software_>> describe the intent of the Builder pattern as:

> Separate the construction of a complex object from its representation so that the same construction process can create different representations.
>
> ...
>
> Use the Builder pattern when
>
> * the algorithm for creating a complex object should be independent of the parts that make up the object and how they’re assembled.
>
> * the construction process must allow different representations for the object that’s constructed.

However, that's not how Java programmers typically write builers.
Most Java builders construct just one kind of object and do not have an abstract interface.
Java programmers do not use builders to separate construction from representation, but to cope with limitations of how Java constructs objects.

Firstly, Java methods do not have named parameters.
This means the code of a constructor call does not make explicit how it is initialising the properties of the object with the values passed to the constructor.

For example, here is a method that builds a FerryBookingRequest from a posted web form:

<!-- begin insert: -->
```java
public FerryBookingRequest formToBookingRequest(Form form, Trip trip) {
    return new FerryBookingRequest(
        form.get("service_id"),
        form.get("berths", asInt, trip::getTravellerCount),
        form.get("cabin_class"),
        iterate(1, i -> form.contains("vehicle_" + i), i -> i + 1)
            .mapToObj(i -> {
                var v = "vehicle_" + i;
                return new VehicleDetails(
                    form.get(v + "_type", asEnum(VehicleType.class)),
                    form.get(v + "_registration"));
            })
            .collect(toList()));
}
```

Apart from making the code difficult to read,  errors can slip in when two or more constructor arguments have the same type.
The constructor parameters `serviceId`  and `cabinClass` are both of type String.
Are we passing the form field values to the correct constructor parameters?
It's impossible to say without refering to the definition of the FerryBookingRequest constructor:

```java
public class FerryBookingRequest {
    private String providerServiceId;
    private ZonedDateTime dateTime;
    private int numberOfBerths;
    private String cabinClass;
    private List<VehicleDetails> vehicles;

    public FerryBookingRequest() {
    }

    public FerryBookingRequest(String providerServiceId,
                               int numberOfBerths,
                               String cabinClass,
                               List<VehicleDetails> vehicles
    ) {
        this.providerServiceId = providerServiceId;
        this.numberOfBerths = numberOfBerths;
        this.cabinClass = cabinClass;
        this.vehicles = vehicles;
    }

    ...
}
```

Phew! We got them right.

A lot of Java codebases avoid these problems by following Bean conventions.
The code calls a constructor with no arguments to create a new object, and then sets the object's properties after it has been constructed.

```java
public FerryBookingRequest formToBookingRequest(Form form, Trip trip) {
    var request = new FerryBookingRequest();
    request.setProviderServiceId(form.get("service_id"));
    request.setNumberOfBerths(
    form.get("berths", asInt, trip::getTravellerCount));
    request.setCabinClass(form.get("cabin_class"));

    for(var i = 1; form.contains("vehicle_" + i); i++) {
        var vehicle = new VehicleDetails();
        vehicle.setType(
            form.get("vehicle_" + i + "_type",
                asEnum(VehicleType.class)));
        vehicle.setRegistration(
            form.get("vehicle_" + i + "_registration"));

        request.getVehicles().add(vehicle);
    }

    return request;
}
```

We can now clearly see which properties the code initialises.
However, this style constructs an object in an invalid state -- the no-argument constructor initialises object references stored by the object to null -- and the type system cannot guarantee that our code puts the object into a valid state before we use it.
If we forget to set all the necessary properties, our code will still compile but methods of the object will fail at runtime with a NullPointerException.
For example, how easily did you notice that the call to `request.getVehicles().add(vehicle)` above will fail, because the no-arg constructor leaves the vehicles property set to a null reference, instead of initialising it to an empty list?

Even if we construct the object correctly, Bean-style initialisation
If someone -- maybe even ourselves -- later adds a property to the class and doesn't change our code to match,
the code will continue to compile but now leave the object partially initialised.
Calls to the object will fail, often far -- in space and time -- from the construction code that is the source of the bug.
You've got to have very thorough test coverage of integrated code to have a good chance of catching these problems.

Another annoyance of Bean-style initialisation is that we need to write imperative code to construct an object.
We cannot use this style of code to create immutable objects.
We have to declare local variables to hold partially constructed objects and write statements to connect our objects together.
Because the construction code is linear, it does not portray the structure of objects it is creating.

The larger the object graph, the more we want our code to portray the structure of the graph, but the more helpful it is to name the properties of our objects.
That's what builders solve for Java programmers.
You can write expressions with builders that mirror the shape of the object graphs being built (unlike beans) _and_ the expressions show how they initialise the objects' properties (unlike constructors).

Here's what that object graph would look like if we construct it with builders:

```java
public FerryBookingRequest formToBookingRequest(Form form, Trip trip) {
    FerryBookingRequestBuilder builder = new FerryBookingRequestBuilder() // <1>
        .withProviderServiceId(form.get("service_id")) // <2>
        .withNumberOfBerths(
            form.get("berths", asInt, trip::getTravellerCount))
        .withCabinClass(form.get("cabin_class"));

    for (var i = 1; form.contains("vehicle_" + i); i++) {
        builder.withVehicle(new VehicleDetailsBuilder() // <3>
            .withType(
                form.get("vehicle_" + i + "_type",
                    asEnum(VehicleType.class)))
            .withRegistration(
                form.get("vehicle_" + i + "_registration"))
            .build()); // <4>
    }
    return builder.build(); // <4>
}
```

<1> We create a builder for a FerryBookingRequest
<2> We collect the values we will use to create the FerryBookingRequest. The methods of the builder make it clear which properties are being set, so we are unlikely to initialise a property from the wrong form field (as long as the form fields are sensibly named).  The methods start with the prefix "with", to distinguish them from bean setters, and return the builder so that the building of the object can be done in a single expression of chained method calls.
<3> We can use other builders to create sub-objects.
<4> We call `build()` to construct the FerryBookingRequest. Can fail in this method if any properties are missing, so that invalid objects are reported in the buggy code that creates the object, rather than distant code that uses it.  Not as good as a type-safe constructor call, but better than Bean conventions.

So, code using builders combines benefits of constructor calls and of JavaBean conventions.

What does it take to write the builder itself?

```java
class FerryBookingRequestBuilder implements travelator.Builder<FerryBookingRequest> {
    private String providerServiceId;
    private int numberOfBerths = 0;
    private String cabinClass;
    private final List<VehicleDetails> vehicles = new ArrayList<>();

    @Override
    public FerryBookingRequest build() {
        return new FerryBookingRequest(
            providerServiceId,
            numberOfBerths,
            cabinClass,
            new ArrayList<>(vehicles));
    }

    public FerryBookingRequestBuilder withProviderServiceId(String providerServiceId) {
        this.providerServiceId = providerServiceId;
        return this;
    }

    public FerryBookingRequestBuilder withNumberOfBerths(int numberOfAdults) {
        this.numberOfBerths = numberOfAdults;
        return this;
    }

    public FerryBookingRequestBuilder withCabinClass(String cabinClass) {
        this.cabinClass = cabinClass;
        return this;
    }

    public FerryBookingRequestBuilder withVehicles(List<VehicleDetails> vehicles) {
        this.vehicles.addAll(vehicles);
        return this;
    }

    public FerryBookingRequestBuilder withVehicle(VehicleDetails vehicle) {
        this.vehicles.add(vehicle);
        return this;
    }
}
```

That's quite a lot of boilerplate code!

Yet Java programmers clearly find builders to be worth the effort.
Lots of open source libraries and even the standard library now provide builders for their classes.

## Test Data Builders

In test code, Java programmers use builders for a different reason.
Here the goal of the builder is to create objects pre-initialised with safe defaults for testing, and allow tests to specify the values only of those properties relevant to the test scenario.

In our Travelator test modules we write static factory methods that create test data builders:

```java
public class BookingExamples {
    public static FerryBookingRequestBuilder aFerryBooking() {
        return new FerryBookingRequestBuilder()
            .withProviderServiceId("example-service")
            .withNumberOfBerths(2)
            .withCabinClass("standard");
    }
    
    public static VehicleDetailsBuilder aVehicle() {
        return new VehicleDetailsBuilder()
            .withType(CAR)
            .withRegistration("CPL593H");
    }
}
```

Our tests import these factory methods statically, so the code reads as a clear explanation of the test scenario.
For example, a test that needs a ferry booking request with a campervan need only specify that the booking has a vehicle with type `CAMPERVAN`.
The rest of the booking properties can be safely left as the default, so it is clear to the reader which properties affect the outcome of this test scenario and which are irrelevant.

```java
public class FerryBookingTest {
    public void booking_with_one_campervan() {
        var request = aFerryBooking()
        .withVehicle(aVehicle()
        .withType(CAMPERVAN)
        .build())
        .build();
        
            // ... use the request in the test
    }
}
```

## Kotlin's alternatives to builders

What alternatives does Kotlin offer?

Named parameters: code that constructs object graphs can be readable.

Apply function: Imperative code in a block, caller can treat it as an expression.

Data classes: copy instead of mutate, constants instead of builders.


## Refactor from Builders to immutable data



## Moving On

