# Starts the screen at http://127.0.0.1:<port>/ (Windows PowerShell 5.1 or later).
# Run "mvn test" once first: it compiles target/classes and downloads the libraries into ~/.m2.
# The library list below is the compile classpath of poi-ooxml 5.5.1 with log4j-api pinned to 2.25.5 (see pom.xml).
param([int]$Port = 8080)
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$m2 = Join-Path $env:USERPROFILE ".m2\repository"
$libs = @(
    "org/apache/poi/poi-ooxml/5.5.1/poi-ooxml-5.5.1.jar",
    "org/apache/poi/poi/5.5.1/poi-5.5.1.jar",
    "commons-codec/commons-codec/1.20.0/commons-codec-1.20.0.jar",
    "org/apache/commons/commons-math3/3.6.1/commons-math3-3.6.1.jar",
    "com/zaxxer/SparseBitSet/1.3/SparseBitSet-1.3.jar",
    "org/apache/poi/poi-ooxml-lite/5.5.1/poi-ooxml-lite-5.5.1.jar",
    "org/apache/xmlbeans/xmlbeans/5.3.0/xmlbeans-5.3.0.jar",
    "org/apache/commons/commons-compress/1.28.0/commons-compress-1.28.0.jar",
    "org/apache/commons/commons-lang3/3.18.0/commons-lang3-3.18.0.jar",
    "commons-io/commons-io/2.21.0/commons-io-2.21.0.jar",
    "com/github/virtuald/curvesapi/1.08/curvesapi-1.08.jar",
    "org/apache/logging/log4j/log4j-api/2.25.5/log4j-api-2.25.5.jar",
    "org/apache/commons/commons-collections4/4.5.0/commons-collections4-4.5.0.jar"
)

$classes = Join-Path $root "target\classes"
if (-not (Test-Path $classes)) { throw "target\classes not found. Run 'mvn test' in $root first." }
$parts = @($classes)
foreach ($lib in $libs) {
    $p = Join-Path $m2 ($lib -replace "/", "\")
    if (-not (Test-Path $p)) { throw "missing library $p. Run 'mvn test' in $root first." }
    $parts += $p
}
$java = "java"
if ($env:JAVA_HOME) { $java = Join-Path $env:JAVA_HOME "bin\java.exe" }
& $java -cp ($parts -join ";") dev.mfgdiff.web.Server --port $Port
