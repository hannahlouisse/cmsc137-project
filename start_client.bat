@echo off
title Guess The Impostor - CLIENT
color 0B

echo =========================================
echo    GUESS THE IMPOSTOR - GAME CLIENT
echo =========================================
echo.

:: Create bin folder if it doesn't exist
if not exist "bin" mkdir bin

echo [1/3] Compiling client files...
javac -d bin src/client/*.java src/utils/*.java

if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b 1
)

echo [2/3] Compilation successful!
echo [3/3] Starting client...

echo.
java -cp bin client.GameClient

pause