package groupnet.euler

import groupnet.euler.dual.MEDCycle
import groupnet.gn.GNDescription
import groupnet.network.NetworkEdge
import groupnet.network.NetworkGraph
import groupnet.network.NetworkNode
import groupnet.util.Bug
import javafx.geometry.Point2D

/**
 * Defines domain specific language API.
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */

// AbstractZone DSL

val azEmpty = AbstractZone.OUTSIDE

fun az(labels: Set<Label>) = AbstractZone(labels)
fun az(informalForm: String) = AbstractZone.from(informalForm)

operator fun AbstractZone.contains(label: Label) = label in labels

operator fun AbstractZone.plus(label: Label) = az(this.labels + label)

operator fun AbstractZone.minus(label: Label) = az(this.labels - label)

operator fun AbstractZone.minus(labels: Collection<Label>) = az(this.labels - labels)

operator fun AbstractZone.plus(other: AbstractZone) = az(this.labels + other.labels)

operator fun AbstractZone.minus(other: AbstractZone) = az(this.labels - other.labels)

// Description DSL

val D0 = D("")

fun D(informalDescription: String) = Description.from(informalDescription)

fun D(informalDescription: String, parent: AbstractZone) = Description.from(informalDescription, parent)

fun D(description: Description, parent: AbstractZone) = Description.from(description.getInformalDescription(), parent)

fun D(abstractZones: Set<AbstractZone>) = Description(abstractZones)

fun D(abstractZones: Set<AbstractZone>, parent: AbstractZone): Description {
    val D = Description(abstractZones)
    D.parent = parent
    return D
}

fun Z(D: Description): Set<AbstractZone> = D.abstractZones

fun L(D: Description): Set<Label> = D.labels

operator fun Description.minus(label: Label): Description {
    return D(Z(this).map { it - label }.toSet())
}

/**
 * 'Slots' [D] using its parent zone into this description.
 */
operator fun Description.plus(D: Description): Description {
    return this + (D.parent to D)
}

operator fun Description.plus(pair: Pair<AbstractZone, Description>): Description {
    val D1 = this
    val (az1, D2) = pair

    if (az1 !in Z(D1))
        throw Bug("Cannot slot $D2 into $D1. No az $az1 in $D1")

    val D = D(Z(D1) + Z(D2).map { it + az1 })
    D.parent = D1.parent
    return D
}

// EulerDiagram DSL

fun C(d: EulerDiagram) = d.curves

fun Z(d: EulerDiagram) = d.zones

fun combine(original: Description, diagrams: List<EulerDiagram>): EulerDiagram {
    val actual = diagrams.map { it.actualDescription.getInformalDescription() }.joinToString(" ")

    println(actual)
    println(original)

    var index = 0

    return EulerDiagram(original, D(actual), diagrams.flatMap {
        val translate = Point2D((index % 3) * 7000.0, (index++ / 3) * 7000.0)

        it.curves.map { it.translate(translate) }
    }.toSet())
}

fun combineNoTranslate(original: Description, diagrams: List<EulerDiagram>): EulerDiagram {
    val actual = diagrams.map { it.actualDescription.getInformalDescription() }.joinToString(" ")

    println(actual)
    println(original)

    var index = 0

    return EulerDiagram(original, D(actual), diagrams.flatMap {
        val translate = Point2D.ZERO

        it.curves.map { it.translate(translate) }
    }.toSet())
}

// MEDCycle DSL

fun V(cycle: MEDCycle) = cycle.nodes

fun E(cycle: MEDCycle) = cycle.edges

// GNDescription DSL

fun D(GND: GNDescription) = GND.description

fun G(GND: GNDescription) = GND.graph

// Graph DSL

fun V(g: NetworkGraph): Set<NetworkNode> = g.nodes

fun E(g: NetworkGraph): Set<NetworkEdge> = g.edges