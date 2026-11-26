@echo off
set V=8.13
set BASE=%USERPROFILE%\.gradle\gradle-%V%
if not exist "%BASE%\bin\gradle.bat" powershell -NoProfile -Command "Invoke-WebRequest https://services.gradle.org/distributions/gradle-%V%-bin.zip -OutFile $env:TEMP\g.zip; Expand-Archive -Force $env:TEMP\g.zip $env:USERPROFILE\.gradle"
call "%BASE%\bin\gradle.bat" %*
