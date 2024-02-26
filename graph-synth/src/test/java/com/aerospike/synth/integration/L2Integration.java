/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.synth.integration;

import com.aerospike.graph.synth.emitter.generator.Generator;
import com.aerospike.graph.synth.emitter.generator.schema.seralization.YAMLSchemaParser;
import com.aerospike.graph.synth.process.tasks.generator.Generate;
import com.aerospike.movement.cli.CLI;
import com.aerospike.movement.encoding.files.csv.GraphCSVEncoder;
import com.aerospike.movement.output.files.DirectoryOutput;
import com.aerospike.movement.runtime.core.driver.impl.RangedOutputIdDriver;
import com.aerospike.movement.runtime.core.driver.impl.RangedWorkChunkDriver;
import com.aerospike.movement.runtime.core.local.LocalParallelStreamRuntime;
import com.aerospike.movement.test.core.AbstractMovementTest;
import com.aerospike.movement.util.core.configuration.ConfigUtil;
import com.aerospike.movement.util.core.runtime.IOUtil;
import com.aerospike.movement.util.core.runtime.RuntimeUtil;
import com.aerospike.movement.util.files.FileUtil;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.MapConfiguration;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import static com.aerospike.movement.config.core.ConfigurationBase.Keys.*;
import static com.aerospike.movement.runtime.core.local.LocalParallelStreamRuntime.Config.Keys.BATCH_SIZE;
import static org.junit.Assert.assertTrue;

/*
  Created by Grant Haywood grant@iowntheinter.net
  7/23/23
*/
public class L2Integration extends AbstractMovementTest {
    @Test
    @Ignore
    public void testL2Integration() throws Exception {
        final String yamlFilePath = IOUtil.copyFromResourcesIntoNewTempFile("gdemo_schema.yaml").getAbsolutePath();
        FileUtil.recursiveDelete(Path.of("/tmp/generate/edges"));
        FileUtil.recursiveDelete(Path.of("/tmp/generate/vertices"));


        final String generateDir = "/tmp/generate";
        final Long scale = 160_000L;
        Configuration testConfig = new MapConfiguration(new HashMap<>() {{
            put(LocalParallelStreamRuntime.Config.Keys.THREADS, 12);
            put(EMITTER, Generator.class.getName());
            put(WORK_CHUNK_DRIVER_PHASE_ONE, RangedWorkChunkDriver.class.getName());
            put(YAMLSchemaParser.Config.Keys.YAML_FILE_PATH, yamlFilePath);
            put(BATCH_SIZE, 1);
            put(Generator.Config.Keys.SCALE_FACTOR, scale);
            put(ENCODER, GraphCSVEncoder.class.getName());
            put(OUTPUT, DirectoryOutput.class.getName());
            put(OUTPUT_ID_DRIVER, RangedOutputIdDriver.class.getName());
            put(RangedOutputIdDriver.Config.Keys.RANGE_BOTTOM, String.valueOf(scale + 1));
            put(RangedOutputIdDriver.Config.Keys.RANGE_TOP, String.valueOf(Long.MAX_VALUE));
            put(DirectoryOutput.Config.Keys.OUTPUT_DIRECTORY, generateDir);
        }});

        final File tmpConfig = Files.createTempFile("testconfig", "properties").toFile();
        final String tmpConfigData = ConfigUtil.configurationToPropertiesFormat(testConfig);
        FileWriter fileWriter = new FileWriter(tmpConfig);
        fileWriter.write(tmpConfigData);
        fileWriter.close();
        final String VPATH = generateDir + "/vertices";
        final String EPATH = generateDir + "/edges";
        Path.of(generateDir).toFile().mkdirs();
        Path.of(VPATH).toFile().mkdir();
        Path.of(EPATH).toFile().mkdir();

        RuntimeUtil.registerTaskAlias(Generate.class.getSimpleName(), Generate.class);

        CLI.main(new String[]{"task=Generate", "--test-mode", "-c", tmpConfig.getAbsolutePath()});

        assertTrue(Files.exists(Path.of(VPATH)));
        assertTrue(Files.exists(Path.of(EPATH)));

        final GraphTraversalSource g = AnonymousTraversalSource.traversal().withRemote(DriverRemoteConnection.using("172.17.0.1", 8182, "g"));
        g.V().drop().iterate();
        g.with("evaluationTimeout", 24 * 60 * 60 * 1000).call("bulk-load")
                .with("aerospike.graphloader.vertices", VPATH)
                .with("aerospike.graphloader.edges", EPATH)
                .iterate();
        Assert.assertEquals(8 * scale, g.with("evaluationTimeout", 30 * 60 * 1000).V().count().next().longValue());
        Assert.assertEquals(7 * scale, g.with("evaluationTimeout", 30 * 60 * 1000).E().count().next().longValue());
    }
}
