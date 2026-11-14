#!/usr/bin/env bash
set -e
rm -rf out
javac -d out src/main/java/ahocorasick/*.java src/test/java/ahocorasick/*.java
java -cp out ahocorasick.Main
echo
java -cp out ahocorasick.AhoCorasickTest
echo
java -cp out ahocorasick.Benchmark results
