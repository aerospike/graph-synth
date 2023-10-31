/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.graph.synth.emitter.generator.schema.definition;

import java.util.Map;

/**
 * @author Grant Haywood (<a href="http://iowntheinter.net">http://iowntheinter.net</a>)
 */
public class GeneratorConfig {
    public String impl;
    public Map<String, Object> args;

    @Override
    public boolean equals(Object o) {
        if (!GeneratorConfig.class.isAssignableFrom(o.getClass()))
            return false;
        GeneratorConfig other = (GeneratorConfig) o;
        if (!impl.equals(other.impl))
            return false;
        for (Map.Entry<String, Object> e : args.entrySet()) {
            if (!other.args.containsKey(e.getKey()))
                return false;
            if (!other.args.get(e.getKey()).equals(e.getValue()))
                return false;
        }
        return true;
    }
}
