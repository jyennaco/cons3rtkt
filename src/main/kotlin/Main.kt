package com.joeyennaco

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import kotlin.system.exitProcess


fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "asset" -> {
            AssetCli().main(args.drop(1).toTypedArray())
        }
        "cons3rt" -> {
            Cons3rtApi().main(args.drop(1).toTypedArray())
        }
        "dep" -> {
            DeploymentCli().subcommands(
                Env(),
                Props(),
                Net())
                .main(args.drop(1).toTypedArray())
        }
        else -> {
            println("Unknown command: ${args.firstOrNull()}")
            exitProcess(127)
        }
    }

}
