#!/bin/bash

# Source the environment
if [ -f /etc/bashrc ] ; then
    . /etc/bashrc
fi
if [ -f /etc/profile ] ; then
    . /etc/profile
fi

# Establish a log file and log tag
logTag="cons3rtkt-install"
logDir="/opt/cons3rt-agent/log"
logFile="${logDir}/${logTag}-$(date "+%Y%m%d-%H%M%S").log"

######################### GLOBAL VARIABLES #########################

# Parent directory for cons3rtkt
cons3rtktHome='/opt'

# Symlink
cons3rtktLink="${cons3rtktHome}/cons3rtkt"

####################### END GLOBAL VARIABLES #######################

# Logging functions
function timestamp() { date "+%F %T"; }
function logInfo() { echo -e "$(timestamp) ${logTag} [INFO]: ${1}" >> ${logFile}; }
function logWarn() { echo -e "$(timestamp) ${logTag} [WARN]: ${1}" >> ${logFile}; }
function logErr() { echo -e "$(timestamp) ${logTag} [ERROR]: ${1}" >> ${logFile}; }

function add_env() {
    logInfo "Adding cons3rtkt to the environment for all users..."

    # Get the globalEnvFile based on what is available
    if [ -f /etc/bashrc ]; then
        globalEnvFile='/etc/bashrc'
    elif [ -f /etc/profile ]; then
        globalEnvFile='/etc/profile'
    else
        logErr "Global env file not found"
        return 1
    fi

    logInfo "Adding the bash_cons3rt library to the environment for all users"
    sed -i "/# cons3rtkt/d" ${globalEnvFile}
    sed -i "/cons3rtkt.sh/d" ${globalEnvFile}
    echo -e "\n# cons3rtkt" >> ${globalEnvFile}
    echo ". /usr/local/cons3rtkt.sh" >> ${globalEnvFile}
    echo -e "\n" >> ${globalEnvFile}
    logInfo "Completed adding cons3rtkt to the environment for all users"
    return 0
}

function create_env_file() {
# This is an example for how to create a dynamic config file
logInfo "Creating the environment file..."
cat << EOF >> /usr/local/cons3rtkt.sh
export JAVA_HOME=/opt/cons3rt-agent/tools/jre
export PATH="/opt/cons3rtkt/bin:$PATH"
EOF
chmod 755 /usr/local/cons3rtkt.sh
}

function install_cons3rtkt() {
    logInfo "Installing cons3rtkt..."

    # Ensure the media directory exists
    if [ ! -d ${mediaDir} ]; then logErr "Media directory not found: [${mediaDir}]"; return 1; fi

    # Clean up existing installations
    local existingInstallItems=( $(ls ${cons3rtktHome}/ | grep 'cons3rtkt') )
    for existingInstallItem in "${existingInstallItems[@]}"; do
        local existingInstallItemPath="${cons3rtktHome}/${existingInstallItem}"
        logInfo "Removing existing installation item: [${existingInstallItemPath}]..."
        rm -Rf ${existingInstallItemPath} >> ${logFile} 2>&1
        if [ $? -ne 0 ]; then logErr "Removing existing installation item: [${existingInstallItemPath}]"; return 1; fi
    done

    # Find the cons3rtkt distribution
    local installers=( $(ls ${mediaDir}/ | grep 'cons3rtkt' | grep 'tar') )
    local installersStr="${installers[@]}"

    # Check the number of files found
    if [ ${#installers[@]} -ne 1 ]; then logErr "Expected 1 installer in [${mediaDir}], found [${#installers[@]}]: [${installersStr}]"; return 1; fi
    local installer="${installers[0]}"
    logInfo "Found installer: [${installer}]"

    # Ensure the installer exists
    installerPath="${mediaDir}/${installer}"
    if [ ! -f ${installerPath} ]; then logErr "cons3rtkt distribution not found: [${installerPath}]"; return 1; fi

    # Extract the installer
    logInfo "Extracting [${installerPath}] to: [${cons3rtktHome}]..."
    tar -xvf ${installerPath} -C ${cons3rtktHome} >> ${logFile} 2>&1
    if [ $? -ne 0 ]; then logErr "Extracting [${installerPath}] to: [${cons3rtktHome}]"; return 1; fi

    # Find the resulting extracted directory
    local extractedDirs=( $(ls ${cons3rtktHome}/ | grep 'cons3rtkt-') )
    local extractedDirsStr="${installers[@]}"
    if [ ${#extractedDirs[@]} -ne 1 ]; then logErr "Expected 1 extracted directory in [${cons3rtktHome}], found [${#extractedDirs[@]}]: [${extractedDirsStr}]"; return 1; fi
    local extractedDir="${extractedDirs[0]}"
    logInfo "Found extracted directory: [${extractedDir}]"

    # Ensure the extracted directory exists
    extractedDirPath="${cons3rtktHome}/${extractedDir}"
    if [ ! -d ${extractedDirPath} ]; then logErr "cons3rtkt extracted directory not found: [${extractedDirPath}]"; return 1; fi

    # Create the symlink
    logInfo "Creating symlink for [${extractedDirPath}] to: [${cons3rtktLink}]..."
    ln -sf ${extractedDirPath} ${cons3rtktLink} >> ${logFile} 2>&1
    if [ $? -ne 0 ]; then logErr "Creating symlink for [${extractedDirPath}] to: [${cons3rtktLink}]"; return 1; fi

    logInfo "Completed installing cons3rtkt"
    return 0
}

function set_asset_dir() {
    # Ensure ASSET_DIR exists, if not assume this script exists in ASSET_DIR/scripts
    SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
    if [ -z "${ASSET_DIR}" ] ; then
        logWarn "ASSET_DIR not found, assuming ASSET_DIR is 1 level above this script ..."
        export ASSET_DIR="${SCRIPT_DIR}/.."
    fi
    mediaDir="${ASSET_DIR}/media"
}


function main() {
    logInfo "Running: ${logTag}"
    set_asset_dir
    install_cons3rtkt
    if [ $? -ne 0 ]; then logErr "Problem installing cons3rtkt"; return 1; fi
    create_env_file
    add_env
    if [ $? -ne 0 ]; then logErr "Problem configuring the environment"; return 2; fi
    logInfo "Successfully completed: ${logTag}"
    return 0
}

# Set up the log file
mkdir -p ${logDir}
chmod 755 ${logDir}
touch ${logFile}
chmod 644 ${logFile}
main
result=$?
logInfo "Exiting with code ${result} ..."
cat ${logFile}
exit ${result}
