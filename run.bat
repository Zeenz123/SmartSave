@echo off
cd /d "%~dp0"
echo Starting SmartSave Application...
call gradlew.bat run
pause
