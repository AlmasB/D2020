package groupnet.decomposition

import groupnet.gn.GNDescription
import javafx.scene.Node
import javafx.scene.Parent

/**
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class DecompositionTree(root: GNDescription) {

    private val root = TreeVertex(root, 0, false)
    private val vertices = mutableSetOf<TreeVertex>(this.root)

    fun addChildren(GND1: GNDescription, GND2: GNDescription, parent: GNDescription) {
        val parentV = vertices.first { it.value == parent }

        val v1 = TreeVertex(GND1, parentV.depth + 1, isLeftChild = true)
        val v2 = TreeVertex(GND2, parentV.depth + 1, isLeftChild = false)

        parentV.childrenMutable.add(v1)
        parentV.childrenMutable.add(v2)

        vertices.add(v1)
        vertices.add(v2)
    }

    fun siblings(): Set<Pair<TreeVertex, TreeVertex>> {
        return (vertices - root).groupBy { it.parent }
                .values
                .map { it[0] to it[1] }
                .toSet()
    }

    fun vertices(): Set<GNDescription> {
        return vertices.map { it.value }.toSet()
    }

    fun leaves(): Set<GNDescription> {
        return vertices.filter { it.childrenMutable.isEmpty() }.map { it.value }.toSet()
    }

    override fun toString(): String {
        return siblings().joinToString("\n") { (v1, v2) -> "${v1.value.description} + (${v2.value.description.parent}) ${v2.value.description} = ${v1.parent.value.description}" }
    }

    class TreeVertex(val value: GNDescription, val depth: Int, val isLeftChild: Boolean) : Parent() {
        val childrenMutable: MutableList<Node> get() = this.children

        val parent: TreeVertex get() = parentProperty().value as TreeVertex
    }
}