.PHONY: build run test clean

build:
	mkdir -p out
	javac -encoding UTF-8 -d out $$(find src -name "*.java")

test: build
	javac -encoding UTF-8 -cp out -d out $$(find tests -name "*.java")
	java -cp out ru.mirea.emulator.CommandParserTest

run: build
	java -cp out ru.mirea.emulator.Main

clean:
	rm -rf out