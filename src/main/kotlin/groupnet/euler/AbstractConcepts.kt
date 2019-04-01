package groupnet.euler

import groupnet.util.Bug
import groupnet.util.Formalizable
import groupnet.util.symmetricDifference
import java.util.*
import kotlin.math.abs

/**
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */

/**
 * A label, \lambda, element of \mathcal{L}.
 * Immutable.
 */
typealias Label = String

/**
 * An abstract zone, az (element of Z = P(\mathcal{L}), is a set of labels.
 * Immutable.
 */
class AbstractZone(labels: Set<Label>) : Comparable<AbstractZone>, Formalizable {

    val labels = Collections.unmodifiableSortedSet(labels.toSortedSet())

    val numLabels: Int
        get() = labels.size

    init {
        if (" " in this.labels)
            throw Bug("Abstract zone spec cannot contain ' '")
    }

    fun getStraddledLabel(other: AbstractZone): Optional<Label> {
        if (abs(numLabels - other.numLabels) != 1)
            return Optional.empty()

        val difference = labels.symmetricDifference(other.labels)

        return if (difference.size != 1) Optional.empty() else Optional.of(difference.first())
    }

    fun isNeighbour(az: AbstractZone): Boolean = getStraddledLabel(az).isPresent

    override fun equals(other: Any?): Boolean {
        if (other !is AbstractZone)
            return false

        return labels == other.labels
    }

    override fun hashCode() = labels.hashCode()

    override fun compareTo(other: AbstractZone): Int {
        if (other.labels.size < labels.size) {
            return 1
        } else if (other.labels.size > labels.size) {
            return -1
        }

        return labels.zip(other.labels)
                .map { (l1, l2) -> l1.compareTo(l2) }
                .find { it != 0 } ?: 0
    }

    override fun toFormal(): String {
        return labels.joinToString(",", "{", "}") { it }
    }

    override fun toInformal(): String {
        return labels.joinToString("") { it }
    }

    override fun toString() = toFormal()

    companion object {
        val OUTSIDE = AbstractZone(emptySet())

        @JvmStatic fun from(informalForm: String): AbstractZone {
            return AbstractZone(informalForm.map { "$it" }.toSet())
        }
    }
}

/**
 * A description, D = (Z, L), of an Euler diagram.
 * Can be viewed as a set of abstract zones.
 * Immutable.
 */
class Description(abstractZones: Set<AbstractZone>) {

    val abstractZones = Collections.unmodifiableSortedSet(abstractZones.toSortedSet())

    /**
     * We don't _really_ need this, we can obtain this from [abstractZones], just for ease of use.
     */
    val labels = Collections.unmodifiableSortedSet(this.abstractZones.flatMap { it.labels }.toSortedSet())

    init {
        if (AbstractZone.OUTSIDE !in this.abstractZones)
            throw Bug("Description does not have abstract zone {}")
    }

    /**
     * Parent abstract zone.
     */
    var parent = AbstractZone.OUTSIDE

    fun getNumZonesIn(label: Label) = abstractZones.filter { label in it }.count()

    fun getInformalDescription(): String {
        return abstractZones.minus(AbstractZone.OUTSIDE).joinToString(" ") { it.toInformal() }
    }

    override fun hashCode(): Int {
        return Objects.hash(abstractZones)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Description)
            return false

        return abstractZones == other.abstractZones
    }

    override fun toString() = abstractZones.joinToString(",") { "$it" }

    companion object {
        @JvmStatic fun from(informalForm: String): Description {

            val zones = informalForm.split(" +".toRegex())
                    .map { AbstractZone.from(it) }
                    // empty abstract zone is always present
                    .plus(AbstractZone.OUTSIDE)
                    .toSet()

            return Description(zones)
        }

        @JvmStatic fun from(informalForm: String, parent: AbstractZone): Description {
            val description = from(informalForm)
            description.parent = parent
            return description
        }
    }
}