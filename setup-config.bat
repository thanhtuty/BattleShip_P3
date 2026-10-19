@echo off
cd /d "%~dp0"
if not exist config\db.properties copy config\db.properties.example config\db.properties
notepad config\db.properties
