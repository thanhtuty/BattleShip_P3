@echo off
cd /d "%~dp0"
call mvn -q compile exec:java -Dexec.mainClass=battleship.part3.server.Server
pause
