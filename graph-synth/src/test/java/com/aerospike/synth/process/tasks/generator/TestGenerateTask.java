/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.synth.process.tasks.generator;

import com.aerospike.graph.synth.emitter.generator.Generator;
import com.aerospike.graph.synth.emitter.generator.schema.seralization.YAMLSchemaParser;
import com.aerospike.graph.synth.process.tasks.generator.Generate;
import com.aerospike.movement.process.core.Task;
import com.aerospike.movement.runtime.core.Runtime;
import com.aerospike.movement.runtime.core.driver.impl.RangedWorkChunkDriver;
import com.aerospike.movement.runtime.core.local.LocalParallelStreamRuntime;
import com.aerospike.movement.runtime.core.local.RunningPhase;
import com.aerospike.movement.test.core.AbstractMovementTest;
import com.aerospike.movement.test.mock.MockUtil;
import com.aerospike.movement.test.mock.encoder.MockEncoder;
import com.aerospike.movement.test.mock.output.MockOutput;
import com.aerospike.movement.util.core.configuration.ConfigUtil;
import com.aerospike.movement.util.core.runtime.IOUtil;
import com.aerospike.movement.util.core.runtime.RuntimeUtil;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.MapConfiguration;
import org.junit.Test;

import java.util.*;

import static com.aerospike.movement.config.core.ConfigurationBase.Keys.ENCODER;
import static com.aerospike.movement.config.core.ConfigurationBase.Keys.OUTPUT;
import static com.aerospike.movement.test.mock.MockUtil.getHitCounter;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestGenerateTask extends AbstractMovementTest {

    @Test
    public void testGenerateTaskIsRegistered() {
        final Map<String, Class<? extends Task>> tasks = RuntimeUtil.getTasks();
        RuntimeUtil.loadClass(Generate.class.getName());
        assertTrue(tasks.containsKey(Generate.class.getSimpleName()));
    }


    @Test
    public void testGenerateConformsToScaleFactor() {
        String schemaPath = IOUtil.copyFromResourcesIntoNewTempFile("example_schema.yaml").getAbsolutePath();
        final long scaleFactor = 100L;

        final Map<String, String> testConfig = new HashMap<>() {{
            put(Generate.Config.Keys.SCALE_FACTOR, String.valueOf(scaleFactor));
            put(YAMLSchemaParser.Config.Keys.YAML_FILE_PATH, schemaPath);
            put(Generator.Config.Keys.SCHEMA_PARSER, YAMLSchemaParser.class.getName());
            put(LocalParallelStreamRuntime.Config.Keys.THREADS, String.valueOf(1));
            put(OUTPUT, MockOutput.class.getName());
            put(ENCODER, MockEncoder.class.getName());
        }};
        MockUtil.setDefaultMockCallbacks();
        testTask(Generate.class, new MapConfiguration(testConfig));
        assertEquals(15 * scaleFactor, getHitCounter(MockEncoder.class, MockEncoder.Methods.ENCODE));
    }


}
