package groupnet.diagram

import groupnet.algorithm.Polylabel
import groupnet.decomposition.dec
import groupnet.decomposition.decTree
import groupnet.euler.*
import groupnet.gn.GNDescription
import groupnet.gn.GNDiagram
import groupnet.gui.SettingsController
import groupnet.gui.SettingsController.*
import groupnet.network.NetworkEdge
import groupnet.network.NetworkGraph
import groupnet.network.NetworkNode
import groupnet.util.Log
import groupnet.util.combinations2
import groupnet.util.negate
import javafx.geometry.Point2D
import math.geom2d.polygon.Polygons2D

/**
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class GNDiagramCreator {

    fun drawGroupedNetworkDiagram(GND: GNDescription): GNDiagram {
        Log.i("Creating gnd from $GND")

        val tree = decTree(GND)
        val gnd: GNDiagram

        Log.i("DecTree: ", tree)

        if (tree.vertices().size == 1) {
            gnd = createAtomicDiagram(GND)
        } else {
            val drawings = mutableMapOf<GNDescription, GNDiagram>()

            tree.leaves().forEach {
                drawings[it] = createAtomicDiagram(it, single = false)
            }

            tree.siblings().sortedByDescending { it.first.depth }.forEach { (v1, v2) ->
                val GND1 = v1.value
                val GND2 = v2.value
                val gnd1 = drawings[GND1]!!
                val gnd2 = drawings[GND2]!!

                drawings[v1.parent.value] = gnd1.embedIntoZone(D(GND2).parent, gnd2)
            }

            // here we know we have more than 1 component, so reposition
            gnd = repositionDisjointComponents(drawings[GND]!!)
        }

        // reposition the curves of disjoint components here

        // draw the _entire_ graph
        val new_g = drawGraph(GND, gnd.d)

        // add edges between disjoint components
        val E = arrayListOf<NetworkEdge>()

        GND.edges.forEach { e ->
            val v1 = new_g.nodes.find { it.label == e.first }
            val v2 = new_g.nodes.find { it.label == e.second }

            if (v1 != null && v2 != null) {
                val e = NetworkEdge(v1, v2)

                if (e !in new_g.edges)
                    E += e
            }
        }

        val newGraph = NetworkGraph(V(new_g), E(new_g) + E)

        val result = GNDiagram(gnd.GND, gnd.d, newGraph)
        resolveNetworkDiagram(result)
        return result
    }

    private fun createAtomicDiagram(GND: GNDescription, single: Boolean = true): GNDiagram {
        val D = GND.description
        val steps = dec(GND)

        val d = EulerDiagramCreator().drawAtomicDiagram(D, steps)

        val graph = if (single) drawGraph(GND, d) else drawGraphForAtomicComponent(GND, d)

        return GNDiagram(GND, d, graph)
    }

    private fun repositionDisjointComponents(gnd: GNDiagram): GNDiagram {
        val connectionsBetweenComponents = gnd.GND.connectionsDisjointComponents()

        // if there are no connections between any of the components
        if (connectionsBetweenComponents.values.all { it == 0 })
            return gnd

        val nodes = arrayListOf<NetworkNode>()
        val edges = arrayListOf<NetworkEdge>()

        val mapCompsToNodes = hashMapOf<Description, NetworkNode>()

        connectionsBetweenComponents
                .forEach { (D1, D2), numConnections ->

                    val n1: NetworkNode
                    val n2: NetworkNode

                    if (!mapCompsToNodes.containsKey(D1)) {
                        n1 = NetworkNode(D1.getInformalDescription(), null)
                        mapCompsToNodes[D1] = n1

                        nodes += n1
                    } else {
                        n1 = mapCompsToNodes[D1]!!
                    }

                    if (!mapCompsToNodes.containsKey(D2)) {
                        n2 = NetworkNode(D2.getInformalDescription(), null)
                        mapCompsToNodes[D2] = n2

                        nodes += n2
                    } else {
                        n2 = mapCompsToNodes[D2]!!
                    }

                    if (numConnections > 0) {
                        edges += NetworkEdge(n1, n2)
                    }
                }

        val graph = NetworkGraph(nodes, edges)


        layoutGraph(graph)



        val newCurves = hashSetOf<Curve>()

        graph.nodes.forEach { n ->

            val D = mapCompsToNodes.getKeyFromValue(n)


            val newPoint = Point2D(n.x, n.y).multiply(4.5)

            newCurves += gnd.d.curves
                    .filter { it.label in L(D) }
                    .map { it.translate(newPoint.subtract(gnd.d.center())) }


            // based on x,y we need to place the disjoint components
        }


        val newED = EulerDiagram(gnd.d.originalDescription, gnd.d.actualDescription, newCurves)

        return GNDiagram(gnd.GND, newED, gnd.g)
    }

    /**
     * Attempt to resolve any node-node crossings and any edge-node crossings.
     */
    private fun resolveNetworkDiagram(gnd: GNDiagram) {
        V(gnd.g).forEach { v ->
            E(gnd.g).forEach { e ->
                if (!v.isIncidentWith(e)) {
                    val vector = v.vectorToEdge(e)

                    if (vector.magnitude() < NODE_SIZE + 10) {
                        val vectorToAdd = vector.multiply(-1.0).normalize().multiply(NODE_SIZE + 15)

                        v.applyVelocity(vectorToAdd)

                        if (!v.hasValidPlacement()) {
                            v.applyVelocity(vectorToAdd.negate())
                            v.applyVelocity(vectorToAdd.negate())

                            if (!v.hasValidPlacement()) {
                                v.applyVelocity(vectorToAdd)
                            }
                        }
                    }
                }
            }
        }

        combinations2(V(gnd.g)).forEach { (v1, v2) ->
            if (v1.crossesNode(v2)) {
                val nodesInZone = V(gnd.g).filter { it.z === v1.z } - v1

                var zonePolygon = v1.z!!.polygonShape

                nodesInZone.forEach {
                    zonePolygon = Polygons2D.difference(zonePolygon, Polygons2D.createRectangle(it.x - (NODE_SIZE + 10), it.y - (NODE_SIZE + 10), it.x + (NODE_SIZE + 10), it.y + (NODE_SIZE + 10)))
                }

                val safePoint = Polylabel.findCenter(zonePolygon)

                v1.pos = safePoint
            }
        }
    }
}

fun <K, V> Map<K, V>.getKeyFromValue(value: V): K {
    return this.entries.find { it.value == value }?.key ?: throw RuntimeException("value $value not found in $this")
}