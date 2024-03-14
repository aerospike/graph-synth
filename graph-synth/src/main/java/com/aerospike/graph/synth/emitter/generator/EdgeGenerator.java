/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.graph.synth.emitter.generator;


import com.aerospike.graph.synth.emitter.generator.schema.definition.EdgeSchema;
import com.aerospike.graph.synth.emitter.generator.schema.definition.GraphSchema;
import com.aerospike.graph.synth.emitter.generator.schema.definition.VertexSchema;
import com.aerospike.movement.emitter.core.Emitable;
import com.aerospike.movement.structure.core.graph.EmittedEdge;
import com.aerospike.movement.output.core.Output;
import com.aerospike.movement.runtime.core.driver.OutputId;
import com.aerospike.movement.runtime.core.driver.OutputIdDriver;
import com.aerospike.movement.structure.core.EmittedId;
import com.aerospike.graph.synth.util.generator.SchemaUtil;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * @author Grant Haywood (<a href="http://iowntheinter.net">http://iowntheinter.net</a>)
 */

/*
 there is an eager and a passive way of creating an edge
 the eager way of creating an edge is traversal.
 the iterator way walks over the edge and emits it and walks to the next vertex across the edge

 the passive way takes 2 emitted vertex ids and just emits an edge for them
 */

public class EdgeGenerator {
    public final EdgeSchema edgeSchema;
    public final GraphSchema graphSchema;
    private GeneratedVertex nextVertex;
    private boolean emitted = false;

    public EdgeGenerator(final EdgeSchema edgeSchema, final GraphSchema graphSchema) {
        this.graphSchema = graphSchema;
        this.edgeSchema = edgeSchema;
    }

    public static EdgeGenerator create(final EdgeSchema edgeSchema, final GraphSchema graphSchema) {
        return new EdgeGenerator(edgeSchema, graphSchema);
    }

    //Step from the generated vertex out over the edge to the next vertex, emit it, then create the edge.
    public Stream<Emitable> walk(final Long outVid, final OutputIdDriver outputIdDriver) {
        return Stream.of(new Path(outVid, this, outputIdDriver));
    }

    public static class GeneratedEdge extends EdgeGenerator implements EmittedEdge {
        private final Long inV;
        private final Long outV;
        private final AtomicBoolean written = new AtomicBoolean(false);

        public GeneratedEdge(final EdgeGenerator edgeGenerator, final Long inV, final Long outV) {
            this(edgeGenerator.edgeSchema, edgeGenerator.graphSchema, inV, outV);
        }

        public GeneratedEdge(final EdgeSchema edgeSchema, final GraphSchema graphSchema, final Long inV, final Long outV) {
            super(edgeSchema, graphSchema);
            this.inV = inV;
            this.outV = outV;
        }

        @Override
        public EmittedId fromId() {
            return EmittedId.from(outV);
        }

        @Override
        public EmittedId toId() {
            return EmittedId.from(inV);
        }

        @Override
        public Stream<String> propertyNames() {
            return edgeSchema.properties.stream().map(p -> p.name);
        }

        @Override
        public Optional<Object> propertyValue(final String name) {
            return Optional.of(EdgeGenerator.getFieldFromEdge(this, name));
        }

        @Override
        public String label() {
            return edgeSchema.label();
        }

        @Override
        public Stream<Emitable> emit(final Output output) {
            if (written.compareAndSet(false, true)) {
                    output
                            .writer(EmittedEdge.class, edgeSchema.label())
                            .writeToOutput(Optional.of(this));
            }
            return Stream.empty();
        }

        @Override
        public Stream<Emitable> stream() {
            throw new IllegalStateException();
        }
    }

    private class Path implements Emitable {
        private final EdgeGenerator edgeGenerator;
        private final OutputIdDriver outputIdDriver;
        private final Long outVid;

        public Path(final Long outVid, final EdgeGenerator edgeGenerator, OutputIdDriver outputIdDriver) {
            this.outVid = outVid;
            this.edgeGenerator = edgeGenerator;
            this.outputIdDriver = outputIdDriver;
        }

        public Stream<Emitable> createNextVertex() {
            final VertexSchema vertexSchema = SchemaUtil.getSchemaFromVertexName(graphSchema, edgeSchema.inVertex);
            final Optional<OutputId> maybeNext = outputIdDriver.getNext();
            if (maybeNext.isEmpty())
                return Stream.empty();
            Long nextInVid = (Long) maybeNext.get().unwrap();
            final GeneratedEdge ge = new GeneratedEdge(edgeGenerator, nextInVid, outVid);
            final VertexContext context = new VertexContext(graphSchema,
                    vertexSchema,
                    outputIdDriver,
                    Optional.of(ge));
            nextVertex = new GeneratedVertex((Long) nextInVid, context);
            return Stream.of(nextVertex);
        }

        @Override
        public Stream<Emitable> emit(final Output output) {
            if (nextVertex == null) { // nextVertex has not been created/written yet
                //get the iterator, look and see if there is a next on the driver stream
                final Iterator<Emitable> nextVertexCrationStream = createNextVertex().iterator();

                if (!nextVertexCrationStream.hasNext())
                    return Stream.empty(); //end of the output tape, don't write anything more.

                //The opposing vertex is available
                final Emitable nextVertex = nextVertexCrationStream.next();

                //write the opposing vertex, then ourselves (the edge)
                return Stream.of(nextVertex, this);
            }
            //nextVertex is already created and written, we can emit the edge (this)
            final GeneratedEdge generatedEdge = new GeneratedEdge(edgeGenerator, (Long) nextVertex.id, outVid);
            // will return the empty stream.
            // no more processing to be done, the nextVertex carries the execution pointer.
            return generatedEdge.emit(output);
        }

        @Override
        public String type() {
            return edgeGenerator.edgeSchema.label();
        }
    }

    private static String getFieldFromEdge(final EmittedEdge edge, final String field) {
        if (field.equals("~label"))
            return edge.label();
        if (field.equals("~to"))
            return String.valueOf(edge.toId().unwrap());
        if (field.equals("~from"))
            return String.valueOf(edge.fromId().unwrap());
        else
            return ((GeneratedEdge) edge).edgeSchema.properties.stream()
                    .filter(p -> p.name.equals(field))
                    .map(p -> ValueGenerator.getGenerator(p.valueGenerator).generate(p.valueGenerator.args))
                    .findFirst().orElseThrow(() -> new NoSuchElementException("could not find generator")).toString();
    }
}


