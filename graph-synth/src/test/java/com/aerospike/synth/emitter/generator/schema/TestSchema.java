package com.aerospike.synth.emitter.generator.schema;

import com.aerospike.graph.synth.emitter.generator.ValueGenerator;
import com.aerospike.graph.synth.emitter.generator.schema.seralization.TinkerPopSchemaParser;
import com.aerospike.graph.synth.emitter.generator.schema.seralization.YAMLSchemaParser;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.translator.GroovyTranslator;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.aerospike.graph.synth.emitter.generator.schema.SchemaBuilder.Keys.*;
import static com.aerospike.movement.util.core.configuration.ConfigUtil.subKey;

public abstract class TestSchema {
    public Graph addToGraph(Graph schemaGraph) {
        GraphTraversalSource sg = schemaGraph.traversal();
        traversal(sg).iterate();
        return schemaGraph;
    }

    public void writeToGraphSon(Path graphsonPath) {
        addToGraph(TinkerGraph.open()).traversal().io(graphsonPath.toAbsolutePath().toString()).write().iterate();
    }

    public void writeToYAML(Path yamlPath) {
        try {
            Files.write(yamlPath, YAMLSchemaParser.dump(TinkerPopSchemaParser.fromGraph(addToGraph(TinkerGraph.open()))).getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    abstract Long edgesForScaleFactor(Long scaleFactor);

    abstract Long verticesForScaleFactor(Long scaleFactor);


    abstract GraphTraversal<?, ?> traversal(GraphTraversalSource g);

    static String convertToString(GraphTraversal<?, ?> graphTraversal) {
        return GroovyTranslator.of("g").
                translate(graphTraversal.asAdmin().getBytecode()).getScript();
    }

    @Override
    public String toString() {
        return convertToString(traversal(TinkerGraph.open().traversal()));
    }

    public static class SimplestTestSchema extends TestSchema {
        public static final TestSchema INSTANCE = new SimplestTestSchema();

        @Override
        Long edgesForScaleFactor(Long scaleFactor) {
            return 1 * scaleFactor;
        }

        @Override
        Long verticesForScaleFactor(Long scaleFactor) {
            return 2 * scaleFactor;
        }

        @Override
        public GraphTraversal<?, ?> traversal(GraphTraversalSource g) {
            return g
                    .addV(VERTEX_TYPE).as("A")
                    .property(T.id, "A")
                    .property("entrypoint", true)
                    .addV("vertexType").as("B")
                    .property(T.id, "B")
                    .addE("edgeType").from("A").to("B")
                    .property(T.id, "AtoB");
        }
    }

    public static class BenchmarkTestData extends TestSchema {
        public static final TestSchema INSTANCE = new BenchmarkTestData();

        public static class Keys {
            public static class PROPERTY {

            }

            public static class V_LABEL {
                public static final String PARTNER_IDENTITY = "PartnerIdentity";
                public static final String ACCOUNT = "Account";
                public static final String SUB_ACCOUNT = "SubAccount";
            }

            public static class E_LABEL {
                public static final String PROVIDED_ACCOUNT = "ProvidedAccount";
                public static final String HAS_SUB_ACCOUNT = "HasSubAccount";

            }
        }

        public static class MapUtil {
            public static <K, V> Map<K, V> of(K k, V v) {
                return new HashMap<>() {{
                    put(k, v);
                }};
            }
        }

        @Override
        Long edgesForScaleFactor(Long scaleFactor) {
            return 1 * scaleFactor;
        }

        @Override
        Long verticesForScaleFactor(Long scaleFactor) {
            return 2 * scaleFactor;
        }

        @Override
        GraphTraversal<?, ?> traversal(GraphTraversalSource g) {
            return g
                    .addV(VERTEX_TYPE).as(Keys.V_LABEL.PARTNER_IDENTITY).property(T.id, Keys.V_LABEL.PARTNER_IDENTITY)
                    .property(ENTRYPOINT, true)
                    .property(subKey(ENTRYPOINT, CHANCES_TO_CREATE), 1)
                    .property(subKey(ENTRYPOINT, LIKELIHOOD), 1.0)
                    .property("partner_id.value.generator.impl", ValueGenerator.RandomDigitSequence.class.getName())
                    .property("partner_id.value.generator.args", MapUtil.of("digits", 12))


                    .addV(VERTEX_TYPE).as(Keys.V_LABEL.ACCOUNT).property(T.id, Keys.V_LABEL.ACCOUNT)
                    .property("partner_id.value.generator.impl", ValueGenerator.RandomDigitSequence.class.getName())
                    .property("partner_id.value.generator.args", MapUtil.of("digits", 8))


                    .addE(EDGE_TYPE).from(Keys.V_LABEL.PARTNER_IDENTITY).to(Keys.V_LABEL.ACCOUNT);
        }
    }

}
