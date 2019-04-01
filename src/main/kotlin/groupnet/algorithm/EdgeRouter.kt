package groupnet.algorithm

import groupnet.euler.Zone
import javafx.scene.shape.Polyline

/**
 *
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
interface EdgeRouter {

    /**
     * Route an edge between two topologically adjacent zones.
     */
    fun route(zone1: Zone, zone2: Zone): Polyline
}