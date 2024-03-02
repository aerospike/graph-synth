A Graph Schema or "meta graph" is a Graph of relations between types (labels) in the Graph.

Graph Synth Schema definitions declare:
1. Entrypoints - where the synthesis process starts and build out from  
2. Vertex Types - a description of a Vertex named by its label with  
    Edge Definitions - what kinds of edges it can have, how many of each type it can have, and how likely they are to occur  
    Property Keys - What property Keys may be attached to a vertex, how likely they are to occurs  
    Property Value Generator - a class that will assign a value to the vertex's property key. There are several good  
    There are several good property value generators included with Graph Synth, and you can write custom Value Generators.  
3. Edge Definitions - What are the types of edges that can occur in the graph. What types of vertices do they connect.  
    The same property generation process is available for Edges as is available for Vertices.  

There are 2 ways to declare your Schema - YAML and Gremlin. 

Declaring the simplest schema in Gremlin is pretty easy
```groovy
g
        .addV("A").as("A")
        .property("entrypoint", true)
        .addV("B").as("B")
        .addE("AtoB").from("A").to("B");
```

YAML is also pretty easy.
```yaml
rootVertexTypes:
  - name: A
    likelihood: 1.0
    chancesToCreate: 1

vertexTypes:
  - name: A
    outEdges:
      - name: AtoB
        likelihood: 1.0
        chancesToCreate: 1
    properties: []

  - name: B
    outEdges: []
    properties: []

edgeTypes:
  - name: AtoB
    inVertex: B
    outVertex: A
    properties: []
```

Its up to you which you prefer.  
You can import a YAML file to a Gremlin Graph, and you can dump a Gremlin Graph to a YAML file. Check out the command line options with --help.  

Now lets say we wanted to attach some property data to this.   
Lets take a look at a slightly more complicated schema.  

```groovy
g
                    .addV("Person").as("Person")
                    .property("entrypoint", true)
                    .property("entrypoint.likelyhood", 1.0)
                    .property("maritalStatus", "String")
                    .property("maritalStatus.likelihood", 1.0)
                    .property("maritalStatus.value.generator", "JFaker")
                    .property("maritalStatus.value.generator.module", "demographic")
                    .property("maritalStatus.value.generator.method", "maritalStatus")
                    .addV("Cat").as("Cat")
                    .property("name", "String")
                    .property("name.likelihood", 1.0)
                    .property("name.value.generator", "ChoiceFromList")
                    .property("name.value.generator.choices", List.of("Ralph", "Fluffy", "Tom"))
                    .addV("Mouse").as("Mouse")
                    .property("color", "String")
                    .property("color.likelihood", 1.0)
                    .property("color.value.generator", "ChoiceFromList")
                    .property("color.value.generator.choices", List.of("Brown", "White", "Grey"))
                    .addE("HasCat").from("Person").to("Cat")
                    .property("likelihood",.5)
                    .property("create.chances",3)
                    .addE("Chased").from("Cat").to("Mouse")
                    .property("likelihood",.2)
                    .property("create.chances",10);
```
or in YAML:

```yaml
rootVertexTypes:
  - name: Person
    likelihood: 1.0
    chancesToCreate: 1

vertexTypes:
  - name: Person
    outEdges:
      - name: HasCat
        likelihood: .5
        chancesToCreate: 3
    properties:
      - name: maritalStatus
        type: String
        likelihood: 1.0
        valueGenerator:
          impl: JFaker
          args:
            module: demographic
            method: maritalStatus

  - name: Cat
    outEdges:
      - name: Chased
        likelihood: .2
        chancesToCreate: 10
    properties:
      - name: name
        likelihood: 1.0
        valueGenerator:
          impl: ChoiceFromList
          args:
            choices: [ "Ralph", "Fluffy", "Tom" ]
  - name: Mouse
    outEdges: [ ]
    properties:
      - name: color
        likelihood: 1.0
        valueGenerator:
          impl: ChoiceFromList
          args:
            choices: [ "Brown", "White", "Grey" ]
edgeTypes:
  - name: HasCat
    outVertex: Person
    inVertex: Cat
    properties: [ ]
  - name: Chased
    outVertex: Cat
    inVertex: Mouse
    properties: [ ]

```

Lets write out the schema from the samples list, and use it to generate some data:  

```shell
 $ java -jar graph-synth/target/GraphSynth-1.0.0-SNAPSHOT.jar --dump-sample PersonCatMouse --output-uri=file:/tmp/tutorial.yaml --clear
 $ java -jar graph-synth/target/GraphSynth-1.0.0-SNAPSHOT.jar --input-uri=file:/tmp/tutorial.yaml   --output-uri=ws://localhost:8182/g   --scale-factor=1000 --clear
```
Now lets open Gremlin Console, and take a look at the data.   


```groovy
gremlin> g = AnonymousTraversalSource.traversal().withRemote(DriverRemoteConnection.using("localhost", 8182, "g"));
gremlin> g.V().hasLabel("Person").groupCount().by(out("HasCat").values())
        ==>[Fluffy:248,Tom:300,Ralph:312]
gremlin> g.V().hasLabel("Cat").groupCount().by(out("Chased").values())
        ==>[Brown:428,White:436,Grey:454]
gremlin> g.V().hasLabel("Person").groupCount() .by(outE().count())
        ==>[0:140,1:352,2:387,3:121]
```

We can see that there is some variation in our data, 140 people have no cats, 352 peple have 1 cat, 387 people have 2 cats, and 121 people have 3 cats. 

Now try to modify the schema a bit. 
