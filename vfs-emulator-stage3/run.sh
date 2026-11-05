#!/usr/bin/env bash
mkdir -p out
javac -d out -encoding UTF-8 src/*.java
java -cp out src.Main --vfs tests/test_vfs_deep.xml --script startup_test_ok.txt
