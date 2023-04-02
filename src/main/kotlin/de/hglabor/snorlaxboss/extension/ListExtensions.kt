package de.hglabor.snorlaxboss.extension

import kotlin.random.Random

fun <T, U> Map<T, U>.random(): Map.Entry<T, U>? =
    if (isNullOrEmpty()) null else entries.elementAtOrNull(Random.nextInt(size))
