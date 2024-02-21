/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.graph.synth.emitter.generator;

import com.aerospike.graph.synth.emitter.generator.schema.definition.GraphSchema;
import com.aerospike.graph.synth.emitter.generator.schema.definition.RootVertexSpec;
import com.aerospike.graph.synth.emitter.generator.schema.definition.VertexSchema;
import com.aerospike.movement.emitter.core.Emitable;
import com.aerospike.movement.output.core.Output;
import com.aerospike.movement.runtime.core.driver.OutputId;
import com.aerospike.movement.runtime.core.driver.OutputIdDriver;
import com.aerospike.movement.util.core.runtime.RuntimeUtil;
import com.aerospike.graph.synth.util.generator.SchemaUtil;
import org.apache.commons.configuration2.Configuration;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Grant Haywood (<a href="http://iowntheinter.net">http://iowntheinter.net</a>)
 */
public class GeneratedArchipelago implements Emitable {


    private final Long driverId;
    private final GraphSchema graphSchema;
    private final OutputIdDriver outputIdDriver;
    private final Configuration config;

    public GeneratedArchipelago(final Long driverId, final GraphSchema graphSchema, final OutputIdDriver outputIdDriver, final Configuration config) {
        this.driverId = driverId;
        this.graphSchema = graphSchema;
        this.outputIdDriver = outputIdDriver;
        this.config = config;
    }


    //Returns a map from rootVertex label to the function used to get its editable stream
    public Map<String, Function<Void, Stream<Emitable>>> getIslandChainGenerators(Map<String, VertexSchema> schemas, Output output) {
        return schemas.values().stream().map(vertexSchema -> {
            final String label = vertexSchema.label();
            final Function<Void, Stream<Emitable>> generatorFn = (v) -> {
                final Optional<OutputId> noi = outputIdDriver.getNext();
                if (noi.isEmpty())
                    return Stream.empty();
                final OutputId oi = noi.get();
                final VertexContext vctx = new VertexContext(
                        this.graphSchema,
                        SchemaUtil.getSchemaFromVertexLabel(this.graphSchema, label),
                        outputIdDriver,
                        Optional.empty());
                final GeneratedVertex gv = new GeneratedVertex((Long) oi.unwrap(), vctx);
                return gv.emit(output);
            };
            return Map.of(label, generatorFn);
        }).reduce(RuntimeUtil::mapReducer).get();
    }

    public Map<String, Stream<Emitable>> getIslandChains(final Map<String, VertexSchema> schemas, final Output output) {
        return getIslandChainGenerators(schemas, output).entrySet().stream().map(labelGenFn -> {
            final String label = labelGenFn.getKey();
            final Function<Void, Stream<Emitable>> generatorFn = labelGenFn.getValue();
            final RootVertexSpec rvs = SchemaUtil.getRootVertexSpecByLabel(this.graphSchema, label);
            final Stream<Emitable> probabilisticStream = ProbabalisticEmittableSequence.create(generatorFn, rvs.likelihood, rvs.chancesToCreate).stream();
            return Map.of(label, probabilisticStream);
        }).reduce(RuntimeUtil::mapReducer).get();
    }


    @Override
    public Stream<Emitable> emit(final Output output) {
        final Map<String, VertexSchema> schemas = SchemaUtil.getRootVertexSchemas(graphSchema);
        final Map<String, Stream<Emitable>> islandChains = getIslandChains(schemas, output);
        return islandChains.values().stream().flatMap(Function.identity());
    }

    @Override
    public String type() {
        return GeneratedArchipelago.class.getSimpleName();
    }
}
