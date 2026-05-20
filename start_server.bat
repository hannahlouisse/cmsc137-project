@echo off
title Guess The Impostor - SERVER
color 0A

echo =========================================
echo    GUESS THE IMPOSTOR - GAME SERVER
echo =========================================
echo.

:: Create bin folder if it doesn't exist
if not exist "bin" mkdir bin

echo [1/4] Cleaning previous compilation...
del /q bin\*.class 2>nul

echo [2/4] Compiling server files...
javac -d bin src/server/*.java src/utils/*.java src/model/*.java src/controller/*.java

if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b 1
)

echo [3/4] Compilation successful!
echo [4/4] Starting server...

echo.
echo =========================================
echo Server is running on port 12345
echo Press Ctrl+C to stop the server
echo =========================================
echo.

:: Run the server
java -cp bin server.GameServer

pause