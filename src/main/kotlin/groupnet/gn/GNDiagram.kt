package groupnet.gn

import groupnet.euler.*
import groupnet.gui.SettingsController
import groupnet.network.NetworkEdge
import groupnet.network.NetworkGraph
import groupnet.util.Log
import groupnet.util.combinations2
import groupnet.util.numIntersectionsLinePolygon
import groupnet.util.symmetricDifference

/**
 *
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class GNDiagram(val GND: GNDescription,
                val d: EulerDiagram,
                val g: NetworkGraph) {

    fun embedIntoZone(az: AbstractZone, gnd: GNDiagram): GNDiagram {
        Log.i("Embedding ${gnd.d.actualDescription} into zone $az")

        val new_GND = GND + gnd.GND
        val new_d = d.drawIntoZone(az, gnd.d)

        val V1 = V(g).toMutableList()
        val E1 = E(g).toMutableList()

        val V2 = V(gnd.g)
        val E2 = E(gnd.g)

        // replace edges (v1, v2) : v1 or v2 is in V2 with v1 / v2 from V2
        V2.filter { it in V1 }.forEach { v ->

            E(g).filter { v.isIncidentWith(it) }.forEach { e ->

                E1 -= e

                if (e.v1 == v) {
                    E1 += NetworkEdge(v, e.v2)
                } else {
                    E1 += NetworkEdge(e.v1, v)
                }
            }

            // find the zone in new_d where v lives and position in that zone randomly
            val zone = (Z(new_d) + new_d.outsideZone).find { it.az == new_GND.aloc(v.label) }!!

            v.pos = zone.visualCenter.add(Math.random(), Math.random())
        }

        // removed duplicated vertices from V1
        // Note: V2 does not have duplications since our method duplicates vertices and adds to V1 only
        V1 -= V2

        // we now have a situation where some nodes in V1 that lived in az have to move
        // since we just slotted gnd.d into az
        // so, reposition unique G1 nodes that are in zone of az

        val zone = (Z(new_d) + new_d.outsideZone ).find { it.az == az }!!

        V1.filter { new_GND.aloc(it.label) == az }.forEach { v ->
            v.pos = zone.visualCenter.add(Math.random(), Math.random())
        }

        // add unique G1 nodes, unique G2 nodes
        // add unique G1 edges, unique G2 edges
        val new_g = NetworkGraph(V1 + V2, E1 + E2)

        return GNDiagram(new_GND, new_d, new_g)
    }

    fun computeNodeNodeCrossings(): Int {
        return combinations2(V(g)).count { (v1, v2) -> v1.crossesNode(v2) }
    }

    fun computeEdgeNodeCrossings(): Int {
        return E(g).sumBy { computeEdgeNodeCrossing(it) }
    }

    fun computeEdgeCrossings(): Int {
        return combinations2(E(g)).filter { (e1, e2) -> !e1.isAdjacent(e2) && e1.crosses(e2) }.count()
    }

    /**
     * @return edge-curve crossing number = actual - abstract min
     */
    fun computeEdgeCurveCrossings(): Int {
        return E(g).sumBy { computeEdgeCurveCrossing(it) }
    }

    private fun computeEdgeCurveCrossing(e: NetworkEdge): Int {
        val az1 = GND.aloc(e.v1.label).labels
        val az2 = GND.aloc(e.v2.label).labels

        val abstractMin = az1.symmetricDifference(az2).size

        val actual = C(d).sumBy { c -> numCrosses(c, e) }

        return actual - abstractMin
    }

    private fun numCrosses(c: Curve, e: NetworkEdge): Int {
        return numIntersectionsLinePolygon(e.v1.pos, e.v2.pos, c.getPolygon())
    }

    private fun computeEdgeNodeCrossing(e: NetworkEdge): Int {
        return V(g).count { it.isNonIncidentAndOnEdge(e, (SettingsController.NODE_SIZE)) }
    }

    // in case description parent is not captured?
    //gnd.GND.description = gnd.d.originalDescription
}