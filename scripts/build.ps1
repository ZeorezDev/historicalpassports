param([string]$JavaHome, [switch]$GameTests)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
if (-not $JavaHome -and (Test-Path 'C:\Program Files\Java\jdk-17')) { $JavaHome = 'C:\Program Files\Java\jdk-17' }
if (-not $JavaHome) { $JavaHome = $env:JAVA_HOME }
if (-not $JavaHome) { throw 'Java 17 gerekli. -JavaHome ile JDK 17 klasörünü belirtin.' }
$env:JAVA_HOME = $JavaHome
& .\gradlew.bat build --console=plain
if ($LASTEXITCODE -ne 0) { throw 'Gradle build başarısız.' }
if ($GameTests) {
    & .\gradlew.bat runGameTestServer --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Forge GameTest başarısız.' }
}
