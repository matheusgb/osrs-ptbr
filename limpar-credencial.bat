@echo off
rem Apaga a credencial da conta Jagex gravada para o atalho "RuneLite PT-BR".
rem Depois disso, o atalho volta a pedir login. Para criar de novo, rode o instalar.bat.
if exist "%USERPROFILE%\.runelite\credentials.properties" (
    del "%USERPROFILE%\.runelite\credentials.properties"
    echo Credencial apagada.
) else (
    echo Nenhuma credencial encontrada.
)
pause
