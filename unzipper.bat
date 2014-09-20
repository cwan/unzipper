@echo off

groovy -classpath "%~dp0lib\hmzip16.jar" "%~dp0unzipper.groovy" "%~1" "%~2"
