---
published: false
title: Single to Multiple Receivers
layout: default
---
# {{ page.title }}

When might you need an extension method of multiple receivers, and how can you implement it?

A future version of Kotlin will have support for extension methods with _multiple_ receivers.

Right now, you can write code that has multiple receivers in nested scopes:

````kotlin
val theList = mutableListOf<Int>()
theList.apply {
   "a string".apply {
       add(length) // appends the length of "a string" to theList
   }
} 
````

But what if you wanted to extract the code `add(length)` into an extension function defined in the outer scope? That function would need _two_ receivers, `MutableList<Int>` and `String`.  That's not something Kotlin supports right now.  So what can we do meanwhile?

One solution is define a class that combines the two receivers and uses interface delegation to delegate calls to them.

Writing a DSL in Kotlin’s DSL builder style.
The language statements are extension methods of the builder type, like:
fun <T> ThingyBuilder.foo() = this.add(Foo())
Some of the statements are loops, and the body of the loop may want to calculate something based on the iteration number (eg to do different things on alternate iterations). Like:

````
repeat(3) {
    block(bg=alternating(lightBlue, darkBlue)) { ... }
}
````

The bar function is an extension of ThingyBuilder, the alternating function is an extension of Int (the iteration index).
The block that calls builder methods and the alternating function therefore has to be an extension of both the ThingyBuilder and Int (the iteration index).
My solution was to bundle the builder and iteration index in a helper class that delegated the builder methods.
data class ThingyBuilderIteration(
builder : ThingyBuilder, val iteration: Int) :
ThingyBuilder by builder
The repeat method creates ThingyBuilderIterations and the body is an extension lambda of ThingyBuilderIteration.
Because of the delegation, I can use all DSL syntax inside the loop body.
The  ‘alternating’ function is an extension of ThingyBuilderIteration so that it can use the iteration index. Which means it can only be used inside the loop body. (edited) 
