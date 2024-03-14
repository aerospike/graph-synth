/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.synth.emitter.generator;

import com.aerospike.graph.synth.emitter.generator.Generator;
import com.aerospike.graph.synth.process.tasks.generator.Generate;
import com.aerospike.graph.synth.util.tinkerpop.InMemorySchemaGraphProvider;
import com.aerospike.movement.config.core.ConfigurationBase;
import com.aerospike.graph.synth.emitter.generator.schema.seralization.TinkerPopSchemaTraversalParser;
import com.aerospike.graph.synth.emitter.generator.schema.seralization.YAMLSchemaParser;
import com.aerospike.graph.synth.emitter.generator.schema.definition.GraphSchema;
import com.aerospike.movement.encoding.tinkerpop.TinkerPopTraversalEncoder;
import com.aerospike.movement.output.tinkerpop.TinkerPopTraversalOutput;
import com.aerospike.movement.runtime.core.driver.impl.RangedOutputIdDriver;
import com.aerospike.movement.runtime.core.driver.impl.RangedWorkChunkDriver;
import com.aerospike.movement.runtime.core.local.LocalParallelStreamRuntime;
import com.aerospike.movement.test.tinkerpop.SharedEmptyTinkerGraphGraphProvider;
import com.aerospike.movement.test.tinkerpop.SharedEmptyTinkerGraphTraversalProvider;;
import com.aerospike.movement.tinkerpop.common.GraphProvider;
import com.aerospike.movement.util.core.configuration.ConfigUtil;
import com.aerospike.movement.util.core.runtime.IOUtil;
import com.aerospike.synth.emitter.generator.schema.ExampleSchemas;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.MapConfiguration;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;

import static com.aerospike.movement.config.core.ConfigurationBase.Keys.*;
import static com.aerospike.movement.test.core.AbstractMovementTest.testTask;
import static junit.framework.TestCase.assertEquals;

public class SchemaGraphIntegration {

    @Before
    @After
    public void clearSchemaGraph() throws Exception {
        InMemorySchemaGraphProvider.getGraphInstance().traversal().V().drop().iterate();
        Graph graph = SharedEmptyTinkerGraphGraphProvider.open().getProvided(GraphProvider.GraphProviderContext.OUTPUT);
        graph.traversal().V().drop().iterate();
        graph.close();
    }

    public static void addSimplestSchemaToGraph(final Graph schemaGraph) {
        ExampleSchemas.Simplest.INSTANCE.addToGraph(schemaGraph);
    }

    @Test
    public void generateFromGremlinStatementsSimple() throws Exception {
        Graph schemaGraph = InMemorySchemaGraphProvider.getGraphInstance();
        addSimplestSchemaToGraph(schemaGraph);

        final Long scaleFactor = 1L;

        final Configuration testConfig = new MapConfiguration(
                new HashMap<>() {{
                    put(Generator.Config.Keys.SCHEMA_PARSER, TinkerPopSchemaTraversalParser.class.getName());
                    put(TinkerPopSchemaTraversalParser.Config.Keys.GRAPH_PROVIDER, InMemorySchemaGraphProvider.class.getName());
                    put(LocalParallelStreamRuntime.Config.Keys.BATCH_SIZE, 1);
                    put(EMITTER, Generator.class.getName());
                    put(ConfigurationBase.Keys.ENCODER, TinkerPopTraversalEncoder.class.getName());
                    put(TinkerPopTraversalEncoder.Config.Keys.TRAVERSAL_PROVIDER, SharedEmptyTinkerGraphTraversalProvider.class.getName());
                    put(ConfigurationBase.Keys.OUTPUT, TinkerPopTraversalOutput.class.getName());
                    put(Generator.Config.Keys.SCALE_FACTOR, String.valueOf(scaleFactor));
                    put(WORK_CHUNK_DRIVER_PHASE_ONE, RangedWorkChunkDriver.class.getName());
                    put(OUTPUT_ID_DRIVER, RangedOutputIdDriver.class.getName());
                    put(RangedWorkChunkDriver.Config.Keys.RANGE_BOTTOM, 0L);
                    put(RangedWorkChunkDriver.Config.Keys.RANGE_TOP, scaleFactor);
                    put(RangedOutputIdDriver.Config.Keys.RANGE_BOTTOM, scaleFactor * 10);
                    put(RangedOutputIdDriver.Config.Keys.RANGE_TOP, Long.MAX_VALUE);
                }});
        System.out.println(ConfigUtil.configurationToPropertiesFormat(testConfig));

        final Graph graph = SharedEmptyTinkerGraphGraphProvider.open().getProvided(GraphProvider.GraphProviderContext.OUTPUT);
        graph.traversal().V().drop().iterate();

        testTask(Generate.class, testConfig);
        long countV = graph.traversal().V().count().next().longValue();
        long countE = graph.traversal().E().count().next().longValue();
        graph.close();

        assertEquals(2L, countV);
        assertEquals(1L, countE);

    }

    @Test
    public void generateFromGremlinStatementsSynthetic() throws Exception {
        Graph schemaGraph = InMemorySchemaGraphProvider.getGraphInstance();
        ExampleSchemas.Synthetic.INSTANCE.addToGraph(schemaGraph);
        final Long scaleFactor = 1L;

        final Configuration testConfig = new MapConfiguration(
                new HashMap<>() {{
                    put(Generator.Config.Keys.SCHEMA_PARSER, TinkerPopSchemaTraversalParser.class.getName());
                    put(TinkerPopSchemaTraversalParser.Config.Keys.GRAPH_PROVIDER, InMemorySchemaGraphProvider.class.getName());
                    put(LocalParallelStreamRuntime.Config.Keys.BATCH_SIZE, 1);
                    put(EMITTER, Generator.class.getName());
                    put(ConfigurationBase.Keys.ENCODER, TinkerPopTraversalEncoder.class.getName());
                    put(TinkerPopTraversalEncoder.Config.Keys.TRAVERSAL_PROVIDER, SharedEmptyTinkerGraphTraversalProvider.class.getName());
                    put(ConfigurationBase.Keys.OUTPUT, TinkerPopTraversalOutput.class.getName());
                    put(Generator.Config.Keys.SCALE_FACTOR, String.valueOf(scaleFactor));
                    put(WORK_CHUNK_DRIVER_PHASE_ONE, RangedWorkChunkDriver.class.getName());
                    put(OUTPUT_ID_DRIVER, RangedOutputIdDriver.class.getName());
                    put(RangedWorkChunkDriver.Config.Keys.RANGE_BOTTOM, 0L);
                    put(RangedWorkChunkDriver.Config.Keys.RANGE_TOP, scaleFactor);
                    put(RangedOutputIdDriver.Config.Keys.RANGE_BOTTOM, scaleFactor * 10);
                    put(RangedOutputIdDriver.Config.Keys.RANGE_TOP, Long.MAX_VALUE);
                }});
        System.out.println(ConfigUtil.configurationToPropertiesFormat(testConfig));

        final Graph graph = SharedEmptyTinkerGraphGraphProvider.open().getProvided(GraphProvider.GraphProviderContext.OUTPUT);
        graph.traversal().V().drop().iterate();

        testTask(Generate.class, testConfig);
        long countV = graph.traversal().V().count().next().longValue();
        long countE = graph.traversal().E().count().next().longValue();
        graph.close();

        assertEquals(2L, countV);
        assertEquals(1L, countE);

    }

    @Test
    public void generateFromGraphSON() throws Exception {
        final Long scaleFactor = 1L;
        final File yamlFile = IOUtil.copyFromResourcesIntoNewTempFile("simplest_schema.yaml");
        GraphSchema schema = YAMLSchemaParser.from(yamlFile.toPath()).parse();
        Path graphsonSchema = Path.of("target/simplest_schema.json");
        TinkerPopSchemaTraversalParser.writeGraphSON(schema, graphsonSchema);

        final Configuration testConfig = new MapConfiguration(
                new HashMap<>() {{
                    put(Generator.Config.Keys.SCHEMA_PARSER, TinkerPopSchemaTraversalParser.class.getName());
                    put(TinkerPopSchemaTraversalParser.Config.Keys.GRAPHSON_FILE, graphsonSchema.toAbsolutePath().toString());
                    put(LocalParallelStreamRuntime.Config.Keys.BATCH_SIZE, 1);
                    put(EMITTER, Generator.class.getName());
                    put(ConfigurationBase.Keys.ENCODER, TinkerPopTraversalEncoder.class.getName());
                    put(TinkerPopTraversalEncoder.Config.Keys.TRAVERSAL_PROVIDER, SharedEmptyTinkerGraphTraversalProvider.class.getName());
                    put(ConfigurationBase.Keys.OUTPUT, TinkerPopTraversalOutput.class.getName());
                    put(Generator.Config.Keys.SCALE_FACTOR, String.valueOf(scaleFactor));
                    put(WORK_CHUNK_DRIVER_PHASE_ONE, RangedWorkChunkDriver.class.getName());
                    put(OUTPUT_ID_DRIVER, RangedOutputIdDriver.class.getName());
                    put(RangedWorkChunkDriver.Config.Keys.RANGE_BOTTOM, 0L);
                    put(RangedWorkChunkDriver.Config.Keys.RANGE_TOP, scaleFactor);
                    put(RangedOutputIdDriver.Config.Keys.RANGE_BOTTOM, scaleFactor * 10);
                    put(RangedOutputIdDriver.Config.Keys.RANGE_TOP, Long.MAX_VALUE);
                }});
        System.out.println(ConfigUtil.configurationToPropertiesFormat(testConfig));

        final Graph graph = SharedEmptyTinkerGraphGraphProvider.open().getProvided(GraphProvider.GraphProviderContext.OUTPUT);
        graph.traversal().V().drop().iterate();

        testTask(Generate.class, testConfig);
        long countV = graph.traversal().V().count().next().longValue();
        long countE = graph.traversal().E().count().next().longValue();
        graph.close();
        assertEquals(2L, countV);
        assertEquals(1L, countE);
    }

    @Test
    public void simplestSchema() throws Exception {
        final Long scaleFactor = 1L;
        final File schemaFile = IOUtil.copyFromResourcesIntoNewTempFile("simplest_schema.yaml");

        final Configuration testConfig = new MapConfiguration(
                new HashMap<>() {{
                    put(YAMLSchemaParser.Config.Keys.YAML_FILE_PATH, schemaFile.getAbsolutePath());
                    put(Generator.Config.Keys.SCHEMA_PARSER, YAMLSchemaParser.class.getName());
                    put(LocalParallelStreamRuntime.Config.Keys.BATCH_SIZE, 1);
                    put(EMITTER, Generator.class.getName());
                    put(ConfigurationBase.Keys.ENCODER, TinkerPopTraversalEncoder.class.getName());
                    put(TinkerPopTraversalEncoder.Config.Keys.TRAVERSAL_PROVIDER, SharedEmptyTinkerGraphTraversalProvider.class.getName());
                    put(ConfigurationBase.Keys.OUTPUT, TinkerPopTraversalOutput.class.getName());
                    put(Generator.Config.Keys.SCALE_FACTOR, String.valueOf(scaleFactor));

                    put(WORK_CHUNK_DRIVER_PHASE_ONE, RangedWorkChunkDriver.class.getName());
                    put(OUTPUT_ID_DRIVER, RangedOutputIdDriver.class.getName());

                    put(RangedWorkChunkDriver.Config.Keys.RANGE_BOTTOM, 0L);
                    put(RangedWorkChunkDriver.Config.Keys.RANGE_TOP, scaleFactor);
                    put(RangedOutputIdDriver.Config.Keys.RANGE_BOTTOM, scaleFactor * 10);
                    put(RangedOutputIdDriver.Config.Keys.RANGE_TOP, Long.MAX_VALUE);
                }});
        System.out.println(ConfigUtil.configurationToPropertiesFormat(testConfig));

        final Graph graph = SharedEmptyTinkerGraphGraphProvider.open().getProvided(GraphProvider.GraphProviderContext.OUTPUT);
        graph.traversal().V().drop().iterate();

        testTask(Generate.class, testConfig);
        long countV = graph.traversal().V().count().next().longValue();
        long countE = graph.traversal().E().count().next().longValue();
        graph.close();

        assertEquals(2L, countV);
        assertEquals(1L, countE);
        GraphSchema schema = YAMLSchemaParser.from(schemaFile.toPath()).parse();
        TinkerPopSchemaTraversalParser.writeGraphSON(schema, Path.of("target/simplest_schema.json"));
    }

}
