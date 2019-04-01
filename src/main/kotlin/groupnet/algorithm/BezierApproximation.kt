package groupnet.algorithm

import groupnet.algorithm.bezier.ClosedBezierSpline
import javafx.geometry.Point2D
import javafx.scene.paint.Color
import javafx.scene.shape.CubicCurveTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path

/**
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
object BezierApproximation {

    fun smoothPath2(originalPoints: MutableList<Point2D>): List<Path> {

        val pair = ClosedBezierSpline.GetCurveControlPoints(originalPoints.toTypedArray())

        val points = originalPoints.plus(originalPoints[0])

        val result = arrayListOf<Path>()

        val firstPt = 0
        val lastPt = points.size

        for (i in firstPt..lastPt - 2){

            val path = Path()

            path.elements.add(MoveTo(points[i].x, points[i].y))

            val j = if (i == lastPt - 2) 0 else i + 1

            path.elements.add(CubicCurveTo(
                    pair.key[i].x, pair.key[i].y,
                    pair.value[j].x, pair.value[j].y,
                    //controlPts[i].second.x, controlPts[i].second.y,
                    //controlPts[i+1].first.x, controlPts[i+1].first.y,
                    points[i+1].x, points[i+1].y)
            )


            path.fill = Color.TRANSPARENT
            result.add(path)
        }

        return result
    }
}