---
title: Test Data Builders to Constants
layout: default
published: false
---
# {{page.title}}

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

