package groupnet.network

/**
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */

/**
 * From https://github.com/Benjoyo/ForceDirectedPlacement under MIT
 *
 * Container class for holding parameters for a simulation.
 * @author Bennet
 */
class Parameter {

    var frameWidth: Int = 0
    var frameHeight: Int = 0
    var isEquilibriumCriterion: Boolean = false
    var criterion: Double = 0.0
    var coolingRate: Double = 0.0
    var frameDelay: Int = 0

    var attractiveForce: (Double, Double) -> Double = { d, k -> d * d / k }
    var repulsiveForce: (Double, Double) -> Double = { d, k -> k * k * k / (d) }
}