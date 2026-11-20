@echo off
setlocal
cd /d "%~dp0"
if not exist "client\target\traffic-client-1.0.0.jar" (
  call mvnw.cmd -f client/pom.xml clean package
  if errorlevel 1 exit /b 1
)
java -jar client\target\traffic-client-1.0.0.jar %*
