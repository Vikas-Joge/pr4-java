@echo off
cd /d "%~dp0"
echo Starting Practical 4 Java Server...
if exist "target\pr4-java.jar" (
    java -jar target\pr4-java.jar
) else (
    mvn compile exec:java
)
pause

