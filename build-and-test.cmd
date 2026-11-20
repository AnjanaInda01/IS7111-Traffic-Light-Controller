@echo off
setlocal
cd /d "%~dp0"
call mvnw.cmd -f server/pom.xml clean verify
if errorlevel 1 exit /b 1
call mvnw.cmd -f client/pom.xml clean verify
if errorlevel 1 exit /b 1
echo Both projects built and tested successfully.
