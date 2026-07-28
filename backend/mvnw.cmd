@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM ----------------------------------------------------------------------------

@echo off
setlocal

set MAVEN_CMD="C:\Program Files\JetBrains\IntelliJ IDEA 2025.3\plugins\maven\lib\maven3\bin\mvn.cmd"

if exist %MAVEN_CMD% (
    %MAVEN_CMD% %*
) else (
    echo Maven executable not found at %MAVEN_CMD%. Please ensure Maven or IntelliJ IDEA is installed.
    exit /b 1
)
