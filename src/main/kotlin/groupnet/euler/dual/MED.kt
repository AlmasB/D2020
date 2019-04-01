package groupnet.euler.dual

import groupnet.algorithm.AStarEdgeRouter
import groupnet.algorithm.Converter
import groupnet.euler.*
import groupnet.graph.cycles.CycleFinder
import groupnet.util.*
import javafx.geometry.Point2D
import javafx.scene.paint.Color
import javafx.scene.shape.*
import math.geom2d.polygon.SimplePolygon2D
import java.util.*

/**
 * Modified Euler dual.
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class MED(private val d: EulerDiagram) {

    lateinit var vertices: MutableList<MEDVertex>
    lateinit var edges: MutableList<MEDEdge>

    private lateinit var outsideNodes: List<MEDVertex>

    val ring = Circle(0.0, null)

    init {
        computeInsideVertices()
        computeInsideEdges()
        computeOutsideVertices()
        computeOutsideEdges()
    }

    private fun computeInsideVertices() {
        vertices = d.zones.map { z -> MEDVertex(z, z.visualCenter) }.toMutableList()

        vertices.forEach { it.isShown = true }
    }

    private fun computeInsideEdges() {
        edges = combinations2(vertices)
                .filter { (v1, v2) -> v1.zone.isTopologicallyAdjacent(v2.zone) }
                .map { (v1, v2) -> createEdge(v1, v2) }
                .toMutableList()

        edges.forEach { it.isShown = true }
    }

    private fun computeOutsideVertices() {
        val bbox = bbox(d.zones)
        val center = bbox.center()

        // 100 - distance between diagram and MED
        val radius = bbox.distToCorner() + 100

        val polygonMED = Converter.toPolygon2D(Converter.makePolygon(radius.toInt(), 16))

        val firstPt = Point2D(center.x - radius, center.y)
        val vector = firstPt.subtract(polygonMED.vertex(0).x(), polygonMED.vertex(0).y())

        // make "distinct" nodes so that jgrapht doesn't think it's a loop
        outsideNodes = polygonMED.vertices().map { MEDVertex(Zone(azEmpty, d.curves), Point2D(it.x(), it.y()).add(vector)) }
    }

    private fun computeOutsideEdges() {
        // add the adjacent edges between outside and inside

        vertices.filter { it.zone.isTopologicallyAdjacent(d.outsideZone) }
                .forEach { node ->
                    val closestMEDNode = outsideNodes.minBy { it.distance(node) }!!

                    closestMEDNode.isShown = true

                    val edge = MEDEdge(node, closestMEDNode,
                            Line(node.point.x, node.point.y, closestMEDNode.point.x, closestMEDNode.point.y))
                    edge.isShown = true

                    edges.add(edge)
                }

        // then add nodesMED to nodes
        vertices.addAll(outsideNodes)

        computeMEDRingEdges(outsideNodes, Point2D(ring.centerX, ring.centerY))
    }

    /**
     * Creates an Euler dual edge between [v1] and [v2] represented by a polyline.
     */
    private fun createEdge(v1: MEDVertex, v2: MEDVertex): MEDEdge {
        Log.d("Creating edge: ${v1.zone} - ${v2.zone}")

        val p1 = v1.zone.visualCenter
        val p2 = v2.zone.visualCenter

        val line = Line(p1.x, p1.y, p2.x, p2.y)

        // the new curve segment must pass through the straddled curve
        // and only through that curve
        val curve = v1.zone.separatingCurve(v2.zone) ?: throw Bug("Zones are not adjacent")

        Log.d("Searching ${v1.zone} - ${v2.zone} : $curve")

        if (doesSegmentPassThroughCurveOnly(line, curve, C(d))) {
            return MEDEdge(v1, v2, line)
        }

        val polyline = AStarEdgeRouter().route(v1.zone, v2.zone)

        return MEDEdge(v1, v2, polyline)
    }

    /**
     * Does curve segment [segment] only pass through [curve] and no other curve in [curves].
     */
    private fun doesSegmentPassThroughCurveOnly(segment: Shape, curve: Curve, curves: Collection<Curve>): Boolean {
        return curves.minus(curve).none { segmentOverlapsCurve(segment, it) }
                && segmentOverlapsCurve(segment, curve)
    }

    /**
     * Does not alter any shape data.
     *
     * @return true iff [segment] (line-ish shape) overlaps (crosses) with a closed [curve]
     */
    private fun segmentOverlapsCurve(segment: Shape, curve: Curve): Boolean {
        val curveShape = curve.getShape()

        val originalFill = curveShape.fill
        val originalStroke = curveShape.stroke

        curveShape.fill = null
        curveShape.stroke = Color.BROWN

        val doesOverlap = !Shape.intersect(curveShape, segment).layoutBounds.isEmpty

        curveShape.fill = originalFill
        curveShape.stroke = originalStroke

        return doesOverlap
    }

    /**
     * @param nodesMED nodes of MED placed in the outside zone
     * @param center center of the MED bounding circle
     */
    private fun computeMEDRingEdges(nodesMED: List<MEDVertex>, center: Point2D) {
        // sort nodes along the MED ring
        // sorting is CCW from 0 (right) to 360
        Collections.sort(nodesMED, { node1, node2 ->
            val v1 = node1.point.subtract(center)
            val angle1 = vectorToAngle(v1)

            val v2 = node2.point.subtract(center)
            val angle2 = vectorToAngle(v2)

            (angle1 - angle2).toInt()
        })

        for (i in nodesMED.indices) {
            val node1 = nodesMED[i]
            val node2 = if (i == nodesMED.size - 1) nodesMED[0] else nodesMED[i+1]

            val p1 = node1.point
            val p2 = node2.point

            edges.add(MEDEdge(node1, node2, Line(p1.x, p1.y, p2.x, p2.y)))
        }
    }

    fun computeCycle(zonesToSplit: Set<AbstractZone>, azPair: Pair<AbstractZone, AbstractZone>?): MEDCycle? {
        if (azPair == null)
            return computeCycle(zonesToSplit)

        Log.d("Computing cycle for $zonesToSplit")

        Profiler.start("Enumerating cycles")
        val cycles = enumerateCycles()
        Profiler.end("Enumerating cycles")

        Log.d("Found cycles: ${cycles.size} for $zonesToSplit")

        //        check that cycle nodes are equal or superset of what is required      cycle selection      and is valid
        return cycles.find { it.nodes.map { it.zone.az }.containsAll(zonesToSplit) && isValid(azPair, it) }
    }

    fun computeCycle(zonesToSplit: Set<AbstractZone>): MEDCycle? {
        Log.d("Computing cycle for $zonesToSplit")

        Profiler.start("Enumerating cycles")
        val cycles = enumerateCycles()
        Profiler.end("Enumerating cycles")

        Log.d("Found cycles: ${cycles.size} for $zonesToSplit")

        //        check that cycle nodes are equal or superset of what is required        and is valid
        return cycles.find { it.nodes.map { it.zone.az }.containsAll(zonesToSplit) && isValid(null, it) }
    }

    /**
     * Enumerate all simple cycles.
     */
    private fun enumerateCycles(): List<MEDCycle> {
        val graph = CycleFinder<MEDVertex, MEDEdge>(MEDEdge::class.java)
        vertices.forEach { graph.addVertex(it) }
        edges.forEach { graph.addEdge(it.v1, it.v2, it) }

        return graph.computeCycles()
    }

    /**
     * A cycle is valid if it can be used to embed a curve.
     */
    private fun isValid(azPair: Pair<AbstractZone, AbstractZone>?, cycle: MEDCycle): Boolean {
        Log.d("Checking cycle: $cycle")

        // this ensures that we do not allow same vertices in the cycle
        // unless it's the outside vertex
        cycle.nodes.groupBy { it.zone.az }.forEach {
            if (it.key != azEmpty && it.value.size > 1) {
                Log.d("Discarding cycle because ${it.key} is present ${it.value.size} times")
                return false
            }
        }

        var tmpPoint = cycle.nodes[0].point

        cycle.polygon = arrayListOf()
        // add the first point
        cycle.polygon.add(tmpPoint)

        cycle.edges.map { it.shape }.forEach { q ->

            when(q) {

                is Line -> {
                    val lineTo = LineTo()

                    // we do this coz source and end vertex might be swapped
                    if (tmpPoint == Point2D(q.startX, q.startY)) {
                        lineTo.x = q.endX
                        lineTo.y = q.endY
                    } else {
                        lineTo.x = q.startX
                        lineTo.y = q.startY
                    }

                    tmpPoint = Point2D(lineTo.x, lineTo.y)

                    cycle.polygon.add(tmpPoint)
                }

                is Polyline -> {

                    val start = Point2D(q.points[0], q.points[1])
                    val end = Point2D(q.points[q.points.size-2], q.points[q.points.size-1])

                    // we do this coz source and end vertex might be swapped
                    val normalOrder = tmpPoint == start

                    tmpPoint = end

                    if (normalOrder) {
                        var i = 2
                        while (i < q.points.size) {

                            val point = Point2D(q.points[i], q.points[++i])

                            cycle.polygon.add(point)

                            i++
                        }
                    } else {
                        var i = q.points.size-3
                        while (i > 0) {

                            val point = Point2D(q.points[i-1], q.points[i])

                            cycle.polygon.add(point)

                            i -= 2
                        }
                    }
                }

                else -> {
                    throw Bug("Unknown edge shape: $q")
                }
            }
        }



        // drop last duplicate of first moveTO
        val lastPt = cycle.polygon.removeAt(cycle.polygon.size - 1)


        cycle.polygon = cycle.polygon.distinct().toMutableList()

        var polygon = SimplePolygon2D(
                cycle.polygon.map { math.geom2d.Point2D(it.x, it.y) }
                        .plus(math.geom2d.Point2D(lastPt.x, lastPt.y))
        )

        polygon = if (polygon.area() < 0) polygon.complement() else polygon

        val hasInside = vertices.minus(cycle.nodes).any { polygon.contains(it.point.x, it.point.y) }

        if (hasInside)
            return false

        if (azPair != null) {
            val v1 = cycle.nodesUnique().find { it.zone.az == azPair.first }
            val v2 = cycle.nodesUnique().find { it.zone.az == azPair.second }

            if (v1 != null && v2 != null) {
                val midpoint = v1.point.midpoint(v2.point)

                if (polygon.contains(midpoint.x, midpoint.y)) {
                    return false
                }
            }
        }

        Log.d("Cycle is valid")
        Log.d("Smoothing data: ${cycle.polygon.joinToString { "" + it.x + " " + it.y  }}")
        return true
    }
}