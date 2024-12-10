#!/bin/bash
#
# build.sh
#
# Builds the cons3rtkt assets
#
# Prerequisites: gradle, java
#
# Usage:
#     ./scripts/buildAssets.sh
#

# Ensure this is executing from the top level directory of the homer git repo
repoDir=$(git rev-parse --show-toplevel)
if [ $? -ne 0 ]; then echo "ERROR: Run this command from the top-level cons3rtkt repo directory"; exit 1; fi
cd ${repoDir}/

# Global variables
buildDir="${repoDir}/build"
assetDirNameLin="asset-cons3rtkt-linux"
assetDirNameWin="asset-cons3rtkt-windows"
assetSrcDirLin="src/asset/${assetDirNameLin}"
assetSrcDirWin="src/asset/${assetDirNameWin}"
assetBuildDirLin="${buildDir}/${assetDirNameLin}"
assetBuildDirWin="${buildDir}/${assetDirNameWin}"
artifactsDir="${buildDir}/distributions"

# Asset version from gradle
gradleVersion=$(grep '^version = ' build.gradle.kts | awk -F = '{print $2}' | awk -F \" '{print $2}' | xargs)

function build_app() {
    echo "INFO: Building the cons3rtkt application..."

    # Ensure gradlew exists
    if [ ! -f ./gradlew ]; then echo "ERROR: gradlew script not found"; return 1; fi

    # Build the tar distribution
    echo "INFO: Building the tar distribution..."
    ./gradlew distTar
    if [ $? -ne 0 ]; then echo "ERROR: Building the tar distribution"; return 1; fi

    # Build the zip distribution
    echo "INFO: Building the zip distribution..."
    ./gradlew distZip
    if [ $? -ne 0 ]; then echo "ERROR: Building the zip distribution"; return 1; fi

    echo "INFO: Completed building the cons3rtkt application"
    return 0
}

function cleanup_asset() {

    local assetBuildDir="${1}"
    local assetSrcDir="${2}"

    # Return the asset_data.yml file updates to the asset source directory and clean up the asset build directory
    echo "INFO: Cleaning up asset build directory: [${assetBuildDir}]..."

    # Return the updated asset_data.yml file to the original asset directory
    local buildAssetYml="${assetBuildDir}/asset_data.yml"
    local sourceAssetYml="${assetSrcDir}/asset_data.yml"

    # If asset_data.yml exists, copy it back to the source
    if [ -f ${buildAssetYml} ]; then
        echo "INFO: Copying [${buildAssetYml}] to: [${sourceAssetYml}]..."
        cp -f ${buildAssetYml} ${sourceAssetYml}
        if [ $? -ne 0 ]; then echo "ERROR: Copying [${buildAssetYml}] to: [${sourceAssetYml}]"; return 1; fi
    else
        echo "INFO: No asset_data.yml found to restore: [${buildAssetYml}]"
    fi

    # Clean up the asset build directory
    if [ -d ${assetBuildDir} ]; then
        echo "INFO: Cleaning up the asset build directory: ${assetBuildDir}"
        rm -Rf ${assetBuildDir}
        if [ $? -ne 0 ]; then echo "ERROR: Cleaning up the asset build directory: ${assetBuildDir}"; return 1; fi
    else
        echo "INFO: No asset build directory to clean up: [${assetBuildDir}]"
    fi

    echo "INFO: Completed cleaning up asset build directory: [${assetBuildDir}}]"
    return 0
}

function create_asset() {

    local assetBuildDir="${1}"

    local assetProps="${assetBuildDir}/asset.properties"

    # Ensure asset properties file exists
    if [ ! -f ${assetProps} ]; then echo "ERROR: asset.properties file not found: ${assetProps}"; return 1; fi

    # Edit asset properties
    echo "INFO: Editing asset properties [${assetProps}], replacing REPLACE_VERSION with: [${gradleVersion}]..."
    sed -i~ "s|REPLACE_VERSION|${gradleVersion}|g" ${assetProps}
    if [ $? -ne 0 ]; then echo "ERROR: Editing asset properties [${assetProps}], replacing REPLACE_VERSION with: [${gradleVersion}]"; return 1; fi
    if [ -f ${assetProps}~ ]; then rm -f ${assetProps}~ ; fi

    # Create the asset
    echo "INFO: Creating/Updating asset from [${assetBuildDir}] to: [${buildDir}]..."
    asset update --asset_dir ${assetBuildDir} --dest_dir ${buildDir} --visibility='COMMUNITY' --keep
    if [ $? -ne 0 ]; then echo "ERROR: Creating/Updating asset from [${assetBuildDir}] to: [${buildDir}]"; return 1; fi

    echo "INFO: Completed creating asset for: ${assetDirName}"
    return 0
}

function create_asset_build_dir() {

    local assetBuildDir="${1}"
    local assetSrcDir="${2}"
    local distType="${3}"
    local mediaDir="${assetBuildDir}/media"

    echo "INFO: Creating the asset build directory: ${assetBuildDir}"

    # Ensure the required directories exist
    if [ ! -d ${assetSrcDir} ]; then echo "ERROR: Asset directory not found: ${assetSrcDir}"; return 1; fi

    # Create/clean the asset build directory
    if [ -d "${assetBuildDir}" ]; then
        echo "INFO: Cleaning out directory: ${assetBuildDir}"
        rm -Rf ${assetBuildDir}/*
        if [ $? -ne 0 ]; then echo "ERROR: Problem cleaning out: ${assetBuildDir}"; return 1; fi
    else
        echo "INFO: Creating directory: ${assetBuildDir}"
        mkdir -p ${assetBuildDir}
        if [ $? -ne 0 ]; then echo "ERROR: Problem creating directory: ${assetBuildDir}"; return 1; fi
    fi

    # Copy source asset files into the build directory
    echo "INFO: Copying asset files from [${assetSrcDir}] into: [${assetBuildDir}]..."
    cp -Rf ${assetSrcDir}/* ${assetBuildDir}/
    if [ $? -ne 0 ]; then echo "ERROR: Problem copying asset files from [${assetSrcDir}] into: [${assetBuildDir}]"; return 1; fi

    # Ensure a media directory was copied in
    if [ ! -d ${mediaDir} ]; then
        echo "INFO: Creating asset media directory: [${mediaDir}]..."
        mkdir -p ${mediaDir}
        if [ $? -ne 0 ]; then echo "ERROR: Creating asset media directory: [${mediaDir}]"; return 1; fi
    fi

    # Stage distribution to the media directory
    echo "INFO: Looking for distribution named [cons3rtkt] of type [${distType}] in artifacts directory: [${artifactsDir}]..."
    local artifacts=( $(ls ${artifactsDir}/ | grep 'cons3rtkt' | grep "${distType}") )

    # Ensure only 1 was found of each artifact
    if [ ${#artifacts[@]} -ne 1 ]; then echo "ERROR: Expected to find 1 artifact in [${artifactsDir}], found [${#artifacts[@]}]: [${artifacts[@]}]"; return 1; fi

    local artifact="${artifactsDir}/${artifacts[0]}"

    # Stage the artifacts
    echo "INFO: Staging artifact: [${artifact}] to media directory in asset build dir: [${mediaDir}]..."
    cp -f ${artifact} ${mediaDir}/
    if [ $? -ne 0 ]; then echo "ERROR: Staging artifact: [${artifact}] to media directory in asset build dir: [${mediaDir}]"; return 1; fi

    echo "INFO: Completed creating the asset build directory"
    return 0
}

function verify_prerequisites() {
    echo "INFO: Verifying prerequisites..."

    # Ensure the gradle command is installed
    echo "INFO: Checking for the [gradle] command..."
    which gradle >> /dev/null 2>&1
    if [ $? -ne 0 ]; then echo "ERROR: The [gradle] command was not found, please run: [brew install gradle]"; return 1; fi

    # Ensure the asset command is installed
    echo "INFO: Checking for the [asset] command..."
    which asset >> /dev/null 2>&1
    if [ $? -ne 0 ]; then echo "ERROR: The [asset] command was not found, please run: [source venv/bin/activate; python3 -m pip install pycons3rt3]"; return 1; fi

    echo "INFO: Completed verifying prerequisites"
    return 0
}

#######################################################################################################################
# Main script execution

function main() {
    echo "INFO: Building the cons3rtkt application and assets..."
    verify_prerequisites
    if [ $? -ne 0 ]; then echo "ERROR: Problem verifying prerequisites"; return 1; fi
    build_app
    if [ $? -ne 0 ]; then echo "ERROR: Problem building the cons3rtkt distributions"; return 2; fi
    create_asset_build_dir "${assetBuildDirLin}" "${assetSrcDirLin}" 'tar'
    if [ $? -ne 0 ]; then echo "ERROR: Problem creating the Linux asset build directory: ${assetBuildDirLin}"; return 3; fi
    create_asset_build_dir "${assetBuildDirWin}" "${assetSrcDirWin}" 'zip'
    if [ $? -ne 0 ]; then echo "ERROR: Problem creating the Windows asset build directory: ${assetBuildDirWin}"; return 4; fi
    create_asset "${assetBuildDirLin}"
    if [ $? -ne 0 ]; then echo "ERROR: Problem creating the Linux asset"; return 5; fi
    create_asset "${assetBuildDirWin}"
    if [ $? -ne 0 ]; then echo "ERROR: Problem creating the Windows asset"; return 6; fi
    cleanup_asset "${assetBuildDirLin}" "${assetSrcDirLin}"
    if [ $? -ne 0 ]; then echo "ERROR: Problem cleaning up the Linux asset"; return 7; fi
    cleanup_asset "${assetBuildDirWin}" "${assetSrcDirWin}"
    if [ $? -ne 0 ]; then echo "ERROR: Problem cleaning up the Windows asset"; return 8; fi
    echo "INFO: Completed building the cons3rtkt application and assets"
    return 0
}

main
exit $?
