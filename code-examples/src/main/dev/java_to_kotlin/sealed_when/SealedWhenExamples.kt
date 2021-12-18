package dev.java_to_kotlin.sealed_when

import dev.java_to_kotlin.sealed_when.Direction.SOUTH


/// begin: enum
enum class Direction {
    NORTH, EAST, SOUTH, WEST
}
/// end: enum


/// begin: nonexhaustive
fun runInDirection(direction: Direction) {
    when (direction) {
        Direction.NORTH -> move(0, -1)
        Direction.EAST -> move(1, 0)
        Direction.WEST -> move(-1, 0)
        // no branch for Direction.SOUTH
    }
}
/// end: nonexhaustive

/// begin: exhaustive_extension
val <T> T.exhaustive get() = this
/// end: exhaustive_extension

/// begin: force_exhaustive
fun walkInDirection(direction: Direction) {
    when (direction) {
        Direction.NORTH -> move(0, -1)
        Direction.EAST -> move(1, 0)
        Direction.WEST -> move(-1, 0)
        /// mute: force_exhaustive [// no branch for Direction.SOUTH]
        Direction.SOUTH -> move(1, 0)
        /// resume: force_exhaustive
    }.exhaustive
}
/// end: force_exhaustive

fun move(dx: Int, dy: Int) {
    // ...
}
