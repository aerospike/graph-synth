/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.graph.synth.emitter.generator.schema.definition;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * @author Grant Haywood (<a href="http://iowntheinter.net">http://iowntheinter.net</a>)
 */
public class EdgeSchema {
    public String name;
    public String inVertex;
    public String outVertex;

    public String label() {
        return name;
    }

    public List<PropertySchema> properties;
    public boolean joiningEdge = false;
    public GeneratorConfig joiningConfig = null;

    public Optional<GeneratorConfig> getJoiningConfig(){
        return Optional.ofNullable(joiningConfig);
    }
    @Override
    public boolean equals(Object o) {
        if (!o.getClass().isAssignableFrom(EdgeSchema.class))
            return false;
        EdgeSchema other = (EdgeSchema) o;
        if (!name.equals(other.name))
            return false;
        if (!inVertex.equals(other.inVertex))
            return false;
        if (!outVertex.equals(other.outVertex))
            return false;
        for (PropertySchema p : properties) {
            final Iterator<PropertySchema> i = other.properties.stream().filter(it -> it.name.equals(p.name)).iterator();
            if (!i.hasNext())
                return false;
            PropertySchema next = i.next();
            if (!next.equals(p))
                return false;
        }
        return true;
    }
}
