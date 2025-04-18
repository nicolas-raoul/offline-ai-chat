#!/bin/bash

# Set Android and Java paths
export ANDROID_HOME=/Users/nicolasraoul/Library/Android/sdk
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home

# Prompt for keystore passwords
read -s -p "Enter keystore password: " KEYSTORE_PASSWORD
echo
read -s -p "Enter key password: " KEY_PASSWORD
echo

# Export passwords
export KEYSTORE_PASSWORD
export KEY_PASSWORD

# Run the release build
./gradlew clean bundleRelease
