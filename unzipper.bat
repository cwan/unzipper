@echo off

groovy -classpath "%~dp0lib\hmzip16.jar" -c UTF-8 "%~dp0unzipper.groovy" "%~1" "%~2"
