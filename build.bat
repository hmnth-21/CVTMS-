@echo off
setlocal

echo Cleaning bin directory...
if exist bin rmdir /s /q bin
mkdir bin

echo Compiling Java source files...
:: Find all Java source files
dir /S /B src\*.java > sources.txt

:: Compile the classes
javac -d bin -cp "lib\*" @sources.txt

if %ERRORLEVEL% EQU 0 (
    echo Compilation successful!
) else (
    echo Compilation failed!
)

:: Clean up the temp file
del sources.txt

endlocal
