---
published: false
title: Reflection to Reification
layout: default
---
# {{ page.title }}

One thing that stands out about Java compared to most other languages is just how much libraries and frameworks do with reflection.
The reasons are largely historical -- the language has supported reflection since the very first Java release, because it was the basis for dynamically loading classes and invoking their methods.
Before generics were introduced in Java 1.5, reflection gave us a way to manipulate objects in a generic way.

Reflection was especially useful for connecting objects to callbacks.
Java had no syntax for lambdas or method references.
It didn't even have anonymous inner classes until Java 1.1.
So we'd use reflection to route a callback from one object to a handler method on another object.

To route notifications from an `Observable` object route to arbitrary methods of other objects, we either write many small classes that implemented `Observer` and called the method we wanted to handle te notification, or we could write a single reflective implementation:

[source,java,from=src/main/java/reflection/callbacks/ObserverMethod.java]
```
public class ObserverMethod implements Observer {
    private final Object target;
    private final Method method;

    public ObserverMethod(Object target, String methodName, Class argumentType) throws NoSuchMethodException {
        this(
            target,
            target.getClass().getMethod(
                methodName,
                new Class[]{argumentType}));
    }

    public ObserverMethod(Object target, Method method) {
        this.target = target;
        this.method = method;
    }

    @Override
    public void update(Observable o, Object arg) {
        try {
            method.invoke(target, new Object[]{arg});
        } catch (IllegalAccessException e) {
            throw new UnsupportedOperationException("cannot invoke observer method", e);
        } catch (InvocationTargetException e) {
            throw new UnsupportedOperationException("error thrown by observer method", e.getTargetException());
        }
    }
}
```

Reflection is fundamental to the JavaBeans API.
JavaBeans specifies a component model in which  "beans" have methods, properties, and can emit and subscribe to events.
Java objects only have fields, usually private, and methods, so the JavaBeans API implemented its component model with reflection.
A read/write property was implemented as two methods -- instances of `java.lang.reflect.Method` -- a getter and a setter.
Event subscription was also implemented as two methods, one to add and the other to remove a listener.
JavaBeans introduced Java programmers to two things: standard coding conventions that required boilerplate code, and how reflection could be used to eliminate that boilerplate.
Reflection took off like wildfire.

Using reflection was verbose and, we found out, considerably slower than making direct method calls.
Each reflective call involves a check by the JVM's SecurityManager.
But it let us write code once, instead of writing many copies of essentially the same class.
For many uses, this was a trade-off worth making.

When Java 1.5 came along, reflection became more tricky thanks to _type erasure_.
To retrofit generic types into Java without breaking any existing code, Java's designers decided that generics would only exist in the compiler.
Collections and other generic types are compiled to "raw" classes.
That is, the type `List<String>` and `List<Money>` are both represented by the same raw class, `List`.
Reflection sees no difference between them.

More recent language features -- lambdas, functional interfaces and method references -- let Java programmers plug objects together with much less boilerplate.  As a result, there has been a wave of new application frameworks that eschew reflection in favour of programmatic composition that makes better use of the static type checker and IDE tools.   However, the previous generation of frameworks are still in wide use, and so reflection and annotations are still a significant part of Java programming.

In contrast, Kotlin downplays reflection in favour of composition and static type checking.  Its type system is more rigorous and more ergonomic than Java's.  Its unobtrusive syntax for lambdas makes them a good alternative for the factory-factory-factory and reflective dependency injection frameworks that are common in Java.

Although Kotlin does have a reflection API, it is an optional standard library.  You have to explicitly add it as a dependency to introspect Kotlin's types dynamically.  What Kotlin provides instead is compile-time _reification_.

TBC...
