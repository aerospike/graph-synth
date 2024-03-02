package com.aerospike.synth.integration;

import com.aerospike.graph.synth.cli.CLI;
import com.aerospike.graph.synth.emitter.generator.schema.definition.GraphSchema;
import com.aerospike.graph.synth.emitter.generator.schema.seralization.TinkerPopSchemaParser;
import com.aerospike.graph.synth.emitter.generator.schema.seralization.YAMLSchemaParser;
import com.aerospike.movement.config.core.ConfigurationBase;
import com.aerospike.movement.runtime.core.driver.impl.RangedOutputIdDriver;
import com.aerospike.movement.runtime.core.driver.impl.RangedWorkChunkDriver;
import com.aerospike.movement.runtime.core.local.LocalParallelStreamRuntime;
import com.aerospike.movement.test.mock.task.MockTask;
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
import static com.aerospike.movement.runtime.core.local.LocalParallelStreamRuntime.Config.Keys.THREADS;
import static com.aerospike.movement.util.core.runtime.RuntimeUtil.getAvailableProcessors;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RemoteTraversalSource {
    public static final String REMOTE_TRAVERSAL_TARGET = "graph.synth.remote.target";
    public static final String REMOTE_TRAVERSAL_SCHEMA_SOURCE = "graph.synth.schema.remote.source";

    public static Optional<String> envOrProperty(final String key) {
        Optional<String> envVal = Optional.ofNullable(System.getenv(key));
        Optional<String> propVal = Optional.ofNullable(System.getProperty(key));
        if (envVal.isPresent())
            return envVal;
        if (propVal.isPresent())
            return propVal;
        return Optional.empty();
    }

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
        final GraphTraversalSource remote;
        if (RuntimeUtil.envOrProperty(REMOTE_TRAVERSAL_TARGET).isEmpty()) {
            URI localhostURI = URI.create("ws://localhost:8182/g");

            inputUriInfo = RemoteGraphTraversalProvider.URIConnectionInfo.from(localhostURI);

        } else {
            final URI remoteUri = URI.create(RuntimeUtil.envOrProperty(REMOTE_TRAVERSAL_TARGET).get());
            inputUriInfo = RemoteGraphTraversalProvider.URIConnectionInfo.from(remoteUri);
        }
        try {
            remote = AnonymousTraversalSource
                    .traversal()
                    .withRemote(DriverRemoteConnection.using(inputUriInfo.host, inputUriInfo.port, inputUriInfo.traversalSourceName));
            remote.V().limit(1).hasNext();

        } catch (Exception e) {
            System.out.println(e);
            System.out.println("skipping test, could not connect to " + inputUriInfo.toString());
            return;
        }

        final File yamlFile = IOUtil.copyFromResourcesIntoNewTempFile("gdemo_schema.yaml");
        GraphSchema fromYamlSchema = YAMLSchemaParser.from(yamlFile.toPath()).parse();
        remote.V().drop().iterate();

        TinkerPopSchemaParser.writeTraversalSource(remote, fromYamlSchema);
        GraphSchema fromGraphSchema = TinkerPopSchemaParser.fromTraversal(remote);
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
        assertEquals(13, remote.V().count().next().longValue());
        assertEquals(13, remote.E().count().next().longValue());
        long filesWritten = Files.walk(outputDir).filter(it -> it.toFile().isFile()).count();
        long linesWritten = Files.walk(outputDir).filter(it -> it.toFile().isFile()).flatMap(it -> {
            try {
                return Files.lines(it);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).count();
        assertEquals(3*TEST_SCALE, linesWritten - filesWritten);
        remote.V().drop().iterate();

    }
}
