package com.aerospike.synth.emitter.generator.schema;

import com.aerospike.graph.synth.emitter.generator.ValueGenerator;
import com.aerospike.graph.synth.emitter.generator.schema.definition.GraphSchema;
import com.aerospike.graph.synth.emitter.generator.schema.seralization.TinkerPopSchemaTraversalParser;
import com.aerospike.graph.synth.emitter.generator.schema.seralization.YAMLSchemaParser;
import com.aerospike.graph.synth.util.tinkerpop.SchemaGraphUtil;
import com.aerospike.movement.util.core.runtime.IOUtil;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.translator.GroovyTranslator;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.aerospike.graph.synth.emitter.generator.schema.SchemaBuilder.Keys.*;
import static com.aerospike.movement.util.core.configuration.ConfigUtil.subKey;

public abstract class ExampleSchemas {
    public Graph addToGraph(Graph schemaGraph) {
        GraphTraversalSource sg = schemaGraph.traversal();
        writeToTraversalSource(sg);
        return schemaGraph;
    }

    public abstract GraphTraversalSource writeToTraversalSource(GraphTraversalSource sg);

    public void writeToGraphSon(Path graphsonPath) {
        addToGraph(TinkerGraph.open()).traversal().io(graphsonPath.toAbsolutePath().toString()).write().iterate();
    }

    public GraphSchema schema() {
        Graph graph = TinkerGraph.open();
        addToGraph(graph);
        return TinkerPopSchemaTraversalParser.fromTraversal(graph.traversal());
    }

    public void writeToYAML(Path yamlPath) {
        try {
            Files.write(yamlPath, YAMLSchemaParser.dumpSchema(TinkerPopSchemaTraversalParser.fromGraph(addToGraph(TinkerGraph.open()))).getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract Long edgesForScaleFactor(Long scaleFactor);

    public abstract Long verticesForScaleFactor(Long scaleFactor);


    static String convertToString(GraphTraversal<?, ?> graphTraversal) {
        return GroovyTranslator.of("g").
                translate(graphTraversal.asAdmin().getBytecode()).getScript();
    }

    public static final List<ExampleSchemas> samples = new ArrayList<>();

    static {
        samples.add(Synthetic.INSTANCE);
        samples.add(Simplest.INSTANCE);
        samples.add(Benchmark2024.INSTANCE);
        samples.add(GDemoSchema.INSTANCE);
        samples.add(PersonCatMouse.INSTANCE);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    public static class Synthetic extends ExampleSchemas {
        public static final ExampleSchemas INSTANCE = new Synthetic();
        private final GraphSchema schema;

        public Synthetic() {
            this.schema = YAMLSchemaParser.from(IOUtil.copyFromResourcesIntoNewTempFile("example_schema.yaml").toPath()).parse();
        }

        @Override
        public GraphTraversalSource writeToTraversalSource(GraphTraversalSource sg) {
            SchemaGraphUtil.writeToTraversalSource(sg, schema);
            return sg;
        }

        @Override
        public Long edgesForScaleFactor(Long scaleFactor) {
            return 7L;
        }

        @Override
        public Long verticesForScaleFactor(Long scaleFactor) {
            return 8L;
        }

    }

    public static class Simplest extends ExampleSchemas {
        public static final ExampleSchemas INSTANCE = new Simplest();


        @Override
        public GraphTraversalSource writeToTraversalSource(GraphTraversalSource sg) {
            traversal(sg).iterate();
            return sg;
        }

        @Override
        public Long edgesForScaleFactor(Long scaleFactor) {
            return scaleFactor;
        }

        @Override
        public Long verticesForScaleFactor(Long scaleFactor) {
            return 2 * scaleFactor;
        }

        public GraphTraversal<?, ?> traversal(GraphTraversalSource g) {
            return g
                    .addV("A").as("A")
                    .property("entrypoint", true)
                    .addV("B").as("B")
                    .addE("AtoB").from("A").to("B");
        }
    }

    public static class GDemoSchema extends ExampleSchemas {
        public static final ExampleSchemas INSTANCE = new GDemoSchema();
        private final GraphSchema schema;

        public GDemoSchema() {
            this.schema = YAMLSchemaParser.from(IOUtil.copyFromResourcesIntoNewTempFile("gdemo_schema.yaml").toPath()).parse();
        }

        @Override
        public GraphTraversalSource writeToTraversalSource(GraphTraversalSource sg) {
            SchemaGraphUtil.writeToTraversalSource(sg, schema);
            return sg;
        }

        @Override
        public Long edgesForScaleFactor(Long scaleFactor) {
            return 35L;
        }

        @Override
        public Long verticesForScaleFactor(Long scaleFactor) {
            return 38L;
        }

    }

    public static class Benchmark2024 extends ExampleSchemas {
        public static final ExampleSchemas INSTANCE = new Benchmark2024();


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
        public GraphTraversalSource writeToTraversalSource(GraphTraversalSource sg) {
            traversal(sg).iterate();
            return sg;
        }

        @Override
        public Long edgesForScaleFactor(Long scaleFactor) {
            return scaleFactor;
        }

        @Override
        public Long verticesForScaleFactor(Long scaleFactor) {
            return 2 * scaleFactor;
        }

        public GraphTraversal<?, ?> traversal(GraphTraversalSource g) {
            return g
                    .addV(Keys.V_LABEL.PARTNER_IDENTITY).as(Keys.V_LABEL.PARTNER_IDENTITY)
                    .property(ENTRYPOINT, true)
                    .property(subKey(ENTRYPOINT, CHANCES_TO_CREATE), 1)
                    .property(subKey(ENTRYPOINT, LIKELIHOOD), 1.0)
                    .property("partner_id", "String")
                    .property("partner_id.value.generator", ValueGenerator.RandomDigitSequence.class.getName())
                    .property("partner_id.value.generator.digits", 12)

                    .addV(Keys.V_LABEL.ACCOUNT).as(Keys.V_LABEL.ACCOUNT)
                    .property("partner_id", "String")
                    .property("partner_id.value.generator", ValueGenerator.RandomDigitSequence.class.getName())
                    .property("partner_id.value.generator.digits", 8)
                    .addE(EDGE_TYPE).from(Keys.V_LABEL.PARTNER_IDENTITY).to(Keys.V_LABEL.ACCOUNT)
                    .property("edge_prop", "String")
                    .property("edge_prop.value.generator", ValueGenerator.RandomDigitSequence.class.getName())
                    .property("edge_prop.value.generator.digits", 8);
        }
    }

    public static class PersonCatMouse extends ExampleSchemas {
        public static final ExampleSchemas INSTANCE = new PersonCatMouse();
        @Override
        public GraphTraversalSource writeToTraversalSource(GraphTraversalSource sg) {
            traversal(sg).iterate();
            return sg;
        }

        @Override
        public Long edgesForScaleFactor(Long scaleFactor) {
            return -1L;
        }

        @Override
        public Long verticesForScaleFactor(Long scaleFactor) {
            return -1L;
        }

        public GraphTraversal<?, ?> traversal(GraphTraversalSource g) {
            return g
                    .addV("Person").as("Person")
                    .property("entrypoint", true)
                    .property("entrypoint.likelyhood", 1.0)
                    .property("maritalStatus", "String")
                    .property("maritalStatus.likelihood", 1.0)
                    .property("maritalStatus.value.generator", "JFaker")
                    .property("maritalStatus.value.generator.module", "demographic")
                    .property("maritalStatus.value.generator.method", "maritalStatus")
                    .addV("Cat").as("Cat")
                    .property("name", "String")
                    .property("name.likelihood", 1.0)
                    .property("name.value.generator", "ChoiceFromList")
                    .property("name.value.generator.choices", List.of("Ralph", "Fluffy", "Tom"))
                    .addV("Mouse").as("Mouse")
                    .property("color", "String")
                    .property("color.likelihood", 1.0)
                    .property("color.value.generator", "ChoiceFromList")
                    .property("color.value.generator.choices", List.of("Brown", "White", "Grey"))
                    .addE("HasCat").from("Person").to("Cat")
                    .property("likelihood",.5)
                    .property("create.chances",3)
                    .addE("Chased").from("Cat").to("Mouse")
                    .property("likelihood",.2)
                    .property("create.chances",10);
        }
    }

}
