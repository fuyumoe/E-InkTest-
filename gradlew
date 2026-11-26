#!/bin/sh
set -e
V=8.13
BASE="$HOME/.gradle/gradle-$V"
if [ ! -x "$BASE/bin/gradle" ]; then curl -L --fail -o "$HOME/.gradle/gradle.zip" "https://services.gradle.org/distributions/gradle-$V-bin.zip"; rm -rf "$BASE"; unzip -q "$HOME/.gradle/gradle.zip" -d "$HOME/.gradle"; rm "$HOME/.gradle/gradle.zip"; fi
exec "$BASE/bin/gradle" "$@"
