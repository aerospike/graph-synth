/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.graph.synth.emitter.generator;

import com.aerospike.movement.emitter.core.Emitable;
import com.aerospike.movement.util.core.runtime.RuntimeUtil;
import com.aerospike.movement.util.core.iterator.ext.IteratorUtils;
import com.aerospike.movement.util.core.math.ProbUtil;

import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Grant Haywood (<a href="http://iowntheinter.net">http://iowntheinter.net</a>)
 */
public class ProbabalisticEmittableSequence implements Iterator<Stream<Emitable>> {
    private final double likelihood;
    private final int chancesToCreate;
    private final Function<Void, Stream<Emitable>> generatorFn;
    private int previousCoinFlips;

    private ProbabalisticEmittableSequence(final Function<Void, Stream<Emitable>> generatorFn, final double likelihood, final int chancesToCreate) {
        this.likelihood = likelihood;
        this.chancesToCreate = chancesToCreate;
        this.previousCoinFlips = 0;
        this.generatorFn = generatorFn;
    }

    public static ProbabalisticEmittableSequence create(final Function<Void, Stream<Emitable>> generatorFn, final double likelihood, final int chancesToCreate) {
        return new ProbabalisticEmittableSequence(generatorFn, likelihood, chancesToCreate);
    }

    @Override
    public boolean hasNext() {
        return previousCoinFlips < chancesToCreate;
    }

    @Override
    public Stream<Emitable> next() {
        previousCoinFlips++;
        if (ProbUtil.coinFlip(likelihood)) {
            try {
                return generatorFn.apply(null);
            } catch (Exception e) {
                throw RuntimeUtil.getErrorHandler(this).handleFatalError(e, this);
            }
        }
        return Stream.empty();
    }

    public Stream<Emitable> stream() {
        final Stream<Stream<Emitable>> x = IteratorUtils.stream(this);
        return x.flatMap(it -> it);
    }
}
