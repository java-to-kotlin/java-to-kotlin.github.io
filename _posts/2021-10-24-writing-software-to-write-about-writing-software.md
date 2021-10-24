---
layout: post
title: Writing Software to Write About Writing Software
tags:
---
Duncan has written about the tools we wrote to help us write the book on [his blog](http://oneeyedmen.com/book-software-part-1.html).

Writing about the evolution of a codebase was a challenge. The book shows how the example code evolves through different versions as we refactor it.  We ended up with the example code in a separate Git repository to the book text and wrote tools that extract code examples from named versions in the code repository and insert them into the text.  The tools allowed us to keep the book text in version control and version the _version history_ of the example code.  Duncan's blog post goes into much more detail about the challenge and our solutions.
