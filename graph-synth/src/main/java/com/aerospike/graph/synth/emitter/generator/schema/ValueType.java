/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.graph.synth.emitter.generator.schema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public enum ValueType {
    STRING(String.class),
    INTEGER(Integer.class),
    LONG(Long.class),
    DOUBLE(Double.class),
    BOOLEAN(Boolean.class),
    LIST(ArrayList.class),
    MAP(HashMap.class),
    SET(HashSet.class);

    private final Class<? extends Serializable> value;

    ValueType(Class<? extends Serializable> value) {
        this.value = value;
    }
    public Class getValue() {
        return value;
    }
}
