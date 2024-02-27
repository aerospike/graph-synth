java -jar graph-synth/target/GraphSynth-1.0.0-SNAPSHOT.jar \
  --output-uri=file:///tmp/generate-cli \
  --input-uri=file:$(pwd)/graph-synth/src/main/resources/gdemo_schema.yaml \
  --scale-factor=10,100,1000
