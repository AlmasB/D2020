package groupnet.diagram

import groupnet.algorithm.BezierApproximation
import groupnet.decomposition.dec
import groupnet.decomposition.decA
import groupnet.euler.*
import groupnet.euler.curves.CircleCurve
import groupnet.euler.curves.PathCurve
import groupnet.euler.dual.MED
import groupnet.euler.dual.MEDCycle
import groupnet.recomposition.PiercingData
import groupnet.recomposition.RecompositionStep
import groupnet.util.Bug
import groupnet.util.Log
import groupnet.util.Profiler
import javafx.geometry.Point2D
import javafx.scene.paint.Color
import javafx.scene.shape.ClosePath
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import java.util.stream.Stream

/**
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class EulerDiagramCreator {

    companion object {
        @JvmField val BASE_RADIUS = 1500.0

        val RADIUS_REDUCTION = 2.0

        private val zoneScores = hashMapOf<AbstractZone, Int>()
    }

    /**
     * Abstract zones we have processed so far.
     */
    private val abstractZones = mutableSetOf<AbstractZone>()

    /**
     * The diagram we generated so far.
     */
    private var d: EulerDiagram = EulerDiagram(D0, D0, emptySet())

    fun drawEulerDiagram(D0: Description): EulerDiagram {
        zoneScores.clear()

        val D = D0



        val components = decA(D)

        // D is atomic
        if (components.size == 1) {
            return drawAtomicDiagram(D, dec(D).reversed())
        }

        // compute zone scores
        components.filter { it.parent != azEmpty }
                .onEach { zoneScores[it.parent] = 0 }
                .forEach { zoneScores[it.parent] = zoneScores[it.parent]!! + 1 + it.labels.size }

        val diagrams = components.map { EulerDiagramCreator().drawAtomicDiagram(it, dec(it).reversed()) }

        val initial = diagrams[0]

        return diagrams.drop(1).fold(initial, { d1, d2 ->
            val az = d2.originalDescription.parent
            val score = getScore(az)
            d1.drawIntoZone(az, d2, score)
        })
    }

    private fun getScore(az: AbstractZone): Int {
        return zoneScores[az] ?: 0
    }

    // in case we need to generate from a Decomposition dec
    // val description = dec.steps.last().to
    // val steps = dec.steps
    internal fun drawAtomicDiagram(D: Description, steps: List<RecompositionStep>): EulerDiagram {
        steps.forEach { data ->
            val curve = drawCurve(data)
            d = EulerDiagram(D, D(abstractZones + azEmpty, D.parent), d.curves + curve)
        }

        updateLabelPositions()

        Log.d("drawAtomicDiagram() done: $abstractZones", d)

        return d
    }

    /**
     * Side-effects:
     *
     * 1. creates MED
     * 2. chooses a cycle
     * 3. creates a curve
     * 4. updates abstract zones
     */
    private fun drawCurve(data: RecompositionStep): Curve {
        Log.d("Draw curve given abstract ZON: ${data.splitZones}")

        var curve: Curve? = null

        if (numCurvesSoFar() == 0) {
            curve = CircleCurve(data.newLabel, BASE_RADIUS, BASE_RADIUS, BASE_RADIUS)

        } else if (data.isMaybeSinglePiercing()) {
            curve = tryDrawSinglePiercing(data)

        } else if (data.isMaybeDoublePiercing()) {
            curve = tryDrawDoublePiercing(data)

        } else if (data.isNested()) {
            throw Bug("Nested curve [$data] cannot exist in an atomic diagram")

        }

        if (curve != null) {
            abstractZones.addAll(data.splitZones.map { it + data.newLabel })
        } else {
            Profiler.start("Creating MED")

            val modifiedDual = MED(d)

            Profiler.end("Creating MED")

            var azPair: Pair<AbstractZone, AbstractZone>? = null

            if (data.connections != null) {
                // best az pair to keep adjacency for this step
                azPair = data.connections!!
                        .filterKeys {
                            it.first in abstractZones && it.second in abstractZones && it.first.isNeighbour(it.second)
                        }
                        .filterValues { it > 0 }
                        .maxBy { it.value }
                        ?.key
            }

            val cycle = modifiedDual.computeCycle(data.splitZones, azPair) ?: throw Bug("Failed to find cycle")

            Log.d("Adding ${data.newLabel} using cycle $cycle")

            curve = when (cycle.lengthUnique()) {
                2 -> drawSinglePiercing(data.newLabel, cycle.nodesUnique().map { it.zone })

            // here a 2node or 3node cycle was upgraded to 4node
                4 -> drawDoublePiercing(data.newLabel, cycle.nodesUnique().map { it.zone })

                else -> PathCurve(data.newLabel, smooth(cycle))
            }

            // we might've used more zones to get a cycle, so we make sure we capture all of the used ones
            // we also call distinct() to ensure we don't reuse the outside zone more than once
            abstractZones.addAll(cycle.nodes.map { it.zone.az + data.newLabel }.distinct())
        }

        return curve
    }

    private fun tryDrawSinglePiercing(data: RecompositionStep): Curve? {
        // we include outsideZone in case
        val piercingData = PiercingData(2, data.splitZones.map { d.getZone(it) }, d.zones.plus(d.outsideZone).toList())
        if (!piercingData.isPiercing())
            return null

        val splitZones = data.splitZones.toList()

        val az0 = splitZones[0]
        val az1 = splitZones[1]

        val newZones = data.splitZones.map { it + (data.newLabel) }

        val az2 = newZones[0]
        val az3 = newZones[1]

        val score0 = getScore(az0)
        val score1 = getScore(az1)
        val score2 = getScore(az2)
        val score3 = getScore(az3)

        val curveBeingPierced = d.curves.find { it.label == az2.getStraddledLabel(az3).get() }!!

        return if (numCurvesSoFar() == 1) {

            val center = curveBeingPierced.getPolygon().centroid()
            val vectorToCenter = Point2D(center.x(), center.y())
                    .subtract(Point2D(BASE_RADIUS * 2, BASE_RADIUS))
                    .normalize()

            val x: Double

            if (score2 == score3) {
                // keep original
                x = BASE_RADIUS * 2
            } else if (score2 > score3) {

                if (az2.labels.size > az3.labels.size) {
                    x = BASE_RADIUS * 2 + vectorToCenter.x * BASE_RADIUS / 2
                } else {
                    x = BASE_RADIUS * 2 - vectorToCenter.x * BASE_RADIUS / 2
                }

            } else { // score2 < score3

                if (az2.labels.size > az3.labels.size) {
                    x = BASE_RADIUS * 2 - vectorToCenter.x * BASE_RADIUS / 2
                } else {
                    x = BASE_RADIUS * 2 + vectorToCenter.x * BASE_RADIUS / 2
                }
            }

            // special: use slightly better position and size
            CircleCurve(data.newLabel, x, BASE_RADIUS, BASE_RADIUS)
            //CircleCurve(data.addedCurve, BASE_RADIUS * 2, BASE_RADIUS, BASE_RADIUS)
        } else {

            val center = curveBeingPierced.getPolygon().centroid()
            val vectorToCenter = Point2D(center.x(), center.y())
                    .subtract(Point2D(piercingData.center!!.x, piercingData.center.y))
                    .normalize()

            val x: Double
            val y: Double

            val r = piercingData.radius / RADIUS_REDUCTION

            if (score2 == score3) {
                // keep original
                x = piercingData.center!!.x
                y = piercingData.center!!.y
            } else if (score2 > score3) {

                if (az2.labels.size > az3.labels.size) {
                    x = piercingData.center!!.x + vectorToCenter.x * r / 2
                    y = piercingData.center!!.y + vectorToCenter.y * r / 2
                } else {
                    x = piercingData.center!!.x - vectorToCenter.x * r / 2
                    y = piercingData.center!!.y - vectorToCenter.y * r / 2
                }

            } else { // score2 < score3

                if (az2.labels.size > az3.labels.size) {
                    x = piercingData.center!!.x - vectorToCenter.x * r / 2
                    y = piercingData.center!!.y - vectorToCenter.y * r / 2
                } else {
                    x = piercingData.center!!.x + vectorToCenter.x * r / 2
                    y = piercingData.center!!.y + vectorToCenter.y * r / 2
                }
            }

            CircleCurve(data.newLabel, x, y, piercingData.radius / RADIUS_REDUCTION)
        }
    }

    private fun tryDrawDoublePiercing(data: RecompositionStep): Curve? {
        // we don't include outsideZone because there are other zones that bound
        val piercingData = PiercingData(4, data.splitZones.map { d.getZone(it) }, d.zones.toList())
        if (!piercingData.isPiercing())
            return null

        return if (numCurvesSoFar() == 2) {
            // special: use slightly better position and size
            CircleCurve(data.newLabel, BASE_RADIUS * 1.5, BASE_RADIUS * 2, BASE_RADIUS)
        } else {
            CircleCurve(data.newLabel, piercingData.center!!.x, piercingData.center.y, piercingData.radius / RADIUS_REDUCTION)
        }
    }

    private fun drawSinglePiercing(abstractCurve: Label, regions: List<Zone>): Curve {
        val piercingData = PiercingData(2, regions, d.zones.plus(d.outsideZone).toList())

        if (!piercingData.isPiercing()) {
            throw Bug("not 1-piercing")
        }

        return CircleCurve(abstractCurve, piercingData.center!!.x, piercingData.center.y, piercingData.radius / RADIUS_REDUCTION)
    }

    private fun drawDoublePiercing(abstractCurve: Label, regions: List<Zone>): Curve {
        val piercingData = PiercingData(4, regions, d.zones.toList())

        if (!piercingData.isPiercing()) {
            throw Bug("not 2-piercing")
        }

        return CircleCurve(abstractCurve, piercingData.center!!.x, piercingData.center.y, piercingData.radius / RADIUS_REDUCTION)
    }

    private fun smooth(cycle: MEDCycle): Path {
        Profiler.start("Smoothing")

        val pathSegments = BezierApproximation.smoothPath2(cycle.polygon)

        val newPath = Path()

        val firstPt = cycle.polygon[0]

        // add moveTo
        newPath.elements.add(MoveTo(firstPt.x, firstPt.y))

        for (path in pathSegments) {
            // drop the first moveTo
            newPath.elements.addAll(path.elements.drop(1))
        }

        newPath.fill = Color.TRANSPARENT
        newPath.elements.add(ClosePath())

        Profiler.end("Smoothing")

        return newPath
    }

    private fun numCurvesSoFar() = C(d).size

    private fun updateLabelPositions() {
        Profiler.start("Compute labels")

        Stream.of(*C(d).toTypedArray())
                .parallel()
                .forEach { updateLabelPosition(it) }

        Profiler.end("Compute labels")
    }

    private fun updateLabelPosition(curve: Curve) {
        val otherCurves = C(d).minus(curve)
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
}