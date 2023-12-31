/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.synth.process.tasks.generator;

import com.aerospike.graph.synth.emitter.generator.schema.seralization.YAMLSchemaParser;
import com.aerospike.graph.synth.process.tasks.generator.Generate;
import com.aerospike.movement.process.core.Task;
import com.aerospike.movement.runtime.core.Runtime;
import com.aerospike.movement.runtime.core.local.LocalParallelStreamRuntime;
import com.aerospike.movement.runtime.core.local.RunningPhase;
import com.aerospike.movement.test.core.AbstractMovementTest;
import com.aerospike.movement.test.mock.MockUtil;
import com.aerospike.movement.test.mock.encoder.MockEncoder;
import com.aerospike.movement.util.core.runtime.IOUtil;
import com.aerospike.movement.util.core.runtime.RuntimeUtil;
import org.apache.commons.configuration2.Configuration;
import org.junit.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
            put(LocalParallelStreamRuntime.Config.Keys.THREADS, String.valueOf(1));
        }};
        final Configuration mockedConfig = getMockConfiguration(testConfig);
        MockUtil.setDefaultMockCallbacks();
        final Task generateTask = (Task) Generate.open(mockedConfig);
        final Configuration taskConfig = generateTask.getConfig(mockedConfig);


        List<Runtime.PHASE> phases = generateTask.getPhases();
        Iterator<RunningPhase> phaseIterator = LocalParallelStreamRuntime.open(taskConfig).runPhases(phases, taskConfig);
        while (phaseIterator.hasNext()) {
            final RunningPhase phase = phaseIterator.next();
            phase.get();
            phase.close();
        }
        assertEquals(15 * scaleFactor, getHitCounter(MockEncoder.class, MockEncoder.Methods.ENCODE));

    }

}
