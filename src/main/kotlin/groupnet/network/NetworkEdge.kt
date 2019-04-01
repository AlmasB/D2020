package groupnet.network

import groupnet.util.lineIntersectsLine
import javafx.geometry.Point2D

/**
 *
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class NetworkEdge(val v1: NetworkNode, val v2: NetworkNode, var size: Double = 1.0) {

    fun isAdjacent(other: NetworkEdge): Boolean {
        return v1 === other.v1 || v1 === other.v2 || v2 === other.v1 || v2 === other.v2
    }

    fun getOtherV(v: NetworkNode): NetworkNode {
        if (v === v1)
            return v2
        else
            return v1
    }

    fun crosses(other: NetworkEdge): Boolean {
        val A = Point2D(v1.pos.x, v1.pos.y)
        val B = Point2D(v2.pos.x, v2.pos.y)
        val C = Point2D(other.v1.pos.x, other.v1.pos.y)
        val D = Point2D(other.v2.pos.x, other.v2.pos.y)
        return lineIntersectsLine(A, B, C, D)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is NetworkEdge)
            return false

        return (v1 == other.v1 && v2 == other.v2)
                || (v1 == other.v2 && v2 == other.v1)
    }

    override fun hashCode(): Int {
        return v1.hashCode() + v2.hashCode()
    }

    override fun toString(): String {
        return "(${v1.label}-${v2.label})"
    }
}