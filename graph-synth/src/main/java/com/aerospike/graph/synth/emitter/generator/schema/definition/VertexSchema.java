/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.graph.synth.emitter.generator.schema.definition;

import java.util.Iterator;
import java.util.List;

/**
 * @author Grant Haywood (<a href="http://iowntheinter.net">http://iowntheinter.net</a>)
 */
public class VertexSchema {
    public String name;
    public List<OutEdgeSpec> outEdges;

    public String label() {
        return name;
    }
    public List<PropertySchema> properties;

    @Override
    public boolean equals(Object o) {
        if (!o.getClass().isAssignableFrom(VertexSchema.class))
            return false;
        VertexSchema other = (VertexSchema) o;
        if (!name.equals(other.name))
            return false;
        for (OutEdgeSpec e : outEdges) {
            final Iterator<OutEdgeSpec> i = other.outEdges.stream().filter(it -> it.name.equals(e.name)).iterator();
            if (!i.hasNext())
                return false;
            if (!i.next().equals(e))
                return false;
        }
        for (PropertySchema p : properties) {
            final Iterator<PropertySchema> i = other.properties.stream().filter(it -> it.name.equals(p.name)).iterator();
            if (!i.hasNext())
                return false;
            if (!i.next().equals(p))
                return false;
        }
        return true;
    }
}
