package groupnet.algorithm

import groupnet.algorithm.voronoi.GraphEdge
import groupnet.algorithm.voronoi.Voronoi
import groupnet.euler.Zone
import groupnet.gui.SettingsController
import groupnet.util.Bug
import javafx.geometry.Point2D
import javafx.scene.shape.Polyline
import math.geom2d.polygon.Polygons2D
import org.jgrapht.alg.DijkstraShortestPath
import org.jgrapht.graph.SimpleGraph

/**
 *
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class VoronoiEdgeRouter : EdgeRouter {

    private val voronoi = Voronoi(15.0)

    override fun route(zone1: Zone, zone2: Zone): Polyline {
        println("Routing $zone1-$zone2")
        println("Centers ${zone1.visualCenter}-${zone2.visualCenter}")

        SettingsController.debugPoints.add(zone1.visualCenter)
        SettingsController.debugPoints.add(zone2.visualCenter)

        val union = Polygons2D.union(zone1.polygonShape, zone2.polygonShape)
        val bbox = union.boundingBox()

        val xValues = union.vertices().map { it.x() }
        val yValues = union.vertices().map { it.y() }

        //SettingsController.debugPoints.addAll(polygon.vertices().map { Point2D(it.x(), it.y()) })
        //SettingsController.debugPoints.add(zone.center)

        val edges = voronoi.generateVoronoi(xValues.toDoubleArray(), yValues.toDoubleArray(), bbox.minX, bbox.maxX, bbox.minY, bbox.maxY)
                .filter { union.contains(it.x1, it.y1) && union.contains(it.x2, it.y2) }
                .filter { union.boundary().signedDistance(it.x1, it.y1) < -40 && union.boundary().signedDistance(it.x2, it.y2) < -40 }
                as MutableList<GraphEdge>

        println(edges)

        val graph = SimpleGraph<Point2D, GraphEdge>(GraphEdge::class.java)
        edges.flatMap { listOf(Point2D(it.x1.toInt().toDouble(), it.y1.toInt().toDouble()), Point2D(it.x2.toInt().toDouble(), it.y2.toInt().toDouble())) }.forEach { graph.addVertex(it) }
        edges.forEach { graph.addEdge(Point2D(it.x1.toInt().toDouble(), it.y1.toInt().toDouble()), Point2D(it.x2.toInt().toDouble(), it.y2.toInt().toDouble())) }









        // so that start and end vertices are exactly the same as requested
        val points = arrayListOf<Double>(zone1.visualCenter.x, zone1.visualCenter.y)

        val startEdge = edges.sortedBy { distanceToEdge(it, zone1.visualCenter, zone2.visualCenter) }.first()

//        val startVertex = edges.map { listOf(Point2D(it.x1, it.y1), Point2D(it.x2, it.y2)) }
//                .flatMap { it }
//                .sortedBy { it.distance(zone1.center) }
//                .first()
//
//        val endVertex = edges.map { listOf(Point2D(it.x1, it.y1), Point2D(it.x2, it.y2)) }
//                .flatMap { it }
//                .sortedBy { it.distance(zone2.center) }
//                .first()

        val startVertex = graph.vertexSet().sortedBy { it.distance(zone1.visualCenter) }.first()
        val endVertex = graph.vertexSet().sortedBy { it.distance(zone2.visualCenter) }.first()




        println("PATH: $startVertex - $endVertex")
        DijkstraShortestPath.findPathBetween(graph, startVertex, endVertex).forEach { println(it) }






        val endEdge = edges.sortedBy { distanceToEdge(it, zone2.visualCenter, zone1.visualCenter) }.first()

        println("Start: $startEdge")
        println("End Vertex: $endVertex")

        var nextX = 0.0
        var nextY = 0.0

        if (zone1.visualCenter.distance(startEdge.x1, startEdge.y1) < zone1.visualCenter.distance(startEdge.x2, startEdge.y2)) {
            points.add(startEdge.x1)
            points.add(startEdge.y1)

            nextX = startEdge.x2
            nextY = startEdge.y2
        } else {
            points.add(startEdge.x2)
            points.add(startEdge.y2)

            nextX = startEdge.x1
            nextY = startEdge.y1
        }

        edges.remove(startEdge)

        var (nextEdge, nextVertex) = findNextEdge(nextX, nextY, edges)

        points.add(nextVertex.x)
        points.add(nextVertex.y)



        // debug

        for (i in 0..points.size-1 step 2) {
            SettingsController.debugPoints.add(Point2D(points[i], points[i+1]))
        }


        while (!nextEdge.hasVertexInt(endVertex.x.toInt(), endVertex.y.toInt())) {

            edges.remove(nextEdge)

            val (e, v) = findNextEdge(nextVertex.x, nextVertex.y, edges)
            nextEdge = e
            nextVertex = v

            points.add(nextVertex.x)
            points.add(nextVertex.y)

            println("next edge is $nextEdge")
            println(endEdge === nextEdge)

            SettingsController.debugPoints.add(Point2D(nextVertex.x, nextVertex.y))
        }


//        if (zone2.center.distance(endEdge.x1, startEdge.y1) < zone2.center.distance(endEdge.x2, startEdge.y2)) {
//            points.add(endEdge.x1)
//            points.add(endEdge.y1)
//        } else {
//            points.add(endEdge.x2)
//            points.add(endEdge.y2)
//        }

        // so that start and end vertices are exactly the same as requested
        points.add(zone2.visualCenter.x)
        points.add(zone2.visualCenter.y)

        return Polyline(*points.toDoubleArray())
    }

    private fun distanceToEdge(edge: GraphEdge, point: Point2D, point2: Point2D): Double {
        return minOf(point.distance(edge.x1, edge.y1), point.distance(edge.x2, edge.y2)) + 0
                //point2.distance(edge.x1, edge.y1) +
                //point2.distance(edge.x2, edge.y2)
    }

    private fun findNextEdge(vertexX: Double, vertexY: Double, edges: List<GraphEdge>): Pair<GraphEdge, Point2D> {
        println("Searching for: ${vertexX.toInt()},${vertexY.toInt()}")

        edges.forEach {
            if (it.x1.toInt() == vertexX.toInt() && it.y1.toInt() == vertexY.toInt())
                return it.to(Point2D(it.x2, it.y2))

            if (it.x2.toInt() == vertexX.toInt() && it.y2.toInt() == vertexY.toInt())
                return it.to(Point2D(it.x1, it.y1))
        }

        throw Bug("Can't find next edge")
    }
}