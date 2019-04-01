package groupnet.util

/**
 * Simple logger w/o configs.
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
object Log {

    private val on = false
    private val debug = false

    fun i(message: String, vararg objects: Any) {
        if (!on)
            return

        println("INFO : $message " + objects.joinToString())
    }

    fun d(message: String, vararg objects: Logable) {
        if (!on)
            return

        if (debug)
            println("DEBUG: $message " + objects.joinToString(",") { it.toLog() })
    }

    fun e(error: Throwable, vararg objects: Logable) {
        if (!on)
            return

        println("ERROR:\n $error\n " + objects.joinToString(",") { it.toLog() })
    }
}

interface Logable {

    fun toLog(): String
}