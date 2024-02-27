#!/usr/bin/env bash
MOVEMENT_BRANCH="dev"
#install dependencies
if ! [ -x "$(command -v git)" ]; then
  apt -y update && apt -y install git
fi
if ! [ -x "$(command -v mvn)" ]; then
  apt -y update && apt -y install maven
fi
mkdir -p target/
git clone https://github.com/aerospike/movement target/movement

cd target/movement && git pull origin $MOVEMENT_BRANCH && git checkout $MOVEMENT_BRANCH && mvn -DskipTests clean install
