#!/usr/bin/env bash
# Gera dist/osrs-translate-ptbr.jar, o jar que o instalar.bat copia para o RuneLite.
# Rode depois de mudar o código: ./scripts/gerar-dist.sh
set -euo pipefail

raiz="$(cd "$(dirname "$0")/.." && pwd)"
cd "$raiz"
./gradlew -q clean jar
mkdir -p dist
jar=$(ls build/libs/*.jar | grep -v -e sources -e javadoc | head -n 1)
cp "$jar" dist/osrs-translate-ptbr.jar
echo "Gerado: dist/osrs-translate-ptbr.jar"
