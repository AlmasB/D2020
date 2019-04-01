package groupnet.network

import javafx.geometry.Rectangle2D
import org.jgrapht.graph.SimpleGraph

/**
 *
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class NetworkGraph(nodes: Collection<NetworkNode>, edges: Collection<NetworkEdge>) {

    private val graph = SimpleGraph<NetworkNode, NetworkEdge>(NetworkEdge::class.java)

    init {
        nodes.forEach { graph.addVertex(it) }
        edges.forEach { graph.addEdge(it.v1, it.v2, it) }
    }

    val nodes: Set<NetworkNode>
        get() = graph.vertexSet()

    val edges: Set<NetworkEdge>
        get() = graph.edgeSet()

    fun bbox(): Rectangle2D {
        val vertices = nodes

        val minX = vertices.minBy { it.x }!!.x
        val minY = vertices.minBy { it.y }!!.y
        val maxX = vertices.maxBy { it.x }!!.x
        val maxY = vertices.maxBy { it.y }!!.y

        return Rectangle2D(minX, minY, maxX - minX, maxY - minY)
    }

    fun degreeOf(v: NetworkNode): Int = graph.degreeOf(v)

    override fun toString(): String {
        return "NetworkGraph($graph)"
    }
}