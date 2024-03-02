/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.graph.synth.emitter.generator.schema.seralization;

import com.aerospike.graph.synth.emitter.generator.schema.definition.*;
import com.aerospike.movement.config.core.ConfigurationBase;
import com.aerospike.graph.synth.emitter.generator.schema.GraphSchemaParser;
import com.aerospike.graph.synth.emitter.generator.schema.SchemaBuilder;
import com.aerospike.movement.tinkerpop.common.GraphProvider;
import com.aerospike.movement.tinkerpop.common.TraversalProvider;
import com.aerospike.movement.util.core.configuration.ConfigUtil;
import com.aerospike.movement.util.core.runtime.RuntimeUtil;
import com.aerospike.graph.synth.util.tinkerpop.SchemaGraphUtil;
import org.apache.commons.configuration2.Configuration;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static com.aerospike.movement.util.core.configuration.ConfigUtil.subKey;


/**
 * @author Created by Grant Haywood grant@iowntheinter.net 7/17/2023
 * <a href="https://gist.github.com/okram/2aa70423e130bfc9118b4dde9c75c183">
 * Inspired by Marko Rodriguez's gist and commentary on Graph Schema
 * </a>
 */

public class TinkerPopSchemaTraversalParser implements GraphSchemaParser {


    public static class Config extends ConfigurationBase {
        public static final Config INSTANCE = new Config();

        private Config() {
            super();
        }

        @Override
        public Map<String, String> defaultConfigMap(final Map<String, Object> config) {
            return DEFAULTS;
        }

        @Override
        public List<String> getKeys() {
            return ConfigUtil.getKeysFromClass(Config.Keys.class);
        }

        public static class Keys {
            public static final String GRAPHSON_FILE = "generator.schema.graphschema.graphson.file";
            public static final String GRAPH_PROVIDER = "generator.schema.graphschema.graph.provider";
        }

        private static final Map<String, String> DEFAULTS = new HashMap<>() {{
        }};
    }


    private final GraphTraversalSource schemaG;

    private TinkerPopSchemaTraversalParser(final GraphTraversalSource schemaGraphSource) {
        this.schemaG = schemaGraphSource;
    }

    private static GraphTraversalSource openSchemaGraph(final Configuration config) {
        final GraphTraversalSource g;
        if (config.containsKey(Config.Keys.GRAPHSON_FILE)) {
            g = readGraphSON(Path.of(config.getString(Config.Keys.GRAPHSON_FILE))).traversal();
        } else if (config.containsKey(Config.Keys.GRAPH_PROVIDER)) {
            Object x = RuntimeUtil.openClassRef(config.getString(Config.Keys.GRAPH_PROVIDER), config);
            if (GraphProvider.class.isAssignableFrom(x.getClass()))
                g = ((GraphProvider) x).getProvided(GraphProvider.GraphProviderContext.INPUT).traversal();
            else if (TraversalProvider.class.isAssignableFrom(x.getClass())) {
                g = ((TraversalProvider) x).getProvided(GraphProvider.GraphProviderContext.INPUT);
            } else {
                throw new IllegalArgumentException("could not load schema graph");
            }
        } else {
            throw new IllegalArgumentException("could not load schema graph");
        }
        return g;
    }

    public static TinkerPopSchemaTraversalParser open(final Configuration config) {
        return new TinkerPopSchemaTraversalParser(openSchemaGraph(config));
    }


    private VertexSchema fromTinkerPop(final Vertex tp3SchemaVertex) {
        final VertexSchema vertexSchema = new VertexSchema();
        final List<PropertySchema> propertySchemas = new ArrayList<>();
        schemaG.V(tp3SchemaVertex).properties().forEachRemaining(tp3VertexProperty -> {
            if (isMetadata(tp3VertexProperty.key())) {
                return;
            }
            final PropertySchema propertySchema = new PropertySchema();
            propertySchema.name = tp3VertexProperty.key();
            propertySchema.type = (String) tp3VertexProperty.value();
            propertySchema.likelihood = (double) getSubKeyFromElement(tp3SchemaVertex, tp3VertexProperty.key(),
                    SchemaBuilder.Keys.LIKELIHOOD).orElse(1.0);
            final GeneratorConfig valueConfig = new GeneratorConfig();
            final String implClassName = (String) getSubKeyFromElement(tp3SchemaVertex, tp3VertexProperty.key(), SchemaBuilder.Keys.VALUE_GENERATOR).orElseThrow(() ->
                    new IllegalArgumentException("No value generator implementation specified for property " + tp3VertexProperty.key())
            );
            final Map<String, Object> generatorArgs = getKeyValuesForSubKey(schemaG, tp3SchemaVertex, tp3VertexProperty.key(), SchemaBuilder.Keys.VALUE_GENERATOR).orElseThrow();

            valueConfig.impl = implClassName;
            valueConfig.args = generatorArgs;
            propertySchema.valueGenerator = valueConfig;
            propertySchemas.add(propertySchema);
        });
        vertexSchema.properties = propertySchemas;
        final List<OutEdgeSpec> outEdgeSpecs = new ArrayList<>();
        schemaG.V(tp3SchemaVertex).outE().forEachRemaining(edge -> {
            final OutEdgeSpec outEdgeSpec = new OutEdgeSpec();
            outEdgeSpec.name = edge.label();
            outEdgeSpec.likelihood = edge.properties(SchemaBuilder.Keys.LIKELIHOOD).hasNext() ?
                    (double) edge.properties(SchemaBuilder.Keys.LIKELIHOOD).next().value() : 1.0;
            outEdgeSpec.chancesToCreate = edge.properties(SchemaBuilder.Keys.CHANCES_TO_CREATE).hasNext() ?
                    (int) edge.properties(SchemaBuilder.Keys.CHANCES_TO_CREATE).next().value() : 1;
            outEdgeSpecs.add(outEdgeSpec);
        });
        vertexSchema.outEdges = outEdgeSpecs;
        vertexSchema.name = tp3SchemaVertex.label();
        return vertexSchema;
    }

    private boolean isMetadata(final String key) {
        return ConfigUtil.isSubKey(key) || key.equals(SchemaBuilder.Keys.ENTRYPOINT);
    }

    public static Optional<Object> getSubKeyFromElement(final Element tp3ele, final String key, final String subKey) {
        String sk = subKey(key, subKey);
        if (!tp3ele.properties(sk).hasNext()) {
            return Optional.empty();
        }
        return Optional.of(tp3ele.properties(subKey(key, subKey)).next().value());
    }

    public static Optional<Map<String, Object>> getKeyValuesForSubKey(GraphTraversalSource g, final Element tp3ele, final String key, final String subKey) {
        String sk = subKey(key, subKey);
        if (!tp3ele.properties(sk).hasNext()) {
            return Optional.empty();
        }
        final Map<String, Object> metaProps = new HashMap<>();
        if (Edge.class.isAssignableFrom(tp3ele.getClass())) {
            g.E(tp3ele)
                    .properties()
                    .toList()
                    .stream()
                    .filter(prop -> prop.key().startsWith(sk))
                    .filter(prop -> !prop.key().equals(sk))
                    .forEach(prop -> metaProps.put(prop.key().replace(sk + ".", ""), prop.value()));
        } else {
            g.V(tp3ele)
                    .properties()
                    .toList()
                    .stream()
                    .filter(prop -> prop.key().startsWith(sk))
                    .filter(prop -> !prop.key().equals(sk))
                    .forEach(prop -> metaProps.put(prop.key().replace(sk + ".", ""), prop.value()));
        }

        return Optional.of(metaProps);
    }

    private EdgeSchema fromTinkerPop(final Edge tp3SchemaEdge) {
        final EdgeSchema edgeSchema = new EdgeSchema();
        edgeSchema.name = tp3SchemaEdge.label();
        edgeSchema.inVertex = tp3SchemaEdge.inVertex().label();
        edgeSchema.outVertex = tp3SchemaEdge.outVertex().label();
        final List<PropertySchema> propertySchemas = new ArrayList<>();
        tp3SchemaEdge.properties().forEachRemaining(tp3EdgeProperty -> {
            if (isMetadata(tp3EdgeProperty.key())) {
                return;
            }
            final PropertySchema propertySchema = new PropertySchema();
            propertySchema.name = tp3EdgeProperty.key();
            propertySchema.type = (String) tp3EdgeProperty.value();
            propertySchema.likelihood = (double) getSubKeyFromElement(tp3SchemaEdge, tp3EdgeProperty.key(),
                    SchemaBuilder.Keys.LIKELIHOOD).orElse(1.0);
            final GeneratorConfig valueConfig = new GeneratorConfig();
            final String implClassName;
            try {
                implClassName = (String) getSubKeyFromElement(tp3SchemaEdge, tp3EdgeProperty.key(),
                        SchemaBuilder.Keys.VALUE_GENERATOR).orElseThrow();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            final Map<String, Object> generatorArgs = getKeyValuesForSubKey(schemaG, tp3SchemaEdge, tp3EdgeProperty.key(), SchemaBuilder.Keys.VALUE_GENERATOR).orElseThrow();

            valueConfig.impl = implClassName;
            valueConfig.args = generatorArgs;
            propertySchema.valueGenerator = valueConfig;
            propertySchemas.add(propertySchema);
        });
        edgeSchema.properties = propertySchemas;
        return edgeSchema;
    }

    @Override
    public GraphSchema parse() {
        final SchemaBuilder builder = SchemaBuilder.create();
        schemaG.V().toList().forEach(tp3Vertex -> {
            final VertexSchema vertexSchema = fromTinkerPop(tp3Vertex);
            builder.withVertexType(vertexSchema);
        });
        schemaG.E().toList().forEach(tp3Edge -> {
            final EdgeSchema edgeSchema = fromTinkerPop(tp3Edge);
            builder.withEdgeType(edgeSchema);
        });


        List<RootVertexSpec> rootVertexSpecs = getRootVertexSpecsFromGraph(schemaG);
        return builder.build(rootVertexSpecs);
    }

    public List<RootVertexSpec> getRootVertexSpecsFromGraph(GraphTraversalSource schemaG) {
        return schemaG
                .V()
                .has(SchemaBuilder.Keys.ENTRYPOINT, true)
                .toList().stream().map(entrypointVertex -> TPvertexToRootVertexSpec(entrypointVertex))
                .collect(Collectors.toList());
    }

    public static class MissingSchemaKeyException extends RuntimeException {
        public static final String MISSING_MESSAGE = "missing schema key: ";

        public MissingSchemaKeyException(String s) {
            super(s);
        }

        public static MissingSchemaKeyException missingKey(final String missingKey) {
            return new MissingSchemaKeyException(MISSING_MESSAGE + missingKey);
        }

    }

    public static RootVertexSpec TPvertexToRootVertexSpec(final Vertex entrypointVertex) {
        final RootVertexSpec rootVertexSpec = new RootVertexSpec();
        rootVertexSpec.chancesToCreate = (Integer) getSubKeyFromElement(entrypointVertex, SchemaBuilder.Keys.ENTRYPOINT, SchemaBuilder.Keys.CHANCES_TO_CREATE).orElse(1);
        rootVertexSpec.likelihood = (Double) getSubKeyFromElement(entrypointVertex, SchemaBuilder.Keys.ENTRYPOINT, SchemaBuilder.Keys.LIKELIHOOD).orElse(1.0);
        rootVertexSpec.name = (String) entrypointVertex.label();
        return rootVertexSpec;
    }


    public static void writeGraphSON(final GraphSchema schema, final Path output) {
        final Graph graph = TinkerGraph.open();
        SchemaGraphUtil.writeToTraversalSource(graph.traversal(), schema);
        graph.traversal().io(output.toAbsolutePath().toString()).write().iterate();
    }

    public static void writeTraversalSource(GraphTraversalSource remote, final GraphSchema schema) {
        SchemaGraphUtil.writeToTraversalSource(remote, schema);
    }


    public static Graph readGraphSON(final Path graphSonPath) {
        if (!graphSonPath.toFile().exists()) {
            throw new RuntimeException(graphSonPath + " file does not exist.");
        }
        final Graph graph = TinkerGraph.open();
        graph.traversal().io(graphSonPath.toAbsolutePath().toString()).read().iterate();
        return graph;
    }

    public static GraphSchema fromGraph(final Graph graph) {
        return new TinkerPopSchemaTraversalParser(graph.traversal()).parse();
    }

    public static GraphSchema fromTraversal(final GraphTraversalSource g) {
        return new TinkerPopSchemaTraversalParser(g).parse();
    }


    public static GraphSchema fromGraphSON(final Path graphSonPath) {
        return fromGraph(readGraphSON(graphSonPath));
    }
}
