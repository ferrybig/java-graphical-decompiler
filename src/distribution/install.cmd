@echo off

echo This script will link this decompiler to the action menu of jar files.
echo Installing decompiler version: ${project.version}
echo:
echo Detecting installed java... >&2

if not "%JAVA_HOME%"=="" goto OkJHome
for %%i in (javaw.exe) do set "JAVACMD=%%~$PATH:i"
goto checkJCmd

:OkJHome
set "JAVACMD=%JAVA_HOME%\bin\javaw.exe"
goto checkJCmd

:checkJCmd
if exist "%JAVACMD%" goto javaversion

echo The JAVA_HOME environment variable is not defined correctly >&2
echo This environment variable is needed to run this program >&2
echo NB: JAVA_HOME should point to a JDK not a JRE >&2
goto error

:javaversion
echo Java should be installed here: "%JAVACMD%"
"%JAVACMD:javaw=java%" -version
if not %ERRORLEVEL% EQU 0 goto javaerror
goto export

:javaerror
echo Problem testing java version, is your java correctly installed? >&2
goto error

:export
echo Preparing registery file >&2
echo Windows Registry Editor Version 5.00 > install.reg
echo: >> install.reg
echo [HKEY_CURRENT_USER\Software\Classes\jarfile\shell\${project.groupId}.${project.artifactId}] >> install.reg
echo @="Decompile" >> install.reg
echo [HKEY_CURRENT_USER\Software\Classes\jarfile\shell\${project.groupId}.${project.artifactId}\command] >> install.reg
set FILE=%%1
set JARFILE=%cd%\${project.build.finalName}.jar
echo @="\"%JAVACMD:\=\\%\" -jar \"%JARFILE:\=\\%\" \"%FILE:\=\\%\"" >> install.reg
reg import %cd%\install.reg
if not %ERRORLEVEL% EQU 0 goto error
goto cleanup

:cleanup
echo Cleaning up >&2
del install.reg
set ERROR_CODE=0
goto done

:error
set ERROR_CODE=1
goto done

:done
pause
cmd /C exit /B %ERROR_CODE%
