package groupnet.decomposition

import groupnet.euler.*
import groupnet.euler.dual.Edge
import groupnet.euler.dual.Vertex
import groupnet.gn.GNDescription
import groupnet.gn.minus
import groupnet.graph.cycles.CycleFinder
import groupnet.recomposition.RecompositionStep
import groupnet.util.Log
import groupnet.util.combinations2
import groupnet.util.combinationsOf
import org.jgrapht.alg.ConnectivityInspector
import org.jgrapht.graph.SimpleGraph
import kotlin.math.abs

/**
 *
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */

/**
 * Defines a strategy used to choose which label to remove in the next step
 * given description.
 */
interface DecompositionStrategy {

    /**
     * Returns which label to remove in the next step
     *
     * @param D the description
     * @return label to remove
     */
    fun labelToRemove(D: Description): Label
}

/**
 * Decomposes [D] into a list of recomposition steps.
 */
fun dec(D: Description) = decompose(D, ICURVES_DECOMPOSITION)

fun dec(GND: GNDescription): List<RecompositionStep> {
    return decompose(GND)
}

private fun decompose(D: Description, strategy: DecompositionStrategy): List<RecompositionStep> {
    val result = arrayListOf<RecompositionStep>()
    var Di = D

    while (Di != D0) {
        val removedLabel = strategy.labelToRemove(Di)

        val step = makeStep(Di, removedLabel)
        result.add(step)

        // use "from" because decomposition is backwards
        Di = step.from
    }

    Log.d("Decomposition begin")
    result.forEach { Log.d("$it") }
    Log.d("Decomposition end")

    return result
}

private fun makeStep(Dnext: Description, label: Label): RecompositionStep {
    var Dprev = Dnext - label

    val IN = Z(Dprev).filter { it + label in Z(Dnext) || it !in Z(Dnext) }
    val OUT = Z(Dprev)

    var splitZones = IN.intersect(OUT)

    if (splitZones.size == 1 && Dprev != D0) {
        (Z(Dprev) - splitZones).find { it.isNeighbour(splitZones.first()) }?.let {
            splitZones += it
        }

        if (splitZones.size == 1) {
            val az1 = splitZones.first()

            // construct az2 that does not exist in Dprev
            val az2 = (combinationsOf(az1.labels, az1.labels.size - 1)
                    .map { AbstractZone(it.toSet()) })
                    .first()

            Dprev = D(Z(Dprev) + az2)

            splitZones += az2
        }
    }

    return RecompositionStep(Dprev, Dnext, label, splitZones)
}

private val ICURVES_DECOMPOSITION = object : DecompositionStrategy {

    // 1. only remove non-disconnecting labels
    // 2. remove 2,3,4-zone labels that can make a potential piercing (single or double)
    // 3. remove based on lower bounds for extra zones, i.e. non-partners + count number of disconnected graphs
    override fun labelToRemove(D: Description): Label {
        L(D).sortedDescending().sortedBy { D.getNumZonesIn(it) }.forEach {
            if (isDrawableAsCircle(it, D) && isNonDisconnecting(it, D)) {
                return it
            }
        }

        L(D).sortedBy { lowerBoundExtraZones(it, D) }.forEach {
            if (isNonDisconnecting(it, D))
                return it
        }

        return L(D).first()
    }

    private fun isDrawableAsCircle(label: Label, D: Description) = when (D.getNumZonesIn(label)) {
        2 -> isSinglePiercing(label, D) || canBeDoublePiercingFrom2(label, D)
        3 -> canBeDoublePiercingFrom3(label, D)
        4 -> canBeDoublePiercingFrom4(label, D)
        else -> {
            Log.d("Label $label is not drawable as a circle")
            false
        }
    }

    private fun lowerBoundExtraZones(label: Label, D: Description): Int {
        val n = Z(D).filter { label in it }.count { it - label !in Z(D) }

        val IN = Z(D - label).filter { it + label in Z(D) }
        val m = smallestSetSizeToFormCycle(IN, label, D)

        return n + m
    }
}

private fun isNonDisconnecting(label: Label, D: Description) = isAtomic(D - label)

private fun isSinglePiercing(label: Label, D: Description): Boolean {
    val azones = Z(D).filter { label in it }

    if (azones[0].isNeighbour(azones[1])) {
        Log.d("Found $label to be a single piercing: ${azones[0]} - ${azones[1]}")
        return true
    } else {
        Log.d("Label $label is not a single piercing")
        return false
    }
}

private fun canBeDoublePiercingFrom2(l: Label, D: Description): Boolean {
    val azones = Z(D).filter { l in it }

    val diff = abs(azones[0].numLabels - azones[1].numLabels)

    if (diff == 0) {
        // can optimize?
        val otherLabels = L(D)

        for ((l1, l2) in combinations2(otherLabels)) {

            if (azones[0] + l1 !in Z(D)) {
                val az3 = azones[0] + l1
                if (az3 - l2 != azones[1])
                    continue

                val az4 = azones[1] - l1

                if (az4 !in Z(D) && az4 + l2 == azones[0]) {
                    Log.d("Found $l to be a double piercing (from 2): ${azones[0]} - $az3 - ${azones[1]} - $az4")
                    return true
                }

            } else if (azones[0] - l1 !in Z(D)) {
                val az3 = azones[0] - l1
                if (az3 + l2 != azones[1])
                    continue

                val az4 = azones[1] + l1

                if (az4 !in Z(D) && az4 - l2 == azones[0]) {
                    Log.d("Found $l to be a double piercing (from 2): ${azones[0]} - $az3 - ${azones[1]} - $az4")
                    return true
                }
            }
        }

    } else if (diff == 2) {

        val smallest = if (azones[0].numLabels < azones[1].numLabels) azones[0] else azones[1]
        val biggest = if (azones[0].numLabels > azones[1].numLabels) azones[0] else azones[1]

        val otherLabels = L(D)

        for ((l1, l2) in combinations2(otherLabels)) {

            if (smallest + l1 !in Z(D)) {
                val az3 = smallest + l1

                if (az3 + l2 != biggest)
                    continue

                val az4 = biggest - l1

                if (az4 !in Z(D) && az4 - l2 == smallest) {
                    Log.d("Found $l to be a double piercing (from 2): $smallest - $az3 - $biggest - $az4")
                    return true
                }

            } else if (smallest + l2 !in Z(D)) {
                val az3 = smallest + l2

                if (az3 + l1 != biggest)
                    continue

                val az4 = biggest - l2

                if (az4 !in Z(D) && az4 - l1 == smallest) {
                    Log.d("Found $l to be a double piercing (from 2): $smallest - $az3 - $biggest - $az4")
                    return true
                }
            }
        }
    }

    Log.d("Label $l is not a double piercing from 2")
    return false
}

private fun canBeDoublePiercingFrom3(l: Label, D: Description): Boolean {
    val zones = Z(D).filter { l in it }

    val az1 = zones[0]
    val az2 = zones[1]
    val az3 = zones[2]

    // 1 - 2 - 3

    var diff1 = az1.getStraddledLabel(az2)
    var diff2 = az2.getStraddledLabel(az3)

    if (diff1.isPresent && diff2.isPresent) {
        if (az1.isNeighbour(az3 + diff1.get()) || az1.isNeighbour(az3 - diff1.get())) {
            Log.d("Found $l to be a double piercing (from 3): $az1 - $az2 - $az3")
            return true
        }
    }

    // 1 - 3 - 2

    diff1 = az1.getStraddledLabel(az3)
    diff2 = az3.getStraddledLabel(az2)

    if (diff1.isPresent && diff2.isPresent) {
        if (az1.isNeighbour(az2 + diff1.get()) || az1.isNeighbour(az2 - diff1.get())) {
            Log.d("Found $l to be a double piercing (from 3): $az1 - $az3 - $az2")
            return true
        }
    }

    // 3 - 1 - 2

    diff1 = az3.getStraddledLabel(az1)
    diff2 = az1.getStraddledLabel(az2)

    if (diff1.isPresent && diff2.isPresent) {
        if (az3.isNeighbour(az2 + diff1.get()) || az3.isNeighbour(az2 - diff1.get())) {
            Log.d("Found $l to be a double piercing (from 3): $az3 - $az1 - $az2")
            return true
        }
    }

    return false
}

private fun canBeDoublePiercingFrom4(l: Label, D: Description): Boolean {

    val zones = Z(D).filter { l in it }.toMutableList()

    val az1 = zones[0]

    zones.remove(az1)

    zones.find { az1.isNeighbour(it) }?.let { az2 ->
        zones.remove(az2)

        zones.find { az2.isNeighbour(it) }?.let { az3 ->
            zones.remove(az3)

            if (az1.isNeighbour(zones[0])) {
                Log.d("Found $l to be a double piercing (from 4): $az1 - $az2 - $az3 - ${zones[0]}")
                return true
            }
        }
    }

    return false
}

private fun smallestSetSizeToFormCycle(zones: List<AbstractZone>, label: Label, D: Description): Int {
    val graph = CycleFinder<Vertex, Edge>(Edge::class.java)

    val Z = combinationsOf(L(D) - label, 1 until L(D).size)
            .map { az(it.toSet()) } + azEmpty

    val vertices = Z.map { Vertex(it) }
    vertices.forEach { graph.addVertex(it) }

    combinations2(Z)
            .filter { (az1, az2) -> az1.isNeighbour(az2) }
            .map { (az1, az2) -> Edge(vertices.find { it.az == az1 }!!, vertices.find { it.az == az2 }!!) }
            .forEach { graph.addEdge(it.v1, it.v2, it) }

    val cycles = graph.computeCyclesAbstract()

    return cycles
            .find { it.vertices.map { it.az }.containsAll(zones.map { it - label }.distinctBy { "$it" }) }
            ?.vertices?.size ?: 0 - zones.size
}

private fun disconnectedSubgraphsSize(zones: List<AbstractZone>): Int {
    val graph = SimpleGraph<AbstractZone, ZoneEdge>(ZoneEdge::class.java)

    zones.forEach { graph.addVertex(it) }

    // go through each pair of nodes
    val pairs = ArrayList< Pair<AbstractZone, AbstractZone> >()

    for (i in zones.indices) {
        var j = i + 1
        while (j < zones.size) {
            val node1 = zones[i]
            val node2 = zones[j]

            pairs.add(node1.to(node2))

            j++
        }
    }

    pairs.filter { it.first.isNeighbour(it.second) }
            .forEach { graph.addEdge(it.first, it.second, ZoneEdge(it.first, it.second)) }

    val inspector = ConnectivityInspector<AbstractZone, ZoneEdge>(graph)

    return inspector.connectedSets().size
}

private class ZoneEdge(val az1: AbstractZone, val az2: AbstractZone)

// GND informed decomposition

private fun decompose(GND: GNDescription): List<RecompositionStep> {
    var Di = GND.description
    var GNDi = GND

    val result = arrayListOf<RecompositionStep>()

    while (Di.labels.isNotEmpty()) {

        val connections = GNDi.connectionsBetweenZones()

        // best az pair to keep adjacency for this step
        val azPair = connections
                .filterKeys {
                    it.first.isNeighbour(it.second) && isNonDisconnecting(it.first.getStraddledLabel(it.second).get(), Di)
                }
                .filterValues { it > 0 }
                .maxBy { it.value }

        // use GND info, if not available, use ED info
        val removedLabel = if (azPair == null) ICURVES_DECOMPOSITION.labelToRemove(Di) else removeLabel(azPair.key)

        val step = makeStep(Di, removedLabel)
        step.connections = connections

        result.add(step)

        // use "from" because decomposition is backwards
        Di = step.from

        GNDi = GNDi - removedLabel
    }

    Log.d("Decomposition begin")
    result.forEach { Log.d("$it") }
    Log.d("Decomposition end")

    return result.reversed()
}

// this is "new" GND, as in after label removal
private fun removeLabel(azPair: Pair<AbstractZone, AbstractZone>): Label {

    // remove a label that doesnt break azPair adj
    val diff = azPair.first.getStraddledLabel(azPair.second)

    return diff.get()
}

class Decomposition(val steps: List<RecompositionStep>) {
    override fun toString(): String {
        return steps.joinToString(separator = "\n")
    }
}