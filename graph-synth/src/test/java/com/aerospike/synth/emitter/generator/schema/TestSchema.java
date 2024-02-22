package com.aerospike.synth.emitter.generator.schema;

import com.aerospike.graph.synth.emitter.generator.schema.SchemaBuilder;
import com.aerospike.movement.util.core.configuration.ConfigUtil;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.translator.GroovyTranslator;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

import java.nio.file.Path;

public abstract class TestSchema {
    public Graph addToGraph(Graph schemaGraph) {
        GraphTraversalSource sg = schemaGraph.traversal();
        traversal(sg).iterate();
        return schemaGraph;
    }

    public void writeToGraphSon(Path graphsonPath) {
        addToGraph(TinkerGraph.open()).traversal().io(graphsonPath.toAbsolutePath().toString()).write().iterate();
    }
    abstract void writeToYAML(final Path yamlPath);

    abstract GraphTraversal<?, ?> traversal(GraphTraversalSource g);

    static String convertToString(GraphTraversal<?, ?> graphTraversal) {
        return GroovyTranslator.of("g").
                translate(graphTraversal.asAdmin().getBytecode()).getScript();
    }

    @Override
    public String toString() {
        return convertToString(traversal(TinkerGraph.open().traversal()));
    }

    class SimplestTestSchema extends TestSchema {


        @Override
        public void writeToYAML(Path yamlPath) {

        }

        @Override
        public GraphTraversal<?, ?> traversal(GraphTraversalSource g) {
            return g
                    .addV("vertexType").as("A")
                    .property(T.id, "A")
                    .property("entrypoint", true)
                    .property(ConfigUtil.subKey("entrypoint", SchemaBuilder.Keys.CHANCES_TO_CREATE), 1)
                    .property(ConfigUtil.subKey("entrypoint", SchemaBuilder.Keys.LIKELIHOOD), 1.0)
                    .addV("vertexType").as("B")
                    .property(T.id, "B")
                    .addE("edgeType").from("A").to("B")
                    .property(T.id, "AtoB");
        }
    }
}
