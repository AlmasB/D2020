package groupnet.decomposition

import groupnet.euler.*
import groupnet.gn.GNDescription
import groupnet.util.Tuple3
import groupnet.util.partition2Lazy

/**
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */

fun isAtomic(D: Description): Boolean {
    val Z = Z(D) - azEmpty
    if (Z.size == 1 && L(D).size > 1)
        return false

    return canSplit(D) == null
}

/**
 * Decomposes [D] into its atomic components.
 */
fun decA(D: Description): List<Description> {
    if (L(D).size <= 1) {
        return listOf(D)
    }

    canSplit(D)?.let { (D1, az1, D2) ->

        D1.parent = D.parent
        D2.parent = D.parent + az1

        return decA(D1) + decA(D2)
    }

    return listOf(D)
}

fun decTree(GND: GNDescription, treeArg: DecompositionTree? = null): DecompositionTree {
    val tree = if (treeArg != null) treeArg else DecompositionTree(GND)

    canSplit(GND.description)?.let { (D1, az1, D2) ->
        val (GND1, GND2) = GND.split(D1, az1, D2)

        tree.addChildren(GND1, GND2, GND)

        decTree(GND1, tree)
        decTree(GND2, tree)
    }

    return tree
}

private fun canSplit(D: Description): Tuple3<Description, AbstractZone, Description>? {
    for ((L1, L2) in partition2Lazy(L(D))) {

        val Z1 = Z(D).map { it - L2 }.toSet()
        val Z2 = Z(D).map { it - L1 }.toSet()

        // generate D1 and D2
        // for each az in D1 check if + D2 gives D
        // for each az in D2 check if + D1 gives D

        val D1 = D(Z1)
        val D2 = D(Z2)

        Z(D1).forEach { az1 ->

            // D2 'slots' into az1 of D1
            if (sum(D1, az1, D2) == D) {
                return Tuple3(D1, az1, D2)
            }
        }

        Z(D2).forEach { az1 ->

            // D1 'slots' into az1 of D2
            if (sum(D2, az1, D1) == D) {
                return Tuple3(D2, az1, D1)
            }
        }
    }

    return null
}

// az1 in D1
private fun sum(D1: Description,
                az1: AbstractZone,
                D2: Description): Description = D1 + (az1 to D2)