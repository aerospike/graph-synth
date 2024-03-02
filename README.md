# Aerospike Graph Synth
![Graph Synthesizer Logo](docs/img/logo-small.jpg)

Graph Synth is a tool for synthesizing graph structured datasets. 

You can run Graph Synth via either a traditional Command Line Interface (CLI) or via a Gremlin call() step. 

To get started, download the latest jar release  [here](https://github.com/aerospike/graph-synth/releases "Graph Synth releases").


## Usage
```shell
$ java -jar GraphSynth-1.0.0.jar --help
Graph Synth, by Aerospike.
Usage: GraphSynth [--help] [--scale-factor=<scaleFactor>]
                  [--input-uri=<inputUri>] [--output-uri=<outputUri>]
                  [--list-sample-schemas] [--load-sample=<loadSample>]
                  [--dump-sample=<dumpSample>] [--export-schema]
                  [--load-schema] [--clear] [--set=<String=String>]... [--debug]
      --help            Help
      --scale-factor=<scaleFactor>
                        Comma delimited list of scale factors
      --input-uri=<inputUri>
                        File or Gremlin Server URI for schema, supported
                          schemes:
                         file://
                         ws://
                         wss://
      --output-uri=<outputUri>
                        File or Gremlin Server URI for output, supported
                          schemes:
                         file://
                         ws://
                         wss://
      --list-sample-schemas
                        List Sample Schemas
      --load-sample=<loadSample>
                        Load Sample to Gremlin Server
      --dump-sample=<dumpSample>
                        Dump Sample Schema to YAML
      --export-schema   Export Schema from Gremlin Server to YAML file
      --load-schema     Load YAML Schema to Gremlin Server
      --clear           Delete and overwrite existing remote graph
      --set=<String=String>
                        Set or override configuration key
      --debug           Show Debug Output

```

Some real world examples:

Using a yaml schema file to write out csv data:
```shell
$ java -jar GraphSynth-1.0.0.jar \
  --input-uri=file:$(pwd)/conf/schema/gdemo_schema.yaml \
  --output-uri=file:/tmp/output-data \
  --scale-factor=10
```

You can list some built-in sample schemas with the --list-sample-schemas command:
```shell
$ java -jar GraphSynth-1.0.0-SNAPSHOT.jar --list-sample-schemas
Synthetic
Simplest
Benchmark2024
GDemoSchema
```

If you have a Gremlin Server handy, you can load a schema into it. This may be useful for exploring and modifying the schema.
Be careful, note the --clear option will erase your existing graph. 

```shell
$ java -jar graph-synth/target/GraphSynth-1.0.0-SNAPSHOT.jar --load-sample GDemoSchema --output-uri=ws://localhost:8182/g --clear
$ 
```
Once you have it loaded, you can use that remote schema to generate data. 

Here is an example of using a remote schema graph to write out csv data at 3 different scales:
```shell
$ java -jar GraphSynth-1.0.0.jar \
  --input-uri=ws://localhost:8182/g \
  --output-uri=file:/tmp/output-data \
  --scale-factor=10,100,1000
...

Files generated at Scale Factor: 10 26
Files generated at Scale Factor: 1000 260
Files generated at Scale Factor: 100 26
$
```

You can also generate directly into a remote gremlin server.

Either from YAML:
```shell
$ java -jar GraphSynth-1.0.0.jar \
   --input-uri=file:$(pwd)/conf/schema/gdemo_schema.yaml \
   --output-uri=ws://localhost:8182/g  \
   --scale-factor=1000 \
   --clear
```

Or directly from a schema graph into another graph:

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

Maven is used as the build tool for this project.
A simple [script](script/build.sh) is provided to build both projects.
```shell
$ script/build.sh

...

[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for GraphSynth 1.0.0-SNAPSHOT:
[INFO] 
[INFO] GraphSynth ......................................... SUCCESS [  0.392 s]
[INFO] GraphSynth ......................................... SUCCESS [  5.627 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  6.228 s
[INFO] Finished at: 2024-03-01T23:38:52-07:00
[INFO] ------------------------------------------------------------------------

$ ls graph-synth/target/GraphSynth-1.0.0-SNAPSHOT.jar
graph-synth/target/GraphSynth-1.0.0-SNAPSHOT.jar
```

## License

This project is provided under the Apache2 software  [license](LICENSE).

## No Warranty
Graph Synth is provided without warranty and is intended for testing and pre-production environments. 
It is not recommended for production operations.