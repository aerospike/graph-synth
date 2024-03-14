#!/usr/bin/env bash
MOVEMENT_BRANCH="release-0.9.0"
MAVEN_VERSION=3.9.6
#install dependencies
REPO_PATH=$(pwd)
mkdir -p target/
apt -y update && apt -y install git wget
if ! [ -f "target/apache-maven-3.9.6/bin/mvn" ]; then
  cd target
  wget https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz
  tar -zxvf apache-maven-3.9.6-bin.tar.gz
  cd ..
fi
MAVEN_PATH=$(realpath target/apache-maven-3.9.6/bin/mvn)
git clone https://github.com/aerospike/movement target/movement
cd target/movement && git pull origin $MOVEMENT_BRANCH && git checkout $MOVEMENT_BRANCH && $MAVEN_PATH -DskipTests clean install
cd $REPO_PATH
