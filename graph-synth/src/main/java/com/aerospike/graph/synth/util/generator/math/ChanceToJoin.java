/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.graph.synth.util.generator.math;

import com.aerospike.movement.util.core.math.ProbUtil;

import java.util.function.BiFunction;

public class ChanceToJoin<A, B> implements BiFunction<A, B, Boolean> {
    private final BiFunction<A, B, Double> getWeight;

    public ChanceToJoin(BiFunction<A, B, Double> getWeight) {
        this.getWeight = getWeight;
    }

    public static <A, B> ChanceToJoin weightedBy(BiFunction<A, B, Double> getWeight) {
        return new ChanceToJoin(getWeight);
    }

    public static ChanceToJoin fair() {
        return weightedBy((a, b) -> .5);
    }

    @Override
    public Boolean apply(A elementA, B elementB) {
        return ProbUtil.coinFlip(getWeight.apply(elementA, elementB));
    }
}
