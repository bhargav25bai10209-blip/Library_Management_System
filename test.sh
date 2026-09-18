#!/usr/bin/env bash
# LibriTrack Pro - Automated Test Runner Script
set -e

mkdir -p bin

javac -encoding UTF-8 -d bin $(find src test -name "*.java")

java -cp bin com.library.TestRunner
