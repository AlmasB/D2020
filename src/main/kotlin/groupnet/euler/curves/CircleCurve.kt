package groupnet.euler.curves

import groupnet.algorithm.Converter
import groupnet.euler.Curve
import groupnet.euler.Label
import javafx.geometry.Point2D
import javafx.scene.shape.Circle
import java.util.*

/**
 * A curve whose shape is a circle.
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class CircleCurve(
        label: Label,

        val centerX: Double,
        val centerY: Double,
        val radius: Double) : Curve(label) {

    fun getMinX() = (centerX - radius).toInt()

    fun getMinY() = (centerY - radius).toInt()

    override fun computeShape() = Circle(centerX, centerY, radius + 0.1)

    override fun computePolygon() = Converter.circleToPolygon(this)

    override fun copyWithNewLabel(newLabel: String): Curve {
        val copy = CircleCurve(newLabel, centerX, centerY, radius)
        copy.setLabelPositionX(getLabelPositionX())
        copy.setLabelPositionY(getLabelPositionY())
        return copy
    }

    override fun translate(translate: Point2D): Curve {
        val copy = CircleCurve(label, centerX + translate.x, centerY + translate.y, radius)
        copy.setLabelPositionX(getLabelPositionX() + translate.x)
        copy.setLabelPositionY(getLabelPositionY() + translate.y)
        return copy
    }

    override fun scale(scale: Double, pivot: Point2D): Curve {
        val scaled = CircleCurve(label, centerX * scale + (1-scale) * pivot.x, centerY * scale + (1-scale) * pivot.y, radius * scale)
        scaled.setLabelPositionX(getLabelPositionX() * scale + (1-scale) * pivot.x)
        scaled.setLabelPositionY(getLabelPositionY() * scale + (1-scale) * pivot.y)
        return scaled
    }

    override fun equals(other: Any?): Boolean {
        if (other !is CircleCurve)
            return false

        return label == other.label &&
                centerX == other.centerX &&
                centerY == other.centerY &&
                radius == other.radius
    }

    override fun hashCode(): Int {
        return Objects.hash(label, centerX, centerY, radius)
    }

    override fun toDebugString(): String {
        return "$this($centerX, $centerY, r=$radius)"
    }
}