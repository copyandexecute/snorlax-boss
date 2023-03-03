package de.hglabor.snorlaxboss.utils

import java.util.*

class WeightedCollection<E> {
    private val random: Random = Random()
    private val map: NavigableMap<Double, E> = TreeMap()
    private var total = 0.0

    infix fun Double.to(result: E): Double {
        if (this <= 0) return this
        total += this
        map[total] = result
        return this
    }

    operator fun next(): E {
        val value = random.nextDouble() * total
        return map.higherEntry(value).value
    }
}

inline fun <E> weightedCollection(block: WeightedCollection<E>.() -> Unit): WeightedCollection<E> {
    val collection = WeightedCollection<E>()
    block(collection)
    return collection
}