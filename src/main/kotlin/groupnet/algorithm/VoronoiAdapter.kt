package groupnet.algorithm

import groupnet.algorithm.voronoi.Voronoi
import groupnet.euler.Zone
import groupnet.gui.SettingsController
import javafx.geometry.Point2D
import javafx.scene.shape.Line
import math.geom2d.polygon.Polygon2D

/**
 *
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
object VoronoiAdapter {

    private val voronoi = Voronoi(15.0)

    fun adapt(zone: Zone): List<Line> {
        val polygon = zone.polygonShape
        val bbox = polygon.boundingBox()

        val xValues = polygon.vertices().map { it.x() }
        val yValues = polygon.vertices().map { it.y() }

        SettingsController.debugPoints.addAll(polygon.vertices().map { Point2D(it.x(), it.y()) })
        SettingsController.debugPoints.add(zone.visualCenter)

        val edges = voronoi.generateVoronoi(xValues.toDoubleArray(), yValues.toDoubleArray(), bbox.minX, bbox.maxX, bbox.minY, bbox.maxY)

        return edges.filter { polygon.contains(it.x1, it.y1) && polygon.contains(it.x2, it.y2) }
                .filter { polygon.boundary().signedDistance(it.x1, it.y1) < -40 && polygon.boundary().signedDistance(it.x2, it.y2) < -40 }
                .map { Line(it.x1, it.y1, it.x2, it.y2) }
    }

    fun adapt(polygon: Polygon2D): List<Line> {
        val bbox = polygon.boundingBox()

        val xValues = polygon.vertices().map { it.x() }
        val yValues = polygon.vertices().map { it.y() }

        SettingsController.debugPoints.addAll(polygon.vertices().map { Point2D(it.x(), it.y()) })
        //SettingsController.debugPoints.add(zone.center)

        val edges = voronoi.generateVoronoi(xValues.toDoubleArray(), yValues.toDoubleArray(), bbox.minX, bbox.maxX, bbox.minY, bbox.maxY)

        return edges
                .filter { polygon.contains(it.x1, it.y1) && polygon.contains(it.x2, it.y2) }
                .filter { polygon.boundary().signedDistance(it.x1, it.y1) < -140 && polygon.boundary().signedDistance(it.x2, it.y2) < -140 }
                .map { Line(it.x1, it.y1, it.x2, it.y2) }
    }
}