---
title: Extensions to Polymorphic Parameters
layout: default
published: false
---
# {{page.title}}

We refactor to extract duplicate logic into extension methods. But refactoring to extension methods _itself_ leads to duplication.

Here we have lots of methods that extract a value from a row of a database query result set.

```kotlin
fun ResultSet.getOffsetDateTime(name: String): OffsetDateTime = ...
fun ResultSet.getPostCode(name: String): PostCode = ...
fun ResultSet.getTelephoneNumber(name: String): TelephoneNumber = ...
fun ResultSet.getEmailAddress(name: String): EmailAddress = ...
```

And because columns can be nullable, we also have:

```kotlin
fun ResultSet.getOffsetDateTimeOrNull(name: String): OffsetDateTime? = ...
fun ResultSet.getPostCodeOrNull(name: String): PostCode? = ...
fun ResultSet.getTelephoneNumberOrNull(name: String): TelephoneNumber? = ...
fun ResultSet.getEmailAddressOrNull(name: String): EmailAddress? = ...
```

Each extension has very similar logic:

```kotlin
fun ResultSet.getPostCodeOrNull(name: String): PostCode? {
    val strValue: String? = getString(name);
    if (wasNull()) {
        return null
    } else {
        val convertedValue = PostCode.parse(strValue!!)
        return convertedValue 
            ?: throw SqlConversionError("could not parse column $name as a PostCode")
    }
}

fun ResultSet.getPostCode(name: String) =
    getPostCodeOrNull(name)
        ?: throw SqlConversionError("column $name was null, expected non-null")
```


(Note: we are using exceptions for error handling here, for clarity.  In practice we would probably use a Result or Either type to represent either a successful conversion or a failure.)

And you address that by introducing a polymorphic parameter that embodies the variation obvious in the the extension function names (String, Date, Money, …)

Then you identify groups of parameters that you always pass around together, and can factor those out into a higher level abstraction that is easier to pass around and compose into yet higher-level abstractions.
In this case, name and converter are tightly related for any result set.  So you might want to combine them into a ResultColumn abstraction.

```kotlin
data class ResultColumn<T>(name: String, toKotlin: (ResultSet,String)->Result<T,ConversionError>)
```

And then you could compose ResultColumns to define converters for structured types, or the structure of your result sets.
