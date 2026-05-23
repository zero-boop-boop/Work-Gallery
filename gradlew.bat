@echo off
setlocal

set JAVA_HOME=D:\Android Developers Tool\jbr
set GRADLE_HOME=%~dp0.gradle-local\gradle-8.9

if not exist "%GRADLE_HOME%\bin\gradle.bat" (
    echo ERROR: Gradle not found at %GRADLE_HOME%
    exit /b 1
)

call "%GRADLE_HOME%\bin\gradle.bat" %*
endlocal
