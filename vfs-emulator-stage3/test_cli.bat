@echo off
chcp 65001 > nul
if not exist out mkdir out
javac -d out -encoding UTF-8 src/*.java

echo === 1. Без параметров ===
start java -cp out src.Main

echo === 2. С минимальной VFS ===
start java -cp out src.Main --vfs tests/test_vfs_min.xml

echo === 3. С глубокой VFS (3+ уровня) и корректным скриптом ===
start java -cp out src.Main --vfs tests/test_vfs_deep.xml --script startup_test_ok.txt

echo === 4. Проверка остановки скрипта при ошибке ===
start java -cp out src.Main --vfs tests/test_vfs_files.xml --script startup_test_fail.txt
