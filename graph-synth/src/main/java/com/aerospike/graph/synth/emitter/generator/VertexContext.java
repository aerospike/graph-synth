/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.graph.synth.emitter.generator;


import com.aerospike.graph.synth.emitter.generator.schema.definition.GraphSchema;
import com.aerospike.graph.synth.emitter.generator.schema.definition.VertexSchema;
import com.aerospike.movement.runtime.core.driver.OutputIdDriver;

import java.util.Optional;

/**
 * @author Grant Haywood (<a href="http://iowntheinter.net">http://iowntheinter.net</a>)
 */
public class VertexContext {
    public final GraphSchema graphSchema;
    public final VertexSchema vertexSchema;
    public final  OutputIdDriver outputIdDriver;
    Optional<EdgeGenerator.GeneratedEdge> creationInEdge;

    public VertexContext(final GraphSchema graphSchema,
                         final VertexSchema vertexSchema,
                         final OutputIdDriver outputIdDriver,
                         final Optional<EdgeGenerator.GeneratedEdge> creationInEdge) {
        this.graphSchema = graphSchema;
        this.vertexSchema = vertexSchema;
        this.outputIdDriver = outputIdDriver;
        this.creationInEdge = creationInEdge;
    }
}
