#!/usr/bin/env bash
GREMLIN_SERVER_IP=${GREMLIN_SERVER_IP:-"127.0.0.1"}
java -jar graph-synth/target/GraphSynth-1.0.0-SNAPSHOT.jar\
  task=Generate \
  -d \
  -c conf/generator/generateToGremlinServer.properties \
  -s generator.schema.yaml.path=graph-synth/src/main/resources/gdemo_schema.yaml \
  -s generator.scaleFactor=40 \
  -s traversalSource.host=$GREMLIN_SERVER_IP \
  -s traversalSource.port=8182