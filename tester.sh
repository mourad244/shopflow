#!/usr/bin/env bash
set -euo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")"

# Sur ce poste, IntelliJ fournit déjà le JDK 21 et Maven.
# JAVA_HOME, s'il est défini par l'utilisateur, reste prioritaire.
if [[ -z "${JAVA_HOME:-}" && -x /opt/intellij-idea-ultimate/jbr/bin/javac ]]; then
    export JAVA_HOME=/opt/intellij-idea-ultimate/jbr
fi

if [[ $# -eq 0 ]]; then
    set -- test
fi

if command -v mvn >/dev/null 2>&1; then
    exec mvn "$@"
elif [[ -x /opt/intellij-idea-ultimate/plugins/maven/lib/maven3/bin/mvn ]]; then
    exec /opt/intellij-idea-ultimate/plugins/maven/lib/maven3/bin/mvn "$@"
else
    echo "Maven est introuvable. Installe Maven 3.9 et un JDK 21, puis lance mvn test." >&2
    exit 1
fi
