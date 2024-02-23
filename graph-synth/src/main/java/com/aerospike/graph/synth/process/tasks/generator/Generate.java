/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.graph.synth.process.tasks.generator;


import com.aerospike.movement.config.core.ConfigurationBase;
import com.aerospike.graph.synth.emitter.generator.Generator;
import com.aerospike.movement.process.core.Task;
import com.aerospike.movement.runtime.core.Runtime;
import com.aerospike.movement.runtime.core.driver.impl.RangedOutputIdDriver;
import com.aerospike.movement.runtime.core.driver.impl.RangedWorkChunkDriver;
import com.aerospike.movement.util.core.configuration.ConfigUtil;
import com.aerospike.movement.util.core.error.ErrorUtil;
import com.aerospike.movement.util.core.runtime.RuntimeUtil;
import com.aerospike.movement.util.core.iterator.ConfiguredRangeSupplier;
import com.aerospike.movement.util.core.iterator.OneShotIteratorSupplier;
import com.aerospike.movement.util.core.iterator.PrimitiveIteratorWrap;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.MapConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

import static com.aerospike.movement.runtime.core.local.LocalParallelStreamRuntime.Config.Keys.BATCH_SIZE;
import static com.aerospike.movement.util.core.runtime.RuntimeUtil.getAvailableProcessors;

/**
 * @author Grant Haywood (<a href="http://iowntheinter.net">http://iowntheinter.net</a>)
 */
public class Generate extends Task {
    static {
        RuntimeUtil.registerTaskAlias(Generate.class.getSimpleName(), Generate.class);
    }

    @Override
    public void init(final Configuration config) {

    }

    @Override
    public void close() throws Exception {

    }

    public static class Config extends ConfigurationBase {
        public static final Config INSTANCE = new Config();

        private Config() {
            super();
        }

        @Override
        public Map<String, String> defaultConfigMap(final Map<String, Object> config) {
            final long workPerProcessor = Long.parseLong(Generator.Config.INSTANCE.getOrDefault(Generator.Config.Keys.SCALE_FACTOR, config)) / getAvailableProcessors();
            final long scaleFactor = Long.parseLong(Generator.Config.INSTANCE.getOrDefault(Generator.Config.Keys.SCALE_FACTOR, config));
            return new HashMap<>() {{
                put(ConfigurationBase.Keys.EMITTER, Generator.class.getName());
                //alias driver range to generator scale factor
                put(RangedWorkChunkDriver.Config.Keys.RANGE_TOP, Generator.Config.INSTANCE.getOrDefault(Generator.Config.Keys.SCALE_FACTOR, config));
                put(RangedWorkChunkDriver.Config.Keys.RANGE_BOTTOM, String.valueOf(0L));
                put(BATCH_SIZE, String.valueOf(Math.min(workPerProcessor, Math.min(scaleFactor / getAvailableProcessors(), 100_000))));

                put(ConfigurationBase.Keys.WORK_CHUNK_DRIVER_PHASE_ONE, RangedWorkChunkDriver.class.getName());
                put(ConfigurationBase.Keys.OUTPUT_ID_DRIVER, RangedOutputIdDriver.class.getName());
                put(RangedOutputIdDriver.Config.Keys.RANGE_BOTTOM, String.valueOf(scaleFactor + 1));
                put(RangedOutputIdDriver.Config.Keys.RANGE_TOP, String.valueOf(Long.MAX_VALUE));
            }};
        }

        @Override
        public List<String> getKeys() {
            return ConfigUtil.getKeysFromClass(Keys.class);
        }

        public static class Keys {
            public static final String EMITTER = "emitter";
            public static final String OUTPUT_ID_DRIVER = "output.idDriver";
            public static final String WORK_CHUNK_DRIVER = "emitter.workChunkDriver";
            public static final String SCALE_FACTOR = Generator.Config.Keys.SCALE_FACTOR;
        }

    }

    private Generate(final Configuration config) {
        super(Config.INSTANCE, config);
    }


    public static Generate open(final Configuration config) {
        return new Generate(getConfigStatic(config));
    }


//    @Override
//    public Configuration setupConfig(final Configuration inputConfig) {
//        return null;
//    }

    @Override
    public Configuration getConfig(final Configuration config) {
        return ConfigUtil.withOverrides(new MapConfiguration(Config.INSTANCE.defaultConfigMap(new HashMap<>())), config);
    }
    public static Configuration getConfigStatic(final Configuration config) {
        return ConfigUtil.withOverrides(new MapConfiguration(Config.INSTANCE.defaultConfigMap(new HashMap<>())), config);
    }

    @Override
    public Map<String, Object> getMetrics() {
        throw ErrorUtil.unimplemented();
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public boolean succeeded() {
        return false;
    }

    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public List<Runtime.PHASE> getPhases() {
        return List.of(Runtime.PHASE.ONE);
    }

//    public static Generate
}
