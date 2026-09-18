@echo off
chcp 65001 > nul
echo ===================================================
echo     LibriTrack Pro - Compiling and Launching...
echo ===================================================

if not exist bin mkdir bin

javac -encoding UTF-8 -d bin src\com\library\*.java src\com\library\model\*.java src\com\library\exception\*.java src\com\library\strategy\*.java src\com\library\observer\*.java src\com\library\repository\*.java src\com\library\service\*.java src\com\library\util\*.java src\com\library\ui\*.java test\com\library\*.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

java -cp bin com.library.LibraryApp
pause
