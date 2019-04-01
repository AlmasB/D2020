package groupnet.ui

import groupnet.diagram.EulerDiagramCreator
import groupnet.diagram.GNDiagramCreator
import groupnet.euler.Description
import groupnet.euler.EulerDiagram
import groupnet.gn.GNDescription
import groupnet.gn.GNDiagram
import groupnet.util.Bug
import groupnet.util.Log

/**
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */

abstract class VisTask<T> {

    private var diagram: T? = null

    fun run() {
        diagram = generate()
    }

    fun render() {
        render(diagram ?: throw Bug("No diagram was generated"))
    }

    protected abstract fun generate(): T
    protected abstract fun render(diagram: T)
}

class EulerDiagramVisTask(val description: Description, val renderer: Renderer) : VisTask<EulerDiagram>() {

    override fun generate(): EulerDiagram {
        Log.i("Drawing: ", description.getInformalDescription())

        return EulerDiagramCreator().drawEulerDiagram(description)
    }

    override fun render(diagram: EulerDiagram) {
        renderer.renderAsImage(diagram)
    }
}

class GNDiagramVisTask(val description: GNDescription, val renderer: Renderer) : VisTask<GNDiagram>() {

    override fun generate(): GNDiagram {
        Log.i("Drawing: ", description)

        val gnd = GNDiagramCreator().drawGroupedNetworkDiagram(description)

        return gnd
    }

    override fun render(diagram: GNDiagram) {
        renderer.renderAsImage(diagram)
    }
}