#!/usr/bin/env pwsh
<#
Installs the cons3rtkt command line application on Windows
#>

# Set the Error action preference when an exception is caught
$ErrorActionPreference = "Stop"

# Start a stopwatch to record asset run time
$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

########################### VARIABLES ###############################

# Get the CONS3RT environment variables
$global:ASSET_DIR = $null

# JDK install variables
$installerFileNamePart   = "cons3rtkt"
$cons3rtktHome           = "C:\Cons3rtKt"
$cons3rtktLink           = "$cons3rtktHome\cons3rtkt"
$javaHome                = "C:\cons3rt-agent\tools\jre"

# exit code
$exitCode = 0

# Configure the log file
$logTag = 'cons3rtkt-install'
$logFileTimestamp = Get-Date -f "yyyyMMdd-HHmmss"
$logFile = "C:\cons3rt-agent\log\$logTag-$logFileTimestamp.log"

######################### END VARIABLES #############################

######################## HELPER FUNCTIONS ############################

# Logging methods
function logger($level, $logstring) {
    $stamp = get-date -f "yyyyMMdd HH:mm:ss"
    "$stamp $logTag [$level]: $logstring"
}
function logErr($logstring) { logger "ERROR" $logstring }
function logWarn($logstring) { logger "WARNING" $logstring }
function logInfo($logstring) { logger "INFO" $logstring }

function get_asset_dir() {
    if ($env:ASSET_DIR) {
        $global:ASSET_DIR = $env:ASSET_DIR
        return
    }
    else {
        logWarn "ASSET_DIR environment variable not set, attempting to determine..."
        if (!$PSScriptRoot) {
            logInfo "Determining script directory using the pre-Powershell v3 method..."
            $scriptDir = split-path -parent $MyInvocation.MyCommand.Definition
        }
        else {
            logInfo "Determining the script directory using the PSScriptRoot variable..."
            $scriptDir = $PSScriptRoot
        }
        if (!$scriptDir) {
            $msg =  "Unable to determine the script directory to get ASSET_DIR"
            logErr $msg
            throw $msg
        }
        else {
            $global:ASSET_DIR = "$scriptDir\.."
            logInfo "Determined ASSET_DIR to be: $global:ASSET_DIR"
        }
    }
}


###################### END HELPER FUNCTIONS ##########################

######################## SCRIPT EXECUTION ############################

new-item $logfile -itemType file -force
start-transcript -append -path $logfile
logInfo "Running $LOGTAG..."

try {
    logInfo "Installing: $logTag"
    
    # Set asset dir
    logInfo "Setting ASSET_DIR..."
    get_asset_dir

	logInfo "ASSET_DIR: $global:ASSET_DIR"
	$mediaDir="$ASSET_DIR\media"

	# Exit if the media directory is not found
	if ( !(test-path $mediaDir) ) {
		$errMsg = "media directory not found: $mediaDir"
		logErr $errMsg
		throw $errMsg
	}
	else {
	    logInfo "Found the media directory: $mediaDir"
	}

	# Remove existing installation
	if (Test-Path $cons3rtktHome) {
	    $msg = "Removing existing installation: $cons3rtktHome"; logInfo "$msg ..."
	    Remove-Item $cons3rtktHome -recurse -force
	    if ($? -eq $False) { logErr $msg; throw $msg }
	}

	# Create the installation directory
	$msg = "Creating installation directory: $cons3rtktHome"; logInfo "$msg ..."
	New-Item -ItemType Directory -Force -Path $cons3rtktHome
	if ($? -eq $False) { logErr $msg; throw $msg }

    # Get the installer file name
    $msg = "Attempting to find the installer with file name part [$installerFileNamePart] in directory: $mediaDir"; logInfo "$msg ..."
    $installerFileName = Get-ChildItem $mediaDir -name | Select-String "$installerFileNamePart"
    $installerFileName = "$installerFileName"
    if (!$installerFileName) { logErr $msg; throw $msg }
    logInfo "Found installer file name: $installerFileName"
    
    # Set the installer file path
    $installerFilePath = "$mediaDir\$installerFileName"
    $msg = "Extracting installer file [$installerFilePath] to folder: $cons3rtktHome"; logInfo "$msg ..."
    Expand-Archive -LiteralPath $installerFilePath -DestinationPath $cons3rtktHome
    if ($? -eq $False) { logErr $msg; throw $msg }
    
    # Ensure the extracted directory was created
    $msg = "Checking for the extracted directory in [$cons3rtktHome]"; logInfo "$msg ..."
    $extractedDirName = Get-ChildItem $cons3rtktHome -name | Select-String "cons3rtkt-"
    if (!$extractedDirName) { logErr $msg; throw $msg }

    # Ensure the extracted directory exists
    $extractedDirPath="$cons3rtktHome\$extractedDirName"
    $msg = "Checking for the extracted directory: $extractedDirPath"
    if (-Not (Test-Path $extractedDirPath)) { logErr $msg; throw $msg }
    logInfo "Extracted the installer to: $extractedDirPath"
    
    # Create the symbolic to the extracted directory
    $msg = "Creating new symbolic link from [$extractedDirPath] to: [$cons3rtktLink]"; logInfo "$msg ..."
    New-Item -Path $cons3rtktLink -ItemType SymbolicLink -Value $extractedDirPath
    if ($? -eq $False) { logErr $msg; throw $msg }
    
    # Ensure the extracted directory was created
    $msg = "Validating symbolic link: $cons3rtktLink"; logInfo "$msg ..."
    if (-Not (Test-Path $cons3rtktLink)) { logErr $msg; throw $msg }
    
    # Set JAVA_HOME
    $msg = "Setting JAVA_HOME to: $javaHome"; logInfo "$msg"
    [Environment]::SetEnvironmentVariable("JAVA_HOME", "$javaHome", "Machine")
    if ($? -eq $False) { logErr $msg; throw $msg }
    
    # Get the current PATH environment variable
    $env:PATH = [Environment]::GetEnvironmentVariable("PATH", "Machine")
    logInfo "Found the current PATH env variable: $env:PATH"
    
    # Add JAVA_HOME\bin to the Windows search Path
    $msg = "Adding JAVA_HOME\bin to the Path Environment Variable"; logInfo "$msg"
    [Environment]::SetEnvironmentVariable("Path", [System.Environment]::GetEnvironmentVariable("Path", [System.EnvironmentVariableTarget]::Machine) + ";$env:JAVA_HOME\bin", [System.EnvironmentVariableTarget]::Machine)
    if ($? -eq $False) { logErr $msg; throw $msg }
}
catch {
    logErr "Caught exception after $($stopwatch.Elapsed): $_"
    $exitCode = 1
    $kill = (gwmi win32_process -Filter processid=$pid).parentprocessid
    if ( (Get-Process -Id $kill).ProcessName -eq "cmd" ) {
        logErr "Exiting using taskkill..."
        Stop-Transcript
        TASKKILL /PID $kill /T /F
    }
}
finally {
    logInfo "$LOGTAG complete in $($stopwatch.Elapsed)"
}

###################### END SCRIPT EXECUTION ##########################

logInfo "Exiting with code: $exitCode"
stop-transcript
get-content -Path $logfile
exit $exitCode
