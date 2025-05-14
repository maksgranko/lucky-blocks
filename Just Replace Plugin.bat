SET FILENAME=lucky-blocks-1.0-SNAPSHOT.jar
SET SERVER_LINK=D:\Java Projects\source\repos\lobby-server
DEL "%SERVER_LINK%\plugins\%FILENAME%"
DEL /S /Q "%SERVER_LINK%\plugins\LuckyBlocks"
DEL "%SERVER_LINK%\%FILENAME%"
MOVE "target\%FILENAME%" "%SERVER_LINK%\plugins\"
