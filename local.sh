#!/bin/bash
set -e

# Install Java 21 if not available
if ! command -v java &> /dev/null; then
    sudo apt-get update -qq
    sudo apt-get install -y openjdk-21-jdk
fi

export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))

cd gcas-api
./mvnw clean verify
