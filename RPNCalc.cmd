REM @echo off
pushd %~dp0
set script_dir=%CD%
popd
start javaw -cp "%script_dir%\bin" com.spdqbr.rpn.RPNCalc