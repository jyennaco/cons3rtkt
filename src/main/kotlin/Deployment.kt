package com.joeyennaco

import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.util.Properties

// The static values of cons3rt-agent home directories
const val cons3rtAgentDirLinux: String = "/opt/cons3rt-agent"
const val cons3rtAgentDirWindows: String = "C:\\cons3rt-agent"

class Deployment(val installScriptDir: String = "") {

    // Deployment environment vars
    val assetDir: String
    val cons3rtCreatedUser: String
    val cons3rtRoleName: String
    val deploymentHome: String
    val deploymentRunHome: String

    // Deployment properties file paths
    val deploymentPropsFile: String
    val deploymentPropsFileSh: String
    val deploymentPropsFilePs1: String

    // Deployment run properties file paths
    val deploymentRunPropsFile: String
    val deploymentRunPropsFileSh: String
    val deploymentRunPropsFilePs1: String
    val deploymentRunPropsFileJson: String
    val deploymentRunPropsFileYaml: String

    // Commonly user deployment run data to set
    val cons3rtAgentHomeDir: String?
    val scenarioRoleNames: List<String>
    val scenarioMasterRoleName: String
    val deploymentId: Int
    val deploymentName: String
    val deploymentRunId: Int
    val deploymentRunName: String
    val virtualizationRealmType: VirtualizationRealmType
    val operatingSystem: OperatingSystemType?

    // Deployment run properties loaded from the json file
    val deploymentRunProps: DeploymentRunProps

    // Deployment properties loaded from the properties file
    val deploymentProps: Properties

    init {
        val osType = determineOperatingSystem()
        this.operatingSystem = osType
        val agentHomeDir = determineCons3rtAgentDirForOs(osType)
        this.cons3rtAgentHomeDir = agentHomeDir
        this.deploymentHome = determineDeploymentHome()
        this.deploymentRunHome = determineDeploymentRunHome()
        this.assetDir = determineAssetDir()
        this.cons3rtRoleName = getEnvVar("CONS3RT_ROLE_NAME")
        this.cons3rtCreatedUser = getEnvVar("CONS3RT_CREATED_USER")
        this.deploymentPropsFile = determineDeploymentPropsFile()
        this.deploymentPropsFileSh = determineDeploymentPropsSh()
        this.deploymentPropsFilePs1 = determineDeploymentPropsPs1()
        this.deploymentRunPropsFile = determineDeploymentRunPropsFile()
        this.deploymentRunPropsFileSh = determineDeploymentRunPropsSh()
        this.deploymentRunPropsFilePs1 = determineDeploymentRunPropsPs1()
        this.deploymentRunPropsFileJson = determineDeploymentRunPropsJson()
        this.deploymentRunPropsFileYaml = determineDeploymentRunPropsYaml()
        this.deploymentProps = determineDeploymentProperties()
        this.deploymentRunProps = readJsonProperties()
        this.deploymentId = determineDeploymentId()
        this.deploymentName = determineDeploymentName()
        this.deploymentRunId = determineDeploymentRunId()
        this.deploymentRunName = determineDeploymentRunName()
        this.scenarioRoleNames = determineScenarioRoleNames()
        this.scenarioMasterRoleName = determineScenarioMasterRoleName()
        this.virtualizationRealmType = determineVirtualizationRealmType()
    }

    /*
     * Gets value of the ASSET_DIR environment variable or attempts to determine where it is
     */
    private fun determineAssetDir() : String {
        // Use the value of ASSET_DIR environment variable
        val assetDirVar = getEnvVar("ASSET_DIR")

        // If ASSET_DIR is set
        if (assetDirVar != "") {
            val assetDirFile = File(assetDirVar)

            // Check if ASSET_DIR exists and is a directory
            if (assetDirFile.exists() && assetDirFile.isDirectory) {
                return assetDirVar
            } else {
                //println("WARNING: ASSET_DIR is set but not found, attempting to determine...")
            }
        }
        //println("INFO: ASSET_DIR is not set, attempting to determine...")

        if (this.installScriptDir == "") {
            //println("WARNING: ASSET_DIR is not set and was not determined due to install script dir not provided")
            return ""
        }

        // Attempt to determine from the passed in install script directory
        val assetDirFound = this.installScriptDir.substringBeforeLast(File.separator)
        val assetDirFoundFile = File(assetDirFound)

        // Check if the found ASSET_DIR exists and is a directory
        if (assetDirFoundFile.exists() && assetDirFoundFile.isDirectory) {
            return assetDirFound
        }

        // Asset dir was never found
        //println("WARNING: ASSET_DIR is not set and was not determined")
        return ""
    }

    /*
     * Determines the cons3rt-agent directory based on the OS
     *
     * @return OperatingSystemType
     */
    private fun determineCons3rtAgentDirForOs(operatingSystemType: OperatingSystemType): String? {
        return when (operatingSystemType) {
            OperatingSystemType.FREEBSD -> cons3rtAgentDirLinux
            OperatingSystemType.LINUX -> cons3rtAgentDirLinux
            OperatingSystemType.MAC -> null
            OperatingSystemType.SOLARIS -> cons3rtAgentDirLinux
            OperatingSystemType.WIN -> cons3rtAgentDirWindows
        }
    }

    private fun determineDeploymentHome(): String {

        // Use the value of DEPLOYMENT_HOME environment variable
        val deploymentHomeVar = getEnvVar("DEPLOYMENT_HOME")

        // If DEPLOYMENT_HOME is set
        if (deploymentHomeVar != "") {
            val runDir = File(deploymentHomeVar)

            // Check if DEPLOYMENT_HOME exists and is a directory
            if (runDir.exists() && runDir.isDirectory) {
                return deploymentHomeVar
            } else {
                //println("WARNING: DEPLOYMENT_HOME is set but not found, attempting to determine...")
            }
        } else {
            //println("INFO: DEPLOYMENT_HOME is not set, attempting to determine...")
        }

        if (this.cons3rtAgentHomeDir == null) {
            throw IllegalStateException("No cons3rt-agent directory, unable to determine DEPLOYMENT_HOME")
        }

        // Set the run directory path
        val runDir = File(this.cons3rtAgentHomeDir, "run")

        // Fail if the run directory is not found
        if (!(runDir.exists()) || !(runDir.isDirectory)) {
            throw IllegalStateException("Unable to find the cons3rt-agent/run directory: [$runDir]")
        }

        // The cons3rt-agent/run directory exists, search for a Deployment directory
        val runDirFiles: Array<File>?
        try {
            runDirFiles = File(runDir.absolutePath).listFiles { runDirFile -> runDirFile.isDirectory }
        } catch (e: SecurityException) {
            throw IllegalStateException("Permissions issues accessing directory: [$runDir]")
        }

        // Check if the list of directories is null
        if (runDirFiles == null) {
            throw IllegalStateException("Unable to find contents in the cons3rt-agent/run directory: [$runDir]")
        }

        // Find the directory starting with "Deployment"
        for (runDirFile in runDirFiles) {
            val dirName = runDirFile.absolutePath.substringAfterLast(File.separator)
            //println("runDirFile: $dirName")
            if (dirName.startsWith("Deployment")) {
                if (!(runDirFile.exists()) || !(runDirFile.isDirectory)) {
                    //println("INFO: Deployment directory is not a directory: $runDirFile")
                } else {
                    //println("INFO: Found deployment home: $runDirFile")
                    return runDirFile.absolutePath
                }
            }
        }

        // Deployment home was never found
        throw IllegalStateException("Unable to determine deployment home from run directory: [$runDir]")
    }

    private fun determineDeploymentId(): Int {
        return this.deploymentRunProps.deploymentRun.deployment.id
    }

    private fun determineDeploymentName(): String {
        return this.deploymentRunProps.deploymentRun.deployment.name
    }

    private fun determineDeploymentProperties(): Properties {
        val props = Properties()
        val fis = FileInputStream(this.deploymentPropsFile)
        props.load(fis)
        return props
    }

    private fun determineDeploymentPropsFile(): String {
        val depProps = File(this.deploymentHome, "deployment.properties")
        return if (depProps.exists()) {
            depProps.absolutePath
        } else {
            ""
        }
    }

    private fun determineDeploymentPropsPs1(): String {
        val depProps = File(this.deploymentHome, "deployment-properties.ps1")
        return if (depProps.exists()) {
            depProps.absolutePath
        } else {
            ""
        }
    }

    private fun determineDeploymentPropsSh(): String {
        val depProps = File(this.deploymentHome, "deployment-properties.sh")
        return if (depProps.exists()) {
            depProps.absolutePath
        } else {
            ""
        }
    }

    private fun determineDeploymentRunHome(): String {

        // Use the value of DEPLOYMENT_HOME environment variable
        val deploymentRunHomeVar = getEnvVar("DEPLOYMENT_RUN_HOME")

        // If DEPLOYMENT_RUN_HOME is set
        if (deploymentRunHomeVar != "") {
            val runDir = File(deploymentRunHomeVar)

            // Check if DEPLOYMENT_RUN_HOME exists and is a directory
            if (runDir.exists() && runDir.isDirectory) {
                return deploymentRunHomeVar
            } else {
                //println("WARNING: DEPLOYMENT_RUN_HOME is set but not found, attempting to determine...")
            }
        } else {
            //println("INFO: DEPLOYMENT_RUN_HOME is not set, attempting to determine...")
        }

        // Set the run directory path
        val runDir = File(this.deploymentHome, "run")

        // Fail if the run directory is not found
        if (!(runDir.exists()) || !(runDir.isDirectory)) {
            throw IllegalStateException("Unable to find the DEPLOYMENT_HOME/run directory: [$runDir]")
        }

        // The cons3rt-agent/run directory exists, search for a Deployment directory
        val runDirFiles: Array<File>?
        try {
            runDirFiles = File(runDir.absolutePath).listFiles { runDirFile -> runDirFile.isDirectory }
        } catch (e: SecurityException) {
            throw IllegalStateException("Permissions issues accessing directory: [$runDir]")
        }

        // Check if the list of directories is null
        if (runDirFiles == null) {
            throw IllegalStateException("Unable to find contents in the DEPLOYMENT_HOME/run directory: [$runDir]")
        }

        // Find the directory starting with "Deployment"
        for (runDirFile in runDirFiles) {
            val dirName = runDirFile.absolutePath.substringAfterLast(File.separator)

            // Attempt to convert the directory name to an Int, to see if it is a run ID
            try {
                dirName.toInt()
            } catch (e: NumberFormatException) {
                //println("INFO: Directory is not a run ID: $runDirFile.")
            }

            // Check if the run ID directory exists
            if (!(runDirFile.exists()) || !(runDirFile.isDirectory)) {
                println("WARNING: Run ID directory is not a directory: $runDirFile")
            } else {
                //println("INFO: Found deployment run home: $runDirFile")
                return runDirFile.absolutePath
            }
        }

        // Deployment run home was never found
        throw IllegalStateException("Unable to determine deployment run home from run directory: [$runDir]")
    }

    private fun determineDeploymentRunId(): Int {
        return this.deploymentRunProps.deploymentRun.id
    }

    private fun determineDeploymentRunName(): String {
        return this.deploymentRunProps.deploymentRun.name
    }

    private fun determineDeploymentRunPropsFile(): String {
        val depRunProps = File(this.deploymentRunHome, "deployment.properties")
        return if (depRunProps.exists()) {
            depRunProps.absolutePath
        } else {
            ""
        }
    }

    private fun determineDeploymentRunPropsJson(): String {
        val depRunProps = File(this.deploymentRunHome, "deploymentRunProperties.json")
        return if (depRunProps.exists()) {
            depRunProps.absolutePath
        } else {
            ""
        }
    }

    private fun determineDeploymentRunPropsPs1(): String {
        val depRunProps = File(this.deploymentRunHome, "deployment-properties.ps1")
        return if (depRunProps.exists()) {
            depRunProps.absolutePath
        } else {
            ""
        }
    }

    private fun determineDeploymentRunPropsSh(): String {
        val depRunProps = File(this.deploymentRunHome, "deployment-properties.sh")
        return if (depRunProps.exists()) {
            depRunProps.absolutePath
        } else {
            ""
        }
    }

    private fun determineDeploymentRunPropsYaml(): String {
        val depRunProps = File(this.deploymentRunHome, "deploymentRunProperties.yml")
        return if (depRunProps.exists()) {
            depRunProps.absolutePath
        } else {
            ""
        }
    }

    /*
     * Determines the Operating System Type and sets the class variable.
     *
     * @return OperatingSystemType
     */
    private fun determineOperatingSystem(): OperatingSystemType {
        val osName = System.getProperty("os.name").lowercase()
        return when {
            osName.contains("win") -> OperatingSystemType.WIN
            osName.contains("mac") -> OperatingSystemType.MAC
            osName.contains("linux") -> OperatingSystemType.LINUX
            osName.contains("sun") -> OperatingSystemType.SOLARIS
            osName.contains("free") -> OperatingSystemType.FREEBSD
            else -> throw IllegalStateException("Unsupported operating system: $osName")
        }
    }

    private fun determineScenarioMasterRoleName(): String {
        for (scenarioRoleName in this.deploymentRunProps.deploymentRun.hostMap.keys) {
            if (this.deploymentRunProps.deploymentRun.hostMap[scenarioRoleName]?.master == true) {
                return scenarioRoleName
            }
        }
        return ""
    }

    private fun determineScenarioRoleNames(): List<String> {
        val scenarioRoleNames = mutableListOf<String>()
        for (scenarioRoleName in this.deploymentRunProps.deploymentRun.hostMap.keys) {
            scenarioRoleNames.add(scenarioRoleName)
        }
        return scenarioRoleNames
    }

    private fun determineVirtualizationRealmType(): VirtualizationRealmType {
        val vrType = this.deploymentRunProps.deploymentRun.virtRealm.type
        return when (vrType) {
            "Amazon" -> VirtualizationRealmType.AMAZON
            "Azure" -> VirtualizationRealmType.AZURE
            "VCloudRest" -> VirtualizationRealmType.VCLOUDREST
            else -> {VirtualizationRealmType.UNKNOWN}
        }
    }

    /*
     * Returns the value of the environment variable or a blank string
     */
    fun getEnvVar(varName: String) : String {
        val envVarValue: String? = System.getenv(varName)
        return envVarValue ?: ""
    }

    /*
     * Returns the IP given the scenario role name and network name
     */
    fun getIpForHostNetwork(scenarioRoleName: String, networkName: String, external: Boolean = false) : String {
        val net = retrieveNetworkForHostAndName(scenarioRoleName, networkName)
        return if (external) {
            net.boundaryIp
        } else {
            net.internalIp
        }
    }

    /*
     * Returns the primary IP address for the provided scenario role name
     */
    fun getHostPrimaryIp(scenarioRoleName: String) : String {
        val host = this.deploymentRunProps.deploymentRun.hostMap[scenarioRoleName] ?: return ""
        val netKeys = host.networkInterfaceMap.keys
        for (netKey in netKeys) {
            val net = host.networkInterfaceMap[netKey] ?: continue
            if (net.primaryConnection) {
                return net.internalIp
            }
        }
        return ""
    }

    /*
     * Given a property name, return the value as a String or a blank string
     */
    fun getCustomDeploymentPropValue(propName: String) : String? {
        // Print a specific custom deployment property
        if (propName in this.deploymentRunProps.deploymentRun.deployment.props.keys) {
            val propVal = this.deploymentRunProps.deploymentRun.deployment.props[propName]
            //println("$propName: $propVal")
            return propVal
        }
        return null
    }

    fun listCustomDeploymentPropNames(): List<String> {
        val propNames = mutableListOf<String>()
        for (propName in this.deploymentRunProps.deploymentRun.deployment.props.keys) {
            propNames.add(propName)
        }
        return propNames
    }

    /*
     * Given a run property name, return the value as a String or a blank string
     */
    fun getCustomRunPropValue(propName: String) : String? {
        // Print a specific custom deployment property
        if (propName in this.deploymentRunProps.deploymentRun.options.properties.keys) {
            val propVal = this.deploymentRunProps.deploymentRun.options.properties[propName]
            //println("$propName: $propVal")
            return propVal
        }
        return null
    }

    /*
     * List run property names
     */
    fun listCustomRunPropNames(): List<String> {
        val propNames = mutableListOf<String>()
        for (propName in this.deploymentRunProps.deploymentRun.options.properties.keys) {
            propNames.add(propName)
        }
        return propNames
    }

    /*
     * Given a run property name, return the value as a String or a blank string
     */
    fun getCustomPropValue(propName: String) : String? {
        return getCustomRunPropValue(propName)
    }

    /*
     * List run property names
     */
    fun listCustomPropNames(): List<String> {
        return listCustomRunPropNames()
    }

    fun getAllHostNetworkStr() : String {
        var allNetworkStr = ""
        val del = ","
        for (scenarioRoleName in this.scenarioRoleNames) {
            val host = this.deploymentRunProps.deploymentRun.hostMap[scenarioRoleName] ?: continue
            val netKeys = host.networkInterfaceMap.keys
            for (netKey in netKeys) {
                val net = host.networkInterfaceMap[netKey] ?: continue
                allNetworkStr += scenarioRoleName + del +
                        retrieveNetworkType(net.cons3rtConnection, net.primaryConnection) + del +
                        net.interfaceName + del +
                        net.networkName + del +
                        net.internalIp + del +
                        net.boundaryIp + "\n"
            }
        }
        return if (allNetworkStr.isNotEmpty()) allNetworkStr.substring(0, allNetworkStr.length - 1) else ""
    }

    fun getHostNetworkStr(scenarioRoleName: String, networkName: String) : String {
        val net = retrieveNetworkForHostAndName(scenarioRoleName, networkName)
        val del = ","
        return scenarioRoleName + del +
                retrieveNetworkType(net.cons3rtConnection, net.primaryConnection) + del +
                net.interfaceName + del +
                networkName + del +
                net.internalIp + del +
                net.boundaryIp
    }

    private fun readJsonProperties(): DeploymentRunProps {
        //println("INFO: Reading JSON deployment run props file: ${this.deploymentRunPropsFileJson}")

        val jsonPropsFile = File(this.deploymentRunPropsFileJson)

        if (!jsonPropsFile.exists()) {
            println("ERROR: JSON deployment run props file not found: ${jsonPropsFile.absolutePath}")
            throw IllegalStateException("JSON deployment run props file not found: ${jsonPropsFile.absolutePath}")
        }

        // Read the JSON properties file
        val jsonProps = FileInputStream(this.deploymentRunPropsFileJson)
        val jsonString = jsonProps.bufferedReader().use { it.readText() }
        //println(jsonString)
        val jsonPropsDecoded = Json.decodeFromString<DeploymentRunProps>(jsonString)
        //println(jsonPropsDecoded)
        return jsonPropsDecoded
    }

    fun retrieveDeploymentPropNamesFromRegex(regexStr: String): List<String> {
        val regex = regexStr.toRegex()
        val matchedPropNames: MutableList<String> = mutableListOf()
        val propNames = this.deploymentProps.stringPropertyNames()
        for (propName in propNames) {
            if (regex.matches(propName)) {
                matchedPropNames.add(propName)
            }
        }
        return matchedPropNames
    }

    fun retrieveDeploymentPropValue(propName: String): String {
        val propVal = this.deploymentProps.getProperty(propName)
        return propVal ?: ""
    }

    fun retrieveNetworkForHostAndName(scenarioRoleName: String, networkName: String):
            NetworkInterfaceProps {
        if (scenarioRoleName !in this.deploymentRunProps.deploymentRun.hostMap) {
            throw IllegalStateException("Scenario role name not found in run props: $scenarioRoleName")
        }
        val host = this.deploymentRunProps.deploymentRun.hostMap[scenarioRoleName]
            ?: throw IllegalStateException("Scenario role name not found in run props: $scenarioRoleName")
        for (netKey in host.networkInterfaceMap.keys) {
            val net = host.networkInterfaceMap[netKey] ?: continue
            if (net.networkName == networkName) {
                return net
            }
        }
        throw IllegalStateException("Network name not found in run props for host [$scenarioRoleName]: $networkName")
    }

    fun retrieveNetworkType(isCons3rt: Boolean, isPrimary: Boolean): String {
        return when {
            isCons3rt -> "cons3rt"
            isPrimary -> "primary"
            else -> "additional"
        }
    }
}