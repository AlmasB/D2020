package groupnet.algorithm

import com.goebl.simplify.PointExtractor
import com.goebl.simplify.Simplify
import groupnet.algorithm.astar.AStarGrid
import groupnet.algorithm.astar.NodeState
import groupnet.euler.Zone
import groupnet.util.Bug
import groupnet.util.Log
import javafx.scene.shape.Polyline
import math.geom2d.Point2D
import math.geom2d.polygon.Polygons2D
import math.geom2d.polygon.SimplePolygon2D

/**
 *
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class AStarEdgeRouter : EdgeRouter {

    private val TILES = 50

    override fun route(zone1: Zone, zone2: Zone): Polyline {

        var union = Polygons2D.union(zone1.polygonShape, zone2.polygonShape)
        val bbox = union.boundingBox()

        val TILE_SIZE = (Math.min(bbox.width, bbox.height) / TILES).toInt()

        // keep only distinct vertices
        union = SimplePolygon2D(union.vertices().map { P(it.x().toInt(), it.y().toInt()) }.toSet().map { Point2D(it.x.toDouble(), it.y.toDouble()) })

        val boundary = union.boundary()

        // signed, so - if inside
        val maxDistance: Double = try {
            -Math.min(boundary.signedDistance(zone1.visualCenter.x, zone1.visualCenter.y), boundary.signedDistance(zone2.visualCenter.x, zone2.visualCenter.y))
        } catch (e: Exception) {
            1000.0
        }

        val grid = AStarGrid(bbox.width.toInt() / TILE_SIZE, bbox.height.toInt() / TILE_SIZE)

        for (y in 0 until grid.height) {
            for (x in 0 until grid.width) {
                val tileCenter = Point2D(x.toDouble() * TILE_SIZE + TILE_SIZE / 2 + bbox.minX, y.toDouble() * TILE_SIZE + TILE_SIZE / 2 + bbox.minY)

                val node = grid.getNode(x, y)

                try {
                    if (union.contains(tileCenter)) {
                        val dist = -boundary.signedDistance(tileCenter).toInt()

                        if (dist < TILE_SIZE) {
                            node.state = NodeState.NOT_WALKABLE
                            continue
                        }

                        node.state = NodeState.WALKABLE
                        node.gCost = ((1 - dist / maxDistance) * 5000).toInt()

                        if (node.gCost < 0) {
                            node.gCost = 0
                        }
                    } else {
                        node.state = NodeState.NOT_WALKABLE
                    }
                } catch (e: Exception) {

                    Log.e(e)

                    node.state = NodeState.NOT_WALKABLE
                }
            }
        }

        val startX = (zone1.visualCenter.x - bbox.minX) / TILE_SIZE
        val startY = (zone1.visualCenter.y - bbox.minY) / TILE_SIZE
        val targetX = (zone2.visualCenter.x - bbox.minX) / TILE_SIZE
        val targetY = (zone2.visualCenter.y - bbox.minY) / TILE_SIZE

        val path = grid.getPath(startX.toInt(), startY.toInt(), targetX.toInt(), targetY.toInt())

        if (path.isEmpty()) {
            throw Bug("Failed to route edge: $zone1 - $zone2")
        }

        val points = arrayListOf<Double>()

        // so that start vertices are exactly the same as requested
        points.add(zone1.visualCenter.x)
        points.add(zone1.visualCenter.y)

        points.addAll(path.map { arrayListOf(it.x, it.y) }
                .flatten()
                .mapIndexed { index, value -> value.toDouble() * TILE_SIZE + TILE_SIZE / 2 + (if (index % 2 == 0) bbox.minX else bbox.minY) }
                .dropLast(2)
        )

        // so that end vertices are exactly the same as requested
        points.add(zone2.visualCenter.x)
        points.add(zone2.visualCenter.y)

        return Polyline(*simplify(points))
    }

    private fun simplify(points: List<Double>): DoubleArray {
        val originalPoints = Converter.doubleArrayToPoints(points.toDoubleArray())

        // create an instance of the simplifier (empty array needed by List.toArray)
        // run simplification process
        val simplePoints = Simplify<javafx.geometry.Point2D>(arrayOf<javafx.geometry.Point2D>(), Extractor())
                .simplify(originalPoints.toTypedArray(), /* tolerance = */ 200.0, /*highQuality =*/ false)
                .toList()

        return Converter.pointsToDoubleArray(simplePoints)
    }

    private class Extractor : PointExtractor<javafx.geometry.Point2D> {
        override fun getY(p0: javafx.geometry.Point2D): Double {
            return p0.y
        }

        override fun getX(p0: javafx.geometry.Point2D): Double {
            return p0.x
        }
    }

    data class P(val x: Int, val y: Int)
}