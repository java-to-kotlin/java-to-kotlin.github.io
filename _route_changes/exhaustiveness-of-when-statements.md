---
title: Exhaustiveness of When Statements 
layout: default 
published: false
---
# {{page.title}}

In Kotlin
1.7, [Kotlin will check both `when` expressions and `when` statements for exhaustiveness in the same way](https://kotlinlang.org/docs/whatsnew1530.html#exhaustive-when-statements-for-sealed-and-boolean-subjects)
. This removes the need for the trick with the no-op `exhaustive` extension method we described in Chapter 18 of the
book.

In Kotlin versions up to and including 1.5, The compiler checks `when` expressions for exhaustiveness but does not
check `when` _statements_. The compiler considers `when` to be a statement if the value of the entire `when` expression
is unused. You can make the compiler to check for exhaustiveness by using the result of the `when`. In the book, we
showed how this can be done with a no-op extension method:

```kotlin
val <T> T.exhaustive get() = this
```

If you call `exhaustive` on the result of a `when` statement, the compiler considers the `when` to be an expression of
type `Unit` and checks it for exhaustiveness.

```kotlin
when (anInstanceOfSealedClass) {
    is SubclassA -> println("A")
    is SubclassB -> println("B")
}.exhaustive
```

In Kotlin 1.7 onwards we can do away with the call to `.exhaustive` – the compiler will check the `when` statement for
exhaustiveness anyway. In 1.6, a non-exhaustive `when` statements will result in a compile-time warning. In 1.7, it will
cause a compile error.
