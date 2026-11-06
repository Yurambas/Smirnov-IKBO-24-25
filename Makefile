SRC := $(shell find src -name '*.java')
TESTS := $(shell find tests -name '*.java')
JAVAC ?= javac -encoding UTF-8
JAVA := java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8

.PHONY: build test run clean

build:
	mkdir -p build/main
	$(JAVAC) -d build/main $(SRC)

test: build
	mkdir -p build/test
	$(JAVAC) -cp build/main -d build/test $(TESTS)
	$(JAVA) -cp build/main:build/test depgraph.TestRunner

run: build
	$(JAVA) -cp build/main depgraph.Main $(ARGS)

clean:
	rm -rf build
