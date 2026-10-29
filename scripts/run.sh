#!/usr/bin/env bash
# Starts the screen at http://127.0.0.1:<port>/ (Linux, macOS, Git Bash on Windows).
# Run "mvn test" once first: it compiles target/classes and downloads the libraries into ~/.m2.
# The library list below is the compile classpath of poi-ooxml 5.5.1 with log4j-api pinned to 2.25.5 (see pom.xml).
set -euo pipefail

port="${1:-8080}"
root="$(cd "$(dirname "$0")/.." && pwd)"
m2="${HOME}/.m2/repository"
libs=(
  "org/apache/poi/poi-ooxml/5.5.1/poi-ooxml-5.5.1.jar"
  "org/apache/poi/poi/5.5.1/poi-5.5.1.jar"
  "commons-codec/commons-codec/1.20.0/commons-codec-1.20.0.jar"
  "org/apache/commons/commons-math3/3.6.1/commons-math3-3.6.1.jar"
  "com/zaxxer/SparseBitSet/1.3/SparseBitSet-1.3.jar"
  "org/apache/poi/poi-ooxml-lite/5.5.1/poi-ooxml-lite-5.5.1.jar"
  "org/apache/xmlbeans/xmlbeans/5.3.0/xmlbeans-5.3.0.jar"
  "org/apache/commons/commons-compress/1.28.0/commons-compress-1.28.0.jar"
  "org/apache/commons/commons-lang3/3.18.0/commons-lang3-3.18.0.jar"
  "commons-io/commons-io/2.21.0/commons-io-2.21.0.jar"
  "com/github/virtuald/curvesapi/1.08/curvesapi-1.08.jar"
  "org/apache/logging/log4j/log4j-api/2.25.5/log4j-api-2.25.5.jar"
  "org/apache/commons/commons-collections4/4.5.0/commons-collections4-4.5.0.jar"
)

[ -d "$root/target/classes" ] || { echo "target/classes not found. Run 'mvn test' in $root first." >&2; exit 1; }
sep=":"
topath() { printf '%s' "$1"; }
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) sep=";"; topath() { cygpath -w "$1"; } ;;
esac

cp="$(topath "$root/target/classes")"
for lib in "${libs[@]}"; do
  [ -f "$m2/$lib" ] || { echo "missing library $m2/$lib. Run 'mvn test' in $root first." >&2; exit 1; }
  cp="$cp$sep$(topath "$m2/$lib")"
done

java_bin="java"
[ -n "${JAVA_HOME:-}" ] && java_bin="$JAVA_HOME/bin/java"
exec "$java_bin" -cp "$cp" dev.mfgdiff.web.Server --port "$port"
