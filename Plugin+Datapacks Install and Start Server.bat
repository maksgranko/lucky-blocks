@echo off
SETLOCAL

SET FILENAME=lucky-blocks-1.0-SNAPSHOT.jar
SET SERVER_LINK=D:\Java Projects\source\repos\lobby-server

REM Удаление старых файлов (с проверками)
IF EXIST "%SERVER_LINK%\plugins\%FILENAME%" DEL "%SERVER_LINK%\plugins\%FILENAME%"
IF EXIST "%SERVER_LINK%\plugins\LuckyBlocks" RMDIR /S /Q "%SERVER_LINK%\plugins\LuckyBlocks"
IF EXIST "%SERVER_LINK%\%FILENAME%" DEL "%SERVER_LINK%\%FILENAME%"
IF EXIST "%SERVER_LINK%\world\datapacks" RMDIR /S /Q "%SERVER_LINK%\world\datapacks"

REM Перемещение нового файла
IF EXIST "target\%FILENAME%" (
    MOVE /Y "target\%FILENAME%" "%SERVER_LINK%\plugins\"
) ELSE (
    echo [ERROR] Файл target\%FILENAME% не найден. Отмена.
    EXIT /B 1
)

REM Копирование датапаков
IF EXIST "datapacks" (
    xcopy "datapacks" "%SERVER_LINK%\world\datapacks\" /E /H /Y /I
) ELSE (
    echo [WARNING] Папка datapacks не найдена.
)

REM Запуск сервера
IF EXIST "%SERVER_LINK%\server.bat" (
    cd /D "%SERVER_LINK%"
    start "" "server.bat"
) ELSE (
    echo [ERROR] server.bat не найден по пути %SERVER_LINK%.
    EXIT /B 1
)

ENDLOCAL
