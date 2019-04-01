package groupnet.util

import groupnet.euler.Description
import groupnet.gn.GNDescription
import java.util.*

/**
 *
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
object Examples {

    val list = ArrayList<Pair<String, Description> >()
    val gndList = arrayListOf<Pair<String, GNDescription>>()

    init {
        add("Adjacency 1", "() a(3) b(4) c() d() ab(5,8) ac() ad() bc() bd() cd() abc(6,7) abd() acd() bcd() abcd()", "3-4,5-6,8-7,5-8")
        add("Adjacency 2", "() a(3) b(4) c() ab(5,8) ac() ad() bc() bd() abc(6,7) abd() acd()", "3-4,5-6")
        add("Nested GND 1", "() P(3,4) PQ(5)", "3-4,4-5")
        add("Nested GND 2", "(3) P(4) PQ(5) PR(2) PRS(6) PQR(1)", "3-4,4-5,1-2,2-6")
        add("Nested GND 3", "() P(1,2) PQ(3) PQS() PR(4)", "1-2,2-3,3-4,1-4")
        add("Nested GND 4", "() P(1,2) PQ(3) PS() PR(4), PT() PRT(5,6)", "1-2,2-3,3-4,1-4,1-5")
        add("Nested GND 5", "() P(1) PQ(2,4,5,6) PQR(3)", "1-2,2-4,5-6,4-6")
        add("Placement 1", "() P() PQ(1) PR(2) S() PS()", "1-2")
        add("Placement 2", "() P(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17)", "")
        add("Placement 3", "() P(1,2,3,4,5,6,7,8,9) PQ(10)", "")
        add("Resolution 1", "() P(3) PQ(1) Q(2)", "3-2")
        add("Resolution 2", "() P(1) PQ(2) PR(3) PRS(5) PS(4)", "1-2,1-3,4-2,3-4")
        add("Test 1", "() P(3,6) R(2) S(4) T(5) Q(1) TR(7) TP(9) TS() TQ(8) TQU() TU()", "1-2,2-3,3-4,2-4,1-5,2-5,3-5,4-5,6-7,8-9,1-3")
        add("Decomposition Tree 1", "() P(2) PQ(3) PQR() PQRS() PQS()", "3-2")
        add("Decomposition Tree 2", "() P(2) PQ(3) PQR() PR() PQS()", "3-2")
        add("Decomposition Tree 3", "() P(2) PQ(3) PQR() PR(5) PQS() PT(4)", "3-2,4-5")
        add("Decomposition Tree Strategy", "() P(5,7,8,9) PQ(1,2,3,6) PQR(4,10)", "1-4,4-5,6-7")
        add("Graph placement", "() P(1,2,3,4) PQ(5,10) Q() R(8,9) PR() QR(6,7) PQRS() QS() RS()", "")
        add("Graph placement 2", "() P(1,2,10,11,12,13,14,15,16) Q(3,4) PQ(5) R(6,7)", "1-2,3-5,3-6,1-6,6-7,10-11,10-13,10-12,10-16,1-15,2-14,14-16")
        add("Multiple", "() P(1,2) Q(3,4)", "1-2,3-4,2-3")
        add("Multiple 2", "() P(1,2) Q(3,4) PQ(5) R(6,7)", "1-2,3-5,3-6,1-6,6-7")
        add("Graph layout 1", "() P(1,2,3,7) PQ(4) Q(5,6)", "1-5,2-6,1-2")
        add("Graph layout 2", "() a(1) b(2) c(3) ab(4) ac(5) bc(6) abc(7)", "6-7,2-3")
        add("Graph layout 3", "(3,7) P(1) PQ(2) Q(4) QR(6,8) QS(5,10) QRS(9)", "1-2,6-7,8-9,5-9,4-5,3-4")
        add("Atomic 1",
                "(18) P(1) Q(2,4,19) R(3) PQ(16) PR(5) QR(6) QS(7) RS(8) PQR(9) PQS(10) PQT(11) PRS(12) QST(13) PQRS(14) PQST(15)",
                "1-2,2-10,9-18,5-18,2-4,7-15,14-16,4-19,6-14,2-19,16-10,14-15,1-19"
        )
        add("Nested 1",
                "() P(1,13) PQ(2,4,5,6) S(7,8,11) ST(3,9) SU(10) STU(12)",
                "1-2,5-6,4-6,2-7,10-9,3-9,8-9,11-9,12-9,5-2,5-13,7-3"
        )

        add("Nested 2",
                "() a(1,2) b(3) c(4) d(5) ab(6) ac(7) ad(8) bc(9) bd(10) cd(11) abc(12) abd(99) acd(13) bcd(98) abcd(97) e() f(15,16) fg() g(17)",
                "1-2,5-6,4-6,2-7,10-9,3-9,8-9,11-9,12-9,5-2,5-13,7-3,1-15,5-16"
        )

        add("Venn-3", "a b c abc ab ac bc")
        add("Venn-4", "a b c d ab ac ad bc bd cd abc abd acd bcd abcd")
        add("Venn-5", "a b c d e ab ac ad ae bc bd be cd ce de abc abd abe acd ace ade bcd bce bde cde abcd abce abde acde bcde abcde")
        add("Venn-5P", "P Q R S T PQ PR PS PT QR QS QT RS RT ST PQR PQS PQT PRS PRT PST QRS QRT QST RST PQRS PQRT PQST PRST QRST PQRST")
        add("Venn-6", "a b c d e f ab ac ad ae bc bd be cd ce de abc abd abe acd ace ade bcd bce bde cde abcd abce abde acde bcde abcde af bf cf df ef abf acf adf aef bcf bdf bef cdf cef def abcf abdf abef acdf acef adef bcdf bcef bdef cdef abcdf abcef abdef acdef bcdef abcdef")

        add("Venn-4 in Venn-3", "a b c ab ac bc abc abcd abce abcf abcg abcde abcdf abcdg abcef abceg abcfg abcdef abcdeg abcdfg abcefg abcdefg")
        add("2 Venn-4 in Venn-3", "a b c ab ac bc cs ct cx cy abc cst csx csy ctx cty cxy abcd abce abcf abcg cstx csty csxy ctxy abcde abcdf abcdg abcef abceg abcfg cstxy abcdef abcdeg abcdfg abcefg abcdefg")

        add("Nested Piercing 1", "ab ac ad ae abc ade")
        add("Nested Piercing 2", "a b c ab ac bc cd ce cf cg ch cj abc")

        add("Single Piercing 1", "a b c ab ac af ag bc be cd abc abe abf abg acd acf afg bcd bce abcd abce abcf abfg ah abh")
        add("Single Piercing 2", "a b c ab ac af bc be cg ch abc abe abf abj bcd bch cgh abcd abcj")
        add("Single Piercing 3", "a b c ab ac bc bd be bf bx cg ch ci abc bcd bce bcf bcg bch bci bfx")

        add("Double Piercing", "a b c ab ac af ag bc be cd abc abe abf abg acd acf afg bcd bce abcd abce abcf abfg")
        add("Double Piercing 1", "a b c d ab ac ad ae bc bd cd abc abd acd ace bcd abcd acde")
        add("Double Piercing 2", "p q r pq pr qr qs rs pqs prs qrs qrt pqrs")
        add("Double Piercing 3", "a b c d ac ad bc bd cd ce df abd acd ace bcd bce bdf cdf abcd abce bcdf")
        add("Double Piercing 4", "a b d e ac ad ae bc bd cd de")

        add("Combined Piercing 1", "a b c ab ac af bc be cg ch co abc abe abf abj aco bcd bch bco cgh abcd abcj abco")
        add("Combined Piercing 2", "a b c d e f k ab ac ak bc bd bu ce ef abc abu bcu abcg abcl abcu abcgl")
        add("Combined Piercing 3", "a b c d e f g h j k ab ac ai aj bc bd bk ce ck df fg gh kl km kn abc abi aci bck ckl kmn abci")
        add("Combined Piercing 4", "a b c f g k l m n ab ac cd ce cf fg fh fi fj gk gl gm gn mn fhi fhj fij gmn fhij")

        add("Combined All 1", "a b c ab ac af bc be cg ch ck co abc abe abf abj ack aco bcd bch bck bco cgh cko abcd abcj abco acko bcko")
        add("Combined All 2", "a b c ab ac ad af bc be cd cg ch abc abe abf abj acd bcd bch cgh abcd abci abcj")
        add("Combined All 3", "a b c j ab ac aj ao bc bd be bf cg ch ci abc abj aco ajo bcd bce bcf bcg bch bci")
        add("Combined All 4", "b c d f h j ab ac bc bd bf bj cf de dj fh hi abc acf bfg abcf")

        add("Edge Route", "a b c ab ac bc bd bf abc abd abf bcd bcf bdf abcd abdf bcdf")
        add("Edge Route 1", "a b c d ab ac ad bc bd be cd abc abd abe acd bcd bce bde abcd abce abde")
        add("Edge Route 2", "a b c d ab ac ad ae af bc bd cd abc abd acd ace acf adf bcd abcd acde adef")

        add("Multidiagram 1", "a b c abc ab ac bc d e de df")

        add("Component Decomposition 1", "a b ab bc bd bcd")
        add("Component Decomposition 2", "a ab abc")
        add("Component Decomposition 3", "a b c ab ac bc cd ce cf abc cde cdf cef abcg abch abcj cdef abcgh abcgj abcghj abcjh")
        add("Component Decomposition 4", "d ad bd cd abd acd bcd bdg bdh abcd bdgh")

        add("Disconnecting Curve 1", "a b c abc ab ac bc x y z xy yz xz xyz ad d dx")
        add("Disconnecting Curve 2", "a b ac abc")

        add("Decomp1", "P Q R PQ PS QS PQR PQS PQRS QRS PRS")
        add("Decomp2", "P Q R PQ PR QR QS QT RS PQR PQS PQT PRS QRS QST PQRS PQST")
        add("Decomp3", "P Q R PQ PR PS QR QS PQR PQS PRS QRS")
        add("Decomp4", "P Q R PQ PR QR QS RS PQR PQS PQT PRS QST PQRS PQST")
    }

    private fun add(name: String, description: String) {
        list.add(name.to(Description.from(description)))
    }

    private fun add(name: String, informalDescription: String, edgeDescription: String) {
        gndList.add(name to (GNDescription.from(informalDescription, edgeDescription)))
    }
}