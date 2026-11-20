@echo off
setlocal
cd /d "%~dp0"
if not exist "server\target\traffic-server-1.0.0.jar" (
  call mvnw.cmd -f server/pom.xml clean package
  if errorlevel 1 exit /b 1
)
java -jar server\target\traffic-server-1.0.0.jar %*
