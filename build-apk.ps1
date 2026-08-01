# ==============================================================================
# Automatic APK build script - Windows
# Right-click this file and choose "Run with PowerShell"
# or run in PowerShell:
#   powershell -ExecutionPolicy Bypass -File .\build-apk.ps1
# ==============================================================================

$ErrorActionPreference = "Stop"

Write-Host "=========================================="
Write-Host "  Automatic APK Build - SMS Location Finder"
Write-Host "=========================================="
Write-Host ""

# --- Step 1: Check Java ---
try {
    $javaVersion = & java -version 2>&1
    Write-Host "Java found:"
    Write-Host $javaVersion
} catch {
    Write-Host "Java was not found."
    Write-Host ""
    Write-Host "Please install Java (JDK 17) from https://adoptium.net"
    Write-Host "then run this script again."
    Write-Host ""
    Write-Host "Press Enter to exit..."
    Read-Host
    exit 1
}
Write-Host ""

# --- Step 2: Set Android SDK install location ---
$SdkDir = "$env:USERPROFILE\android-sdk-minimal"
$CmdlineToolsVersion = "11076708"
$CmdlineToolsUrl = "https://dl.google.com/android/repository/commandlinetools-win-${CmdlineToolsVersion}_latest.zip"

Write-Host "Android SDK install path: $SdkDir"
Write-Host ""

# --- Step 3: Download and install Command Line Tools ---
if (-Not (Test-Path "$SdkDir\cmdline-tools\latest")) {
    Write-Host "Downloading Android Command Line Tools..."
    New-Item -ItemType Directory -Force -Path "$SdkDir\cmdline-tools" | Out-Null
    $TmpZip = "$env:TEMP\cmdline-tools.zip"
    Invoke-WebRequest -Uri $CmdlineToolsUrl -OutFile $TmpZip

    Write-Host "Extracting..."
    Expand-Archive -Path $TmpZip -DestinationPath "$SdkDir\cmdline-tools" -Force
    Move-Item "$SdkDir\cmdline-tools\cmdline-tools" "$SdkDir\cmdline-tools\latest"
    Remove-Item $TmpZip
    Write-Host "Command Line Tools installed."
} else {
    Write-Host "Android Command Line Tools already installed."
}
Write-Host ""

$env:ANDROID_SDK_ROOT = $SdkDir
$env:ANDROID_HOME = $SdkDir
$SdkManager = "$SdkDir\cmdline-tools\latest\bin\sdkmanager.bat"

# --- Step 4: Accept licenses ---
Write-Host "Accepting Android SDK licenses..."
$licenseInput = "y`ny`ny`ny`ny`ny`ny`ny`n"
$licenseInput | & $SdkManager --licenses | Out-Null
Write-Host "Licenses accepted."
Write-Host ""

# --- Step 5: Install required packages ---
Write-Host "Installing platform-tools, build-tools and platform-34 (this can take a few minutes)..."
& $SdkManager "platform-tools" "platforms;android-34" "build-tools;34.0.0" | Out-Null
Write-Host "SDK packages installed."
Write-Host ""

# --- Step 6: Create local.properties for Gradle ---
$ScriptDir = $PSScriptRoot
$SdkDirEscaped = $SdkDir -replace '\\', '\\\\'
"sdk.dir=$SdkDirEscaped" | Out-File -FilePath "$ScriptDir\local.properties" -Encoding ASCII
Write-Host "local.properties file created."
Write-Host ""

# --- Step 7: Run Gradle build ---
Write-Host "Building APK (this can take a few minutes)..."
Set-Location $ScriptDir
& .\gradlew.bat assembleDebug --no-daemon

Write-Host ""
$ApkPath = "$ScriptDir\app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $ApkPath) {
    Write-Host "=========================================="
    Write-Host "APK build finished successfully!"
    Write-Host "File location:"
    Write-Host "   $ApkPath"
    Write-Host "=========================================="
    Write-Host ""
    Write-Host "Now copy this file to your phone (USB cable, cloud, etc.) and install it."
} else {
    Write-Host "Something went wrong. The APK file was not created."
    Write-Host "Press Enter to exit..."
    Read-Host
    exit 1
}

Write-Host ""
Write-Host "Press Enter to close this window..."
Read-Host
