#!/usr/bin/env bash
source script/build-deps.sh
${MAVEN_PATH:-"mvn"} clean test