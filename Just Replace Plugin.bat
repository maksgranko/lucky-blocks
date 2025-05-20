@echo off
SETLOCAL

SET FILENAME=lucky-blocks-1.0-SNAPSHOT.jar
SET SERVER_LINK=D:\Java Projects\source\repos\lobby-server

IF EXIST "%SERVER_LINK%\plugins\%FILENAME%" DEL "%SERVER_LINK%\plugins\%FILENAME%"
IF EXIST "%SERVER_LINK%\plugins\LuckyBlocks" RMDIR /S /Q "%SERVER_LINK%\plugins\LuckyBlocks"
IF EXIST "%SERVER_LINK%\%FILENAME%" DEL "%SERVER_LINK%\%FILENAME%"

IF EXIST "target\%FILENAME%" (
    MOVE /Y "target\%FILENAME%" "%SERVER_LINK%\plugins\"
) ELSE (
    echo [ERROR] JAR-файл не найден: target\%FILENAME%
    EXIT /B 1
)

ENDLOCAL
