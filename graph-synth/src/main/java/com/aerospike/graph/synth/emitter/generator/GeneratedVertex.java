/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.graph.synth.emitter.generator;

import com.aerospike.graph.synth.emitter.generator.schema.definition.EdgeSchema;
import com.aerospike.graph.synth.emitter.generator.schema.definition.OutEdgeSpec;
import com.aerospike.movement.emitter.core.Emitable;
import com.aerospike.movement.structure.core.graph.EmittedVertex;
import com.aerospike.movement.output.core.Output;
import com.aerospike.movement.structure.core.EmittedId;

import com.aerospike.movement.util.core.iterator.ext.IteratorUtils;
import com.aerospike.graph.synth.util.generator.SchemaUtil;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * you create a root vertex, then iterate over it
 * the pattern is you emit it, then call next() on it
 * from the root vertex that will yield an iterator of edges
 * you call emit on each of those, then next.
 * They will return iterators of vertices, and the pattern repeats until
 * the composite iterator chain returns no elements
 * <p>
 * when you generate a new dataset, you specify the number of root verticies
 * ids 0 -> rootVertexCount are root verticies
 * when stitching later you can randomly pick from this range
 */

public class GeneratedVertex implements Emitable, EmittedVertex {
    public final VertexContext context;
    public final Long id;

    private AtomicBoolean emitted = new AtomicBoolean(false);

    public GeneratedVertex(final Long id,
                           final VertexContext vertexContext) {
        this.id = id;
        this.context = vertexContext;
    }

    public EmittedId id() {
        return EmittedId.from(this.id);
    }

    /*
    it would be nice to be able to remember the generated out edges on the vertex
    we need this information later when stitching
     */


    public Stream<Emitable> emit(Output output) {
        if (!emitted.getAndSet(true))
            output.writer(GeneratedVertex.class, this.context.vertexSchema.label()).writeToOutput(Optional.of(this));
        return stream();
    }

    @Override
    public Stream<String> propertyNames() {
        return context.vertexSchema.properties.stream().map(p -> p.name);
    }

    @Override
    public Optional<Object> propertyValue(final String name) {
        return Optional.of(getFieldFromVertex(this, name));
    }

    @Override
    public String label() {
        return getFieldFromVertex(this, "~label").toString();
    }

    public Stream<Emitable> stream() {
        return paths();
    }


    public Stream<Emitable> paths() {
        List<OutEdgeSpec> outEdges = context.vertexSchema.outEdges;
        try {
            return IteratorUtils.stream(new Paths(this, outEdges.stream()
                    .flatMap(outEdgeSpec -> {
                        if(outEdgeSpec.toEdgeSchema(context.graphSchema).inVertex.equals(this.label())){
                            throw new RuntimeException("Defining an OUT edge that refers to the same Vertex type as IN is not currently supported.");
                        }
                        final EdgeSchema edgeSchema;
                        final Double likelihood;
                        final Integer chancesToCreate;
                        try {
                            edgeSchema = SchemaUtil.getSchemaFromEdgeName(context.graphSchema, outEdgeSpec.name);
                            likelihood = outEdgeSpec.likelihood;
                            chancesToCreate = outEdgeSpec.chancesToCreate;
                        } catch (NullPointerException e) {
                            throw new RuntimeException(e);
                        }
                        final Function<Void,Stream<Emitable>> edgeGeneratorFunction = (v) -> EdgeGenerator
                                .create(edgeSchema, this.context.graphSchema)
                                .walk(this.id, context.outputIdDriver);

                        return ProbabalisticEmittableSequence
                                .create(edgeGeneratorFunction,
                                        likelihood,
                                        chancesToCreate)
                                .stream();
                    })));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private class Paths implements Iterator<Emitable> {
        private final GeneratedVertex vertex;
        private final Iterator<Emitable> iterator;

        public Paths(GeneratedVertex generatedVertex, Stream<Emitable> stream) {
            this.vertex = generatedVertex;
            this.iterator = stream.iterator();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Emitable next() {
            return iterator.next();
        }
    }

    private static Object getFieldFromVertex(final GeneratedVertex vertex, final String field) {
        if (field.equals("~id"))
            return String.valueOf(vertex.id);
        if (field.equals("~label"))
            return vertex.context.vertexSchema.label();
        else
            return vertex.context.vertexSchema.properties.stream()
                    .filter(p ->
                            p.name.equals(field))
                    .map(p ->
                            ValueGenerator.getGenerator(p.valueGenerator).generate(p.valueGenerator.args))
                    .findFirst().orElseThrow(() ->
                            new NoSuchElementException("could not find generator")).toString();
    }
}
