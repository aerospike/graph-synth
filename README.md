# Aerospike Graph Synth
![Graph Synthesizer Logo](docs/img/logo-small.jpg)

Graph Synth is a tool project for synthesizing graph structured datasets. 

You can run Graph Synth via either a traditional Command Line Interface (CLI) or via a Gremlin call() step. 

To get started, download the latest jar release  [here](https://github.com/aerospike/graph-synth/releases "Graph Synth releases").


## Usage
```shell
$ java -jar graph-synth/target/GraphSynth-1.0.0-SNAPSHOT.jar --help
Graph Synth, by Aerospike.
Usage: GraphSynth [--debug] [--help] [--test-mode] [--input-uri=<inputUri>]
                  [--output-uri=<outputUri>] [--scale-factor=<scaleFactor>]
                  [--set=<String=String>]...
      --debug       Show Debug Output
      --help        Help
      --input-uri=<inputUri>
                    Directory URI for source files, supported schemes file://
      --output-uri=<outputUri>
                    Set the output URI. Supported URI schemes are
                     ws://
                     wss://
      --scale-factor=<scaleFactor>
                    scale factor, comma delimited list
      --set=<String=String>
                    Set or override configuration key
      --test-mode   Test Mode
```

A real world example:
```shell
$ java -jar graph-synth/target/GraphSynth-1.0.0-SNAPSHOT.jar \
  --output-uri=file:///tmp/generate-cli \
  --input-uri=file:/home/user/software/graph-synth/conf/schema/gdemo_schema.yaml \
  --scale-factor=10,100,1000
```

## Configuration and Schema

Most configuration options can be provided on the command line. You will however need to provide a Graph Schema

You can declare a schema in YAML or in Gremlin

Sample schema YAML files are provided in conf/schema

Read more about Schema declerations in [the docs section](docs/Schema.md)
## Building

Before building and running this project, you need to download and build [Movement](https://github.com/aerospike/movement)

Maven is used as the build tool for this project, a simple [script](script/build.sh) is provided to build both projects.

## License

This project is provided under the Apache2 software  [license](LICENSE).