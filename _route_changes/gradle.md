---
title: Gradle Build Rules
layout: default
published: true
---
# {{page.title}}

Adding Kotlin to a project is now even simpler than shown in the book because the Kotlin plugin
[adds the dependency on the Kotlin stdlib automatically](https://kotlinlang.org/docs/gradle.html#dependency-on-the-standard-library). You don't have to add it explicitly.

So the example on page 18 can be simplified to:

```groovy
plugins {
    id 'org.jetbrains.kotlin.jvm' version "1.5.31"
}

... no change to project settings or dependencies ...

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile) {
    kotlinOptions {
        jvmTarget = "11"
        javaParameters = true
        freeCompilerArgs = ["-Xjvm-default=all"]
    }
}
```

Thanks to Piotr Krzemiński for pointing this out.
