---
title: Prefix to Infix Syntax
layout: default
published: false
---
# {{page.title}}

Java does not provide very good facilities for creating domain-specific embedded languages. However, Java programmers have found ways to squeeze some expressiveness from the language by using static imports, "fluent" APIs and builders to create little languages.  Does Kotlin have any features that make it easier? 


The _Travelator_ application contains a utility Java class that declares static methods for building examples of JSON data in tests.
This allows us to write self-contained tests that are not split between Java code and JSON resource files, and we don't have to build JSON examples in the tests by string concatenation.

To build a JSON tree, we statically import syntactic sugar methods from the `Json` class:

<!-- begin-insert: tags/jsondsl_mixed.0:src/test/java/colloquiumatic/rating/RateSessionCommandParserTest.java#json_dsl_import -->
```java
import static colloquiumatic.json.Json.obj;
import static colloquiumatic.json.Json.prop;
```
<!-- end-insert -->

Code that constructs JSON data then looks like this:

<!-- begin-insert: tags/jsondsl_mixed.0:src/test/java/colloquiumatic/rating/RateSessionCommandParserTest.java#json_dsl_usage -->
```java
JsonNode commandAsJson = obj(
    prop("userId", exampleUserId),
    prop("scheduledSessionId", exampleSessionId),
    prop("wouldRecommend", 1),
    prop("highlights", "things the user liked"),
    prop("suggestions", "suggestions for improvements")
);
```
<!-- end-insert -->

Our Json class acts as a _Domain-Specific Embedded Language_ (DSEL).
It wraps the open source [Jackson library](https://github.com/FasterXML/jackson), which does all the work of representing JSON data and serialising/deserialising JSON to and from strings and streams, and tries to make Java code that builds JSON data look as close to JSON syntax as possible.
That's not _very_ close, to be fair, but its closer than is possible by using the Jackson library directly.

Here is the implementation of the JSON DSEL:

<!-- begin-insert: tags/jsondsl_mixed.0:src/test/java/colloquiumatic/json/Json.java#json_dsl -->
```java
public class Json {
    private static final JsonNodeFactory nodes = JsonNodeFactory.instance;

    @SafeVarargs
    public static ObjectNode obj(Map.Entry<String, JsonNode>... props) {
        return obj(asList(props));
    }

    public static ObjectNode obj(Iterable<Map.Entry<String, JsonNode>> props) {
        ObjectNode node = nodes.objectNode();
        props.forEach(p -> node.set(p.getKey(), p.getValue()));
        return node;
    }

    public static Map.Entry<String, JsonNode> prop(String name, JsonNode value) {
        return new AbstractMap.SimpleImmutableEntry<>(name, value);
    }

    public static Map.Entry<String, JsonNode> prop(String name, String textValue) {
        return prop(name, nodes.textNode(textValue));
    }

    public static Map.Entry<String, JsonNode> prop(String name, Integer intValue) {
        return prop(name, nodes.numberNode(intValue));
    }

    // and similar syntactic sugar for other primitive JSON types, and JSON arrays
}
```
<!-- end-insert -->

There's only so much we can do to create a domain-specific language in Java.
They always end up looking a bit LISP-ish or involve a lot of boilerplate code to implement the _Builder_ pattern.
Kotlin offers more options for expressive syntax.
As we convert more and more of our codebase to Kotlin, we'll get more benefit from using Kotlin's features to make the Kotlin code using our domain-specific language more concise and easier to read.

We need to decide what we convert first: the `Json` class, or the code that depends on it.
Until now, we've preferred to start with classes that are leaves in the dependency tree, pushing the Kotlin boundary outwards to create an expanding region of code within which we take full advantage Kotlin's type safety and convenience features.

This case is different: the `Json` class provides syntactic sugar to our Java code and has almost no logic itself.
At best, converting it to Kotlin will leave the Java code unchanged, and so not be worth the effort.
The conversion could make it _worse_ as a convenient notation in Java, and we'll have to annotate the Kotlin API to get it back to where we started.
Even then we won't be able to apply Kotlin-specific features to improve the DSL because what works well as a notation in Kotlin will not carry over to the Java code.

If we start from the other direction, by converting the code that uses the `Json` class, we'll end up with a notation in the Kotlin code that is no worse than our existing Java, and that we can improve when we convert the `Json` class to Kotlin.

We can decide when to convert the `Json` class to Kotlin as we go.
At some point, we'll have enough Kotlin code using our Json notation that it's worth converting to Kotlin and annotating it to support Java code.
From there we can introduce features for the benefit of Kotlin code.
Eventually we'll convert the last bit of Java code, and we can delete anything that remains in the `Json` class solely for the benefit of Java.

## Start by converting uses of the DSL

Let's get started by converting the Java test we saw earlier that uses the `Json` class.
The converter leaves us with code like this:

<!-- begin-insert: tags/jsondsl_mixed.1:src/test/java/colloquiumatic/rating/RateSessionCommandParserTest.kt#json_dsl_usage -->
```kotlin
val commandAsJson: JsonNode = Json.obj(
    Json.prop("userId", exampleUserId),
    Json.prop("scheduledSessionId", exampleSessionId),
    Json.prop("wouldRecommend", 1),
    Json.prop("highlights", "things the user liked"),
    Json.prop("suggestions", "suggestions for improvements")
)
```
<!-- end-insert -->

Unfortunately, IntelliJ didn't carry the static imports across into Kotlin.
It imported the `Json` class and translated `obj` and `prop` to `Json.obj` and `Json.prop`, messing up our carefully crafted notation.
It didn't know that the `Json` class exists to define syntactic sugar, and that the way that the Java imported its functions was significant.

We can get IntelliJ to add these imports automatically by hitting _Alt-Enter_ on the first call to each method in the file we translated and choosing to import the method directly.

<!-- begin-insert: tags/jsondsl_mixed.2:src/test/java/colloquiumatic/rating/RateSessionCommandParserTest.kt#json_dsl_import -->
```kotlin
import colloquiumatic.json.Json.obj
import colloquiumatic.json.Json.prop
```
<!-- end-insert -->

leading us back to the notation we want:

<!-- begin-insert: tags/jsondsl_mixed.2:src/test/java/colloquiumatic/rating/RateSessionCommandParserTest.kt#json_dsl_usage -->
```kotlin
val commandAsJson: JsonNode = obj(
    prop("userId", exampleUserId),
    prop("scheduledSessionId", exampleSessionId),
    prop("wouldRecommend", 1),
    prop("highlights", "things the user liked"),
    prop("suggestions", "suggestions for improvements")
)
```
<!-- end-insert -->

**Note:** Unlike Java, Kotlin does not distinguish between imports and "static imports". The Kotlin import statement can import top-level declarations from other packages, static members of Java classes, and members from object declarations.

Eventually enough of the code that uses the `Json` class is Kotlin that it's worth converting the `Json` class itself.
The converter translates the Java class of static methods into a top-level Kotlin object.
Because we still have Java code calling those static methods, the converter annotates them with @JvmStatic.
The compiler will generate static methods in the Java bytecode, leaving the Java code unaffected.

<!-- begin-insert: tags/jsondsl_mixed.3:src/test/java/colloquiumatic/json/Json.kt#json_dsl -->
```kotlin
object Json {
    private val nodes = JsonNodeFactory.instance

    @SafeVarargs
    fun obj(vararg props: Map.Entry<String?, JsonNode?>?): ObjectNode {
        return obj(Arrays.asList(*props))
    }

    @JvmStatic
    fun obj(props: Iterable<Map.Entry<String?, JsonNode?>>): ObjectNode {
        val node = nodes.objectNode()
        props.forEach(Consumer { p: Map.Entry<String?, JsonNode?> ->
            node.set<JsonNode>(p.key, p.value)
        })
        return node
    }

    fun prop(name: String, value: JsonNode): Map.Entry<String, JsonNode> {
        return SimpleImmutableEntry(name, value)
    }

    fun prop(name: String, textValue: String?): Map.Entry<String, JsonNode> {
        return prop(name, nodes.textNode(textValue))
    }

    @JvmStatic
    fun prop(name: String, intValue: Int?): Map.Entry<String, JsonNode> {
        return prop(name, nodes.numberNode(intValue))
    }

    // and similar syntactic sugar for other primitive JSON types, and JSON arrays
}
```
<!-- end-insert -->

<!-- TODO: use callouts here instead of a paragraph of sentences -->
At the time of writing, IntelliJ doesn't get the conversion quite right.
It marks more types as nullable than we would want.
It  doesn't infer nullability correctly, so the converted code doesn't compile.
It doesn't annotate all method overloads with @JvmStatic, so Java code that depends on the Json object doesn't compile either.

Happily it's easy to fix: we can remove the spurious nullability modifiers and add the required @JvmStatic annotations:

<!-- begin-insert: tags/jsondsl_mixed.4:src/test/java/colloquiumatic/json/Json.kt#json_dsl -->
```kotlin
object Json {
    private val nodes = JsonNodeFactory.instance

    @JvmStatic
    @SafeVarargs
    fun obj(vararg props: Map.Entry<String, JsonNode>): ObjectNode {
        return obj(Arrays.asList(*props))
    }

    @JvmStatic
    fun obj(props: Iterable<Map.Entry<String, JsonNode>>): ObjectNode {
        val node = nodes.objectNode()
        props.forEach(Consumer { p: Map.Entry<String, JsonNode> ->
            node.set<JsonNode>(p.key, p.value)
        })
        return node
    }

    @JvmStatic
    fun prop(name: String, value: JsonNode): Map.Entry<String, JsonNode> {
        return SimpleImmutableEntry(name, value)
    }

    @JvmStatic
    fun prop(name: String, textValue: String?): Map.Entry<String, JsonNode> {
        return prop(name, nodes.textNode(textValue))
    }

    @JvmStatic
    fun prop(name: String, intValue: Int?): Map.Entry<String, JsonNode> {
        return prop(name, nodes.numberNode(intValue))
    }

    // and similar syntactic sugar for other primitive JSON types, and JSON arrays
}
```
<!-- end-insert -->

<!-- 
TODO: use callouts here instead of a paragraph of sentences
TODO: explain nullability & Jackson's API, the +node.set<JsonNode>+ weirdness, and keeping the @SafeVarargs for Java.
 -->

## Simplifying the Kotlin code

Now everything is working again, let's simplify the implementation of the `Json` class and improve the notation it provides to Kotlin code, while ensuring any remaining Java code that uses it is unaffected.

Let's pick off some quick, easy changes first.
We can remove the SafeVarargs annotation: it's not needed in Kotlin code.
We can remove the reference to Java's Consumer functional interface and use a plain lambda.
We can make functions that are a single-expression use single-expression function syntax.
We can use Kotlin's `apply` function to turn the `obj` function into a single-expression function.
Finally, we can replace the call to Java's Arrays.asList with Kotlin's `asList()` extension function.

This removes a lot of boilerplate from the class:

<!-- begin-insert: tags/jsondsl_mixed.5:src/test/java/colloquiumatic/json/Json.kt#tidy_implementation -->
```kotlin
object Json {
    private val nodes = JsonNodeFactory.instance

    @JvmStatic
    @SafeVarargs
    fun obj(vararg props: Map.Entry<String, JsonNode>) =
        obj(props.asList())

    @JvmStatic
    fun obj(props: Iterable<Map.Entry<String, JsonNode>>): ObjectNode =
        nodes.objectNode().apply {
            props.forEach { p -> set<ObjectNode>(p.key, p.value) }
        }

    @JvmStatic
    fun prop(name: String, value: JsonNode) =
        SimpleImmutableEntry(name, value)

    @JvmStatic
    fun prop(name: String, textValue: String?) =
        prop(name, nodes.textNode(textValue))

    @JvmStatic
    fun prop(name: String, intValue: Int?) =
        prop(name, nodes.numberNode(intValue))

    // and the rest of the syntactic sugar...
}
```
<!-- end-insert -->

The `nodes` val at the top of the object declaration is only there to give a concise name to Jackson's global JsonNodeFactory instance.
In Kotlin we can do that in import statement.
Recall that Kotlin can import static members from Java classes.
It can also import a feature under a different name within the importing file.

<!-- begin-insert: tags/jsondsl_mixed.6:src/test/java/colloquiumatic/json/Json.kt#import_nodes -->
```kotlin
... the rest of our imports
import com.fasterxml.jackson.databind.node.JsonNodeFactory.instance as nodes

object Json {
    @JvmStatic
    @SafeVarargs
    fun obj(vararg props: Map.Entry<String, JsonNode>) =
        obj(props.asList())

    ...
}
```
<!-- end-insert -->

What stands out now is the use of Map.Entry and AbstractMap.SimpleImmutableEntry.
In our Java code, we used them to hold key/value pairs, because Java doesn't have pair type.
Kotlin does -- let's use it.

## Using Kotlin's Pair type

There's no automatic refactoring that can replace Map.Entry and AbstractMap.SimpleImmutableEntry with Pair in one go.
However, thanks to Kotlin type inference and our use of single-expression functions, Map.Entry is only referred to in three places: as parameters of the `obj` functions and in the implementation of `prop(String,JsonNode)`.
The code that _uses_ our syntactic sugar does not refer to the Map.Entry type at all.
So, it's easy enough to change these by text editing.
However, with a bit of refactoring-fu we can perform the transformation as a series of safe additions of temporary code and automated transformation steps without ever introducing a compilation error.

First, we Alt-Enter on the "Entry" in `Map.Entry`, and select "add import for 'kotlin.collections.Map.Entry'".
Now all references in the file to the Map.Entry type are via the unqualified name "Entry".

Secondly, we add a type alias below the imports, aliasing Entry and SimpleImmutableEntry to Pair:

<!-- begin-insert: tags/jsondsl_mixed.7:src/test/java/colloquiumatic/json/Json.kt#type_aliases -->
```kotlin
private typealias Entry<K,V> = Pair<K,V>
private typealias SimpleImmutableEntry<K,V> = Pair<K,V>
```
<!-- end-insert -->

IntelliJ will highlight them as unused, because the imports of Entry and SimpleImmutableEntry take precedence over the type aliases in the file.
But hold on: those imports are going to disappear shortly.

Next we define two extension properties on Pair to match the properties of Map.Entry:

<!-- begin-insert: tags/jsondsl_mixed.7:src/test/java/colloquiumatic/json/Json.kt#temporary_extension_properties -->
```kotlin
private val <K,V> Pair<K,V>.key get() = first
private val <K,V> Pair<K,V>.value get() = second
```
<!-- end-insert -->

Again, IntelliJ will highlight them as unused, but they will become used as soon as the imports disappear.

Now we delete the imports of kotlin.collections.Map.Entry and java.util.AbstractMap.SimpleImmutableEntry.
All uses of Entry in the file are now referring to kotlin.Pair.
IntelliJ no longer highlights the type aliases and extension properties as unused.

Finally, we clean up by inlining the type aliases and the temporary extension properties, so that the code refers to the Pair class directly, and removing unnecessary type parameters that the inline refactoring left in call to the Pair constructor.

That leaves the code looking like:

<!-- TODO: fix the @JvmStatic annotation on the same line -->
<!-- begin-insert: tags/jsondsl_mixed.8:src/test/java/colloquiumatic/json/Json.kt#json_dsl -->
```kotlin
object Json {
    @JvmStatic
    @SafeVarargs
    fun obj(vararg props: Pair<String, JsonNode>) =
        obj(props.asList())

    @JvmStatic
    fun obj(props: Iterable<Pair<String, JsonNode>>): ObjectNode =
        nodes.objectNode().apply {
            props.forEach { p -> set<ObjectNode>(p.first, p.second) }
        }

    @JvmStatic
    fun prop(name: String, value: JsonNode) =
        Pair<String, JsonNode>(name, value)

    @JvmStatic
    fun prop(name: String, textValue: String?) =
        prop(name, nodes.textNode(textValue))

    @JvmStatic
    fun prop(name: String, intValue: Int?) =
        prop(name, nodes.numberNode(intValue))

    ... and the rest of the syntactic sugar
}
```
<!-- end-insert -->

****
Was that worth it for this small class?
Maybe not.
In practice, I'd have probably done that by text editing because the change only touched three lines in a single file and did not affect any other code.
When transformations affect lots of places across the code, it's easier and less risky to perform the refactoring as a sequence of small steps in which the IDE automatically applies changes to dependent code.
Think of this as a warm-up, using the technique in the small, so we can use it for larger changes later.
****

The definition of the first prop function now looks a bit iffy.
In Kotlin we don't usually create pairs by calling the Pair constructor.
Alt-Enter on the call to `Pair` in the prop function, and choose the option "convert to 'to'".
It now looks like:

<!-- begin-insert: tags/jsondsl_mixed.9:src/test/java/colloquiumatic/json/Json.kt#use_to -->
```kotlin
@JvmStatic
fun prop(name: String, value: JsonNode) =
    name to value
```
<!-- end-insert -->

The `to` operator for constructing Pairs is not a language feature.
It's an _infix_ function defined in the standard library.
An infix function is a method or extension function that takes one parameter and is marked with the `infix` modifier.
It can be called with operator-like syntax: you don't need to use a dot to reference the method or enclose the parameter in parentheses.

That gives us a clue as to how to improve the notation now we are working in Kotlin.
Wouldn't it be nice if object properties did not have to be wrapped in a call to `prop`, but used infix notation instead.
We can't call our JSON infix operator "to" -- that's been taken for Pairs -- but we can use a similarly short word.
How about "of"?
That would make Kotlin code that builds JSON look like:

<!-- begin-insert: tags/jsondsl_mixed.12:src/test/java/colloquiumatic/rating/RateSessionCommandParserTest.kt#json_dsl_usage -->
```kotlin
val commandAsJson: JsonNode = obj(
    "userId" of exampleUserId,
    "scheduledSessionId" of exampleSessionId,
    "wouldRecommend" of 1,
    "highlights" of "things the user liked",
    "suggestions" of "suggestions for improvements"
)
```
<!-- end-insert -->


How do we turn a function into an infix function?
What do we do about the code in Java, which doesn't have the concept of infix functions?

## Making a Function Infix 

To make a function infix, we must first make it an extension function, and then mark it as infix.
IntelliJ can automatically do the first step: _Alt-Enter_ on the first parameter of each `prop` function and select "Convert parameter to receiver".
Our JSON-building code then looks like this:

<!-- begin-insert: tags/jsondsl_mixed.10:src/test/java/colloquiumatic/rating/RateSessionCommandParserTest.kt#json_dsl_usage -->
```kotlin
val commandAsJson: JsonNode = obj(
    "userId".prop(exampleUserId),
    "scheduledSessionId".prop(exampleSessionId),
    "wouldRecommend".prop(1),
    "highlights".prop("things the user liked"),
    "suggestions".prop("suggestions for improvements")
)
```
<!-- end-insert -->

At the time of writing, IntelliJ cannot automatically refactor an extension function into an infix function.
We must add the infix modifier to the prop function by hand.
IntelliJ will then be able to refactor between infix and dotted method call syntax for a single call site, but doesn't automate the refactoring in bulk for _all_ uses of an infix function across the codebase.
Going through every call site and refactoring them one by one sounds terribly tedious!

## Second Go

Let's roll back and take another run at it: we'll try using a similar combo of adding code, delegating and inlining that we used to swap out the Map.Entry type for Pair.
We'll add new, infix functions called "of" and make the "prop" functions delegate to them.

While we're adding new functions, let's also take the opportunity to start moving the implementation of our DSL out of the Json object.
The Json object is not idiomatic Kotlin.
We're not using it as an object -- we never pass around a reference to it.
It's merely a namespace to hold the functions that define our little domain-specific language.
In Kotlin, we can declare those functions at the top level of the file.

So, we'll stop adding more stuff to the Json object and declare our infix "of" functions at the top level.
We'll delegate to them from the existing prop function in the Json object.
The "prop" functions will still be used by Java code, but the infix "of" functions will be for use by Kotlin code.

<!-- begin-insert: tags/jsondsl_mixed.11:src/test/java/colloquiumatic/json/Json.kt#prepare_for_inline -->
```kotlin
object Json {
    ...
    @JvmStatic
    fun prop(name: String, value: JsonNode) =
        name of value

    @JvmStatic
    fun prop(name: String, textValue: String?) =
        name of textValue

    @JvmStatic
    fun prop(name: String, intValue: Int?) =
        name of intValue
    ... and the rest of the syntactic sugar for Java
}

infix fun String.of(value: JsonNode) =
    this to value

infix fun String.of(textValue: String?) =
    this of nodes.textNode(textValue)

infix fun String.of(intValue: Int?) =
    this of nodes.numberNode(intValue)
... and the rest of the syntactic sugar for Kotlin
```
<!-- end-insert -->

Now we can inline the prop functions.
IntelliJ reports that it was unable to inline all the uses of prop.
That's just what we want!
We can't inline Kotlin code into Java, so IntelliJ has left the Json object unchanged.
All our Kotlin code is now using our infix notation:

<!-- begin-insert: tags/jsondsl_mixed.12:src/test/java/colloquiumatic/rating/RateSessionCommandParserTest.kt#json_dsl_usage -->
```kotlin
val commandAsJson: JsonNode = obj(
    "userId" of exampleUserId,
    "scheduledSessionId" of exampleSessionId,
    "wouldRecommend" of 1,
    "highlights" of "things the user liked",
    "suggestions" of "suggestions for improvements"
)
```
<!-- end-insert -->

But the Java code is still calling prop:

<!-- begin-insert: tags/jsondsl_mixed.12:src/test/java/colloquiumatic/signup/SessionSignUpCommandParserTest.java#json_dsl_usage -->
```java
JsonNode commandAsJson = obj(
    prop("userId", exampleUserId),
    prop("scheduledSessionId", exampleSessionId),
    prop("additionalRequirements", "example requirements")
);
```
<!-- end-insert -->

## Making idiomatic Kotlin usable from Java

Now all that remains to get to 100% conventional Kotlin is to move the declarations to the top-level and delete the Json object, all without disrupting the Java code that depends on it.

We can move the implementations of the `obj` functions to the top level in three steps.

First, we extract the bodies of `obj` as new functions.
We'll call our extracted functions `jsonObject` instead of `obj` to avoid name clash.
We can rename them back to our terse notation when we finish.

Then we move them to the top level of the file via the _Alt-Enter_ menu:

<!-- begin-insert: tags/jsondsl_mixed.13:src/test/java/colloquiumatic/json/Json.kt#json_dsl -->
```kotlin
object Json {
    @JvmStatic
    @SafeVarargs
    fun obj(vararg props: Pair<String, JsonNode>) =
        jsonObject(*props)

    @JvmStatic
    fun obj(props: Iterable<Pair<String, JsonNode>>) = jsonObject(props)

    @JvmStatic
    fun prop(name: String, value: JsonNode) =
        name of value

    @JvmStatic
    fun prop(name: String, textValue: String?) =
        name of textValue

    @JvmStatic
    fun prop(name: String, intValue: Int?) =
        name of intValue
    ... and the rest of the syntactic sugar for Java
}

fun jsonObject(vararg props: Pair<String, JsonNode>) =
    jsonObject(props.asList())

fun jsonObject(props: Iterable<Pair<String, JsonNode>>): ObjectNode =
    nodes.objectNode().apply {
        props.forEach { p -> set<ObjectNode>(p.first, p.second) }
    }

infix fun String.of(value: JsonNode) =
    this to value

infix fun String.of(textValue: String?) =
    this of nodes.textNode(textValue)

infix fun String.of(intValue: Int?) =
    this of nodes.numberNode(intValue)
... and the rest of the syntactic sugar for Kotlin
```
<!-- end-insert -->

At this point, our Kotlin code is still calling the obj methods of the Json object.
So we inline the obj methods, to make the Kotlin code call the top-level jsonObject functions.
(Recall that inlining a Kotlin function called from Java will leave the Java code unaffected.)

<!-- begin-insert: tags/jsondsl_mixed.14:src/test/java/colloquiumatic/rating/RateSessionCommandParserTest.kt#json_dsl_usage -->
```kotlin
val commandAsJson: JsonNode = jsonObject(
    "userId" of exampleUserId,
    "scheduledSessionId" of exampleSessionId,
    "wouldRecommend" of 1,
    "highlights" of "things the user liked",
    "suggestions" of "suggestions for improvements"
)
```
<!-- end-insert -->

Now to make the Java code call the top-level functions.
We'll annotate the DSL implementation to make the Kotlin compiler translate the idiomatic Kotlin code into bytecode compatible with the remaining Java code.
Then we'll switch out the Json object, leaving only the idiomatic Kotlin.

First, we annotate the top-level methods with @JvmName so that they are compiled to the same name as those that the Java code calls on the Json object:

<!-- begin-insert: tags/jsondsl_mixed.15:src/test/java/colloquiumatic/json/Json.kt#json_dsl -->
```kotlin
object Json {
    @JvmStatic
    @SafeVarargs
    fun obj(vararg props: Pair<String, JsonNode>) =
        jsonObject(*props)

    @JvmStatic
    fun obj(props: Iterable<Pair<String, JsonNode>>) = jsonObject(props)

    @JvmStatic
    fun prop(name: String, value: JsonNode) =
        name of value

    @JvmStatic
    fun prop(name: String, textValue: String?) =
        name of textValue

    @JvmStatic
    fun prop(name: String, intValue: Int?) =
        name of intValue
    ... and the rest of the syntactic sugar for Java
}

@SafeVarargs
@JvmName("obj")
fun jsonObject(vararg props: Pair<String, JsonNode>) =
    jsonObject(props.asList())

@JvmName("obj")
fun jsonObject(props: Iterable<Pair<String, JsonNode>>): ObjectNode =
    nodes.objectNode().apply {
        props.forEach { p -> set<ObjectNode>(p.first, p.second) }
    }

@JvmName("prop")
infix fun String.of(value: JsonNode) =
    this to value

@JvmName("prop")
infix fun String.of(textValue: String?) =
    this of nodes.textNode(textValue)

@JvmName("prop")
infix fun String.of(intValue: Int?) =
    this of nodes.numberNode(intValue)
... and the rest of the syntactic sugar for Kotlin
```
<!-- end-insert -->

Then we annotate the source file with @JvmName to compile it to a Java class file named "Json" and delete the entire Json object.

<!-- begin-insert: tags/jsondsl_mixed.16:src/test/java/colloquiumatic/json/Json.kt#json_dsl -->
```kotlin
@file:JvmName("Json")
... package declaration and imports ...

@SafeVarargs
@JvmName("obj")
fun jsonObject(vararg props: Pair<String, JsonNode>) =
    jsonObject(props.asList())

@JvmName("obj")
fun jsonObject(props: Iterable<Pair<String, JsonNode>>): ObjectNode =
    nodes.objectNode().apply {
        props.forEach { p -> set<ObjectNode>(p.first, p.second) }
    }

@JvmName("prop")
infix fun String.of(value: JsonNode) =
    this to value

@JvmName("prop")
infix fun String.of(textValue: String?) =
    this of nodes.textNode(textValue)

@JvmName("prop")
infix fun String.of(intValue: Int?) =
    this of nodes.numberNode(intValue)
... and the rest of the syntactic sugar
```
<!-- end-insert -->

// TODO: reword and/or move this above the preceding code example?
A Kotlin file called Json.kt that contains top-level declarations is compiled to a JVM class called JsonKt.
The top-level functions in the file are compiled to static methods of the JVM class.
An object named Json in that file is compiled to a JVM class called Json that implements the _Singleton_ pattern, and any methods annotated with `@JvmStatic` are compiled to static methods of that `Json` class.
The annotations we applied to the top-level declarations make the compiler generate the `JsonKt` and `Json` classes with static methods that had the same names and signatures, allowing us to replace one with the other without touching the Java code.

As a final clean-up, we can rename the `jsonObject` functions back to "obj" and delete their now unnecessary @JvmName annotations:

<!-- begin-insert: tags/jsondsl_mixed.17:src/test/java/colloquiumatic/json/Json.kt#json_dsl -->
```kotlin
@file:JvmName("Json")
... package declaration and imports ...

@SafeVarargs
fun obj(vararg props: Pair<String, JsonNode>) =
    obj(props.asList())

fun obj(props: Iterable<Pair<String, JsonNode>>): ObjectNode =
    nodes.objectNode().apply {
        props.forEach { p -> set<ObjectNode>(p.first, p.second) }
    }

@JvmName("prop")
infix fun String.of(value: JsonNode) =
    this to value

@JvmName("prop")
infix fun String.of(textValue: String?) =
    this of nodes.textNode(textValue)

@JvmName("prop")
infix fun String.of(intValue: Int?) =
    this of nodes.numberNode(intValue)
... and the rest of the syntactic sugar
```
<!-- end-insert -->

Eventually we will convert the last Java code using our JSON notation to Kotlin.
We can remove the annotations from our Kotlin code, leaving the implementation about as minimal as it can be:

<!-- begin-insert: tags/jsondsl_mixed.20:src/test/java/colloquiumatic/json/Json.kt#json_dsl -->
```kotlin
fun obj(vararg props: Pair<String, JsonNode>) =
    obj(props.asList())

fun obj(props: Iterable<Pair<String, JsonNode>>): ObjectNode =
    nodes.objectNode().apply {
        props.forEach { p -> set<ObjectNode>(p.first, p.second) }
    }

infix fun String.of(value: JsonNode) =
    this to value

infix fun String.of(textValue: String?) =
    this of nodes.textNode(textValue)

infix fun String.of(intValue: Int?) =
    this of nodes.numberNode(intValue)
... and the rest of the syntactic sugar
```
<!-- end-insert -->

## Discussion

In this chapter we explored a tiny _Domain-Specific Embedded Language_ that uses nested function calls to compose data structures.  This kind of notation is easy to implement but rather limited in what it can express and construct.

<!-- TODO more here... Something about builders. --> 