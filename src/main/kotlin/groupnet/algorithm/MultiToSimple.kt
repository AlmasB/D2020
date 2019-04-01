package groupnet.algorithm

import java.util.*

/**
 *
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class MultiToSimple {

    companion object {

        @JvmStatic fun decompose(D0: String): List<String> {
            val tokens = D0.split(" +".toRegex()).toMutableList()

            val elements = ArrayDeque<Char>()

            val result = arrayListOf<String>()

            while (tokens.isNotEmpty()) {

                var D1 = ""

                elements.add(tokens[0][0])
                var usedElements = "" + elements.first

                while (elements.isNotEmpty()) {
                    val e = elements.pop()

                    val iter = tokens.iterator()
                    while (iter.hasNext()) {
                        val token = iter.next()

                        if (token.contains(e)) {
                            D1 += token + " "

                            token.filter { !usedElements.contains(it) }.forEach {
                                elements.add(it)
                                usedElements += it
                            }
                            iter.remove()
                        }
                    }
                }

                result.add(D1.trim())
            }

            return result
        }
    }
}