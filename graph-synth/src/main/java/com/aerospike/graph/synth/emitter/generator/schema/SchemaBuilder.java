/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.graph.synth.emitter.generator.schema;
/*
  Created by Grant Haywood grant@iowntheinter.net
  7/17/23
*/

import com.aerospike.graph.synth.emitter.generator.schema.definition.EdgeSchema;
import com.aerospike.graph.synth.emitter.generator.schema.definition.GraphSchema;
import com.aerospike.graph.synth.emitter.generator.schema.definition.RootVertexSpec;
import com.aerospike.graph.synth.emitter.generator.schema.definition.VertexSchema;

import java.util.*;

public class SchemaBuilder {
    public static class Keys {
        public static final String CHANCES_TO_CREATE = "create.chances";
        public static final String LIKELIHOOD = "likelihood";
        public static final String VALUE_GENERATOR = "value.generator";
        public static final String ENTRYPOINT = "entrypoint";
        public static final String EDGE_TYPE = "edgeType";
        public static final String OUT_VERTEX_DISTRIBUTION = "outVertexDistribution";
        public static final String IN_VERTEX_DISTRIBUTION = "inVertexDistribution";
    }

    final Set<VertexSchema> vertexTypes;
    final Set<EdgeSchema> edgeTypes;

    public SchemaBuilder withVertexType(VertexSchema vertexSchema) {
        this.pushVertexSchema(vertexSchema);
        return this;
    }

    private SchemaBuilder() {
        this.vertexTypes = new HashSet<>();
        this.edgeTypes = new HashSet<>();
    }

    public static SchemaBuilder create() {
        return new SchemaBuilder();
    }

    private void pushVertexSchema(final VertexSchema vertexSchema) {
        vertexTypes.add(vertexSchema);
    }

    public SchemaBuilder withEdgeType(EdgeSchema edgeSchema) {
        this.pushEdgeSchema(edgeSchema);
        return this;
    }

    private void pushEdgeSchema(final EdgeSchema edgeSchema) {
        edgeTypes.add(edgeSchema);
    }

    public GraphSchema build(final List<RootVertexSpec> entrypointSpecs) {
        return build(entrypointSpecs.toArray(RootVertexSpec[]::new));
    }

    public GraphSchema build(final RootVertexSpec... entrypointSpecs) {
        final GraphSchema schema = new GraphSchema();
        schema.vertexTypes = new ArrayList<>(vertexTypes);
        schema.edgeTypes = new ArrayList<>(edgeTypes);
        schema.rootVertexTypes = Arrays.asList(entrypointSpecs);
        return schema;
    }
}
