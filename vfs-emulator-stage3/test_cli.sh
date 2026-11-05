#!/usr/bin/env bash
mkdir -p out
javac -d out -encoding UTF-8 src/*.java

echo "=== 1. Без параметров ==="
java -cp out src.Main &

echo "=== 2. С минимальной VFS ==="
java -cp out src.Main --vfs tests/test_vfs_min.xml &

echo "=== 3. С глубокой VFS и скриптом ==="
java -cp out src.Main --vfs tests/test_vfs_deep.xml --script startup_test_ok.txt &

echo "=== 4. Скрипт с ошибкой (проверка прерывания) ==="
java -cp out src.Main --vfs tests/test_vfs_files.xml --script startup_test_fail.txt &
