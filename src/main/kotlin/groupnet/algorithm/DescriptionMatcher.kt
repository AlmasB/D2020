package groupnet.algorithm

import groupnet.euler.Description
import java.util.*
import java.util.stream.Stream
import kotlin.collections.ArrayList

/**
 *
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
object DescriptionMatcher {

    /**
     * Matches the actual string (description) with a given pattern.
     * Given a pattern "a b ab" and actual "xy x y", the result
     * is map containing a = x, b = y.
     *
     * @return empty map if no match, else mapping for how pattern terms needs to be replaced
     */
    fun match(pattern: String, actual: String): Map<String, String> {
        if (pattern.length != actual.length)
            return emptyMap()

        val actualTerms = actual.map { "$it" }.toSet().minus(" ")
        val patternTerms = pattern.map { "$it" }.toSet().minus(" ")

        if (actualTerms.size != patternTerms.size)
            return emptyMap()

        // do replacement pattern matching
        val termsMap = HashMap<String, String>()

        val result = permute(patternTerms)

        val actualTermsList = actualTerms.toList()

        return Stream.of(*result.toTypedArray())
                //.parallel()
                .map {
                    val tmpMap = hashMapOf<String, String>()

                    // "it" refers to a single permutation
                    for (i in 0 until it.length) {
                        tmpMap[actualTermsList[i]] = it[i].toString()
                        termsMap[it[i].toString()] = actualTermsList[i]
                    }

                    var replaced = ""
                    actual.forEach {
                        replaced += tmpMap[it.toString()] ?: " "
                    }

                    replaced = Description.from(replaced).getInformalDescription()

                    if (replaced == pattern) {
                        termsMap
                    } else {
                        emptyMap<String, String>()
                    }
                }
                .filter { it.isNotEmpty() }
                .findAny()
                .orElse(emptyMap())
    }
}

/**
 * Permute a set of 1-length strings without repetition.
 * Given, setOf("a", "b", "c"), the result is "abc", "acb", "bac", "bca", "cab", "cba".
 * The result size is given set size factorial.
 */
fun permute(set: Set<String>): Set<String> {
    val result = arrayListOf<String>()
    permute(0, ArrayList(set), result)
    return result.toSet()
}

private fun permute(n: Int, list: MutableList<String>, result: MutableList<String>) {
    if (n == list.size - 1) {

        val s = list.fold("", { acc, x -> acc + x })
        result.add(s)

    } else {
        for (i in n+1..list.size) {
            permute(n + 1, ArrayList<String>(list), result)

            if (i < list.size) {
                Collections.swap(list, n, i)
            }
        }
    }
}