@REM Maven Wrapper startup batch script
@REM ============================================================================
@setlocal
@set WRAPPER_JAR="%~dp0\.mvn\wrapper\maven-wrapper.jar"
@set WRAPPER_PROPERTIES="%~dp0\.mvn\wrapper\maven-wrapper.properties"
@FOR /F "usebackq tokens=1,2 delims==" %%A IN (%WRAPPER_PROPERTIES%) DO @(
    IF "%%A"=="distributionUrl" SET MAVEN_DIST_URL=%%B
)
@set MAVEN_PROJECTBASEDIR=%~dp0
@java -jar %WRAPPER_JAR% %*
