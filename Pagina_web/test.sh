#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/classes
find src/main/java src/test/java -name '*.java' > build/test-sources.txt
javac -encoding UTF-8 -d build/classes @build/test-sources.txt
java -cp build/classes co.edu.uco.orbita.Pruebas
