/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.graph.synth.util.tinkerpop;

import com.aerospike.movement.tinkerpop.common.GraphProvider;
import org.apache.commons.configuration2.Configuration;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

public class InMemorySchemaGraphProvider implements GraphProvider {
    private static final Graph tinkerGraph = TinkerGraph.open();
    private final Configuration config;

    public static Graph getGraphInstance() {
        return tinkerGraph;
    }

    public InMemorySchemaGraphProvider(final Configuration config) {
        this.config = config;
    }


    public static InMemorySchemaGraphProvider open(final Configuration config) {
        return new InMemorySchemaGraphProvider(config);
    }

    @Override
    public Graph getProvided(GraphProviderContext ctx) {
        return tinkerGraph;
    }
}
