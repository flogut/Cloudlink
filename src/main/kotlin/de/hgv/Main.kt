package de.hgv

import org.jboss.logging.Logger
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        Logger.getLogger(MyAPI::class.java).fatal("Argument missing. Usage: java -jar <jar> <pictures directory>")
        exitProcess(0)
    }

    MyAPI(File(args[0]))
}