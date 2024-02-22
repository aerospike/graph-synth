/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.graph.synth.emitter.generator.schema.definition;

/**
 * @author Grant Haywood (<a href="http://iowntheinter.net">http://iowntheinter.net</a>)
 */
public class RootVertexSpec {
    public String name;
    public Double likelihood = 1.0;
    public Integer chancesToCreate = 1;

    public VertexSchema toVertexSchema(GraphSchema schema) {
        return schema.vertexTypes.stream()
                .filter(vertexSchema -> vertexSchema.name.equals(name)).findFirst()
                .orElseThrow(() -> new RuntimeException("No vertex type found for " + name));
    }

    @Override
    public boolean equals(Object o) {
        if (!RootVertexSpec.class.isAssignableFrom(o.getClass()))
            return false;
        RootVertexSpec other = (RootVertexSpec) o;
        if (!name.equals(other.name))
            return false;
        if (!likelihood.equals(other.likelihood))
            return false;
        if (!chancesToCreate.equals(other.chancesToCreate))
            return false;
        return true;
    }
}
