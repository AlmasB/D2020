package groupnet.euler.dual

import javafx.geometry.Point2D


/**
 *
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
data class MEDCycle(val nodes: List<MEDVertex>, val edges: List<MEDEdge>) {

    lateinit var polygon: MutableList<Point2D>

    fun length() = nodes.size

    /**
     * Computes length - nodes that lie in the same zone
     */
    fun lengthUnique() = nodesUnique().size

    fun nodesUnique() = nodes.distinctBy { it.zone.toString() }

    override fun toString(): String {
        return nodes.joinToString()
    }
}

fun distinctSize(vertices: List<MEDVertex>): Int {
    return vertices.distinctBy { it.zone.toString() }.size
}