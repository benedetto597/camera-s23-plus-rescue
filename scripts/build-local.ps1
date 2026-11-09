$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$ToolsRoot = "C:\Users\bened\.gemini\antigravity\brain\91cc6e57-4416-4b56-a368-6fa258b5dd9b\scratch\tools"

$JdkDir = Join-Path $ToolsRoot "jdk\jdk-17.0.21.0.2+1"
$Javac = Join-Path $JdkDir "bin\javac.exe"
$Java = Join-Path $JdkDir "bin\java.exe"
$Keytool = Join-Path $JdkDir "bin\keytool.exe"

$BuildToolsDir = Join-Path $ToolsRoot "sdk\build-tools\android-14"
$Aapt2 = Join-Path $BuildToolsDir "aapt2.exe"
$Zipalign = Join-Path $BuildToolsDir "zipalign.exe"
$D8Jar = Join-Path $BuildToolsDir "lib\d8.jar"
$ApkSignerJar = Join-Path $BuildToolsDir "lib\apksigner.jar"
$AndroidJar = Join-Path $ToolsRoot "sdk\platforms\android-34\android.jar"

Write-Host "[1/6] Ejecutando pruebas unitarias..."
& powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "test-local.ps1")

$BuildDir = Join-Path $ProjectRoot "build\apk-build"
$DistDir = Join-Path $ProjectRoot "dist"
if (Test-Path $BuildDir) {
    Remove-Item -Recurse -Force $BuildDir
}
New-Item -ItemType Directory -Force -Path "$BuildDir\compiled_res" | Out-Null
New-Item -ItemType Directory -Force -Path "$BuildDir\gen" | Out-Null
New-Item -ItemType Directory -Force -Path "$BuildDir\classes" | Out-Null
New-Item -ItemType Directory -Force -Path "$BuildDir\dex" | Out-Null
New-Item -ItemType Directory -Force -Path $DistDir | Out-Null

Write-Host "[2/6] Compilando recursos Android con aapt2..."
& $Aapt2 compile --dir (Join-Path $ProjectRoot "app\src\main\res") -o "$BuildDir\compiled_res\res.zip"
& $Aapt2 link `
    -I $AndroidJar `
    --manifest (Join-Path $ProjectRoot "app\src\main\AndroidManifest.xml") `
    --min-sdk-version 30 `
    --target-sdk-version 34 `
    --version-code 1 `
    --version-name "1.0.0" `
    -o "$BuildDir\unaligned.apk" `
    --java "$BuildDir\gen" `
    "$BuildDir\compiled_res\res.zip"

Write-Host "[3/6] Compilando codigo fuente Java 17..."
$JavaSources = Get-ChildItem -Path (Join-Path $ProjectRoot "app\src\main\java"), "$BuildDir\gen" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
& $Javac -encoding UTF-8 -source 17 -target 17 -classpath $AndroidJar -d "$BuildDir\classes" $JavaSources
if ($LASTEXITCODE -ne 0) { throw "Fallo en compilacion javac" }

Write-Host "[4/6] Convirtiendo bytecode a DEX con d8..."
$ClassFiles = Get-ChildItem -Path "$BuildDir\classes" -Recurse -Filter "*.class" | Select-Object -ExpandProperty FullName
& $Java -cp $D8Jar com.android.tools.r8.D8 --min-api 30 --lib $AndroidJar --output "$BuildDir\dex" $ClassFiles
if ($LASTEXITCODE -ne 0) { throw "Fallo en d8" }

Write-Host "[5/6] Empaquetando y alineando APK (zipalign)..."
Copy-Item "$BuildDir\unaligned.apk" "$BuildDir\unsigned.apk"
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$Zip = [System.IO.Compression.ZipFile]::Open("$BuildDir\unsigned.apk", [System.IO.Compression.ZipArchiveMode]::Update)
[System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($Zip, "$BuildDir\dex\classes.dex", "classes.dex") | Out-Null
$Zip.Dispose()

& $Zipalign -f -p 4 "$BuildDir\unsigned.apk" "$BuildDir\aligned.apk"

Write-Host "[6/6] Firmando APK (v1 + v2 + v3) con apksigner..."
$Keystore = Join-Path $BuildDir "debug.keystore"
& $Keytool -genkeypair -v -keystore $Keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US" | Out-Null

$FinalApk = Join-Path $DistDir "s23-plus-camera-v1.0.0.apk"
& $Java -jar $ApkSignerJar sign --ks $Keystore --ks-pass pass:android --key-pass pass:android --out $FinalApk "$BuildDir\aligned.apk"

Write-Host "EXITO: APK generado y firmado en $FinalApk"
Get-Item $FinalApk | Select-Object FullName, Length, LastWriteTime
