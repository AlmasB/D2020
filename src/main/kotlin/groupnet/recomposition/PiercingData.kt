package groupnet.recomposition

import groupnet.diagram.EulerDiagramCreator
import groupnet.euler.Zone
import javafx.geometry.Point2D
import math.geom2d.polygon.MultiPolygon2D

/**
 *
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class PiercingData(numZones: Int, private val cluster: List<Zone>, private val allZones: List<Zone>) {

    val center: Point2D?
    val radius: Double

    init {
        if (numZones == 4) {
            center = cluster.map { it.polygonShape.vertices() }
                    .flatten()
                    .groupBy { it.asInt }
                    // we search for a vertex that is present in all 4 zones (sometimes we can have duplicates)
                    .filter { it.value.size >= 4 }
                    // ensure that each zone in cluster has such a vertex
                    .filter { entry ->
                        cluster.all { it.polygonShape.vertices().map { it.asInt }.any { it.x == entry.key.x && it.y == entry.key.y } }
                    }
                    .map { Point2D(it.key.getX(), it.key.getY()) }
                    // select the bottom circle, then top
                    .sortedByDescending { it.y }
                    .firstOrNull()

        } else { // if 2

            val map = cluster.map { it.polygonShape.vertices() }
                    .flatten()
                    .groupBy({ it.asInt })
                    // we search for vertices present along 2 zone bounds (collisions)
                    .filter { it.value.size >= numZones }
                    .filter { entry ->
                        cluster.all { it.polygonShape.vertices().map { it.asInt }.any { it.x == entry.key.x && it.y == entry.key.y } }
                    }
                    .map { Point2D(it.key.getX(), it.key.getY()) }
                    // remove vertices that occur in other zone bounds
                    // to filter out the corner vertices
                    .minus(
                            allZones.minus(cluster)
                                    .map { it.polygonShape.vertices() }
                                    .flatten()
                                    .groupBy { it.asInt }
                                    .map { Point2D(it.key.getX(), it.key.getY()) }
                    )
                    .groupBy { computeRadius(it) }
                    .toSortedMap()

            center = if (map.isEmpty()) null else map[map.lastKey()]!!.sortedByDescending { it.x }.firstOrNull()
        }

        if (center != null) {
            radius = computeRadius(center)
        } else {
            radius = 0.0
        }
    }

    fun isPiercing() = center != null

    private fun computeRadius(potentialCenter: Point2D): Double {
        return allZones
                .minus(cluster)
                .map {
                    if (it.polygonShape is MultiPolygon2D) {

                        // check signed distance and also of the complement
                        val dist1 = Math.abs(it.polygonShape.boundary().signedDistance(potentialCenter.x, potentialCenter.y))
                        val dist2 = Math.abs(it.polygonShape.complement().boundary().signedDistance(potentialCenter.x, potentialCenter.y))

                        Math.min(dist1, dist2)
                    } else {
                        it.polygonShape.distance(potentialCenter.x, potentialCenter.y)
                    }
                }
                .sorted()
                // null occurs when we split a single curve?
                .firstOrNull() ?: EulerDiagramCreator.BASE_RADIUS * 2
    }
}