package groupnet.network

import groupnet.euler.Zone
import groupnet.gui.SettingsController
import groupnet.gui.SettingsController.*
import groupnet.util.circleIntersectsLine
import groupnet.util.distancePolygonPoint
import groupnet.util.vectorFromPointToLine
import javafx.geometry.Point2D
import java.util.*

/**
 *
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class NetworkNode(val label: String,
                  val z: Zone?,
                  p: Point2D = Point2D.ZERO) {

    var pos: Point2D = p

    val x get() = pos.x
    val y get() = pos.y

    var vel: Point2D = Point2D.ZERO

    fun crossesNode(other: NetworkNode): Boolean {
        val dx = other.x - this.x
        val dy = other.y - this.y

        val d = (NODE_SIZE * 2)

        return dx*dx + dy*dy <= d*d
    }

    fun isNonIncidentAndOnEdge(edge: NetworkEdge, nodeRadius: Double): Boolean {
        if (edge.v1 == this || edge.v2 == this)
            return false

        return circleIntersectsLine(nodeRadius, pos, edge.v1.pos, edge.v2.pos)
    }

    fun isIncidentWith(edge: NetworkEdge): Boolean {
        return edge.v1 == this || edge.v2 == this
    }

    fun vectorToEdge(edge: NetworkEdge): Point2D {
        return vectorFromPointToLine(pos, edge.v1.pos, edge.v2.pos)
    }

    fun applyAcceleration(force: Point2D) {
        vel = vel.add(force)
    }

    fun applyVelocity(force: Point2D) {
        pos = pos.add(force)
    }

    fun resetVelocity() {
        vel = Point2D.ZERO
    }

    private val minDistToZone = Random().nextInt(100) + 170

    /**
     * Node is valid if it is within its zone boundaries.
     */
    fun hasValidPlacement(): Boolean {
        return z!!.polygonShape.contains(pos.x, pos.y)
                && z.polygonShape.contains(pos.x- NODE_SIZE * 1.5, pos.y)
                && z.polygonShape.contains(pos.x+ NODE_SIZE * 1.5, pos.y)
                && z.polygonShape.contains(pos.x, pos.y-NODE_SIZE * 1.5)
                && z.polygonShape.contains(pos.x, pos.y+NODE_SIZE * 1.5)
                && distancePolygonPoint(z.polygonShape, pos) > minDistToZone
    }

    fun isGNDBased() = z != null

    override fun toString() = label

    override fun hashCode(): Int {
        return label.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is NetworkNode)
            return false

        return hasSameLabel(other)
    }

    fun hasSameLabel(o: NetworkNode) = this.label == o.label
}