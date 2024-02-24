#!/usr/bin/env bash
java -jar graph-synth/target/GraphSynth-1.0.0-SNAPSHOT.jar\
  task=Generate \
  -d \
  -c conf/generator/generateToCSVFiles.properties \
  -s generator.schema.yaml.path=graph-synth/src/main/resources/gdemo_schema.yaml \
  -s generator.scaleFactor=100000 \
  -s runtime.threads=1 \
  -s output.directory=/core/datasets/gdemo/gdemocsv1
