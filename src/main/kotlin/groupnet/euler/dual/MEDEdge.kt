package groupnet.euler.dual

import javafx.scene.shape.Shape

/**
 *
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class MEDEdge(val v1: MEDVertex, val v2: MEDVertex, val shape: Shape) {

    var isShown = false

    override fun toString(): String {
        return "($v1 -> $v2)"
    }
}