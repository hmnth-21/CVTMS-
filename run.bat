@echo off
setlocal EnableExtensions

call build.bat
if %ERRORLEVEL% NEQ 0 (
	echo Backend build failed. Aborting run.
	endlocal
	exit /b 1
)

if /I "%~1"=="api" goto :run_api

echo Running CVTMS console application...
java -cp "bin;lib\*" com.cvtms.ui.MainMenu
set "APP_EXIT=%ERRORLEVEL%"
endlocal
exit /b %APP_EXIT%

:run_api
powershell -NoProfile -Command "$conn = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1; if ($conn) { $proc = Get-Process -Id $conn.OwningProcess -ErrorAction SilentlyContinue; if ($proc -and $proc.ProcessName -ieq 'java') { Write-Host ('Existing Java process on port 8080 detected (PID ' + $conn.OwningProcess + '). Restarting...'); Stop-Process -Id $conn.OwningProcess -Force } else { Write-Host ('Port 8080 is already in use by a different process (PID ' + $conn.OwningProcess + '). Stop that process and run again.'); exit 1 } }"
if %ERRORLEVEL% NEQ 0 (
	endlocal
	exit /b 1
)

echo Running CVTMS API backend...
java -cp "bin;lib\*" com.cvtms.api.ApiServer
set "APP_EXIT=%ERRORLEVEL%"
endlocal
exit /b %APP_EXIT%

