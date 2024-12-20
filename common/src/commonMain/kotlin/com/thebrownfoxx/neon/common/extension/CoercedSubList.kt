package com.thebrownfoxx.neon.common.extension

fun <T> List<T>.coercedSubList(range: IntRange): List<T> {
    val start = range.first.coerceAtLeast(0)
    val end = range.last.coerceAtMost(lastIndex)
    return subList(start, end + 1)
}