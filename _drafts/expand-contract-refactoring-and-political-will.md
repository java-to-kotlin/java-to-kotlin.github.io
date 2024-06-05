---
title: Expand/Contract Refactoring and Political Will
layout: post
---

In our "Mastering Kotlin Refactoring" workshop at KotlinConf 2024, we looked at how to apply refactorings that require more than just committing code changes to trunk.
A refactoring that takes a few seconds in IntelliJ can have a wide impact on the codebase, and disrupt work being done by other team members, or break running systems if not deployed carefully.

For example, consider a rename in IntelliJ. When you rename a local variable, the rename has no impact beyond the function you are working on and your change is unlikely to conflict with anybody else's. When you rename a package or widely used type, that change will affect many files in the codebase and very likely cause merge conflicts when you push the commit to origin, disrupting the work of other team members. But when you rename a database column, you cannot make that change atomically unless you are able to bring the system down during an upgrade. If you don't have that luxury, you have to plan a sequence of application deployments and database migrations to avoid the application and database schema becoming incompatible during the deployment.

To avoid these problems, we break refactorings into phases using an ["expand/contract" process](2024-06-01-refactoring-beyond-the-commit.md). 

When you use expand/contract to propagate a refactoring across a large codebase or system, you inevitably make the codebase worse before it gets better. 
During the process, the codebase has more than one way of doing the same thing. Some code still does things the old way and other code has been migrated onto the new way. Hopefully, new code is being written in the new way, but there is always the risk that not everyone has got the message.

The longer a refactoring takes, the greater the risk that it will be abandoned, leaving the codebase in worse shape than it was before. 
Therefore, the more its success depends on the _[political will](https://scholarworks.montana.edu/items/6371b0ba-ce5d-4d46-b393-ac4da8ac8d17)_ of the engineers to push the initiative through to completion in the face of changing priorities.

We can make this easier by making the entire process take less time.

For _multiple commit_ refactorings, the bottleneck is the time it takes to merge the change to trunk.
The most effective strategy I know is trunk-based development and continuous integration. 
If you are still using pull requests (PRs), then the team needs to ensure that reviews are done quickly, in minutes, not hours or days. 
I treat PRs as attestations of compliance – that the change does not break the system or break the law – and have another process of code review for design improvement that does not block delivery.

For _multiple deployment_ refactorings, the bottleneck is the release cadence.
If you have a relational database with atomic schema changes, triggers, and computed columns, you can rename that column with "only" four production deployments.
If your database does not have those features, and you have to implement backward compatibility at the application level, you might need seven production deployments.

If it takes you a few minutes to test and release a change to production, then you can complete the refactoring in a day, or maybe two. If you release once a day, it'll take you a week. If you release once a week, it'll take you a couple of months. If you release once a month, it'll take you the best part of a year. How likely is it that the refactoring will ever be finished if that is the case?
