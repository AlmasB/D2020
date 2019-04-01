package groupnet.recomposition

import groupnet.euler.AbstractZone
import groupnet.euler.Description
import groupnet.euler.L
import groupnet.euler.Label

/**
 * Single recomposition step.
 * A valid step has following features:
 *
 * 1. The added contour data must be a single curve
 * 2. Added curve must NOT be present in previous description and must be present in the next one
 *
 * Note: number of steps == number of curves.
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class RecompositionStep(

        /**
         * @return description before this step
         */
        val from: Description,

        /**
         * @return description after this step
         */
        val to: Description,

        /**
         * The curve added at this step.
         */
        val newLabel: Label,
        /**
         * The zones we split at this step. The zones are
         * in the "from" abstract description.
         */
        val splitZones: Set<AbstractZone>) {

    // for GND cycle selection strategy
    var connections: Map<Pair<AbstractZone, AbstractZone>, Int>? = null

    init {
        val l = newLabel

        require(l !in L(from)) { "$l is already present in $from" }
        require(l in L(to)) { "$l is not present in $to" }
    }

    override fun toString() = "R_Step[added=$newLabel,split=$splitZones,From=$from To=$to]"

    /**
     * @return true iff the added curve is nested (splits 1 zone)
     */
    fun isNested() = splitZones.size == 1

    fun isMaybeSinglePiercing() = splitZones.size == 2

    fun isMaybeDoublePiercing() = splitZones.size == 4

    fun isNotPiercing() = splitZones.size > 4
}