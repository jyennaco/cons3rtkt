package com.joeyennaco

import kotlinx.serialization.Serializable

@Serializable
data class DeploymentProps(
    val id: Int,
    val name: String,
    val props: Map<String, String>,
    val scenarioMap: Map<String, ScenarioProps>,
    val testBundles: List<String>
)

@Serializable
data class RunProps(
    val deployment: DeploymentProps,
    val id: Int,
    val hostMap: Map<String, HostProps>,
    val options: OptionsProps,
    val project: ProjectProps,
    val team: TeamProps,
    val user: UserProps,
    val virtRealm: VirtRealmProps
)

@Serializable
data class DeploymentRunProps(
    val version: String,
    val creationTime: String,
    val deploymentRun: RunProps,
    val site: SiteProps,
)

@Serializable
data class DiskProps(
    val additionalDisk: Boolean,
    val bootDisk: Boolean,
    val capacityInMegabytes: Int,
    val createOrder: Int,
    val id: Int,
    val name: String? = null,
)

@Serializable
data class HostProps(
    val cons3rtNetworkIp: String,
    val createdUsername: String,
    val disks: List<DiskProps>,
    val hostname: String,
    val gpuEnabled: Boolean,
    val id: Int,
    val instanceTypeName: String,
    val master: Boolean,
    val osFamily: String,
    val networkInterfaceMap: Map<String, NetworkInterfaceProps>,
    val packageManagement: String,
    val physicalMachineDataOrTemplateUuid: String,
    val physicalMachineOrTemplateName: String,
    val powerShellVersion: String,
    val primaryNetworkIp: String,
    val primaryNetworkName: String,
    val provisionable: Boolean,
    val serviceManagement: String,
    val systemRole: String,
    val virtual: Boolean,
)

@Serializable
data class NetworkInterfaceProps(
    val boundaryIp: String,
    val cons3rtConnection: Boolean,
    val interfaceName: String,
    val internalIp: String,
    val macAddress: String,
    val networkName: String,
    val primaryConnection: Boolean,
    val subnetIdentifier: String,
)

@Serializable
data class OptionsProps(
    val name: String,
    val properties: Map<String, String>,
)

@Serializable
data class ProjectProps(
    val id: Int,
    val name: String,
)

@Serializable
data class ScenarioProps(
    val id: Int,
    val name: String,
    val buildOrder: Int,
)

@Serializable
data class SiteProps(
    val siteId: Int,
    val siteAddress: String,
)

@Serializable
data class TeamProps(
    val id: Int,
    val name: String,
)

@Serializable
data class UserProps(
    val email: String,
    val username: String,
)

@Serializable
data class VirtRealmProps(
    val cons3rtNetwork: String,
    val displayName: String,
    val id: Int,
    val internalName: String,
    val props: Map<String, String>,
    val remoteAccessIp: String,
    val type: String,
)