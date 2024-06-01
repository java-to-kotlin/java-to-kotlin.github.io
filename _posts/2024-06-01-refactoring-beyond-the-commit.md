---
layout: post
title: Refactoring Beyond the Commit
tags:
---

In our "Mastering Kotlin Refactoring" workshop at KotlinConf 2024, we categorised refactorings by the effort involved to propagate the code improvements beyond your local workspace.  This resulted in three categories of refactoring, of increasing effort:

**Single Commit:** 

* Affects a single codebase and built artefact.
* Can be applied by one commit, without disrupting other team members.

For example, renaming a local variable.

**Multiple Commit:**

* Creates widespread change in a single codebase & built artefact.
* Applied by a sequence of commits with expand/contract to reduce conflicts with other team members' work in progress.

For example, renaming a widely used function, type or package. 

**Multiple Deployment:**

* Requires change in multiple codebases or deployed artefacts.
* Applied by multiple deployments with expand/contract to avoid downtime.

For example, changes to HTTP APIs or database schema used by systems that must be upgraded without downtime.


## Expand/Contract

_Multiple Commit_ and _Multiple Deployment_ refactorings require changes to be made by "expand/contract", so that the refactoring does not disrupt the work of others or breaks the running system.  

Expand/contract delivers a refactoring in phases.  First, we "expand" the system, adding features to the codebase that allow both old and new forms of the code to coexist.  Then we migrate code that uses the old form to use the new form.  Finally, we "contract" the system, removing the now unused code that supports the old form.

During an expand/contract refactoring, the code gets worse before it gets better. We introduce some duplication to allow the code to gradually move from one design to another.  During that period, it is important that everyone working on the code knows how the design is changing: what we are moving away from, and what is the new way of doing things. We don't want new code being written in the old way, creating even more refactoring work.

# IDE Support

IntelliJ has excellent support for Single Commit refactorings.  It has _some_ support for Multiple Commit refactorings.  Occasionally it will offer an option to expand the code, leaving old and new forms side by side -- although so occasionally that I could not find an example while writing this article! I think it's something that IntelliJ does better when refactoring Java than Kotlin.

Kotlin's deprecation mechanism helps with _Multiple Commit_ refactorings.  You can annotate old features in the codebase as `@Deprecated` and provide the new form of the code in the annotation. Other developers can then migrate code to the new form automatically in the IDE when it is convenient for them to do so.  The `@Deprecated` annotation can also be used hide old forms from IDE autocompletion, preventing people using the old forms accidentally.  We'll look at these features in more detail in a future post.

IntelliJ doesn't help at all with _Multiple Deployment_ refactorings.  But who knows, maybe in the future the IDE will integrate code editing with deployment orchestration tools, such as Flyway, Liquibase, Kubernetes, Helm, Monopolis, etc., and generate migrations and schedule deployments as you refactor.

