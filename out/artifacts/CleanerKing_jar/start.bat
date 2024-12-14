@echo off
:: 设置命令行窗口编码为 UTF-8
chcp 65001 > nul


title CleanerKing - 全能电脑清理工具
color 0A
echo ==============================================
echo          CleanerKing - 全能电脑清理工具
echo ==============================================
echo.

:: 确保 CleanerKing.jar 在当前目录
java -Dfile.encoding=UTF-8 -jar "%~dp0CleanerKing.jar"
echo.
echo 清理工具已运行完成，感谢使用！
pause
