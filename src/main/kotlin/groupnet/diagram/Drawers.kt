package groupnet.diagram

import groupnet.euler.EulerDiagram
import groupnet.euler.V
import groupnet.euler.Z
import groupnet.euler.azEmpty
import groupnet.gn.GNDescription
import groupnet.gui.SettingsController
import groupnet.network.NetworkEdge
import groupnet.network.NetworkGraph
import groupnet.network.NetworkNode
import groupnet.network.Parameter
import groupnet.util.*
import javafx.geometry.Point2D
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Line
import math.geom2d.polygon.MultiPolygon2D
import java.lang.Math.*
import java.util.*

/**
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */

fun drawGraphForAtomicComponent(GND: GNDescription, d: EulerDiagram): NetworkGraph {
    if (GND.nodes.isEmpty())
        return NetworkGraph(emptySet(), emptySet())

    if (GND.nodes.size == 1) {
        val v = GND.nodes.first()
        val z = (Z(d) + d.outsideZone).find { it.az == GND.aloc(v) }!!

        val node = NetworkNode(v, z, z.visualCenter)
        return NetworkGraph(setOf(node), emptySet())
    }

    val V = arrayListOf<NetworkNode>()
    val E = arrayListOf<NetworkEdge>()

    val random = Random()

    GND.mapping.forEach { az, nodes ->

        (Z(d) + d.outsideZone).find { it.az == az }!!.let { z ->
            nodes.forEach {
                val p = z.visualCenter.add(random.nextDouble() * 20.0, random.nextDouble() * 20.0)
                val v = NetworkNode(it, z, p)

                V += v
            }
        }
    }

    GND.edges.forEach { e ->
        val v1 = V.find { it.label == e.first }
        val v2 = V.find { it.label == e.second }

        if (v1 != null && v2 != null) {
            E += NetworkEdge(v1, v2)
        }
    }

    return NetworkGraph(V, E)
}

fun drawGraph(GND: GNDescription, d: EulerDiagram): NetworkGraph {
    Log.d("Drawing network from ${GND.graph}")

    Profiler.start("Drawing network")

    if (GND.nodes.isEmpty())
        return NetworkGraph(emptySet(), emptySet())

    if (GND.nodes.size == 1) {
        val v = GND.nodes.first()
        val z = (Z(d) + d.outsideZone).find { it.az == GND.aloc(v) }!!

        val node = NetworkNode(v, z, z.visualCenter)
        return NetworkGraph(setOf(node), emptySet())
    }

    val V = arrayListOf<NetworkNode>()
    val E = arrayListOf<NetworkEdge>()

    val random = Random()

    GND.mapping.forEach { az, nodes ->

        (Z(d) + d.outsideZone).find { it.az == az }!!.let { z ->
            nodes.forEach {
                val p = z.visualCenter.add(random.nextDouble() * 20.0, random.nextDouble() * 20.0)
                val v = NetworkNode(it, z, p)

                V += v
            }
        }
    }

    GND.edges.forEach { e ->
        val v1 = V.find { it.label == e.first }
        val v2 = V.find { it.label == e.second }

        if (v1 != null && v2 != null) {
            E += NetworkEdge(v1, v2)
        }
    }

    val dbox = d.bbox()

    val p = Parameter()
    p.frameWidth = min(dbox.width.toInt(), 1400)
    p.frameHeight = min(dbox.height.toInt(), 1400)
    p.isEquilibriumCriterion = true
    p.criterion = 15.0
    p.coolingRate = 0.01
    p.frameDelay = 5

    val graph = NetworkGraph(V, E)

    Simulation(graph, p).run()

    V(graph).forEach {
        Log.i("Placed $it at ${it.pos}")
    }

    Profiler.end("Drawing network")

    return graph
}

fun layoutGraph(graph: NetworkGraph) {
    val p = Parameter()
    p.frameWidth = 1000
    p.frameHeight = 1000
    p.isEquilibriumCriterion = true
    p.criterion = 15.0
    p.coolingRate = 0.01
    p.frameDelay = 5

    p.attractiveForce = { d, k -> d / k }
    p.repulsiveForce = { d, k -> k * k / d }

    V(graph).forEach {
        it.pos = Point2D(random(), random()).multiply(10.0)
    }

    Simulation(graph, p).run()
}

/**
 * Adapted from https://github.com/Benjoyo/ForceDirectedPlacement under MIT
 *
 * https://github.com/gephi/gephi/wiki/Fruchterman-Reingold
 */
class Simulation(private val graph: NetworkGraph, private val p: Parameter) {

    companion object {

        private const val C = 0.4
    }

    private val frameWidth: Int
    private val frameHeight: Int
    private val equi: Boolean
    private val criterion: Double
    private val coolingRate: Double

    private var iteration = 0

    private var area: Int = 0
    private var k: Double = 0.0
    private var t: Double = 0.0

    private var equilibriumReached = false

    init {
        this.frameWidth = p.frameWidth
        this.frameHeight = p.frameHeight
        this.equi = p.isEquilibriumCriterion
        this.criterion = p.criterion
        this.coolingRate = p.coolingRate
    }

    /**
     * Starts the simulation.
     *
     * @return number of iterations used until criterion is met
     */
    fun run() {
        iteration = 0
        equilibriumReached = false

        area = min(frameWidth * frameWidth, frameHeight * frameHeight)

        k = C * sqrt((area / graph.nodes.size).toDouble())
        t = (frameWidth / 10).toDouble()

        if (equi) {
            // simulate until mechanical equilibrium
            while (!equilibriumReached && iteration < 250) {
                simulateStep()
            }
        } else {
            // simulate iterations-steps
            var i = 0
            while (i < criterion) {
                simulateStep()
                i++
            }
        }
    }

    /**
     * Simulates a single step.
     */
    private fun simulateStep() {
        // calculate repulsive forces (from every vertex to every other)
        for (v in graph.nodes) {
            // reset displacement vector for new calculation
            v.vel = Point2D.ZERO
            for (u in graph.nodes) {
                if (v != u) {
                    // normalized difference position vector of v and u

                    var delta = v.pos.subtract(u.pos)

                    val length = delta.magnitude()

                    // displacement depending on repulsive force
                    delta = delta.normalize().multiply(p.repulsiveForce(length, k))

                    v.applyAcceleration(delta)
                }
            }

            if (v.isGNDBased()) {
                applyZoneForcesEdges(v)
            }
        }

        // calculate attractive forces (only between neighbors)
        for (e in graph.edges) {

            // normalized difference position vector of v and u
            var delta = e.v1.pos.subtract(e.v2.pos)

            val length = delta.magnitude()

            // displacements depending on attractive force

            delta = delta.normalize().multiply(p.attractiveForce(length, k))

            if (e.v1.isGNDBased() && e.v2.isGNDBased()) {

                if (e.v1.z!!.az == e.v2.z!!.az) {

                    // push v1 to v2
                    e.v1.applyAcceleration(delta.multiply(-1.0))

                    // push v2 to v1
                    e.v2.applyAcceleration(delta)
                }

            } else {

                // push v1 to v2
                e.v1.applyAcceleration(delta.multiply(-1.0))

                // push v2 to v1
                e.v2.applyAcceleration(delta)
            }
        }

        //applyEdgeEdgeForces()

        applyEdgeNodeForces()

        // assume equilibrium
        equilibriumReached = true

        for (v in graph.nodes) {

            if (v.isGNDBased()) {
                applyZoneCenterForces(v)
            }

            val length = v.vel.magnitude()

            // no equilibrium if one vertex has too high net force
            if (length > criterion) {
                equilibriumReached = false
            }

            // limit maximum displacement by temperature t
            val disp = v.vel.normalize().multiply(min(length, t))

            v.applyVelocity(disp)

            // undo if invalid placement
            if (v.isGNDBased() && !v.hasValidPlacement()) {
                v.applyVelocity(disp.multiply(-1.0))
            }
        }

        // reduce the temperature as the layout approaches a better
        // configuration but always let vertices move at least 1px
        t = max(t * (1 - coolingRate), 1.0)

        iteration++
    }

    private fun applyEdgeEdgeForces() {
        // custom forces for edge crossings
        combinations2(graph.edges).forEach { pair ->
            val e1 = pair.first
            val e2 = pair.second

            if (!e1.isAdjacent(e2) && e1.crosses(e2)) {

                val v1 = if (e1.v1.vectorToEdge(e2).magnitude() < e1.v2.vectorToEdge(e2).magnitude()) e1.v1 else e1.v2
                val v2 = if (e2.v1.vectorToEdge(e1).magnitude() < e2.v2.vectorToEdge(e1).magnitude()) e2.v1 else e2.v2

                var force: Double

                if (v1.vectorToEdge(e2).magnitude() < v2.vectorToEdge(e1).magnitude()) {
                    var vector = v1.vectorToEdge(e2)

                    force = vector.magnitude()
                    force = force * force * force * force

                    vector = vector.normalize().multiply(force)

                    e1.v1.applyAcceleration(vector)
                    e1.v2.applyAcceleration(vector)

                    e2.v1.applyAcceleration(vector.multiply(-1.0))
                    e2.v2.applyAcceleration(vector.multiply(-1.0))

                } else {
                    var vector = v2.vectorToEdge(e1)

                    force = vector.magnitude()
                    force = force * force * force * force

                    vector = vector.normalize().multiply(force)

                    e2.v1.applyAcceleration(vector)
                    e2.v2.applyAcceleration(vector)

                    e1.v1.applyAcceleration(vector.multiply(-1.0))
                    e1.v2.applyAcceleration(vector.multiply(-1.0))
                }
            }
        }
    }

    private fun applyEdgeNodeForces() {
        for (e in graph.edges) {
            for (v in graph.nodes) {
                if (v.isIncidentWith(e))
                    continue

                val vector = v.vectorToEdge(e)

                val vectorToAdd = vector.multiply(-1.0).normalize().multiply(p.repulsiveForce(vector.magnitude(), k))

                v.applyAcceleration(vectorToAdd)
            }
        }
    }

    private fun applyZoneCenterForces(v: NetworkNode) {
        val zone = v.z!!

        if (zone.az == azEmpty)
            return

        val vector = zone.visualCenter.subtract(v.pos)

        val vectorToAdd = vector.normalize().multiply(p.attractiveForce(vector.magnitude() * 0.5 * graph.degreeOf(v), k))

        v.applyAcceleration(vectorToAdd)
    }

    private fun applyZoneForcesEdges(v: NetworkNode) {
        val polygon = v.z!!.polygonShape
        val dist = abs(polygon.boundary().signedDistance(v.x, v.y))

        if (dist > 1000)
            return

        polygon.edges()
                .map {
                    val midpoint = Point2D(it.firstPoint().x(), it.firstPoint().y()).midpoint(Point2D(it.lastPoint().x(), it.lastPoint().y()))
                    val v = Point2D(it.lastPoint().x() - it.firstPoint().x(), it.lastPoint().y() - it.firstPoint().y())

                    Tuple3(it, midpoint, v)
                }
                .forEach { (e, midpoint, vector) ->
                    var normal = Point2D(vector.y, -vector.x)

                    val testPt = midpoint.add(normal)




                    if (polygon is MultiPolygon2D) {

                        // check signed distance and also of the complement
                        val dist1 = Math.abs(polygon.boundary().signedDistance(testPt.x, testPt.y))
                        val dist2 = Math.abs(polygon.complement().boundary().signedDistance(testPt.x, testPt.y))

                        if (Math.min(dist1, dist2) > 0) {
                            normal = normal.multiply(-1.0)
                        }
                    } else {

                        // if test point lies outside of polygon then invert the normal
                        if (polygon.boundary().signedDistance(testPt.x, testPt.y) > 0) {
                            normal = normal.multiply(-1.0)
                        }
                    }






                    val debugNormals = false
                    if (debugNormals) {
                        val l = Line(midpoint.x, midpoint.y, midpoint.x + normal.x, midpoint.y + normal.y)

                        val color = Color.color(Math.random(), Math.random(), Math.random())

                        l.stroke = color
                        l.strokeWidth = 10.0

                        SettingsController.debugNodes.addAll(
                                listOf(l, Circle(midpoint.x, midpoint.y, 25.0, color))
                        )
                    }





                    val distToEdge = abs(e.signedDistance(v.x, v.y))
                    //if (distToEdge < 75) {

                        normal = normal.normalize().multiply(
                                k * k * k / (distToEdge * distToEdge)
                        )

                        v.applyAcceleration(normal)
                    //}
                }
    }
}
