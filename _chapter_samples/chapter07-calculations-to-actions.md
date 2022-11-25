---
title: Sample Chapter 7 - Calculations and Actions
layout: sample
---

Neither Java nor Kotlin makes any formal distinction between imperative and functional code, although Kotlin’s emphasis on immutability and expressions generally leads to more functional programs. Can we improve our code by mak‐ ing more of it functional?

## Functions

As an industry, we have invented a lot of phrases to describe callable subprograms within a larger program.
We have the very generic _subroutine_.
Some languages (notably Pascal) distinguish between _functions_ that return a result, and _procedures_, which don't; but most developers use the terms interchangeably.
Then there are _methods_, which are subroutines associated with an object (or a class, in the case of static methods).

The C language calls them all functions but has a special `void` type to represent the absence of a return value.
This was carried forward into Java.
Kotlin uses `Unit` in almost the same way, except that `Unit` is not the absence of a return value, but rather a singleton value that is returned instead.

In this book we use the term _function_ to refer to both result-returning and non-result-returning subroutines, whether freestanding or associated with an object.
Where it's significant that they are associated with an object, we'll call them methods.

Whatever we call them, functions are one of the fundamental building blocks of our software.
We define them with some sort of notation, generally the programming language we are using.
They are also generally fixed during a run of the program; in static languages, at least, we don't usually redefine functions on the fly.

This is in contrast to the other fundamental building block: data.
We expect data to vary as we run our program, and different data is bound to variables.
Variables are called variables because they are, wait for it, variable.
Even when they are `final`, or `val`, they are usually bound to different data in different invocations of a function.

We hinted earlier at a subdivision of functions into those that return a result and those that do not.
This might seem like a fundamental difference, but in practice there is a more useful way to divide functions: into _calculations_ and _actions_.

Actions are functions that depend on when or how many times they are run; calculations are functions that don't—they are timeless.
Most functions that we write are actions, because we have to take special care to write code that doesn't depend on when it is run.
How would we go about doing that?

## Calculations

To be a calculation, a function must always return the same result given the same inputs.
The inputs to a function are its parameters, which are bound to arguments when the function is called.
So a calculation always returns the same result when called with the same arguments.

Take a `fullName` function:

```kotlin
fun fullName(customer: Customer) = "${customer.givenName} ${customer.familyName}"
```

`fullName` is a calculation: it will always return the same value when supplied the same `Customer`.
This is true only if `Customer` is immutable, or at least `givenName` and `familyName` cannot change.
To keep things simple, we'll say that calculations can only have parameters that are values, as defined in [Chapter 5](https://java-to-kotlin.dev/).

Methods, and the disguised methods that are member properties, can also be calculations:

```kotlin
data class Customer(
    val givenName: String,
    val familyName: String
) {
    fun upperCaseGivenName() = givenName.toUpperCase()

    val fullName get() = "$givenName $familyName"
}
```

For a method or extension, the receiver `this`, and any property accessed via `this`, is also an input.
So both `upperCaseGivenName` and `fullName` are calculations because `givenName` and `familyName` are both values.

An extension function or property can also be a calculation if the data it depends on is a value:

```kotlin
fun Customer.fullName() = "$givenName $familyName"

val Customer.fullName get() = "$givenName $familyName"
```

****
### Sidebar - Computed Property or Function?
You may have wondered when to define a computed property and when to have a function that returns a result.
Computed properties are confusing if they return different results at different times, at least when defined on value types (and you'll be realizing by now that your authors think that most of our types should be value types).
So a good rule of thumb is to reserve computed properties for calculations.

We expand on this topic in [Chapter 11](https://java-to-kotlin.dev/).

****

The result of a calculation may depend on data that is not passed as parameters, but only if that data does not change.
Otherwise, the function's result would be different before and after the change, which would make it an action.
Even if a function always returns the same result for the same parameters, it may still be an action if it mutates something (either a parameter or an external resource such as a global variable or a database).
For example:

```kotlin
println("hello")
```

`println` always returns the same `Unit` result given the same `hello` input, but it is not a calculation.
It is an action.

## Actions

`println` is an action because it _does_ depend on when and how many times it is run.
If we don't call it, nothing is output, which is different from calling it once, which is different from calling it twice.
The order that we call `println` with different arguments also matters to the results we see on the console.

We call `println` for its _side effect_—the effect it has on its environment.
Side effect is a bit of a misleading term because, unlike drug side effects, they are often exactly the thing that we want to happen.
Maybe _outside effect_ would be a better name, to emphasize that they are external to a function's parameters, local variables, and return value.
In any case, functions with observable side effects are actions not calculations.
Functions returning `void` or `Unit` are almost always actions, because if they do anything, they have to do it by side effect.

As we saw previously, code that reads from external mutable state must also be an action (provided that anything does actually mutate the state).

Let's look at a `Customers` service:

```kotlin
class Customers {
    fun save(data: CustomerData): Customer {
        ... 
    }
    fun find(id: String): Customer? {
        ...
    }
}
```

Both `save` and `find` are actions; `save` creates a new customer record in our database and returns it.
This is an action because the state of our database depends on when we call it.
The result of `find` is also time sensitive, because it depends on previous calls to `save`.

Functions that have no parameters (this doesn't include methods or extension functions, which can have implicit parameters accessed via `this`) must either be returning a constant or be reading from some other source and so be categorized as actions.
Without looking at its source, we can deduce that a top-level function `requestRate` is almost certainly an action, reading from some global mutable state:

```kotlin
fun requestRate(): Double {
    ...
}
```

If a function with the same apparent signature is defined as a method, it is probably a calculation that depends on properties of `Metrics` (provided `Metrics` is immutable):

```kotlin
class Metrics(
...
) {

    fun requestRate(): Double {
        ...
    }
}
```

We say _probably_ because in languages like Java or Kotlin that allow input, output, or accessing global mutable data from any code, there is no way to be sure whether a function represents a calculation or action short of examining it and all the functions that it calls.
We'll return to that problem soon.

## Why Should We Care?

We should obviously pay special attention to some actions in our software.
Sending the same email to every user twice is a bug, as is not sending it at all.
We care exactly how many times it is sent.
We may even care that it is sent at exactly 8:07 a.m., so that our offer for a free first-class upgrade is at the top of our customer's inbox when they read their email over breakfast.

Other seemingly innocuous actions may be more nocuous than we think.
Changing the order of read and write actions causes concurrency bugs.
Error handling is much more complicated if the second of two sequential actions fails after the first succeeded.
Actions prevent us from having free rein to refactor our code, because doing so may change when or whether they are invoked.

Calculations, on the other hand, can be invoked at any time, with no consequences for calling them again and again with the same arguments except a waste of time and energy.
If we are refactoring code and find that we don't need the result of a calculation, we can safely not invoke it.
If it is an expensive calculation, we can safely cache its result; if it is inexpensive, we can safely recalculate it on demand if that simplifies things.
It is this feeling of safety that puts the smug smile on the faces of functional programmers (well, that and knowing that a monad is just a monoid in the category of endofunctors).
Those functional programmers also have a term for the property of a function that makes it a calculation: _referential transparency_.
If a function is referentially transparent, we can replace its call with its result, and we can only do that if it doesn't matter when or if we call it.

****
### Sidebar - Procedural Code

Nat and Duncan are both old enough to have learned to program in Sinclair BASIC on the ZX81.
This dialect had no immutable data, and no support for subroutines, parameters, or local variables.
It requires real discipline to program in such a system, because practically every line of code is an action and so potentially affects the functioning of every other statement.

This is in fact very close to the way that our computers actually work, with mutable values held in registers and global memory, manipulated by machine-code actions.
The evolution of programming languages has been a process of restricting the ultimate flexibility of this model, so that humans can better reason with the code that they create.

****

## Why Prefer Calculations?

We like calculations because they are so much easier to work with, but ultimately our software needs to have an effect on the world, which is an action.
There is no overlap though; code can't be an action and a calculation, both timeless and time-dependent.
If we take some code that is a calculation and have it invoke an action, then it becomes an action, because it will now depend on when or whether it is called.
We can think of calculations as the purer code, where code inherits the most tainted level of all of its dependencies.
We see the same thing with susceptibility to errors in [Chapter 19](https://java-to-kotlin.dev/).
If we value purity (which in all these cases brings ease of reasoning and refactoring), we must strive to pull the boundary between impure and pure code to the outer layers of our system—those closest to the entry points.
If we succeed, then a significant proportion of our code can be calculations and, hence, easily tested, reasoned with, and refactored.

What if we don't succeed in keeping actions at the bottom of our call stack?
Then we can fix things with refactoring!
