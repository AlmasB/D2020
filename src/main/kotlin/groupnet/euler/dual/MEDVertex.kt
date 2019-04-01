package groupnet.euler.dual

import groupnet.euler.Zone
import javafx.geometry.Point2D
import java.util.*

/**
 *
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class MEDVertex(val zone: Zone, val point: Point2D) {

    var isShown = false

    fun distance(other: MEDVertex) = point.distance(other.point)

    override fun hashCode(): Int {
        return Objects.hash(zone, point)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is MEDVertex)
            return false

        return zone == other.zone && point == other.point
    }

    override fun toString(): String {
        return zone.toString()
    }
}