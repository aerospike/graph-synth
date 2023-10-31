/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.graph.synth.emitter.generator.schema;

import com.aerospike.graph.synth.emitter.generator.schema.definition.GraphSchema;

public interface GraphSchemaParser {
    GraphSchema parse();
}