package com.aerospike.synth.process.tasks.generator;

import com.aerospike.graph.synth.emitter.generator.Generator;
import com.aerospike.graph.synth.emitter.generator.schema.seralization.TinkerPopSchemaParser;
import com.aerospike.graph.synth.process.tasks.generator.Generate;
import com.aerospike.graph.synth.util.tinkerpop.InMemorySchemaGraphProvider;
import com.aerospike.movement.config.core.ConfigurationBase;
import com.aerospike.movement.encoding.tinkerpop.TinkerPopGraphEncoder;
import com.aerospike.movement.encoding.tinkerpop.TinkerPopTraversalEncoder;
import com.aerospike.movement.output.tinkerpop.TinkerPopGraphOutput;
import com.aerospike.movement.output.tinkerpop.TinkerPopTraversalOutput;
import com.aerospike.movement.plugin.PluginInterface;
import com.aerospike.movement.plugin.tinkerpop.CallStepPlugin;
import com.aerospike.movement.plugin.tinkerpop.PluginServiceFactory;
import com.aerospike.movement.runtime.core.local.LocalParallelStreamRuntime;
import com.aerospike.movement.test.mock.MockUtil;
import com.aerospike.movement.test.tinkerpop.SharedEmptyTinkerGraphGraphProvider;
import com.aerospike.movement.test.tinkerpop.SharedEmptyTinkerGraphTraversalProvider;
import com.aerospike.movement.test.tinkerpop.SharedTinkerClassicGraphProvider;
import com.aerospike.movement.tinkerpop.common.GraphProvider;
import com.aerospike.movement.util.core.runtime.RuntimeUtil;
import com.aerospike.synth.emitter.generator.SchemaGraphIntegration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.MapConfiguration;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.aerospike.graph.synth.emitter.generator.Generator.Config.Keys.SCALE_FACTOR;
import static com.aerospike.graph.synth.emitter.generator.Generator.Config.Keys.SCHEMA_PARSER;
import static org.junit.Assert.assertEquals;

public class TestGenerateCall {
    @Before
    @After
    public void clearSchemaGraph() {
        InMemorySchemaGraphProvider.getGraphInstance().traversal().V().drop().iterate();
    }

    @Test
    public void testGeneratePlugin() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
        RuntimeUtil.registerTaskAlias(Generate.class.getSimpleName(), Generate.class);
        final Long TEST_SCALE_FACTOR = 100L;
        Graph schemaGraph = InMemorySchemaGraphProvider.getGraphInstance();
        SchemaGraphIntegration.addSimplestSchemaToGraph(schemaGraph);

        final Map<String, String> configMap =
                Generate.Config.INSTANCE.defaultConfigMap(new HashMap<>() {{
                    put(LocalParallelStreamRuntime.Config.Keys.THREADS, String.valueOf(1)); //TinkerGraph is not thread safe
                }});

        final Graph graph = SharedEmptyTinkerGraphGraphProvider.getGraphInstance();
        graph.traversal().V().drop().iterate();
        final Configuration config = new MapConfiguration(configMap);
        final Object plugin = RuntimeUtil.openClassRef(CallStepPlugin.class.getName(), config);

        plugin.getClass().getMethod(PluginInterface.Methods.PLUG_INTO, Object.class).invoke(plugin, graph);
        System.out.println(graph.traversal().call("--list").toList());

        MockUtil.setDefaultMockCallbacks();
        final GraphProvider inputGraphProvider = SharedTinkerClassicGraphProvider.open(config);
        long start = System.nanoTime();
        Map<String, Object> x = (Map<String, Object>) graph.traversal()
                .call(Generate.class.getSimpleName())
                .with(TinkerPopSchemaParser.Config.Keys.GRAPH_PROVIDER, InMemorySchemaGraphProvider.class.getName())
                .with(SCHEMA_PARSER, TinkerPopSchemaParser.class.getName())
                .with(TinkerPopGraphEncoder.Config.Keys.GRAPH_PROVIDER, SharedEmptyTinkerGraphGraphProvider.class.getName())
                .with(ConfigurationBase.Keys.OUTPUT, TinkerPopGraphOutput.class.getName())
                .with(ConfigurationBase.Keys.ENCODER, TinkerPopGraphEncoder.class.getName())
                .with(SCALE_FACTOR, TEST_SCALE_FACTOR)
                .next();

        UUID id = (UUID) x.get("id");
        System.out.println(x);

        Iterator<?> status = graph.traversal()
                .call(PluginServiceFactory.TASK_STATUS)
                .with(LocalParallelStreamRuntime.TASK_ID_KEY, id.toString());

        if (status.hasNext()) System.out.println(status.next());
        RuntimeUtil.waitTask(id);
        long elapsed = System.nanoTime() - start;
        System.out.printf("elapsed time: %d ms\n", TimeUnit.NANOSECONDS.toMillis(elapsed));

        long loadedVertexCount = graph.traversal().V().count().next();
        long loadedEdgeCount = graph.traversal().E().count().next();
        long expectedVertexCount = TEST_SCALE_FACTOR * 2;
        long expectedEdgeCount = TEST_SCALE_FACTOR * 1;
        System.out.printf("%d vertices Generated, %d expected\n", loadedVertexCount, expectedVertexCount);
        assertEquals(expectedVertexCount, loadedVertexCount);
        System.out.printf("%d edges Generated, %d expected\n", loadedEdgeCount, expectedEdgeCount);
        assertEquals(expectedEdgeCount, loadedEdgeCount);
    }
}
