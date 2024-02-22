/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.graph.synth.emitter.generator;


import com.aerospike.graph.synth.emitter.generator.schema.GraphSchemaParser;
import com.aerospike.graph.synth.emitter.generator.schema.definition.GraphSchema;
import com.aerospike.graph.synth.emitter.generator.schema.definition.VertexSchema;
import com.aerospike.graph.synth.emitter.generator.schema.seralization.YAMLSchemaParser;
import com.aerospike.graph.synth.process.tasks.generator.Generate;
import com.aerospike.movement.config.core.ConfigurationBase;
import com.aerospike.movement.emitter.core.Emitable;
import com.aerospike.movement.emitter.core.Emitter;
import com.aerospike.movement.runtime.core.Runtime;
import com.aerospike.movement.runtime.core.driver.OutputIdDriver;
import com.aerospike.movement.runtime.core.driver.WorkChunkDriver;
import com.aerospike.movement.runtime.core.local.Loadable;
import com.aerospike.movement.test.mock.output.MockOutput;
import com.aerospike.movement.util.core.configuration.ConfigUtil;
import com.aerospike.movement.util.core.error.ErrorUtil;
import com.aerospike.movement.util.core.iterator.ext.IteratorUtils;
import com.aerospike.movement.util.core.runtime.RuntimeUtil;
import com.aerospike.graph.synth.util.generator.SchemaUtil;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.MapConfiguration;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Grant Haywood (<a href="http://iowntheinter.net">http://iowntheinter.net</a>)
 */
public class Generator extends Loadable implements Emitter {


    @Override
    public void init(final Configuration config) {

    }

    // Configuration first.
    public static class Config extends ConfigurationBase {
        public static final Config INSTANCE = new Config();

        private Config() {
            super();
        }

        @Override
        public Map<String, String> defaultConfigMap(final Map<String, Object> config) {
            return DEFAULTS;
        }

        @Override
        public List<String> getKeys() {
            return ConfigUtil.getKeysFromClass(Config.Keys.class);
        }


        public static class Keys {
            public static final String SCALE_FACTOR = "generator.scaleFactor";
            public static final String CHANCE_TO_JOIN = "generator.chanceToJoin";
            public static final String SCHEMA_PARSER = "generator.schemaGraphParser";
        }

        private static final Map<String, String> DEFAULTS = new HashMap<>() {{
            put(Generate.Config.Keys.EMITTER, Generator.class.getName());
            put(Keys.SCALE_FACTOR, "100");
        }};
    }

    private final Configuration config;

    //Static variables
    //...

    //Final class variables
    private final OutputIdDriver outputIdDriver;

    private final Long scaleFactor;
    private final Map<String, VertexSchema> rootVertexSchemas;
    private final GraphSchema graphSchema;

    //Mutable variables

    //Constructor
    private Generator(final OutputIdDriver outputIdDriver, GraphSchemaParser schemaGraphSchemaParser, final Configuration config) {
        super(MockOutput.Config.INSTANCE, config);
        this.config = config;
        this.rootVertexSchemas = SchemaUtil.getRootVertexSchemas(schemaGraphSchemaParser.parse());
        this.graphSchema = schemaGraphSchemaParser.parse();
        this.scaleFactor = Long.valueOf(Config.INSTANCE.getOrDefault(Config.Keys.SCALE_FACTOR, config));
        this.outputIdDriver = outputIdDriver;
    }

    //Open, create or getInstance methods
    public static Generator open(final Configuration config) {
        final GraphSchemaParser GraphSchemaParser = (GraphSchemaParser) RuntimeUtil.load(Config.Keys.SCHEMA_PARSER, ConfigUtil.withOverrides(new MapConfiguration(Config.DEFAULTS), config));
        final OutputIdDriver driver = (OutputIdDriver) RuntimeUtil.lookupOrLoad(OutputIdDriver.class, config);
        return new Generator(driver, GraphSchemaParser, config);
    }


    //Public static methods.
    public static Configuration getEmitterConfig() {
        return new MapConfiguration(new HashMap<>() {{
            put(ConfigurationBase.Keys.EMITTER, Generator.class.getName());
        }});
    }

    //Implementation seventh.

    @Override
    public Stream<Emitable> stream(final WorkChunkDriver workChunkDriver, final Runtime.PHASE phase) {

        if (phase == Runtime.PHASE.ONE) {
            final Stream<Long> rootIds = Stream.iterate(workChunkDriver.getNext(), Optional::isPresent, i -> workChunkDriver.getNext())
                    .flatMap(wc -> wc.get().stream())
                    .map(id -> {
                        return (Long) id.get().unwrap();
                    });
            return rootIds.map(archipelagoId ->
                    new GeneratedArchipelago(archipelagoId, graphSchema, outputIdDriver, config));
        } else if (phase == Runtime.PHASE.TWO) {
            return Stream.empty(); //Not yet implemented
        } else {
            return Stream.empty();
        }
    }


    @Override
    public List<Runtime.PHASE> phases() {
        return List.of(Runtime.PHASE.ONE);
    }

    //Public methods eighth.


    public List<String> getAllPropertyKeysForVertexLabel(final String label) {
        return graphSchema.vertexTypes.stream()
                .filter(it -> Objects.equals(it.label(), label))
                .findFirst().orElseThrow(() -> new RuntimeException("Could not find vertex type " + label + graphSchema))
                .properties.stream()
                .map(it -> it.name)
                .collect(Collectors.toList());
    }

    public List<String> getAllPropertyKeysForEdgeLabel(final String label) {
        return graphSchema.edgeTypes.stream()
                .filter(it -> Objects.equals(it.label(), label))
                .findFirst().orElseThrow(() -> new RuntimeException("Could not find edge type" + label))
                .properties.stream()
                .map(it -> it.name)
                .collect(Collectors.toList());
    }


    //Private static methods.

    //Public methods

    //Private methods
    //...

    //Inner classes
    //...

    //toString eleventh.
    @Override
    public String toString() {
        return this.getClass().getName();

    }

    //Close methods twelfth.
    @Override
    public void close() {

    }


}
