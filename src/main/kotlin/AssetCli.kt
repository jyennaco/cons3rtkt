package com.joeyennaco

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import kotlin.system.exitProcess

class AssetCli : CliktCommand(name = "asset") {
    override fun run() {
        echo("Retrieving info about the asset provisioning environment.")
        exitProcess(0)
    }
}