/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.graph.synth.util.tinkerpop;
/*
  Created by Grant Haywood grant@iowntheinter.net
  7/17/23
*/

import com.aerospike.graph.synth.emitter.generator.schema.SchemaBuilder;
import com.aerospike.graph.synth.emitter.generator.schema.definition.GraphSchema;
import com.aerospike.graph.synth.emitter.generator.schema.definition.RootVertexSpec;
import com.aerospike.graph.synth.util.generator.SchemaUtil;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.aerospike.movement.util.core.configuration.ConfigUtil.subKey;


public class SchemaGraphUtil {
    public static Object[] mapToKeyValues(Map<?, ?> map) {
        List<Object> results = new ArrayList<>();
        map.forEach((k, v) -> results.addAll(List.of(k, v)));
        return results.toArray(new Object[0]);
    }

    public static void writeToTraversalSource(final GraphTraversalSource g, final GraphSchema graphSchema) {
        final Map<String, RootVertexSpec> rootVertexSpecMap = new HashMap<>();
        graphSchema.rootVertexTypes.stream().forEach(it -> rootVertexSpecMap.put(it.name, it));
        graphSchema.vertexTypes.forEach(vertexSchema -> {
            final Vertex v = g.addV(vertexSchema.label()).next();
            if (graphSchema.rootVertexTypes.stream().anyMatch(it -> it.name.equals(vertexSchema.label()))) {
                g.V(v).property(SchemaBuilder.Keys.ENTRYPOINT, true).next();
                g.V(v).property(subKey(SchemaBuilder.Keys.ENTRYPOINT, SchemaBuilder.Keys.CHANCES_TO_CREATE), rootVertexSpecMap.get(vertexSchema.name).chancesToCreate).next();
                g.V(v).property(subKey(SchemaBuilder.Keys.ENTRYPOINT, SchemaBuilder.Keys.LIKELIHOOD), rootVertexSpecMap.get(vertexSchema.name).likelihood).next();
            }
            vertexSchema.properties.forEach(vertexPropertySchema -> {
                g.V(v).property(vertexPropertySchema.name, vertexPropertySchema.type).next();
                g.V(v).property(subKey(vertexPropertySchema.name, SchemaBuilder.Keys.LIKELIHOOD), vertexPropertySchema.likelihood).next();
                g.V(v).property(subKey(vertexPropertySchema.name, SchemaBuilder.Keys.VALUE_GENERATOR), vertexPropertySchema.valueGenerator.impl).next();
                vertexPropertySchema.valueGenerator.args.forEach((key, value) -> {
                    g.V(v).property(subKey(subKey(vertexPropertySchema.name, SchemaBuilder.Keys.VALUE_GENERATOR), key), value).next();
                });
            });
        });
        graphSchema.edgeTypes.forEach(edgeSchema -> {
            final Vertex inV = g.V().hasLabel(edgeSchema.inVertex).next();
            final Vertex outV = g.V().hasLabel(edgeSchema.outVertex).next();
            final Edge schemaEdge = g.V(outV).addE(edgeSchema.label()).to(__.V(inV)).next();
            g.E(schemaEdge).property(SchemaBuilder.Keys.CHANCES_TO_CREATE,
                    SchemaUtil.getSchemaFromVertexName(graphSchema, edgeSchema.outVertex)
                            .outEdges
                            .stream()
                            .filter(it -> it.name.equals(edgeSchema.name))
                            .findFirst()
                            .get()
                            .chancesToCreate
            ).next();
            edgeSchema.properties.forEach(edgePropertySchema -> {
                g.E(schemaEdge).property(edgePropertySchema.name, edgePropertySchema.type).next();
                g.E(schemaEdge).property(subKey(edgePropertySchema.name, SchemaBuilder.Keys.LIKELIHOOD), edgePropertySchema.likelihood).next();
                g.E(schemaEdge).property(subKey(edgePropertySchema.name, SchemaBuilder.Keys.VALUE_GENERATOR), edgePropertySchema.valueGenerator.impl).next();
                edgePropertySchema.valueGenerator.args.forEach((key, value) -> {
                    String propkey = subKey(subKey(edgePropertySchema.name, SchemaBuilder.Keys.VALUE_GENERATOR), key);
                    g.E(schemaEdge).property(propkey, value).next();
                });
            });
        });
    }


}
