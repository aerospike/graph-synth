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
public class GraphSchema {
    public List<EdgeSchema> edgeTypes;
    public List<VertexSchema> vertexTypes;
    public List<RootVertexSpec> rootVertexTypes;
    public JoiningConfig joining;

    @Override
    public boolean equals(Object o) {
        if (!GraphSchema.class.isAssignableFrom(o.getClass()))
            return false;
        final GraphSchema other = (GraphSchema) o;
        for (final EdgeSchema e : edgeTypes) {
            final Iterator<EdgeSchema> i = other.edgeTypes.stream()
                    .filter(it -> it.label().equals(e.label())).iterator();
            if (!i.hasNext())
                return false;
            if (!i.next().equals(e))
                return false;
        }
        for (final VertexSchema v : vertexTypes) {
            final Iterator<VertexSchema> i = other.vertexTypes.stream()
                    .filter(it -> it.label().equals(v.label())).iterator();
            if (!i.hasNext())
                return false;
            VertexSchema next = i.next();
            if (!next.equals(v))
                return false;
        }
        for (final RootVertexSpec rvs : rootVertexTypes) {
            final Iterator<RootVertexSpec> i = other.rootVertexTypes.stream()
                    .filter(it -> it.name.equals(rvs.name)).iterator();
            if (!i.hasNext())
                return false;
            if (!i.next().equals(rvs))
                return false;
        }
        return true;
    }
}
