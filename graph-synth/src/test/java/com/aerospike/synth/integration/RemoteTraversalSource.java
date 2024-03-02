package com.aerospike.synth.integration;

import com.aerospike.graph.synth.cli.CLI;
import com.aerospike.graph.synth.emitter.generator.schema.definition.GraphSchema;
import com.aerospike.graph.synth.emitter.generator.schema.seralization.TinkerPopSchemaTraversalParser;
import com.aerospike.graph.synth.emitter.generator.schema.seralization.YAMLSchemaParser;
import com.aerospike.graph.synth.util.tinkerpop.SchemaGraphUtil;
import com.aerospike.movement.tinkerpop.common.RemoteGraphTraversalProvider;
import com.aerospike.movement.util.core.runtime.IOUtil;
import com.aerospike.movement.util.core.runtime.RuntimeUtil;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static com.aerospike.movement.cli.CLI.setEquals;
import static com.aerospike.movement.runtime.core.local.LocalParallelStreamRuntime.Config.Keys.BATCH_SIZE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RemoteTraversalSource {
    public static final String REMOTE_TRAVERSAL_TARGET = "graph.synth.remote.target";
    public static final String REMOTE_TRAVERSAL_SCHEMA_SOURCE = "graph.synth.schema.remote.source";

    @Test
    public void testGenerateToRemoteTraversalTarget() throws Exception {

        final RemoteGraphTraversalProvider.URIConnectionInfo info;
        final GraphTraversalSource remote;
        if (RuntimeUtil.envOrProperty(REMOTE_TRAVERSAL_TARGET).isEmpty()) {
            URI localhostURI = URI.create("ws://localhost:8182/g");

            info = RemoteGraphTraversalProvider.URIConnectionInfo.from(localhostURI);

        } else {
            final URI remoteUri = URI.create(RuntimeUtil.envOrProperty(REMOTE_TRAVERSAL_TARGET).get());
            info = RemoteGraphTraversalProvider.URIConnectionInfo.from(remoteUri);
        }
        try {
            remote = AnonymousTraversalSource
                    .traversal()
                    .withRemote(DriverRemoteConnection.using(info.host, info.port, info.traversalSourceName));
            remote.V().limit(1).hasNext();

        } catch (Exception e) {
            System.out.println(e);
            System.out.println("skipping test, could not connect to " + info.toString());
            return;
        }
        int TEST_SCALE = 10;
        Path yamlFilePath = IOUtil.copyFromResourcesIntoNewTempFile("gdemo_schema.yaml").toPath();
        final String[] args = {
                CLI.GraphSynthCLI.ArgNames.TEST_MODE,
                CLI.GraphSynthCLI.ArgNames.DEBUG_LONG,
                CLI.GraphSynthCLI.ArgNames.OUTPUT_URI_LONG, info.uri().toString(),
                CLI.GraphSynthCLI.ArgNames.INPUT_URI_LONG, yamlFilePath.toUri().toString(),
                CLI.GraphSynthCLI.ArgNames.SCALE_FACTOR, String.valueOf(TEST_SCALE),
                CLI.GraphSynthCLI.ArgNames.SET_LONG, setEquals(BATCH_SIZE, String.valueOf(1)),
        };
        remote.V().drop().iterate();

        CLI.main(args);
        assertEquals(38 * TEST_SCALE, remote.V().count().next().longValue());
        assertEquals(35 * TEST_SCALE, remote.E().count().next().longValue());
        remote.V().drop().iterate();

    }

    @Test
    public void testGenerateFromRemoteSchemaSource() throws Exception {


        final RemoteGraphTraversalProvider.URIConnectionInfo inputUriInfo;
        final GraphTraversalSource remoteSchema;
        if (RuntimeUtil.envOrProperty(REMOTE_TRAVERSAL_SCHEMA_SOURCE).isEmpty()) {
            URI localhostURI = URI.create("ws://localhost:8182/schema");

            inputUriInfo = RemoteGraphTraversalProvider.URIConnectionInfo.from(localhostURI);

        } else {
            final URI remoteUri = URI.create(RuntimeUtil.envOrProperty(REMOTE_TRAVERSAL_TARGET).get());
            inputUriInfo = RemoteGraphTraversalProvider.URIConnectionInfo.from(remoteUri);
        }
        try {
            remoteSchema = AnonymousTraversalSource
                    .traversal()
                    .withRemote(DriverRemoteConnection.using(inputUriInfo.host, inputUriInfo.port, inputUriInfo.traversalSourceName));
            remoteSchema.V().limit(1).hasNext();

        } catch (Exception e) {
            System.out.println(e);
            System.out.println("skipping test, could not connect to " + inputUriInfo.toString());
            return;
        }

        final File yamlFile = IOUtil.copyFromResourcesIntoNewTempFile("gdemo_schema.yaml");
        GraphSchema fromYamlSchema = YAMLSchemaParser.from(yamlFile.toPath()).parse();
        remoteSchema.V().drop().iterate();

        TinkerPopSchemaTraversalParser.writeTraversalSource(remoteSchema, fromYamlSchema);
        GraphSchema fromGraphSchema = TinkerPopSchemaTraversalParser.fromTraversal(remoteSchema);
        assertTrue(fromGraphSchema.equals(fromYamlSchema));
        int TEST_SCALE = 100;
        Path outputDir = IOUtil.createTempDir();
        final String[] args = {
                CLI.GraphSynthCLI.ArgNames.TEST_MODE,
                CLI.GraphSynthCLI.ArgNames.DEBUG_LONG,
                CLI.GraphSynthCLI.ArgNames.OUTPUT_URI_LONG, outputDir.toUri().toString(),
                CLI.GraphSynthCLI.ArgNames.INPUT_URI_LONG, inputUriInfo.uri().toString(),
                CLI.GraphSynthCLI.ArgNames.SCALE_FACTOR, String.valueOf(TEST_SCALE),
                CLI.GraphSynthCLI.ArgNames.SET_LONG, setEquals(BATCH_SIZE, String.valueOf(1)),
        };

        CLI.main(args);
        assertEquals(13, remoteSchema.V().count().next().longValue());
        assertEquals(13, remoteSchema.E().count().next().longValue());
        long filesWritten = Files.walk(outputDir).filter(it -> it.toFile().isFile()).count();
        long linesWritten = Files.walk(outputDir).filter(it -> it.toFile().isFile()).flatMap(it -> {
            try {
                return Files.lines(it);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).count();
        assertEquals(73 * TEST_SCALE, linesWritten - filesWritten);
        remoteSchema.V().drop().iterate();
    }

    @Test
    public void testGenerateFromRemoteSchemaSourceToRemoteGraph() throws Exception {


        final RemoteGraphTraversalProvider.URIConnectionInfo schemaURIConfig;
        final GraphTraversalSource remoteSchema;
        if (RuntimeUtil.envOrProperty(REMOTE_TRAVERSAL_SCHEMA_SOURCE).isEmpty()) {
            URI localhostURI = URI.create("ws://localhost:8182/schema");

            schemaURIConfig = RemoteGraphTraversalProvider.URIConnectionInfo.from(localhostURI);

        } else {
            final URI remoteUri = URI.create(RuntimeUtil.envOrProperty(REMOTE_TRAVERSAL_TARGET).get());
            schemaURIConfig = RemoteGraphTraversalProvider.URIConnectionInfo.from(remoteUri);
        }
        try {
            remoteSchema = AnonymousTraversalSource
                    .traversal()
                    .withRemote(DriverRemoteConnection.using(schemaURIConfig.host, schemaURIConfig.port, schemaURIConfig.traversalSourceName));
            remoteSchema.V().limit(1).hasNext();

        } catch (Exception e) {
            System.out.println(e);
            System.out.println("skipping test, could not connect to " + schemaURIConfig.toString());
            return;
        }

        final RemoteGraphTraversalProvider.URIConnectionInfo targetURIConfig;
        final GraphTraversalSource remoteGraph;
        if (RuntimeUtil.envOrProperty(REMOTE_TRAVERSAL_TARGET).isEmpty()) {
            URI localhostURI = URI.create("ws://localhost:8182/g");

            targetURIConfig = RemoteGraphTraversalProvider.URIConnectionInfo.from(localhostURI);

        } else {
            final URI remoteUri = URI.create(RuntimeUtil.envOrProperty(REMOTE_TRAVERSAL_TARGET).get());
            targetURIConfig = RemoteGraphTraversalProvider.URIConnectionInfo.from(remoteUri);
        }
        try {
            remoteGraph = AnonymousTraversalSource
                    .traversal()
                    .withRemote(DriverRemoteConnection.using(targetURIConfig.host, targetURIConfig.port, targetURIConfig.traversalSourceName));
            remoteGraph.V().limit(1).hasNext();

        } catch (Exception e) {
            System.out.println(e);
            System.out.println("skipping test, could not connect to " + targetURIConfig.toString());
            return;
        }

        final File yamlFile = IOUtil.copyFromResourcesIntoNewTempFile("gdemo_schema.yaml");
        GraphSchema fromYamlSchema = YAMLSchemaParser.from(yamlFile.toPath()).parse();
        remoteSchema.V().drop().iterate();

        TinkerPopSchemaTraversalParser.writeTraversalSource(remoteSchema, fromYamlSchema);
        GraphSchema fromGraphSchema = TinkerPopSchemaTraversalParser.fromTraversal(remoteSchema);
        assertTrue(fromGraphSchema.equals(fromYamlSchema));
        int TEST_SCALE = 100;
        Path outputDir = IOUtil.createTempDir();
        final String[] args = {
                CLI.GraphSynthCLI.ArgNames.TEST_MODE,
                CLI.GraphSynthCLI.ArgNames.DEBUG_LONG,
                CLI.GraphSynthCLI.ArgNames.OUTPUT_URI_LONG, targetURIConfig.uri().toString(),
                CLI.GraphSynthCLI.ArgNames.INPUT_URI_LONG, schemaURIConfig.uri().toString(),
                CLI.GraphSynthCLI.ArgNames.SCALE_FACTOR, String.valueOf(TEST_SCALE),
                CLI.GraphSynthCLI.ArgNames.SET_LONG, setEquals(BATCH_SIZE, String.valueOf(1)),
        };

        CLI.main(args);
        assertEquals(13, remoteSchema.V().count().next().longValue());
        assertEquals(13, remoteSchema.E().count().next().longValue());
        long generatedVcount = remoteGraph.V().count().next().longValue();
        long generatedEcount = remoteGraph.E().count().next().longValue();
        assertEquals(73 * TEST_SCALE, generatedEcount + generatedVcount);
        remoteSchema.V().drop().iterate();
    }
}
