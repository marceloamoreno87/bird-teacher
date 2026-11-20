[CmdletBinding()]
param(
    [string]$Distribution,
    [string]$Destination = (Join-Path $PSScriptRoot '..\app\build\windows')
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
if (!$env:JAVA_HOME) { throw 'Defina JAVA_HOME para a pasta do JDK 21 x64.' }
$jpackage = Join-Path $env:JAVA_HOME 'bin\jpackage.exe'
if (!(Test-Path $jpackage)) { throw "jpackage.exe ausente em $env:JAVA_HOME" }
if (!$Distribution) {
    Push-Location $root
    try {
        & .\gradlew.bat :app:installDist
        if ($LASTEXITCODE) { throw 'Falha ao compilar o Bird Teacher.' }
    } finally { Pop-Location }
    $Distribution = Join-Path $root 'app\build\install\bird-teacher'
}
$inputDir = (Resolve-Path (Join-Path $Distribution 'lib')).Path
$destinationDir = [IO.Directory]::CreateDirectory($Destination).FullName
$stage = [IO.Directory]::CreateDirectory((Join-Path ([IO.Path]::GetTempPath()) ([guid]::NewGuid().ToString()))).FullName
try {
    $emblem = Join-Path $root 'app\src\main\resources\br\birdteacher\app\art\bird-teacher.ico'
    & $jpackage --type app-image --name 'Bird Teacher' --app-version '1.7.1' --icon $emblem --dest $stage --input $inputDir --main-jar app.jar --main-class br.birdteacher.app.Main --add-modules 'java.base,java.desktop,java.logging,java.net.http,java.prefs,java.management,java.xml,jdk.crypto.ec,jdk.unsupported'
    if ($LASTEXITCODE) { throw 'Falha ao criar o aplicativo Windows.' }
    Copy-Item $emblem (Join-Path $stage 'Bird Teacher\falcao.ico')
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = Join-Path $stage 'Bird-Teacher-Windows-x64.zip'
    [IO.Compression.ZipFile]::CreateFromDirectory((Join-Path $stage 'Bird Teacher'), $zip, [IO.Compression.CompressionLevel]::Optimal, $true)
    Move-Item -Force $zip (Join-Path $destinationDir 'Bird-Teacher-Windows-x64.zip')
    Write-Output (Join-Path $destinationDir 'Bird-Teacher-Windows-x64.zip')
} finally { Remove-Item -Recurse -Force $stage }
