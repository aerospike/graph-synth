java -jar graph-synth/target/GraphSynth-1.0.0-SNAPSHOT.jar \
  --output-uri=file:///tmp/generate-cli \
  --input-uri=file:$(pwd)/graph-synth/src/main/resources/gdemo_schema.yaml \
  --set output.entriesPerFile=40000 \
  --set output.writesBeforeFlush=40000 \
  --scale-factor=1000000
