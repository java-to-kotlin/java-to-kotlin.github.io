---
title: Replace Conditional With Calculation
published: false
layout: default
---
# {{ page.title }}

Among the many refactorings described in Martin Fowler’s Refactoring, "Replace Conditional with Polymorphism" eliminates branching control flow by replacing it with polymorphic calls. An alternative is to replace branching code with straight-line calculations. This is frequently used in graphics and games programming. Unsurprisingly, I call this refactoring "Replace Conditional with Calculation".

## Worked Example

For example, consider the Luhn checksum algorithm that detects mistyped credit card numbers:

```kotlin
fun String.isValidCardNumber(): Boolean =
    this.reversed()
        .map { ch -> ch.digitToInt() }
        .mapIndexed { index, digit ->
            when (index % 2) {
                0    -> digit
                else -> digit * 2
            }
        }
        .sumOf {
            when {
                it >= 10 -> it / 10 + it % 10
                else     -> it
            }
        }
        .let { checkSum -> checkSum % 10 == 0 }
```

This has two conditional statements, both of which can be replaced by straight-line integer calculations.

However, although IntelliJ encodes some mathematical reasoning into its refactoring tools, particularly [De Morgan’s laws of boolean algebra](), it does not encode enough arithmetical rules to automatically replace conditional statements with numeric calculations. We have to rely on our own knowledge of arithmetic and the behaviour of Kotlin's integer arithmetic operators to recognise where and how we can replace conditionals with calculations.

To see how, let's start with the first when expression in the function:

```kotlin
when (index % 2) {
    0 -> digit
    else -> digit * 2
}
```

We can duplicate the `else` branch for the case when `index % 2` is 1, without changing the meaning of the when expression:

```kotlin
when (index % 2) {
    0 -> digit
    1 -> digit * 2
    else -> digit * 2
}
```

Because `index % 2` is either zero or one, the else branch is unreachable code. We can prove this to ourselves by changing the else branch to throw an exception and seeing that our tests still pass: 

```kotlin
when (index % 2) {
    0 -> digit
    1 -> digit * 2
    else -> error("unreachable")
}
```

Let's make the two branches have the same "shape":

```kotlin
when (index % 2) {
    0 -> digit * 1
    1 -> digit * 2
    else -> error("unreachable")
}
```

We can now lift the multiplication out of the when expression. 


Note: At the time of writing, IntelliJ can lift _return_ statements out of a conditional expression, but unfortunately it cannot do the same for common subexpressions. We have to do it by hand.


```kotlin
digit * when (index % 2) {
    0 -> 1
    1 -> 2
    else -> error("unreachable")
}
```

Now it is obvious that the conditional is calculating `index % 2 + 1`, so we can replace the entire when expression with that calculation:

```kotlin
digit * (index % 2 + 1)
```

This leaves the function as:

```kotlin
fun String.isValidCardNumber(): Boolean =
    this.reversed()
        .map { ch -> ch.digitToInt() }
        .mapIndexed { index, digit -> digit * (index % 2 + 1) }
        .sumOf {
            when {
                it >= 10 -> it / 10 + it % 10
                else     -> it
            }
        }
        .let { checkSum -> checkSum % 10 == 0 }
```

Because we now have one reference to `digit` we can combine the first two map and mapIndexed calls into one:

```kotlin
fun String.isValidCardNumber(): Boolean =
    this.reversed()
        .mapIndexed { index, ch -> ch.digitToInt()  * (index % 2 + 1) }
        .sumOf {
            when {
                it >= 10 -> it / 10 + it % 10
                else     -> it
            }
        }
        .let { checkSum -> checkSum % 10 == 0 }
```

The second conditional is an almost direct translation from a description of the Luhn algorithm into Kotlin: "if an intermediate value calculated from a digit of the card number has two digits, add the two digits together, otherwise use the digit". This calculation makes sense if calculating the checksum by pen and paper. But given the behaviour of Kotlin's integer arithmetic operators, the branching is unnecessary in the Kotlin code. If the intermediate value, `it`, is less than ten, then `it/10` would be zero, and `it % 10` would be equal to `it`, meaning we can use the same expression for both branches:

```kotlin
when {
    it >= 10 -> it / 10 + it % 10
    else     -> it / 10 + it % 10
}
```

Therefore, we can replace the entire when expression with `it / 10 + it % 10`, leaving the function as: 

```kotlin
fun String.isValidCardNumber(): Boolean =
    this.reversed()
        .mapIndexed { index, ch -> ch.digitToInt() * (index % 2 + 1) }
        .sumOf { it / 10 + it % 10 }
        .let { checkSum -> checkSum % 10 == 0 }
```

## Replace Calculation with Conditional

The intent of a calculation can be harder to understand than of explicit conditional code. Just as you can refactor between conditionals and polymorphism in either direction, from conditionals to polymorphism or from polymorphism to conditionals, so you can refactor between conditionals and calculations.
