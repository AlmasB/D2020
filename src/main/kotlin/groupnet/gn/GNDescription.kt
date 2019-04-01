package groupnet.gn

import groupnet.algorithm.MultiToSimple
import groupnet.euler.*
import groupnet.util.Log
import groupnet.util.combinations2
import org.jgrapht.EdgeFactory
import org.jgrapht.graph.SimpleGraph
import org.jgrapht.graph.UnmodifiableGraph
import java.util.*

/**
 * A grouped network description is a tuple, GND = (D, G, aloc),
 * where D is an Euler diagram description, G is a graph and aloc (abstract location)
 * is a function, aloc : V(G) -> Z(D), that identifies the abstract zones in
 * which the vertices are located.
 * Immutable.
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class GNDescription(val description: Description,
                    nodes: Collection<String>,
                    edges: Collection<Pair<String, String>>,
                    val aloc: Map<String, AbstractZone>
                    ) {

    val graph: UnmodifiableGraph<String, Pair<String, String>>

    val nodes: Set<String>
        get() = graph.vertexSet()

    val edges: Set<Pair<String, String>>
        get() = graph.edgeSet()

    // Z(D) -> P V(G)
    val mapping: Map<AbstractZone, Set<String>>

    init {
        val G = SimpleGraph(EdgeFactory<String, Pair<String, String>> { v1, v2 -> v1 to v2 })

        nodes.forEach { G.addVertex(it) }
        edges.forEach { (v1, v2) -> G.addEdge(v1, v2) }

        graph = UnmodifiableGraph(G)

        val map = hashMapOf<AbstractZone, Set<String>>()

        aloc.entries.groupBy { it.value }.forEach { az, entries ->
            map[az] = entries.map { it.key }.toSet()
        }

        Z(description).forEach {
            if (!map.containsKey(it)) {
                map[it] = emptySet()
            }
        }

        mapping = Collections.unmodifiableMap(map)
    }

    fun aloc(v: String) = aloc[v]!!

    companion object {

        /**
         * (e) A(a,b,f) B(k,d) AB(g,o) AC()
         * a-b, b-f, b-d, k-e
         */
        @JvmStatic fun from(informalDescription: String, edgeDescription: String): GNDescription {

            val nodes = arrayListOf<String>()
            val aloc = hashMapOf<String, AbstractZone>()

            // build ED description, nodes and map az to nodes
            val zones = informalDescription.split(" +".toRegex())
                    .map {
                        val azString = it.substringBefore("(")
                        val az = az(azString)

                        val nodesRaw = it.substringAfter("(").substringBefore(")").split(",".toRegex()).map { it.trim() }.toMutableList()

                        // just in case
                        nodesRaw.removeIf { it.isEmpty() }

                        nodesRaw.forEach { aloc[it] = az }

                        nodes += nodesRaw

                        az
                    }
                    .toSet()

            val description = Description(zones)

            val edges = arrayListOf<Pair<String, String>>()


            if (edgeDescription.isNotEmpty()) {
                edgeDescription.split(",".toRegex())
                        .map { it.trim() }
                        .forEach {
                            val edgePoints = it.split("-".toRegex())

                            edges.add(edgePoints[0] to edgePoints[1])
                        }
            }

            return GNDescription(description, nodes, edges, aloc)
        }

        @JvmStatic fun from(description: Description,
                            mapping: Map<AbstractZone, Set<String>>,
                            edges: Collection<Pair<String, String>>): GNDescription {

            val nodes = mapping.values.flatten().toSet()

            val aloc = hashMapOf<String, AbstractZone>()

            mapping.forEach { az, V ->
                V.forEach {
                    aloc[it] = az
                }
            }

            return GNDescription(description, nodes, edges, aloc)
        }

        /**
         * Returns true if given node is part of some zone.
         * We use this to check if node is in the outside zone
         */
        private fun isNodeInZone(node: String, GND: GNDescription): Boolean {
            return GND.mapping.values.flatMap { it }.contains(node)
        }
    }

    /**
     * Number of connections between sets, where String is set label.
     */
    fun connectionsBetweenSets(): Map<Pair<String, String>, Int> {
        val sets = description.labels

        val map = hashMapOf<Pair<String, String>, Int>()

        combinations2(sets).forEach { (set1, set2) ->
            map[makeOrderedPair(set1, set2)] = 0
        }

        edges.forEach {
            val az1 = findAZ(it.first)
            val az2 = findAZ(it.second)

            if (az1 != azEmpty && az2 != azEmpty) {
                az1.labels.forEach { l1 ->
                    az2.labels.forEach { l2 ->

                        if (l1 != l2) {

                            val pair = makeOrderedPair(l1, l2)

                            val num = map[pair]!!
                            map[pair] = num + 1
                        }
                    }
                }
            }
        }

        val valueMap = hashMapOf<String, Int>()

        sets.forEach {
            valueMap[it] = 0
        }

        map.forEach { pair, value ->
            val firstValue = valueMap[pair.first]!! + value
            val secondValue = valueMap[pair.second]!! + value

            valueMap[pair.first] = firstValue
            valueMap[pair.second] = secondValue
        }

        return map
    }

    fun connectionsBetweenZones(): Map<Pair<AbstractZone, AbstractZone>, Int> {
        val zones = this.mapping.keys

        val map = hashMapOf<Pair<AbstractZone, AbstractZone>, Int>()

        combinations2(zones).forEach { (z1, z2) ->
            map[makeOrderedPair(z1, z2)] = 0
        }

        edges.forEach {
            val az1 = findAZ(it.first)
            val az2 = findAZ(it.second)

            if (az1 != az2) {

                val pair = makeOrderedPair(az1, az2)

                val num = map[pair]!!
                map[pair] = num + 1
            }
        }

        return map
    }

    fun toInformal(): Pair<String, String> {
        var result = ""

        mapping.toSortedMap().forEach { az, nodes ->
            result += ("${az.labels.joinToString("")}(${nodes.joinToString(",")}) ")
        }

        return result.trim() to edges.joinToString(",") { "${it.first}-${it.second}" }
    }

    /**
     * Number of connections between disjoint components.
     */
    fun connectionsDisjointComponents(): Map<Pair<Description, Description>, Int> {
        val disjointComponents = MultiToSimple.decompose(description.getInformalDescription()) + " "

        val map = hashMapOf<Pair<Description, Description>, Int>()
        val descriptions = hashSetOf<Description>()

        combinations2(disjointComponents).forEach { (c1, c2) ->
            val D1 = D(c1)
            val D2 = D(c2)

            descriptions += D1
            descriptions += D2

            map[makePair(D1, D2)] = 0
        }

        // edges
        edges.forEach {
            val D1 = findDescription(it.first, descriptions)
            val D2 = findDescription(it.second, descriptions)

            if (D1 != D2) {
                val pair = makePair(D1, D2)

                val numConn = map[pair]!!
                map[pair] = numConn + 1
            }
        }

        return map
    }

    private fun findDescription(nodeID: String, descriptions: Set<Description>): Description {
        val az = findAZ(nodeID)

        if (az != azEmpty) {
            return descriptions.find { az in Z(it) }!!
        } else {
            return descriptions.find { it.abstractZones.size == 1 }!!
        }
    }

    private fun findAZ(nodeID: String): AbstractZone {
        return mapping.entries.find { it.value.contains(nodeID) }!!.key
    }

    fun split(D1: Description, az1: AbstractZone, D2: Description): Pair<GNDescription, GNDescription> {
        Log.d("Splitting $this into ($D1, $az1, $D2)")

        D1.parent = description.parent
        D2.parent = az1

        val newMapping1 = hashMapOf<AbstractZone, MutableSet<String>>()
        val newMapping2 = hashMapOf<AbstractZone, MutableSet<String>>()

        // for each vertex
        mapping.forEach { (az, vertices) ->

            // aloc(v) \cap L(D_2) = \emptyset
            if (L(D2).intersect(az.labels).isEmpty()) {
                newMapping1[az] = vertices.toMutableSet()
            } else {
                newMapping2[az - az1] = vertices.toMutableSet()
            }
        }

        val V1 = newMapping1.values.flatten()
        val V2 = newMapping2.values.flatten()

        val E1 = arrayListOf<Pair<String, String>>()
        val E2 = arrayListOf<Pair<String, String>>()

        edges.forEach { e ->
            val (v1, v2) = e

            if (v1 in V1 && v2 in V1) {
                E1 += e

            } else if (v1 in V2 && v2 in V2) {

                E2 += e
            } else {

                if (newMapping1[az1] == null) {
                    newMapping1[az1] = hashSetOf()
                }

                // duplicate vertex in GND1 and edge there
                newMapping1[az1]!!.add( if (v1 in V1) v2 else v1 )

                E1 += e
            }
        }

        val GND1 = GNDescription.from(D1, newMapping1, E1)
        val GND2 = GNDescription.from(D2, newMapping2, E2)

        return GND1 to GND2
    }

    override fun equals(other: Any?): Boolean {
        if (other !is GNDescription)
            return false

        return description == other.description
                && graph == other.graph
                && aloc == other.aloc
    }

    override fun toString(): String {
        return "GND(D=$description, edges=$edges, mapping=$mapping"
    }
}

fun <T : Comparable<T>> makeOrderedPair(obj1: T, obj2: T): Pair<T, T> {
    return if (obj1 <= obj2) obj1 to obj2 else obj2 to obj1
}

fun makePair(D1: Description, D2: Description): Pair<Description, Description> {
    if (D1.getInformalDescription() <= D2.getInformalDescription()) {
        return D1 to D2
    } else {
        return D2 to D1
    }
}

operator fun GNDescription.minus(label: Label): GNDescription {
    val description = this.description - label

    val newMapping = this.mapping.filterKeys { !it.contains(label) }
    val edges = this.edges.filter {
        val nodes = newMapping.values.flatten()

        nodes.contains(it.first) && nodes.contains(it.second)
    }

    return GNDescription.from(description, newMapping, edges)
}

operator fun GNDescription.plus(GND: GNDescription): GNDescription {
    val newD = this.description + GND.description
    val newMapping = hashMapOf<AbstractZone, MutableSet<String>>()

    val az1 = GND.description.parent

    val V1 = this.nodes
    val V2 = GND.nodes

    val V = V1 + V2

    V.forEach { v ->
        if (v in V1 && v !in V2) {
            val aloc1 = this.aloc(v)

            val vertices = newMapping[aloc1] ?: hashSetOf()

            vertices += v

            newMapping[aloc1] = vertices
        }

        if (v in V2 && v !in V1) {
            val aloc = GND.aloc(v) + az1

            val vertices = newMapping[aloc] ?: hashSetOf()

            vertices += v

            newMapping[aloc] = vertices
        }

        if (v in V1 && v in V2) {
            val aloc2 = GND.aloc(v)

            if (aloc2 == azEmpty) {
                val aloc1 = this.aloc(v)

                val vertices = newMapping[aloc1] ?: hashSetOf()

                vertices += v

                newMapping[aloc1] = vertices
            } else {
                val aloc = aloc2 + az1

                val vertices = newMapping[aloc] ?: hashSetOf()

                vertices += v

                newMapping[aloc] = vertices
            }
        }
    }

    val newEdges = this.edges + GND.edges

    return GNDescription.from(newD, newMapping, newEdges)
}