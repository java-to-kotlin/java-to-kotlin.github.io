---
title: Exhaustive Extension to Sealed When
layout: default
published: false
---
# {{page.title}}

Kotlin 1.6 adds "sealed whens" to the language.  The compiler will check a sealed when for exhaustiveness, even when it is used as a statement.  Furthermore, all when statements will be checked for exhaustiveness, whether marked sealed or not, if they branch on elements of an enum type or Boolean. This removes the need for the trick with the no-op `exhaustive` extension method we described in Chapter 18 of the book.

In Kotlin versions up to and including 1.5, The compiler checks `when` expressions for exhaustiveness but does not check `when` _statements_.
The compiler considers `when` to be a statement if the value of the entire `when` expression is unused.
You can make the compiler to check for exhaustiveness by using the result of the `when`, even when it is of type `Unit`.

In the book, we showed how this can be done with a no-op extension method:

[source,kotlin]
```
val <T> T.exhaustive get() = this
```

You can call `exhaustive` on the result of a `when` if you want it to be checked for exhaustiveness but the compiler considers it to be a statement:

[source,kotlin]
```
when (anInstanceOfSealedClass) {
is SubclassA -> println("A")
is SubclassB -> println("B")
}.exhaustive
```


In Kotlin 1.6 onwards we can replace the calls to `exhaustive` with sealed when statements.

[source,kotlin]
```
sealed when (anInstanceOfSealedClass) {
    is SubclassA -> println("A")
    is SubclassB -> println("B")
}
```

If switching on values of an enum class or Boolean, the compiler will treat when statements as sealed without an explicit modifier:

[source,kotlin]
```
enum class E {
    ELEMENT_A, ELEMENT_B, ELEMENT_C
}

sealed when (anInstanceOfE) {
    E.ELEMENT_A -> println("A")
    E.ELEMENT_B -> println("B")
    // compile error: no branch for ELEMENT_C 
}
```

A sealed when is not permitted to have an `else` clause.
