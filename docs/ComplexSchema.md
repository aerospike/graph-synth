A real world schema includes some structure

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
        chancesToCreate: 4
    properties: []

  - name: B
    outEdges:
      - name: BtoC
        likelihood: .3
        chancesToCreate: 1
    properties: []
    


edgeTypes:
  - name: AtoB
    inVertex: B
    outVertex: A
    properties: []
  - name: BtoC
    inVertex: C
    outVertex: B
    properties: []

```














lets add some properties to these elements


```yaml
entrypointVertexType: A

vertexTypes:

  - name: A
    outEdges:
      - name: AtoB
        likelihood: 1.0
        chancesToCreate: 4
    properties: [ ]

  - name: B
    outEdges:
      - name: BtoC
        likelihood: .3
        chancesToCreate: 1
    properties:
      - name: name
        likelihood: 1.0
        valueGenerator:
          impl: ChoiceFromList
          args:
            choices: [ "Andy", "Bob", "Carol" ]
  - name: C
    outEdges: [ ]
    properties: [ ]


edgeTypes:
  - name: AtoB
    inVertex: B
    outVertex: A
    properties: [ ]
  - name: BtoC
    inVertex: C
    outVertex: B
    properties: [ ]
```
You san see there are a few parameters here. \
`likelihood: .3`
says there is a 30% chance for a BtoC edge to be created ( and thus chance for its incident C vertex to be created)
```yaml
    properties:
      - name: name
        likelihood: 1.0
        valueGenerator:
          impl: ChoiceFromList
          args:
            choices: [ "Andy", "Bob", "Carol" ]
```
this is a configuration for a property key. 
there is a 1.0 (100%) likelyhood the key will be created. \
the value for the property will be chosen from the list `[ "Andy", "Bob", "Carol" ]` \
There are several value generators available, and can be configured by the `impl` argument.