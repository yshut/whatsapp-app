param(
 [string]$SdkRoot=$env:ANDROID_SDK_ROOT,
 [string]$JavaHome=$env:JAVA_HOME,
 [string]$GradleHome,
 [switch]$WithTests
)
$ErrorActionPreference='Stop'
if(-not $SdkRoot){$SdkRoot=$env:ANDROID_HOME}
if(-not $SdkRoot -or -not(Test-Path -LiteralPath $SdkRoot -PathType Container)){throw 'Set ANDROID_SDK_ROOT or pass -SdkRoot.'}
if(-not $JavaHome -or -not(Test-Path -LiteralPath (Join-Path $JavaHome 'bin/java.exe') -PathType Leaf)){throw 'Set JAVA_HOME to a JDK 17 installation or pass -JavaHome.'}
foreach($component in @('platforms/android-36/android.jar','build-tools/36.0.0','ndk/27.1.12297006')){
 if(-not(Test-Path -LiteralPath (Join-Path $SdkRoot $component))){throw "Missing Android SDK component: $component"}
}
$env:JAVA_HOME=$JavaHome
$env:ANDROID_SDK_ROOT=$SdkRoot
$env:ANDROID_HOME=$SdkRoot
$gradleCommand=Join-Path $PSScriptRoot 'gradlew.bat'
if($GradleHome){$gradleCommand=Join-Path $GradleHome 'bin/gradle.bat'}
if(-not(Test-Path -LiteralPath $gradleCommand -PathType Leaf)){throw 'Gradle launcher is missing.'}
$tasks=@(':manager:assembleDebug')
if($WithTests){$tasks+= ':manager:assembleDebugAndroidTest'}
Push-Location $PSScriptRoot
try {
 & $gradleCommand @tasks --console=plain --no-daemon --max-workers=2
 if($LASTEXITCODE -ne 0){throw "Build failed: $LASTEXITCODE"}
} finally {Pop-Location}
