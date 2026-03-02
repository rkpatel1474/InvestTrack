@echo off
setlocal
set GRADLE_OPTS=%GRADLE_OPTS% -Xmx2048m
"%JAVA_HOME%\bin\java" -jar "%~dp0gradle\wrapper\gradle-wrapper.jar" %*
