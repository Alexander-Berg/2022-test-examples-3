@echo off

SET DBDIR=..\..\..\db

CALL %DBDIR%\_DB.conf.bat
ECHO * %0 (%DB_USER%:%DB_PASS% @ %DB_HOST%:%DB_PORT%/%DB_NAME%)

time /t
java -jar %DBDIR%\yandex-mysql-diff-app.jar ^
    "jdbc:mysql://%DB_HOST%:%DB_PORT%/%DB_NAME%?user=%DB_USER%&password=%DB_PASS%" ^
    %DBDIR%\dbchanges_noalter.sql ^
    > mysqldiff.sql
time /t
