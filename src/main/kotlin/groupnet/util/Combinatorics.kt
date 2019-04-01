package groupnet.util

import org.paukov.combinatorics3.Generator

/**
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */

/**
 * There is a performance reason to represent a combination using a List,
 * rather than a Set.
 * However, all combinations are guranteed to be sets.
 */
typealias Combination<T> = List<T>

/**
 * A k-combination of a set T is a subset of k distinct elements of T.
 */
fun <T> combinationsOf(elements: Collection<T>, range: IntRange): List<Combination<T>> {
    val result = ArrayList<Combination<T>>()

    for (k in range) {
        result.addAll(Generator.combination(elements).simple(k).toList())
    }

    return result
}

fun <T> combinations2(elements: Collection<T>): List< Pair<T, T> > {
    return combinationsOf(elements, 2)
            .map { it.first() to it.last() }
}

fun <T> combinationsOf(elements: Collection<T>, combinationSize: Int): List<Combination<T>> {
    return combinationsOf(elements, combinationSize..combinationSize)
}

/**
 * A partition of a set T is a set of non-empty subsets of T
 * such that every element t in T is in exactly one of these subsets.
 */
fun <T> partition2(elements: Collection<T>): List< Pair<List<T>, List<T>> > {
    return combinationsOf(elements, 1..elements.size / 2)
            .map { it to (elements - it) }
}

fun <T> partition2Lazy(elements: Collection<T>): Iterable< Pair<List<T>, List<T>> > {

    return object : Iterable<Pair<List<T>, List<T>>> {
        override fun iterator(): Iterator<Pair<List<T>, List<T>>> {
            return object : Iterator<Pair<List<T>, List<T>>> {
                var index = 1
                var gen: Iterator<List<T>> = Generator.combination(elements).simple(index).iterator()

                override fun hasNext(): Boolean {
                    return index <= elements.size / 2
                }

                override fun next(): Pair<List<T>, List<T>> {
                    val leftSet = gen.next()

                    if (!gen.hasNext()) {
                        index++
                        gen = Generator.combination(elements).simple(index).iterator()
                    }

                    return leftSet to (elements - leftSet)
                }
            }
        }
    }
}