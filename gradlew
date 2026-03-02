#!/bin/sh
GRADLE_OPTS="${GRADLE_OPTS:-"-Xmx2048m -Dfile.encoding=UTF-8"}"
APP_HOME="$(cd "$(dirname "$0")" && pwd)"
exec "${JAVA_HOME:-$(which java)}" $GRADLE_OPTS -jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"
