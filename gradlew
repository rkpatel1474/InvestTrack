#!/bin/sh
APP_HOME="$(cd "$(dirname "$0")" && pwd)"
exec "$JAVA_HOME/bin/java" -jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"
