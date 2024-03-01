# Aerospike Graph Synth
![Graph Synthesizer Logo](docs/img/logo-small.jpg)

Graph Synth is a tool for synthesizing graph structured datasets. 

You can run Graph Synth via either a traditional Command Line Interface (CLI) or via a Gremlin call() step. 

To get started, download the latest jar release  [here](https://github.com/aerospike/graph-synth/releases "Graph Synth releases").


## Usage
```shell
$ java -jar GraphSynth-1.0.0.jar --help
Graph Synth, by Aerospike.
Usage: GraphSynth [--debug] [--help] [--input-uri=<inputUri>]
                  [--output-uri=<outputUri>] [--scale-factor=<scaleFactor>]
                  [--set=<String=String>]...
      --debug   Show Debug Output
      --help    Help
      --input-uri=<inputUri>
                File or Gremlin Server URI for schema, supported schemes:
                 file://
                 ws://
                 wss://
      --output-uri=<outputUri>
                File or Gremlin Server URI for output, supported schemes:
                 file://
                 ws://
                 wss://
      --scale-factor=<scaleFactor>
                Comma delimited list of scale factors
      --set=<String=String>
                Set or override configuration key
```

Some real world examples:

Using a yaml schema file to write out csv data:
```shell
$ java -jar GraphSynth-1.0.0.jar \
  --input-uri=file:/home/user/software/graph-synth/conf/schema/gdemo_schema.yaml \
  --output-uri=file:/tmp/output-data \
    --scale-factor=10
```

Using a schema graph to write out csv data at 3 different scales:
```shell
$ java -jar GraphSynth-1.0.0.jar \
  --input-uri=ws://my-gremlin-schema-server:8182/g \
  --output-uri=file:/tmp/output-data \
  --scale-factor=10,100,1000
```

Using a schema graph to write out csv data:
```shell
$ java -jar GraphSynth-1.0.0.jar \
  --input-uri=ws://my-gremlin-schema-server:8182/g \
  --output-uri=file:/tmp/output-data \
  --scale-factor=66,100000
```

Using a schema graph to write data directly do a different graph, both graphs are served as different traversal endpoints:  
note that only 1 scale factor is supported when writing directly to a graph
```shell
$ java -jar GraphSynth-1.0.0.jar \
  --input-uri=ws://my-gremlin-schema-server:8182/schema \
  --output-uri=ws://my-gremlin-schema-server:8182/g \
  --scale-factor=77
```

## Configuration and Schema

Most configuration options can be provided on the command line. You will however need to provide a Graph Schema

You can declare a schema in yaml or in Gremlin

Sample schema yaml files are provided in [the conf directory](conf/schema)

Read more about Schema declerations in [the docs section](docs/Schema.md)
## Building

Before building and running this project, you need to download and build [Movement](https://github.com/aerospike/movement)

Maven is used as the build tool for this project, a simple [script](script/build.sh) is provided to build both projects.

## License

This project is provided under the Apache2 software  [license](LICENSE).

## No Warranty
Graph Synth is provided without warranty and is intended for testing and pre-production environments. 
It is not recommended for production operations.