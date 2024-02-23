package com.aerospike.synth.emitter.generator.schema;

import com.aerospike.graph.synth.emitter.generator.Generator;
import com.aerospike.graph.synth.emitter.generator.schema.seralization.TinkerPopSchemaParser;
import com.aerospike.graph.synth.util.tinkerpop.InMemorySchemaGraphProvider;
import com.aerospike.movement.config.core.ConfigurationBase;
import com.aerospike.movement.encoding.tinkerpop.TinkerPopTraversalEncoder;
import com.aerospike.movement.output.tinkerpop.TinkerPopTraversalOutput;
import com.aerospike.movement.runtime.core.Runtime;

import com.aerospike.movement.runtime.core.driver.impl.RangedOutputIdDriver;
import com.aerospike.movement.runtime.core.driver.impl.RangedWorkChunkDriver;
import com.aerospike.movement.runtime.core.local.LocalParallelStreamRuntime;
import com.aerospike.movement.runtime.core.local.RunningPhase;
import com.aerospike.movement.test.tinkerpop.SharedEmptyTinkerGraphTraversalProvider;
import com.aerospike.movement.util.core.configuration.ConfigUtil;
import com.aerospike.movement.util.core.iterator.ConfiguredRangeSupplier;
import com.aerospike.movement.util.core.iterator.ext.IteratorUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.MapConfiguration;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;

import static com.aerospike.movement.config.core.ConfigurationBase.Keys.*;
import static junit.framework.TestCase.assertEquals;


@RunWith(Parameterized.class)
public class TestSchemas {

    private final TestSchema testSchema;

    @Parameterized.Parameters
    public static Collection<TestSchema> data() {
        return List.of(TestSchema.SimplestTestSchema.INSTANCE, TestSchema.BenchmarkTestData.INSTANCE);
    }

    public TestSchemas(TestSchema testSchema) {
        this.testSchema = testSchema;
    }

    @Before
    @After
    public void clearSchemaGraph() {
        InMemorySchemaGraphProvider.getGraphInstance().traversal().V().drop().iterate();
    }


    @Test
    public void generateFromGremlinStatementsSimple() {
        Graph schemaGraph = InMemorySchemaGraphProvider.getGraphInstance();
        testSchema.addToGraph(schemaGraph);

        final Long scaleFactor = 1L;

        final Configuration testConfig = new MapConfiguration(
                new HashMap<>() {{
                    put(Generator.Config.Keys.SCHEMA_PARSER, TinkerPopSchemaParser.class.getName());
                    put(TinkerPopSchemaParser.Config.Keys.GRAPH_PROVIDER, InMemorySchemaGraphProvider.class.getName());
                    put(LocalParallelStreamRuntime.Config.Keys.BATCH_SIZE, 1);
                    put(EMITTER, Generator.class.getName());
                    put(ConfigurationBase.Keys.ENCODER, TinkerPopTraversalEncoder.class.getName());
                    put(TinkerPopTraversalEncoder.Config.Keys.TRAVERSAL_PROVIDER, SharedEmptyTinkerGraphTraversalProvider.class.getName());
                    put(ConfigurationBase.Keys.OUTPUT, TinkerPopTraversalOutput.class.getName());

                    put(WORK_CHUNK_DRIVER_PHASE_ONE, RangedWorkChunkDriver.class.getName());
                    put(OUTPUT_ID_DRIVER, RangedOutputIdDriver.class.getName());
                    put(RangedWorkChunkDriver.Config.Keys.RANGE_BOTTOM, 0L);
                    put(RangedWorkChunkDriver.Config.Keys.RANGE_TOP, scaleFactor);
                    put(RangedOutputIdDriver.Config.Keys.RANGE_BOTTOM, scaleFactor * 10);
                    put(RangedOutputIdDriver.Config.Keys.RANGE_TOP, Long.MAX_VALUE);
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
        assertEquals((long) testSchema.verticesForScaleFactor(scaleFactor), g.V().count().next().longValue());
        assertEquals((long) testSchema.edgesForScaleFactor(scaleFactor), g.E().count().next().longValue());
    }
}
