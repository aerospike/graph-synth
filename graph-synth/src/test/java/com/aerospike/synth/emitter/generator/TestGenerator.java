/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.synth.emitter.generator;

import com.aerospike.graph.synth.emitter.generator.EdgeGenerator;
import com.aerospike.graph.synth.emitter.generator.GeneratedVertex;
import com.aerospike.graph.synth.emitter.generator.Generator;
import com.aerospike.movement.config.core.ConfigurationBase;
import com.aerospike.graph.synth.emitter.generator.schema.seralization.YAMLSchemaParser;
import com.aerospike.movement.logging.core.Logger;
import com.aerospike.movement.runtime.core.Runtime;
import com.aerospike.movement.runtime.core.driver.impl.GeneratedOutputIdDriver;
import com.aerospike.movement.runtime.core.local.RunningPhase;
import com.aerospike.graph.synth.test.generator.SchemaTestConstants;
import com.aerospike.movement.test.mock.MockCallback;
import com.aerospike.movement.test.mock.driver.MockOutputIdDriver;
import com.aerospike.movement.util.core.runtime.RuntimeUtil;
import com.aerospike.movement.util.core.iterator.OneShotIteratorSupplier;
import com.aerospike.movement.runtime.core.driver.impl.SuppliedWorkChunkDriver;
import com.aerospike.movement.runtime.core.local.LocalParallelStreamRuntime;
import com.aerospike.movement.test.core.AbstractMovementTest;
import com.aerospike.movement.test.mock.MockUtil;
import com.aerospike.movement.test.mock.encoder.MockEncoder;
import com.aerospike.movement.test.mock.output.MockOutput;
import com.aerospike.movement.util.core.configuration.ConfigurationUtil;
import com.aerospike.movement.util.core.runtime.IOUtil;
import com.aerospike.movement.util.core.iterator.PrimitiveIteratorWrap;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.MapConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.LongStream;

import static com.aerospike.movement.runtime.core.local.LocalParallelStreamRuntime.Config.Keys.THREADS;
import static com.aerospike.movement.test.mock.MockUtil.*;
import static junit.framework.TestCase.*;

public class TestGenerator extends AbstractMovementTest {
    final int THREAD_COUNT = 4;
    final int TEST_SIZE = 20;
    final Logger logger = RuntimeUtil.getLogger(this);

    final Configuration emitterConfig = Generator.getEmitterConfig();


    @Before
    public void setup() {
        super.setup();
    }

    @After
    public void cleanup() {
        super.cleanup();
    }


    @Test
    public void doesNotGenerateDuplicateIds() {
        final long DUPLICATE_TEST_SIZE = 100_000;


        final Map<String, String> configMap = new HashMap<>() {{
            put(THREADS, String.valueOf(THREAD_COUNT));
            put(Generator.Config.Keys.SCALE_FACTOR, String.valueOf(TEST_SIZE));
            put(YAMLSchemaParser.Config.Keys.YAML_FILE_PATH, IOUtil.copyFromResourcesIntoNewTempFile("example_schema.yaml").getAbsolutePath());
        }};
        final Configuration mockConfig = getMockConfiguration(configMap);
        final Configuration defaultConfig = ConfigurationUtil.configurationWithOverrides(mockConfig, emitterConfig);


        final Configuration config = ConfigurationUtil.configurationWithOverrides(defaultConfig, new MapConfiguration(new HashMap<>() {{
            put(ConfigurationBase.Keys.OUTPUT_ID_DRIVER, GeneratedOutputIdDriver.class.getName());
            put(ConfigurationBase.Keys.WORK_CHUNK_DRIVER_PHASE_ONE, SuppliedWorkChunkDriver.class.getName());
            put(ConfigurationBase.Keys.WORK_CHUNK_DRIVER_PHASE_TWO, SuppliedWorkChunkDriver.class.getName());
            put(GeneratedOutputIdDriver.Config.Keys.RANGE_BOTTOM, String.valueOf(DUPLICATE_TEST_SIZE + 1));
            put(GeneratedOutputIdDriver.Config.Keys.RANGE_TOP, String.valueOf(Long.MAX_VALUE));
        }}));
        registerCleanupCallback(() -> {
            LocalParallelStreamRuntime.getInstance(config).close();
        });

        final Set<Object> emittedIds = Collections.synchronizedSet(new HashSet<>());
        final Runtime runtime = LocalParallelStreamRuntime.getInstance(config);

        SuppliedWorkChunkDriver.setIteratorSupplierForPhase(Runtime.PHASE.ONE, OneShotIteratorSupplier.of(() -> PrimitiveIteratorWrap.wrap(LongStream.range(0, DUPLICATE_TEST_SIZE).iterator())));
        SuppliedWorkChunkDriver.setIteratorSupplierForPhase(Runtime.PHASE.TWO, OneShotIteratorSupplier.of(() -> PrimitiveIteratorWrap.wrap(LongStream.range(0, DUPLICATE_TEST_SIZE).iterator())));


        MockUtil.setDefaultMockCallbacks();
        final AtomicBoolean duplicateIdDetected = new AtomicBoolean(false);
        MockUtil.setDuplicateIdOutputVerification(emittedIds, duplicateIdDetected);
        assertFalse(duplicateIdDetected.get());

        final long msTaken = iteratePhasesTimed(runtime, config);
        assertEquals(DUPLICATE_TEST_SIZE * 15, getHitCounter(MockEncoder.class, MockEncoder.Methods.ENCODE));
        assertEquals(DUPLICATE_TEST_SIZE * 15, getHitCounter(MockOutput.class, MockOutput.Methods.WRITE_TO_OUTPUT));
    }

    public void runGeneratorHighLevel() {
        final long TEST_SIZE = 100_000;
        final Map<String, String> configMap = new HashMap<>() {{
            put(THREADS, String.valueOf(THREAD_COUNT));
            put(Generator.Config.Keys.SCALE_FACTOR, String.valueOf(TEST_SIZE));
            put(YAMLSchemaParser.Config.Keys.YAML_FILE_PATH, IOUtil.copyFromResourcesIntoNewTempFile("example_schema.yaml").getAbsolutePath());
        }};
        final Configuration mockConfig = getMockConfiguration(configMap);
        final Configuration defaultConfig = ConfigurationUtil.configurationWithOverrides(mockConfig, emitterConfig);

        final Configuration config = ConfigurationUtil.configurationWithOverrides(defaultConfig, new MapConfiguration(new HashMap<>() {{
            put(ConfigurationBase.Keys.OUTPUT_ID_DRIVER, GeneratedOutputIdDriver.class.getName());
            put(ConfigurationBase.Keys.WORK_CHUNK_DRIVER_PHASE_ONE, SuppliedWorkChunkDriver.class.getName());
            put(ConfigurationBase.Keys.WORK_CHUNK_DRIVER_PHASE_TWO, SuppliedWorkChunkDriver.class.getName());

            put(GeneratedOutputIdDriver.Config.Keys.RANGE_BOTTOM, String.valueOf(TEST_SIZE + 1));
            put(GeneratedOutputIdDriver.Config.Keys.RANGE_TOP, String.valueOf(Long.MAX_VALUE));
            put(LocalParallelStreamRuntime.Config.Keys.THREADS, 8);
        }}));
        registerCleanupCallback(() -> {
            LocalParallelStreamRuntime.getInstance(config).close();
        });

        System.out.println(ConfigurationUtil.configurationToPropertiesFormat(config));

        final Set<Object> emittedIds = Collections.synchronizedSet(new HashSet<>());

        SuppliedWorkChunkDriver.setIteratorSupplierForPhase(Runtime.PHASE.ONE, OneShotIteratorSupplier.of(() -> PrimitiveIteratorWrap.wrap(LongStream.range(0, TEST_SIZE).iterator())));
        SuppliedWorkChunkDriver.setIteratorSupplierForPhase(Runtime.PHASE.TWO, OneShotIteratorSupplier.of(() -> PrimitiveIteratorWrap.wrap(LongStream.range(0, TEST_SIZE).iterator())));


        MockUtil.setDefaultMockCallbacks();
        final AtomicBoolean duplicateIdDetected = new AtomicBoolean(false);
        MockUtil.setDuplicateIdOutputVerification(emittedIds, duplicateIdDetected);
        assertFalse(duplicateIdDetected.get());
        MockUtil.setCallback(MockEncoder.class, MockEncoder.Methods.ENCODE,
                MockCallback.create((object, args) -> {
                    MockUtil.incrementHitCounter(MockEncoder.class, MockEncoder.Methods.ENCODE);
                    if (args.length == 1 && args[0] instanceof GeneratedVertex) {
                        MockUtil.incrementHitCounter(GeneratedVertex.class, MockEncoder.Methods.ENCODE);
                        MockUtil.incrementMetadataCounter(((GeneratedVertex) args[0]).label(), GeneratedVertex.class.getSimpleName());
                    }
                    if (args.length == 1 && args[0] instanceof EdgeGenerator.GeneratedEdge) {
                        MockUtil.incrementHitCounter(EdgeGenerator.GeneratedEdge.class, MockEncoder.Methods.ENCODE);
                        MockUtil.incrementMetadataCounter(((EdgeGenerator.GeneratedEdge) args[0]).label(), EdgeGenerator.GeneratedEdge.class.getSimpleName());
                    }
                    return Optional.of(args[0]);
                }));


    }

    @Test
    public void testGenerateElementCounts() {
        final long TEST_SIZE = 100_000;

        final Map<String, String> configMap = new HashMap<>() {{
            put(THREADS, String.valueOf(THREAD_COUNT));
            put(Generator.Config.Keys.SCALE_FACTOR, String.valueOf(TEST_SIZE));
            put(YAMLSchemaParser.Config.Keys.YAML_FILE_PATH, IOUtil.copyFromResourcesIntoNewTempFile("example_schema.yaml").getAbsolutePath());
        }};
        final Configuration mockConfig = getMockConfiguration(configMap);
        final Configuration defaultConfig = ConfigurationUtil.configurationWithOverrides(mockConfig, emitterConfig);

        final Configuration config = ConfigurationUtil.configurationWithOverrides(defaultConfig, new MapConfiguration(new HashMap<>() {{
            put(ConfigurationBase.Keys.OUTPUT_ID_DRIVER, GeneratedOutputIdDriver.class.getName());
            put(ConfigurationBase.Keys.WORK_CHUNK_DRIVER_PHASE_ONE, SuppliedWorkChunkDriver.class.getName());
            put(ConfigurationBase.Keys.WORK_CHUNK_DRIVER_PHASE_TWO, SuppliedWorkChunkDriver.class.getName());

            put(GeneratedOutputIdDriver.Config.Keys.RANGE_BOTTOM, String.valueOf(TEST_SIZE + 1));
            put(GeneratedOutputIdDriver.Config.Keys.RANGE_TOP, String.valueOf(Long.MAX_VALUE));
            put(LocalParallelStreamRuntime.Config.Keys.THREADS, 8);
        }}));
        registerCleanupCallback(() -> {
            LocalParallelStreamRuntime.getInstance(config).close();
        });

        System.out.println(ConfigurationUtil.configurationToPropertiesFormat(config));

        final Set<Object> emittedIds = Collections.synchronizedSet(new HashSet<>());
        final Runtime runtime = LocalParallelStreamRuntime.getInstance(config);

        SuppliedWorkChunkDriver.setIteratorSupplierForPhase(Runtime.PHASE.ONE, OneShotIteratorSupplier.of(() -> PrimitiveIteratorWrap.wrap(LongStream.range(0, TEST_SIZE).iterator())));
        SuppliedWorkChunkDriver.setIteratorSupplierForPhase(Runtime.PHASE.TWO, OneShotIteratorSupplier.of(() -> PrimitiveIteratorWrap.wrap(LongStream.range(0, TEST_SIZE).iterator())));

        MockUtil.setDefaultMockCallbacks();
        final AtomicBoolean duplicateIdDetected = new AtomicBoolean(false);
        MockUtil.setDuplicateIdOutputVerification(emittedIds, duplicateIdDetected);
        assertFalse(duplicateIdDetected.get());
        MockUtil.setCallback(MockEncoder.class, MockEncoder.Methods.ENCODE,
                MockCallback.create((object, args) -> {
                    MockUtil.incrementHitCounter(MockEncoder.class, MockEncoder.Methods.ENCODE);
                    if (args.length == 1 && args[0] instanceof GeneratedVertex) {
                        MockUtil.incrementHitCounter(GeneratedVertex.class, MockEncoder.Methods.ENCODE);
                        MockUtil.incrementMetadataCounter(((GeneratedVertex) args[0]).label(), GeneratedVertex.class.getSimpleName());
                    }
                    if (args.length == 1 && args[0] instanceof EdgeGenerator.GeneratedEdge) {
                        MockUtil.incrementHitCounter(EdgeGenerator.GeneratedEdge.class, MockEncoder.Methods.ENCODE);
                        MockUtil.incrementMetadataCounter(((EdgeGenerator.GeneratedEdge) args[0]).label(), EdgeGenerator.GeneratedEdge.class.getSimpleName());
                    }
                    return Optional.of(args[0]);
                }));

        final Iterator<RunningPhase> x = runtime.runPhases(
                List.of(Runtime.PHASE.ONE, Runtime.PHASE.TWO),
                config);

        iteratePhasesAndCloseRuntime(x,runtime);

        assertEquals(TEST_SIZE * 15, getHitCounter(MockEncoder.class, MockEncoder.Methods.ENCODE));
        assertEquals(TEST_SIZE * 15, getHitCounter(MockOutput.class, MockOutput.Methods.WRITE_TO_OUTPUT));
        logger.info(String.format("%d verticies encoded", getHitCounter(GeneratedVertex.class, MockEncoder.Methods.ENCODE)));
        logger.info(String.format("%d edges encoded", getHitCounter(EdgeGenerator.GeneratedEdge.class, MockEncoder.Methods.ENCODE)));
        getMetadataTypes().forEach(metadataTypeName -> {
            MockUtil.getMetadataSubtypes(metadataTypeName).forEach(subtype -> {
                logger.info(String.format("%d %s of type %s encoded", getMetadataHitCounter(metadataTypeName, subtype), metadataTypeName, subtype));
            });
        });

        final String GenVertexName = GeneratedVertex.class.getSimpleName();
        final String GenEdgeName = EdgeGenerator.GeneratedEdge.class.getSimpleName();

        SchemaTestConstants.verifyCount(SchemaTestConstants.SchemaKeys.GOLDEN_ENTITY,
                getMetadataHitCounter(GenVertexName, SchemaTestConstants.SchemaKeys.GOLDEN_ENTITY), TEST_SIZE);
        SchemaTestConstants.verifyCount(SchemaTestConstants.SchemaKeys.COOKIE,
                getMetadataHitCounter(GenVertexName, SchemaTestConstants.SchemaKeys.COOKIE), TEST_SIZE);
        SchemaTestConstants.verifyCount(SchemaTestConstants.SchemaKeys.CONTACT_MEDIUM,
                getMetadataHitCounter(GenVertexName, SchemaTestConstants.SchemaKeys.CONTACT_MEDIUM), TEST_SIZE);
        SchemaTestConstants.verifyCount(SchemaTestConstants.SchemaKeys.INDIVIDUAL,
                getMetadataHitCounter(GenVertexName, SchemaTestConstants.SchemaKeys.INDIVIDUAL), TEST_SIZE);
        SchemaTestConstants.verifyCount(SchemaTestConstants.SchemaKeys.HOUSEHOLD,
                getMetadataHitCounter(GenVertexName, SchemaTestConstants.SchemaKeys.HOUSEHOLD), TEST_SIZE);
        SchemaTestConstants.verifyCount(SchemaTestConstants.SchemaKeys.IP_ADDRESS,
                getMetadataHitCounter(GenVertexName, SchemaTestConstants.SchemaKeys.IP_ADDRESS), TEST_SIZE);
        SchemaTestConstants.verifyCount(SchemaTestConstants.SchemaKeys.DIGITAL_ENTITY,
                getMetadataHitCounter(GenVertexName, SchemaTestConstants.SchemaKeys.DIGITAL_ENTITY), TEST_SIZE);
        SchemaTestConstants.verifyCount(SchemaTestConstants.SchemaKeys.RESOLVES_TO_INDIVIDUAL,
                getMetadataHitCounter(GenEdgeName, SchemaTestConstants.SchemaKeys.RESOLVES_TO_INDIVIDUAL), TEST_SIZE);
        SchemaTestConstants.verifyCount(SchemaTestConstants.SchemaKeys.ASSOCIATED_WITH_COOKIE,
                getMetadataHitCounter(GenEdgeName, SchemaTestConstants.SchemaKeys.ASSOCIATED_WITH_COOKIE), TEST_SIZE);
        SchemaTestConstants.verifyCount(SchemaTestConstants.SchemaKeys.OBSERVED_DIGITAL_ENTITY,
                getMetadataHitCounter(GenEdgeName, SchemaTestConstants.SchemaKeys.OBSERVED_DIGITAL_ENTITY), TEST_SIZE);
        SchemaTestConstants.verifyCount(SchemaTestConstants.SchemaKeys.CONNECTED_FROM_IP,
                getMetadataHitCounter(GenEdgeName, SchemaTestConstants.SchemaKeys.CONNECTED_FROM_IP), TEST_SIZE);
        SchemaTestConstants.verifyCount(SchemaTestConstants.SchemaKeys.HAS_CONTACT_MEDIUM,
                getMetadataHitCounter(GenEdgeName, SchemaTestConstants.SchemaKeys.HAS_CONTACT_MEDIUM), TEST_SIZE);
        SchemaTestConstants.verifyCount(SchemaTestConstants.SchemaKeys.ASSIGNED_SSN,
                getMetadataHitCounter(GenEdgeName, SchemaTestConstants.SchemaKeys.ASSIGNED_SSN), TEST_SIZE);
        SchemaTestConstants.verifyCount(SchemaTestConstants.SchemaKeys.LIVES_AT,
                getMetadataHitCounter(GenEdgeName, SchemaTestConstants.SchemaKeys.LIVES_AT), TEST_SIZE);
    }

    @Test
    public void testWillErrorOnDuplicateId() {

        final Map<String, String> configMap = new HashMap<>() {{
            put(THREADS, String.valueOf(THREAD_COUNT));
            put(Generator.Config.Keys.SCALE_FACTOR, String.valueOf(TEST_SIZE));
            put(YAMLSchemaParser.Config.Keys.YAML_FILE_PATH, IOUtil.copyFromResourcesIntoNewTempFile("example_schema.yaml").getAbsolutePath());
        }};
        final Configuration mockConfig = getMockConfiguration(configMap);
        final Configuration defaultConfig = ConfigurationUtil.configurationWithOverrides(mockConfig, emitterConfig);

        final Configuration config = ConfigurationUtil.configurationWithOverrides(defaultConfig, new MapConfiguration(new HashMap<>() {{
            put(ConfigurationBase.Keys.OUTPUT_ID_DRIVER, MockOutputIdDriver.class.getName());
            put(ConfigurationBase.Keys.WORK_CHUNK_DRIVER_PHASE_ONE, SuppliedWorkChunkDriver.class.getName());
            put(ConfigurationBase.Keys.WORK_CHUNK_DRIVER_PHASE_TWO, SuppliedWorkChunkDriver.class.getName());

            put(GeneratedOutputIdDriver.Config.Keys.RANGE_BOTTOM, String.valueOf(TEST_SIZE + 1));
            put(GeneratedOutputIdDriver.Config.Keys.RANGE_TOP, String.valueOf(Long.MAX_VALUE));
        }}));
        registerCleanupCallback(() -> {
            LocalParallelStreamRuntime.getInstance(config).close();
        });
        SuppliedWorkChunkDriver.setIteratorSupplierForPhase(Runtime.PHASE.ONE, OneShotIteratorSupplier.of(() -> PrimitiveIteratorWrap.wrap(LongStream.of(1L, 2L, 1L).iterator())));
        SuppliedWorkChunkDriver.setIteratorSupplierForPhase(Runtime.PHASE.TWO, OneShotIteratorSupplier.of(() -> PrimitiveIteratorWrap.wrap(LongStream.range(0, TEST_SIZE).iterator())));

        final Set<Object> emittedIds = Collections.synchronizedSet(new HashSet<>());
        final Runtime runtime = LocalParallelStreamRuntime.getInstance(config);
        final AtomicBoolean passed = new AtomicBoolean(false);

        MockUtil.setDefaultMockCallbacks();
        MockUtil.setCallback(MockOutputIdDriver.class, MockOutputIdDriver.Methods.GET_NEXT, MockCallback.create((object, args) -> {
            incrementHitCounter(MockOutputIdDriver.class, MockOutputIdDriver.Methods.GET_NEXT);
            return Optional.of(1L); //return the same value over and over
        }));
        MockUtil.setDuplicateIdOutputVerification(emittedIds, passed);
        iteratePhasesTimed(runtime, config);

        assertTrue(passed.get());
    }



}
