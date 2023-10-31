/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.graph.synth.util.generator;

import com.aerospike.graph.synth.emitter.generator.schema.SchemaBuilder;
import com.aerospike.graph.synth.emitter.generator.schema.definition.EdgeSchema;
import com.aerospike.graph.synth.emitter.generator.schema.definition.GraphSchema;
import com.aerospike.graph.synth.emitter.generator.schema.definition.RootVertexSpec;
import com.aerospike.graph.synth.emitter.generator.schema.definition.VertexSchema;
import com.aerospike.movement.util.core.runtime.RuntimeUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class SchemaUtil {
    public static Map<Long, Long> getDistributionConfig(VertexSchema vs, EdgeSchema es) {
        final Map<String, Object> rawConfig;
        rawConfig = es.getJoiningConfig()
                .map(it -> {
                    return (Map<String, Object>) it.args.get(es.inVertex.equals(vs.label()) ? SchemaBuilder.Keys.IN_VERTEX_DISTRIBUTION : SchemaBuilder.Keys.OUT_VERTEX_DISTRIBUTION);
                })
                .orElse(new HashMap<>());

        es.getJoiningConfig().map(it -> it.args).orElse(new HashMap<>());
        final Map<Long, Long> parsedConfig = new HashMap<>();
        for (Map.Entry<String, Object> entry : rawConfig.entrySet()) {
            parsedConfig.put(Long.valueOf(entry.getKey()), (Long) entry.getValue());
        }
        return parsedConfig;
    }

    public static EdgeSchema getSchemaFromEdgeName(final GraphSchema schema, final String edgeTypeName) {
        return schema.edgeTypes.stream()
                .filter(edgeSchema ->
                        edgeSchema.name.equals(edgeTypeName)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No edge type found for " + edgeTypeName));
    }

    public static EdgeSchema getSchemaFromEdgeLabel(final GraphSchema schema, final String edgeTypeLabel) {
        return schema.edgeTypes.stream()
                .filter(edgeSchema ->
                        edgeSchema.label().equals(edgeTypeLabel)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No edge type found for " + edgeTypeLabel));
    }

    public static VertexSchema getSchemaFromVertexName(final GraphSchema schema, final String vertexTypeName) {
        return schema.vertexTypes.stream()
                .filter(vertexSchema ->
                        vertexSchema.name.equals(vertexTypeName)).findFirst()
                .orElseThrow(() ->
                        new NoSuchElementException("No vertex type found for " + vertexTypeName));
    }

    public static VertexSchema getSchemaFromVertexLabel(final GraphSchema schema, final String vertexTypeLabel) {
        return schema.vertexTypes.stream()
                .filter(vertexSchema ->
                        vertexSchema.label().equals(vertexTypeLabel)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No vertex type found for " + vertexTypeLabel));
    }

    public static RootVertexSpec getRootVertexSpecByLabel(final GraphSchema schema, final String rootVertexLabel) {
        return schema.rootVertexTypes.stream()
                .filter(rvs ->
                        rvs.name.equals(rootVertexLabel)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No root vertex spec type found for " + rootVertexLabel));
    }

    public static Map<String, VertexSchema> getRootVertexSchemas(final GraphSchema schema) {
        return schema.rootVertexTypes.stream()
                .map(it -> Map.of(it.name, it.toVertexSchema(schema)))
                .reduce(new HashMap<>(), RuntimeUtil::mapReducer);
    }

    public static List<EdgeSchema> getJoiningEdgeSchemas(GraphSchema graphSchema) {
        return graphSchema.edgeTypes.stream().filter(it -> it.joiningEdge).collect(Collectors.toList());
    }
}
