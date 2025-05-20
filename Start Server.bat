@echo off
SETLOCAL

SET SERVER_LINK=D:\Java Projects\source\repos\lobby-server

IF EXIST "%SERVER_LINK%\server.bat" (
    cd /D "%SERVER_LINK%"
    start "" "server.bat"
) ELSE (
    echo [ERROR] server.bat не найден по пути %SERVER_LINK%
    EXIT /B 1
)

ENDLOCAL
