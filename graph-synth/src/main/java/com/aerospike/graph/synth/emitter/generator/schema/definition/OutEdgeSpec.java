/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.graph.synth.emitter.generator.schema.definition;

/**
 * @author Grant Haywood (<a href="http://iowntheinter.net">http://iowntheinter.net</a>)
 */
public class OutEdgeSpec {
    public String name;
    public Double likelihood;
    public Integer chancesToCreate;

    public EdgeSchema toEdgeSchema(GraphSchema schema) {
        return schema.edgeTypes.stream()
                .filter(edgeSchema -> edgeSchema.name.equals(name)).findFirst()
                .orElseThrow(() -> new RuntimeException("No edge type found for " + name));
    }

    @Override
    public boolean equals(Object o) {
        if (!OutEdgeSpec.class.isAssignableFrom(o.getClass()))
            return false;
        OutEdgeSpec other = (OutEdgeSpec) o;
        if (!name.equals(other.name))
            return false;
        if (!likelihood.equals(other.likelihood))
            return false;
        if (!chancesToCreate.equals(other.chancesToCreate))
            return false;
        return true;
    }
}
