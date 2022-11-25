---
title: Sample Chapter 5 - Beans to Values
layout: sample
---

Many Java projects have settled on mutable JavaBeans or POJO (plain old Java object) conventions for representing data.
Mutability brings complications, though.
Why are immutable values a better choice, and how can we reduce the cost of mutability in a codebase?

## Beans

As we discussed in the introduction, JavaBeans were introduced to allow the development of drag-and-drop GUI builders in the Visual Basic style.
A developer could drop a button onto a form, change its title and icon, and then wire in an on-click handler.
Behind the scenes, the GUI builder would write code to instantiate a button object and then call setters for the properties that the developer had changed.

To define a JavaBean, a class needs a default (no-argument) constructor, getters for its properties, and setters for its mutable properties. (We'll gloss over the `Serializable` requirement, because even Sun never really took this seriously.)
This makes sense for objects that have a lot of properties.
GUI components typically have foreground and background colors, font, label, borders, size, alignments, paddings, and so on.
Mostly the defaults for these properties are fine, so calling setters for just the special values minimizes the amount of code to be generated.
Even today, a mutable component model is a solid choice for GUI toolkits.

When JavaBeans were introduced, though, we thought of most objects as mutable, not just UI components.
I mean, why not? The point of objects was to encapsulate properties and manage the relationships between them.
They were _designed_ to solve problems like updating the width of a component when its bounds are changed, or the total of a shopping cart as items are added.
Objects were the solution to the problem of managing mutable state.
Java was quite radical at the time in having an immutable `String` class (although it couldn't help itself and still plumped for a mutable `Date`).

As a profession, we have a more sophisticated understanding these days.
We appreciate that we can use objects to represent different types of things—values, entities, services, actions, transactions, and so on.
And yet the default pattern for a Java object is still the JavaBean, a mutable object with getters and setters for its properties.
Although it may be appropriate for a UI toolkit, this is not a good default pattern.
For most things that we want to represent with objects, a value would be better.

## Values

_Value_ is a much overloaded term in English.
In computing, we say that variables, parameters, and fields have values: the primitive or reference that they are bound to.
When we refer to _a value_ in this book, we are referring to a specific type of primitive or reference: those with value semantics.
An object has value semantics if only its value is significant in its interactions, not its identity.
Java primitives all have value semantics: every `7` is equal to every other `7`.
Objects may or may not have value semantics though; in particular, mutable objects do not.
In later chapters we'll look at finer distinctions, but for now, let's just define a _value_ to be an immutable piece of data, and a _value type_ to be a type that defines the behavior of an immutable piece of data.

So `7` is a value, and the boxed `Integer` is a value type (because boxed types are immutable), `banana` is a value (because `String`s are immutable), a `URI` is a value (because `URI`s are immutable), but `java.util.Date` is not a value type (because we can call `setYear` and others on the date).

An instance of an immutable `DBConnectionInfo` is a value, but an instance of `Database` is not a value, even if all its properties are immutable. This is because it is not a piece of data; it is a means of accessing, and mutating, pieces of data.

Are JavaBeans values?
UI component JavaBeans are not values because UI components aren't just data—two otherwise identical buttons have different identities.
In the case of beans used to represent plain data, it will depend on whether they are immutable.
It is possible to create immutable beans, but most developers would think of these more as plain old java objects.

Are POJOs values?
The term was coined to refer to classes that don't have to extend from framework types to be useful.
They usually represent data and conform to the JavaBeans conventions for accessor methods.
Many POJOs will not have a default constructor, but instead define constructors to initialize properties that don't have sensible defaults.
Because of this, immutable POJOs are common and may have value semantics.
Mutable POJOs still seem to be the default though, so much so that many people consider that object-oriented programming in Java is synonymous with mutable objects.
Mutable POJOs are not values.

In summary, a bean could technically be a value but rarely is.
POJOs more often have value semantics, especially in the modern Java age.
So whereas _Beans to Values_ is snappy, in this chapter we're really looking at refactoring from mutable objects to immutable data, so maybe we should have called it __Mutable POJOs to Values__.
We hope you'll forgive the sloppy title.

## Why Should We Prefer Values?

A value is immutable data.
Why should we prefer immutable objects to mutable objects, and objects that represent data to other types of objects?
This is a theme that we will visit time and again in this book.
For now, let's just say that immutable objects are easier to reason about because they don't change, and so:

* We can put them into sets or use them as map keys.
* We never have to worry about an immutable collection changing as we iterate over its contents.
* We can explore different scenarios without having to deep-copy initial states (which also makes it easy to implement undo and redo).
* We can safely share immutable objects between different threads.
