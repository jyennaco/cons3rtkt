package com.joeyennaco

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlin.system.exitProcess


class DeploymentCli : CliktCommand(name = "dep") {
    override fun run() = Unit
}

class Env : CliktCommand() {
    private val assetVarNameArg by option("-v", "--var", help="Environment variable to retrieve")
    override fun help(context: Context) = "Retrieve data about the asset provisioning environment"
    override fun run() {
        //echo("Retrieving info about the deployment provisioning environment.")

        // Initialize deployment data
        val dep = Deployment()

        // If the --var arg was provided, return only the value of the requested variable, otherwise print the
        // environment
        assetVarNameArg?.let { assetVarName ->
            when (assetVarName) {
                "ASSET_DIR" -> echo(dep.assetDir)
                "CONS3RT_CREATED_USER" -> echo(dep.cons3rtCreatedUser)
                "CONS3RT_ROLE_NAME" -> echo(dep.cons3rtRoleName)
                "DEPLOYMENT_HOME" -> echo(dep.deploymentHome)
                "DEPLOYMENT_RUN_HOME" -> echo(dep.deploymentRunHome)
                else -> printEnvVar(dep, assetVarName)
            }
        } ?: run {
            echo("ASSET_DIR: " + dep.assetDir)
            echo("CONS3RT_CREATED_USER: " + dep.cons3rtCreatedUser)
            echo("CONS3RT_ROLE_NAME: " + dep.cons3rtRoleName)
            echo("DEPLOYMENT_HOME: " + dep.deploymentHome)
            echo("DEPLOYMENT_RUN_HOME: " + dep.deploymentRunHome)
        }

    }

    private fun printEnvVar(dep: Deployment, varName: String) {
        val envVarValue: String = dep.getEnvVar(varName)
        echo(envVarValue)
    }
}

class Net : CliktCommand() {
    override fun help(context: Context) = "Retrieve network data about the deployment run"
    private val externalArg by option("-e", "--external", help="Return the external IP address").flag(
        default = false)
    private val hostArg by option("-h", "--host", help="Host to query by scenario role name")
    private val infoArg by option("-i", "--info", help="Set to retrieve full info about the provided host " +
            "and network").flag(default = false)
    private val listArg by option("-l", "--list", help="List network names and IP addresses").flag(
        default = false)
    private val nameArg by option("-n", "--name", help="Network name").default("user-net")
    override fun run() {
        //echo("Retrieving info about the deployment and deployment run props.")

        // Initialize the deployment data
        val dep = Deployment()

        // If --list was specified, print the list of custom props and exit
        if (listArg) {
            printAllNetworkNamesAndIps(dep)
            exitProcess(0)
        }

        // If --info is specified, then --host is required
        val host: String
        if (infoArg) {
            host = if (hostArg == null) {
                // If host arg was not provided, default to the CONS3RT_ROLE_NAME if set
                dep.cons3rtRoleName
            } else {
                hostArg as String
            }
            printNetwork(retrieveNetworkForHostAndName(dep, host, nameArg), host)
            exitProcess(0)
        }

        // Return the IP address for the provided network name
        printIpForNetworkName(dep, nameArg, externalArg)
        exitProcess(0)
    }

    private fun printAllNetworkNamesAndIps(dep: Deployment) {
        for (scenarioRoleName in dep.scenarioRoleNames) {
            val host = dep.deploymentRunProps.deploymentRun.hostMap[scenarioRoleName] ?: continue
            val netKeys = host.networkInterfaceMap.keys
            for (netKey in netKeys) {
                val net = host.networkInterfaceMap[netKey] ?: continue
                printNetworkInfo(scenarioRoleName=scenarioRoleName,
                    networkType = retrieveNetworkType(net.cons3rtConnection, net.primaryConnection),
                    interfaceName = netKey,
                    networkName = net.networkName,
                    internalIp = net.internalIp,
                    boundaryIp = net.boundaryIp)
            }
        }
    }

    private fun printIpForHostNetwork(dep: Deployment, scenarioRoleName: String, networkName: String,
                                      external: Boolean = false) {
        val net = retrieveNetworkForHostAndName(dep, scenarioRoleName, networkName)
        if (external) {
            echo(net.boundaryIp)
        } else {
            echo(net.internalIp)
        }
    }

    private fun printIpForNetworkName(dep: Deployment, networkName: String, external: Boolean = false) {
        printIpForHostNetwork(dep, dep.cons3rtRoleName, networkName, external)
    }

    private fun printNetwork(net: NetworkInterfaceProps, scenarioRoleName: String) {
        printNetworkInfo(
            scenarioRoleName,
            retrieveNetworkType(net.cons3rtConnection, net.primaryConnection),
            net.interfaceName,
            net.networkName,
            net.internalIp,
            net.boundaryIp
        )
    }

    private fun printNetworkInfo(scenarioRoleName: String, networkType: String, interfaceName: String,
                                 networkName: String, internalIp: String, boundaryIp: String) {
        val del = ","
        echo(scenarioRoleName + del + networkType + del + interfaceName + del + networkName + del +
                internalIp + del + boundaryIp)
    }

    private fun retrieveNetworkForHostAndName(dep: Deployment, scenarioRoleName: String, networkName: String):
            NetworkInterfaceProps {
        if (scenarioRoleName !in dep.deploymentRunProps.deploymentRun.hostMap) {
            throw IllegalStateException("Scenario role name not found in run props: $scenarioRoleName")
        }
        val host = dep.deploymentRunProps.deploymentRun.hostMap[scenarioRoleName]
            ?: throw IllegalStateException("Scenario role name not found in run props: $scenarioRoleName")
        for (netKey in host.networkInterfaceMap.keys) {
            val net = host.networkInterfaceMap[netKey] ?: continue
            if (net.networkName == networkName) {
                return net
            }
        }
        throw IllegalStateException("Network name not found in run props for host [$scenarioRoleName]: $networkName")
    }

    private fun retrieveNetworkType(isCons3rt: Boolean, isPrimary: Boolean): String {
        return when {
            isCons3rt -> "cons3rt"
            isPrimary -> "primary"
            else -> "additional"
        }
    }
}

class Props : CliktCommand() {
    override fun help(context: Context) = "Retrieve data about the deployment and deployment run props"
    private val typeArg by option("-t", "--type", help="Type of properties to use").default("json")
    private val propArg by option("-p", "--prop", help="Property name to retrieve")
    private val listArg by option("-l", "--list", help="List custom properties by name").flag(default = false)
    private val regexArg by option("-r", "--regex", help="Regular expression to query property names")
    override fun run() {
        //echo("Retrieving info about the deployment and deployment run props.")

        // Initialize the deployment data
        val dep = Deployment()

        // If --list was specified, print the list of custom props and exit
        if (listArg) {
            printCustomPropNames(dep)
            exitProcess(0)
        }

        // Process the --prop arg based on the --type [json|properties], defaults to json
        typeArg.let {
            when (typeArg) {
                "properties" -> {
                    handleClassicDeploymentProps(dep, propArg, regexArg)
                    exitProcess(0)
                }

                "json" -> {
                    printCustomPropValue(propArg, dep)
                    exitProcess(0)
                }

                else -> {
                    println("ERROR: Unrecognized value for --type/-t: $typeArg")
                    exitProcess(127)
                }
            }
        }

    }

    private fun handleClassicDeploymentProps(dep: Deployment, propName: String?, regex: String?) {
        if (propName != null) {
            printDeploymentPropValue(propArg, dep)
            exitProcess(0)
        } else if (regex != null) {
            printDeploymentPropNamesMatchingRegex(regex, dep)
        }
    }

    private fun printDeploymentPropNamesMatchingRegex(regex: String, dep: Deployment) {
        val matchingPropNames = dep.retrieveDeploymentPropNamesFromRegex(regex)
        for (matchingPropName in matchingPropNames) {
            echo(matchingPropName)
        }
    }

    private fun printDeploymentPropValue(propArg: String?, dep: Deployment) {
        if (propArg != null) {
            val propValue: String = dep.retrieveDeploymentPropValue(propArg)
            echo(propValue)
        } else {
            echo("")
        }

    }

    private fun printCustomPropNames(dep: Deployment) {
        val propNames = dep.listCustomPropNames()
        for (propName in propNames) {
            echo(propName)
        }
    }

    private fun printCustomPropValue(propName: String?, dep: Deployment) {
        if (propName != null) {
            echo(dep.getCustomPropValue(propName))
        } else {
            echo("")
        }
    }
}