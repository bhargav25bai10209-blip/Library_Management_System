#!/usr/bin/env bash
# LibriTrack Pro - Unix / Linux / macOS Launcher Script
set -e

echo "==================================================="
echo "    LibriTrack Pro - Compiling and Launching..."
echo "==================================================="

mkdir -p bin

javac -encoding UTF-8 -d bin $(find src test -name "*.java")

java -cp bin com.library.LibraryApp
