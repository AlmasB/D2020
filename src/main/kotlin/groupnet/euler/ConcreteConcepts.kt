package groupnet.euler

import groupnet.algorithm.Polylabel
import groupnet.euler.curves.CircleCurve
import groupnet.gui.SettingsController
import groupnet.util.*
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.geometry.Point2D
import javafx.geometry.Rectangle2D
import javafx.scene.shape.Shape
import math.geom2d.polygon.MultiPolygon2D
import math.geom2d.polygon.Polygon2D
import math.geom2d.polygon.Polygons2D
import java.util.*
import kotlin.math.sqrt

/**
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */

/**
 * A closed curve, c (element of C).
 */
abstract class Curve(val label: Label) : Comparable<Curve> {

    private val labelPositionX = SimpleDoubleProperty()
    private val labelPositionY = SimpleDoubleProperty()

    fun labelPositionXProperty(): DoubleProperty = labelPositionX
    fun labelPositionYProperty(): DoubleProperty = labelPositionY

    fun getLabelPositionX(): Double = labelPositionX.value
    fun getLabelPositionY(): Double = labelPositionY.value

    fun setLabelPositionX(value: Double) {
        labelPositionX.value = value
    }

    fun setLabelPositionY(value: Double) {
        labelPositionY.value = value
    }

    val cachedPolygon by lazy { computePolygon() }

    val cachedShape by lazy { computeShape() }

    /**
     * @return a curve model for computational geometry
     */
    fun getPolygon() = computePolygon()

    /**
     * @return a bitmap view for rendering
     */
    fun getShape() = computeShape()

    abstract fun computePolygon(): Polygon2D

    abstract fun computeShape(): Shape

    abstract fun copyWithNewLabel(newLabel: String): Curve

    abstract fun translate(translate: Point2D): Curve

    abstract fun scale(scale: Double, pivot: Point2D): Curve

    abstract fun toDebugString(): String

    override fun compareTo(other: Curve): Int {
        return label.compareTo(other.label)
    }

    override fun toString() = label
}

/**
 * A zone, z (element of Z), in an Euler diagram.
 */
class Zone(

        /**
         * Abstract representation of this zone.
         */
        val az: AbstractZone,

        /**
         * All curves present in the Euler diagram (to which this zone belongs).
         */
        diagramCurves: Set<Curve>) {

    /**
     * Curves inside this zone.
     */
    val containingCurves = diagramCurves.filter { it.label in az }.toSet()

    /**
     * Curves outside of this zone.
     */
    val excludingCurves = diagramCurves - containingCurves

    init {
        if (containingCurves.size != az.numLabels)
            throw Bug("Abstract: $az does not match concrete: $containingCurves")
    }

    fun getShape(): Shape {
        var shape: Shape = SettingsController.fxBBox

        for (curve in containingCurves) {
            shape = Shape.intersect(shape, curve.getShape())
        }

        for (curve in excludingCurves) {
            shape = Shape.subtract(shape, curve.getShape())
        }

        return shape
    }

    val visualCenter by lazy { computeVisualCentre() }

    private fun computeVisualCentre(): Point2D {
        if (az == AbstractZone.OUTSIDE) {
            val minCurveX = excludingCurves.map { it.getPolygon().boundingBox().minX }.min()!!
            val maxCurveX = excludingCurves.map { it.getPolygon().boundingBox().maxX }.max()!!
            val minCurveY = excludingCurves.map { it.getPolygon().boundingBox().minY }.min()!!

            return Point2D((minCurveX + maxCurveX) / 2, (SettingsController.geomBBox.boundingBox().minY * 0.1 + minCurveY * 0.9))
        }

        return Polylabel.findCenter(polygonShape)
    }

    val polygonShape by lazy { computePolygonShape() }

    private fun computePolygonShape(): Polygon2D {
        var pShape = SettingsController.geomBBox

        containingCurves.map { c -> c.getPolygon() }.forEach { p -> pShape = Polygons2D.intersection(pShape, p) }

        excludingCurves.map { c -> c.getPolygon() }.forEach { p -> pShape = Polygons2D.difference(pShape, p) }

        return pShape
    }

    fun isTopologicallyAdjacent(other: Zone): Boolean {
        if (!az.isNeighbour(other.az))
            return false

        for (v1 in polygonShape.vertices()) {
            for (v2 in other.polygonShape.vertices()) {
                if (v1.asInt == v2.asInt)
                    return true
            }
        }

        return false
    }

    fun separatingCurve(other: Zone): Curve? {
        val diff = containingCurves.symmetricDifference(other.containingCurves)
        return if (diff.size == 1) diff.first() else null
    }

    fun shortestDistanceToOtherZone(point: Point2D): Double {
        if (polygonShape is MultiPolygon2D) {
            return Math.abs(polygonShape.complement().boundary().signedDistance(point.x, point.y))
        } else {
            return Math.abs(-polygonShape.boundary().signedDistance(point.x, point.y))
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Zone)
            return false

        return az == other.az &&
                containingCurves.size == other.containingCurves.size &&
                excludingCurves.size == other.excludingCurves.size &&
                containingCurves.containsAll(other.containingCurves) &&
                excludingCurves.containsAll(other.excludingCurves)
    }

    private val hash1 = containingCurves.sumBy { it.hashCode() }
    private val hash2 = excludingCurves.sumBy { it.hashCode() }

    override fun hashCode(): Int {
        return Objects.hash(az, hash1, hash2)
    }

    override fun toString() = az.toString()
}

/**
 * An Euler diagram, d = (C, l).
 */
class EulerDiagram(val originalDescription: Description,
                   val actualDescription: Description,
                   curvesInternal: Set<Curve>) : Logable {

    val props = hashMapOf<Any, Any>()

    val curves = Collections.unmodifiableSet(curvesInternal)

    /**
     * All zones of this Euler diagram, including shaded zones.
     * Does not include the outside zone.
     */
    val zones = actualDescription.abstractZones.minus(AbstractZone.OUTSIDE).map { Zone(it, curves) }.toSet()

    val shadedZones = zones.filter { it.az !in Z(originalDescription) }

    val outsideZone = Zone(AbstractZone.OUTSIDE, curves)

    val numNonCircles = C(this).size - C(this).filter { it is CircleCurve }.size

    fun getZone(az: AbstractZone): Zone {
        if (az == azEmpty)
            return outsideZone

        return zones.find { it.az == az } ?: throw Bug("No zone exists with abstraction $az")
    }

    fun drawIntoZone(az: AbstractZone, diagram: EulerDiagram, zoneScore: Int = 1): EulerDiagram {
        Log.d("Drawing into $az", diagram)

        // [diagram] will be embedded "to" this center point
        val newCenter: Point2D

        val scaleRatio: Double
        val new_d: EulerDiagram

        if (az == azEmpty) {
            val bbox1 = this.bbox()

            val bbox2 = diagram.bbox()
            val diagramCenter = bbox2.center()

            newCenter = diagramCenter.add(bbox1.maxX + 4000.0, 0.0)

            scaleRatio = 1.0

            val scaledCurves = diagram.curves.map { it.scale(scaleRatio, diagramCenter) }

            val newCurves = curves.plus(scaledCurves.map { it.translate(newCenter.subtract(diagramCenter)) })

            new_d = EulerDiagram(originalDescription + diagram.originalDescription, actualDescription + diagram.actualDescription, newCurves)

        } else {
            val zone = zones.find { it.az == az } ?: throw Bug("No zone $az found in $this")

            newCenter = zone.visualCenter

            val minRadius: Double = zone.shortestDistanceToOtherZone(newCenter)

            val bbox = diagram.bbox()
            val diagramCenter = bbox.center()

            val actualRadius = maxOf(bbox.width, bbox.height)

            // we are embedding into az, so score of az can never be 0
            scaleRatio = minRadius / actualRadius / maxOf(sqrt(zoneScore.toDouble()) * 0.5, 0.75)

            val scaledCurves = diagram.curves.map { it.scale(scaleRatio, diagramCenter) }

            val newCurves = curves.plus(scaledCurves.map { it.translate(newCenter.subtract(diagramCenter)) })

            new_d = EulerDiagram(originalDescription + diagram.originalDescription, actualDescription + diagram.actualDescription, newCurves)
        }

        if (az != azEmpty) {
            new_d.curves.filter { it.label in diagram.curves.map { it.label } }.forEach { new_d.updateLabelPosition(it) }
        }

        new_d.props["embedCenter"] = newCenter
        new_d.props["scaleRatio"] = scaleRatio
        return new_d
    }

    private fun updateLabelPosition(curve: Curve) {
        val otherCurves = C(this).minus(curve)
        val polygon = curve.cachedPolygon
        val center = polygon.centroid()

        val labelPos = polygon.vertices()
                .map { Point2D(it.x(), it.y()) }
                .map {
                    val magnitude = capMagnitude(if (curve is CircleCurve) curve.radius / 3 else 150.0)

                    val r = if (curve is CircleCurve) curve.radius else 0.0

                    // compute vector outwards
                    it.add(it.subtract(center.x(), center.y()).normalize().multiply(magnitude))
                }
                .sortedBy {
                    val minDistance = minDistanceToOtherCurves(it, otherCurves)
                    val numCurves = numCurvesThatContainPoint(it, otherCurves)

                    // number of curves has a more significant impact
                    2000 * numCurves - minDistance
                }
                .first()!!

        curve.setLabelPositionX(labelPos.x)
        curve.setLabelPositionY(labelPos.y)
    }

    private fun capMagnitude(mag: Double): Double {
        if (mag > 300)
            return 300.0

        if (mag < 150)
            return 150.0

        return mag
    }

    private fun numCurvesThatContainPoint(point: Point2D, curves: Set<Curve>) = curves.count { it.cachedShape.contains(point) }

    private fun minDistanceToOtherCurves(point: Point2D, curves: Set<Curve>): Double {
        return curves.map { it.cachedPolygon.boundary().signedDistance(point.x, point.y) }
                // -20 is threshold on how "close" we think it is
                // because of polygon <-> smooth representations we might lose precision
                .filter { it >= -20 }
                .min()
                ?: 0.0
    }

    fun bbox(): Rectangle2D {
        val polygons = curves.map { it.getPolygon() }
        val vertices = polygons.flatMap { it.vertices() }

        val minX = vertices.minBy { it.x() }!!.x()
        val minY = vertices.minBy { it.y() }!!.y()
        val maxX = vertices.maxBy { it.x() }!!.x()
        val maxY = vertices.maxBy { it.y() }!!.y()

        return Rectangle2D(minX, minY, maxX - minX, maxY - minY)
    }

    fun size(): Double {
        val bbox = bbox()
        return maxOf(bbox.width, bbox.height)
    }

    fun center(): Point2D {
        val bbox = bbox()
        return Point2D(bbox.minX + bbox.maxX / 2, bbox.minY + bbox.maxY / 2)
    }

    fun translate(vector: Point2D): EulerDiagram {
        return EulerDiagram(originalDescription, actualDescription, curves.map { it.translate(vector) }.toSet())
    }

    fun scale(pivot: Point2D, ratio: Double): EulerDiagram {
        return EulerDiagram(originalDescription, actualDescription, curves.map { it.scale(ratio, pivot) }.toSet())
    }

    override fun toLog(): String {
        return "ED[o=$originalDescription, actual=$actualDescription, curves=$curves, zones=$zones]"
    }

    override fun hashCode(): Int {
        return Objects.hash(originalDescription, actualDescription, curves, zones, shadedZones, outsideZone)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is EulerDiagram)
            return false

        return originalDescription == other.originalDescription &&
                actualDescription == other.actualDescription &&
                curves == other.curves &&
                zones == other.zones &&
                shadedZones == other.shadedZones &&
                outsideZone == other.outsideZone
    }

    override fun toString(): String {
        return "ED[D=$actualDescription]"
    }
}