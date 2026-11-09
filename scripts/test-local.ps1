$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$Javac = "C:\Users\bened\.gemini\antigravity\brain\91cc6e57-4416-4b56-a368-6fa258b5dd9b\scratch\tools\jdk\jdk-17.0.21.0.2+1\bin\javac.exe"
$Java = "C:\Users\bened\.gemini\antigravity\brain\91cc6e57-4416-4b56-a368-6fa258b5dd9b\scratch\tools\jdk\jdk-17.0.21.0.2+1\bin\java.exe"

if (-not (Test-Path $Javac)) {
    $Javac = "javac"
    $Java = "java"
}

$OutDir = Join-Path $ProjectRoot "build\test-classes"
if (Test-Path $OutDir) {
    Remove-Item -Recurse -Force $OutDir
}
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

& $Javac -encoding UTF-8 -d $OutDir `
    (Join-Path $ProjectRoot "app\src\main\java\com\s23plus\camera\CameraConfig.java") `
    (Join-Path $ProjectRoot "app\src\test\java\com\s23plus\camera\CameraConfigTest.java")
if ($LASTEXITCODE -ne 0) { throw "Fallo compilando CameraConfigTest" }

& $Java -cp $OutDir com.s23plus.camera.CameraConfigTest
if ($LASTEXITCODE -ne 0) { throw "Fallo ejecutando CameraConfigTest" }
