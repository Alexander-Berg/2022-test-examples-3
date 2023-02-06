@echo off
setlocal

call :btoa b64[0] "Hello world!"
call :btoa b64[1] "This is fun."
call :btoa b64[2] "wheeeeee!"
call :atob b64[3] SGVsbG8gd29ybGQh

set b64
goto :EOF

:btoa <var_to_set> <str>
for /f "delims=" %%I in (
    'powershell "[convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes(\"%~2\"))"'
) do set "%~1=%%I"
goto :EOF

:atob <var_to_set> <str>
for /f "delims=" %%I in (
    'powershell "[Text.Encoding]::UTF8.GetString([convert]::FromBase64String(\"%~2\"))"'
) do set "%~1=%%I"
goto :EOF