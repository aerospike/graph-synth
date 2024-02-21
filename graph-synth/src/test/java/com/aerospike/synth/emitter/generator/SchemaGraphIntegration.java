/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.synth.emitter.generator;

import com.aerospike.graph.synth.emitter.generator.Generator;
import com.aerospike.movement.config.core.ConfigurationBase;
import com.aerospike.graph.synth.emitter.generator.schema.seralization.TinkerPopSchemaParser;
import com.aerospike.graph.synth.emitter.generator.schema.seralization.YAMLSchemaParser;
import com.aerospike.graph.synth.emitter.generator.schema.definition.GraphSchema;
import com.aerospike.movement.encoding.tinkerpop.TinkerPopTraversalEncoder;
import com.aerospike.movement.output.tinkerpop.TinkerPopTraversalOutput;
import com.aerospike.movement.runtime.core.Runtime;
import com.aerospike.movement.runtime.core.driver.impl.GeneratedOutputIdDriver;
import com.aerospike.movement.runtime.core.driver.impl.SuppliedWorkChunkDriver;
import com.aerospike.movement.runtime.core.local.LocalParallelStreamRuntime;
import com.aerospike.movement.runtime.core.local.RunningPhase;
import com.aerospike.movement.test.tinkerpop.SharedEmptyTinkerGraphTraversalProvider;;
import com.aerospike.movement.util.core.configuration.ConfigUtil;
import com.aerospike.movement.util.core.iterator.ConfiguredRangeSupplier;
import com.aerospike.movement.util.core.iterator.ext.IteratorUtils;
import com.aerospike.movement.util.core.runtime.IOUtil;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.MapConfiguration;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static com.aerospike.movement.config.core.ConfigurationBase.Keys.*;
import static junit.framework.TestCase.assertEquals;

public class SchemaGraphIntegration {

    @Test
    public void generateFromGraphSON() {
        final Long scaleFactor = 1L;
        final File yamlFile = IOUtil.copyFromResourcesIntoNewTempFile("simplest_schema.yaml");
        GraphSchema schema = YAMLSchemaParser.from(yamlFile.toPath()).parse();
        Path graphsonSchema = Path.of("target/simplest_schema.json");
        TinkerPopSchemaParser.writeGraphSON(schema, graphsonSchema);

        final Configuration testConfig = new MapConfiguration(
                new HashMap<>() {{
                    put(Generator.Config.Keys.SCHEMA_PARSER, TinkerPopSchemaParser.class.getName());
                    put(TinkerPopSchemaParser.Config.Keys.GRAPHSON_FILE, graphsonSchema.toAbsolutePath().toString());
                    put(LocalParallelStreamRuntime.Config.Keys.BATCH_SIZE, 1);
                    put(EMITTER, Generator.class.getName());
                    put(ConfigurationBase.Keys.ENCODER, TinkerPopTraversalEncoder.class.getName());
                    put(TinkerPopTraversalEncoder.Config.Keys.TRAVERSAL_PROVIDER, SharedEmptyTinkerGraphTraversalProvider.class.getName());
                    put(ConfigurationBase.Keys.OUTPUT, TinkerPopTraversalOutput.class.getName());

                    put(WORK_CHUNK_DRIVER_PHASE_ONE, SuppliedWorkChunkDriver.class.getName());
                    put(OUTPUT_ID_DRIVER, GeneratedOutputIdDriver.class.getName());
                    put(SuppliedWorkChunkDriver.Config.Keys.ITERATOR_SUPPLIER_PHASE_ONE, ConfiguredRangeSupplier.class.getName());
                    put(ConfiguredRangeSupplier.Config.Keys.RANGE_BOTTOM, 0L);
                    put(ConfiguredRangeSupplier.Config.Keys.RANGE_TOP, scaleFactor);
                    put(GeneratedOutputIdDriver.Config.Keys.RANGE_BOTTOM, scaleFactor * 10);
                    put(GeneratedOutputIdDriver.Config.Keys.RANGE_TOP, Long.MAX_VALUE);
                }});
        System.out.println(ConfigUtil.configurationToPropertiesFormat(testConfig));

        final GraphTraversalSource g = SharedEmptyTinkerGraphTraversalProvider.getGraphInstance().traversal();
        g.V().drop().iterate();


        final Runtime runtime = LocalParallelStreamRuntime.open(testConfig);
        final Iterator<RunningPhase> x = runtime.runPhases(List.of(Runtime.PHASE.ONE), testConfig);
        while (x.hasNext()) {
            final RunningPhase y = x.next();
            IteratorUtils.iterate(y);
            y.get();
            y.close();
        }
        runtime.close();
        assertEquals(2L, g.V().count().next().longValue());
        assertEquals(1L, g.E().count().next().longValue());
    }

    @Test
    public void simplestSchema() {
        final Long SCALE_FACTOR = 1L;
        final File schemaFile = IOUtil.copyFromResourcesIntoNewTempFile("simplest_schema.yaml");

        final Configuration testConfig = new MapConfiguration(
                new HashMap<>() {{
                    put(YAMLSchemaParser.Config.Keys.YAML_FILE_PATH, schemaFile.getAbsolutePath());
                    put(LocalParallelStreamRuntime.Config.Keys.BATCH_SIZE, 1);
                    put(EMITTER, Generator.class.getName());
                    put(ConfigurationBase.Keys.ENCODER, TinkerPopTraversalEncoder.class.getName());
                    put(TinkerPopTraversalEncoder.Config.Keys.TRAVERSAL_PROVIDER, SharedEmptyTinkerGraphTraversalProvider.class.getName());
                    put(ConfigurationBase.Keys.OUTPUT, TinkerPopTraversalOutput.class.getName());

                    put(WORK_CHUNK_DRIVER_PHASE_ONE, SuppliedWorkChunkDriver.class.getName());
                    put(OUTPUT_ID_DRIVER, GeneratedOutputIdDriver.class.getName());
                    put(SuppliedWorkChunkDriver.Config.Keys.ITERATOR_SUPPLIER_PHASE_ONE, ConfiguredRangeSupplier.class.getName());

                    put(ConfiguredRangeSupplier.Config.Keys.RANGE_BOTTOM, 0L);
                    put(ConfiguredRangeSupplier.Config.Keys.RANGE_TOP, SCALE_FACTOR);
                    put(GeneratedOutputIdDriver.Config.Keys.RANGE_BOTTOM, SCALE_FACTOR * 10);
                    put(GeneratedOutputIdDriver.Config.Keys.RANGE_TOP, Long.MAX_VALUE);
                }});
        System.out.println(ConfigUtil.configurationToPropertiesFormat(testConfig));

        final GraphTraversalSource g = SharedEmptyTinkerGraphTraversalProvider.getGraphInstance().traversal();
        g.V().drop().iterate();


        final Runtime runtime = LocalParallelStreamRuntime.open(testConfig);
        final Iterator<RunningPhase> x = runtime.runPhases(List.of(Runtime.PHASE.ONE), testConfig);
        while (x.hasNext()) {
            final RunningPhase y = x.next();
            IteratorUtils.iterate(y);
            y.get();
            y.close();
        }
        runtime.close();
        assertEquals(2L, g.V().count().next().longValue());
        assertEquals(1L, g.E().count().next().longValue());
        GraphSchema schema = YAMLSchemaParser.from(schemaFile.toPath()).parse();
        TinkerPopSchemaParser.writeGraphSON(schema, Path.of("target/simplest_schema.json"));
    }

}
