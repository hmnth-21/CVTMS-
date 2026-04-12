@echo off
setlocal

echo Cleaning bin directory...
if exist bin rmdir /s /q bin
mkdir bin

echo Compiling backend source files (src\main\java)...
:: Find only main source files (exclude tests)
dir /S /B src\main\java\*.java > sources.txt

for %%A in (sources.txt) do if %%~zA==0 (
    echo No Java source files found under src\main\java
    del sources.txt
    endlocal
    exit /b 1
)

:: Compile the classes
javac -d bin -cp "lib\*" @sources.txt

if %ERRORLEVEL% EQU 0 (
    echo Compilation successful!
) else (
    echo Compilation failed!
    del sources.txt
    endlocal
    exit /b 1
)

:: Clean up the temp file
del sources.txt

endlocal
