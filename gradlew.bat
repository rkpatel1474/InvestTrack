@echo off
setlocal
set GRADLE_OPTS=%GRADLE_OPTS% -Xmx2048m
set "WRAPPER_JAR=%~dp0gradle\wrapper\gradle-wrapper.jar"

if not "%JAVA_HOME%"=="" (
  "%JAVA_HOME%\bin\java" -jar "%WRAPPER_JAR%" %*
  exit /b %ERRORLEVEL%
)

java -jar "%WRAPPER_JAR%" %*
