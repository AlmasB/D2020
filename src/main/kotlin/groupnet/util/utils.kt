package groupnet.util

import groupnet.euler.Zone
import javafx.geometry.Point2D
import javafx.geometry.Rectangle2D
import math.geom2d.polygon.MultiPolygon2D
import math.geom2d.polygon.Polygon2D
import java.lang.Math.abs
import java.lang.Math.sqrt

fun L(letter: Char) = letter.toString()

interface Formalizable {

    fun toFormal(): String
    fun toInformal(): String
}

open class Settings(
        private val threading: Boolean = false,
        private val smooth: Int = 10,
        private val MED: Boolean = false,
        private val cache: Boolean = false) {

    /**
     * Use multithreading.
     */
    open fun isParallel(): Boolean = threading

    /**
     * How smooth should curves be.
     * Value of 0 will disable smoothing.
     */
    open fun smoothFactor(): Int = smooth

    /**
     * Show modified dual?
     */
    open fun showMED(): Boolean = MED

    /**
     * Use diagram library to cache and retrieve diagrams.
     * Also when building if parts of diagrams already exist in the library,
     * use them.
     */
    open fun useLibrary(): Boolean = cache
}

fun Point2D.negate(): Point2D {
    return this.multiply(-1.0)
}

fun Rectangle2D.center(): Point2D {
    return Point2D((this.minX + this.maxX) / 2, (this.minY + this.maxY) / 2)
}

fun Rectangle2D.distToCorner(): Double {
    return Math.sqrt(this.width * this.width + this.height * this.height) / 2
}

fun bbox(zones: Collection<Zone>): Rectangle2D {
    val bounds = zones.map { it.polygonShape.boundingBox() }

    val minX = bounds.map { it.minX }.min()!!
    val minY = bounds.map { it.minY }.min()!!
    val maxX = bounds.map { it.maxX }.max()!!
    val maxY = bounds.map { it.maxY }.max()!!

    return Rectangle2D(minX, minY, maxX - minX, maxY - minY)
}

/**
 * @return lexicographically ordered String
 */
fun String.normalize(): String {
    val set = this.map { "$it" }.toSortedSet()

    return set.joinToString("")
}

data class Tuple3<T, U, R>(val comp1: T, val comp2: U, val comp3: R)

class Bug(message: String) : RuntimeException(message)

fun <T> Set<T>.symmetricDifference(other: Set<T>): Set<T> {
    return (this - other) + (other - this)
}

fun distancePolygonPoint(polygon: Polygon2D, p: Point2D): Double {
    if (polygon is MultiPolygon2D) {

        // check signed distance and also of the complement
        val dist1 = Math.abs(polygon.boundary().signedDistance(p.x, p.y))
        val dist2 = Math.abs(polygon.complement().boundary().signedDistance(p.x, p.y))

        return abs(Math.min(dist1, dist2))
    } else {
        return abs(polygon.boundary().signedDistance(p.x, p.y))
    }
}

/**
 * @return angle in [0..360]
 */
fun vectorToAngle(v: Point2D): Double {
    var angle = -Math.toDegrees(Math.atan2(v.y, v.x))

    if (angle < 0) {
        val delta = 180 - (-angle)
        angle = delta + 180
    }

    return angle
}

fun vectorFromAngle(angle: Double): Point2D {
    return Point2D(Math.cos(Math.toRadians(angle)), Math.sin(Math.toRadians(angle)))
}

// from https://stackoverflow.com/questions/5227373/minimal-perpendicular-vector-between-a-point-and-a-line
fun vectorFromPointToLine(p: Point2D, lineStart: Point2D, lineEnd: Point2D): Point2D {
    // (P-A).D == |X-A|

    val d = lineEnd.subtract(lineStart).normalize()

    //X == A + ((P-A).D)D
    val x = lineStart.add(d.multiply(p.subtract(lineStart).dotProduct(d)))

    return x.subtract(p)
}

// from https://stackoverflow.com/questions/1073336/circle-line-segment-collision-detection-algorithm
fun circleIntersectsLine(r: Double, circleCenter: Point2D, lineStart: Point2D, lineEnd: Point2D): Boolean {

    val d = lineEnd.subtract(lineStart)
    val f = lineStart.subtract(circleCenter)


    val a = d.dotProduct(d)
    val b = 2 * f.dotProduct(d)
    val c = f.dotProduct(f) - r * r

    var discriminant = b * b - 4f * a * c
    if (discriminant < 0) {
        // no intersection
        return false
    } else {
        // ray didn't totally miss sphere,
        // so there is a solution to
        // the equation.

        discriminant = sqrt(discriminant)

        // either solution may be on or off the ray so need to test both
        // t1 is always the smaller value, because BOTH discriminant and
        // a are nonnegative.
        val t1 = (-b - discriminant) / (2 * a)
        val t2 = (-b + discriminant) / (2 * a)

        // 3x HIT cases:
        //          -o->             --|-->  |            |  --|->
        // Impale(t1 hit,t2 hit), Poke(t1 hit,t2>1), ExitWound(t1<0, t2 hit),

        // 3x MISS cases:
        //       ->  o                     o ->              | -> |
        // FallShort (t1>1,t2>1), Past (t1<0,t2<0), CompletelyInside(t1<0, t2>1)

        if (t1 >= 0 && t1 <= 1) {
            // t1 is the intersection, and it's closer than t2
            // (since t1 uses -b - discriminant)
            // Impale, Poke
            return true
        }

        // here t1 didn't intersect so we are either started
        // inside the sphere or completely past it
        if( t2 >= 0 && t2 <= 1 )
        {
            // ExitWound
            return true
        }

        return false
    }
}

// From https://stackoverflow.com/questions/3838329/how-can-i-check-if-two-segments-intersect

fun lineIntersectsLine(A: Point2D, B: Point2D, C: Point2D, D: Point2D): Boolean {
    return ccw(A,C,D) != ccw(B,C,D) && ccw(A,B,C) != ccw(A,B,D)
}

fun ccw(A: Point2D, B: Point2D, C: Point2D): Boolean {
    return (C.y-A.y) * (B.x-A.x) > (B.y-A.y) * (C.x-A.x)
}

fun numIntersectionsLinePolygon(A: Point2D, B: Point2D, p: Polygon2D): Int {
    return p.edges().map { Point2D(it.firstPoint().x(), it.firstPoint().y()) to Point2D(it.lastPoint().x(), it.lastPoint().y()) }
            .filter { lineIntersectsLine(A, B, it.first, it.second) }
            .count()
}