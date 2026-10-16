#!/usr/bin/env bash
set -e

mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
javac -encoding UTF-8 -cp out -d out $(find tests -name "*.java")
java -cp out ru.mirea.emulator.CommandParserTest
