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
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.HashMap;
import java.util.Map;

import static com.aerospike.movement.util.core.configuration.ConfigurationUtil.subKey;

public class SchemaGraphUtil {
    public static void writeToGraph(final Graph graph, final GraphSchema graphSchema) {
        final Map<String, RootVertexSpec> rootVertexSpecMap = new HashMap<>();
        graphSchema.rootVertexTypes.stream().forEach(it -> rootVertexSpecMap.put(it.name, it));
        graphSchema.vertexTypes.forEach(vertexSchema -> {
            final Vertex v = graph.addVertex(T.label, SchemaBuilder.Keys.VERTEX_TYPE, T.id, vertexSchema.label());
            if (graphSchema.rootVertexTypes.stream().anyMatch(it -> it.name.equals(vertexSchema.label()))) {
                v.property(SchemaBuilder.Keys.ENTRYPOINT, true);
                v.property(subKey(SchemaBuilder.Keys.ENTRYPOINT, SchemaBuilder.Keys.CHANCES_TO_CREATE), rootVertexSpecMap.get(vertexSchema.name).chancesToCreate);
                v.property(subKey(SchemaBuilder.Keys.ENTRYPOINT, SchemaBuilder.Keys.LIKELIHOOD), rootVertexSpecMap.get(vertexSchema.name).likelihood);
            }
            vertexSchema.properties.forEach(vertexPropertySchema -> {
                v.property(vertexPropertySchema.name, vertexPropertySchema.type);
                v.property(subKey(vertexPropertySchema.name, SchemaBuilder.Keys.LIKELIHOOD), vertexPropertySchema.likelihood);
                v.property(subKey(vertexPropertySchema.name, SchemaBuilder.Keys.VALUE_GENERATOR_IMPL), vertexPropertySchema.valueGenerator.impl);
                v.property(subKey(vertexPropertySchema.name, SchemaBuilder.Keys.VALUE_GENERATOR_ARGS), vertexPropertySchema.valueGenerator.args);
            });
        });
        graphSchema.edgeTypes.forEach(edgeSchema -> {
            final Vertex inV = graph.vertices(edgeSchema.inVertex).next();
            final Vertex outV = graph.vertices(edgeSchema.outVertex).next();
            final Edge schemaEdge = outV.addEdge(SchemaBuilder.Keys.EDGE_TYPE, inV, T.id, edgeSchema.label());
            edgeSchema.properties.forEach(edgePropertySchema -> {
                schemaEdge.property(edgePropertySchema.name, edgePropertySchema.type);
                schemaEdge.property(subKey(edgePropertySchema.name, SchemaBuilder.Keys.LIKELIHOOD), edgePropertySchema.likelihood);
                schemaEdge.property(subKey(edgePropertySchema.name, SchemaBuilder.Keys.VALUE_GENERATOR_IMPL), edgePropertySchema.valueGenerator.impl);
                schemaEdge.property(subKey(edgePropertySchema.name, SchemaBuilder.Keys.VALUE_GENERATOR_ARGS), edgePropertySchema.valueGenerator.args);
            });
        });
    }

}
