/*
 * @author Grant Haywood <grant.haywood@aerospike.com>
 * Developed May 2023 - Oct 2023
 * Copyright (c) 2023 Aerospike Inc.
 */

package com.aerospike.synth.emitter.generator;

import com.aerospike.graph.synth.emitter.generator.schema.seralization.TinkerPopSchemaTraversalParser;
import com.aerospike.graph.synth.emitter.generator.schema.seralization.YAMLSchemaParser;
import com.aerospike.graph.synth.emitter.generator.schema.definition.GraphSchema;
import com.aerospike.movement.test.core.AbstractMovementTest;
import com.aerospike.movement.test.tinkerpop.SharedEmptyTinkerGraphGraphProvider;
import com.aerospike.movement.tinkerpop.common.GraphProvider;
import com.aerospike.movement.util.core.runtime.IOUtil;
import com.aerospike.graph.synth.util.tinkerpop.SchemaGraphUtil;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.MapConfiguration;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/*
  Created by Grant Haywood grant@iowntheinter.net
  7/18/23
*/
public class SchemaGraphTest extends AbstractMovementTest {
    @Test
    public void testYamlYamlEquality() throws Exception {
        final File schemaFile = IOUtil.copyFromResourcesIntoNewTempFile("example_schema.yaml");


        final Configuration config = new MapConfiguration(new HashMap<>() {{
            put(YAMLSchemaParser.Config.Keys.YAML_FILE_PATH, schemaFile.getAbsolutePath());
            put(TinkerPopSchemaTraversalParser.Config.Keys.GRAPH_PROVIDER, SharedEmptyTinkerGraphGraphProvider.class.getName());
        }});

        //parse the yaml file to a GraphSchema
        GraphSchema fromYaml = YAMLSchemaParser.open(config).parse();

        GraphSchema fromYaml2 = YAMLSchemaParser.open(config).parse();

        //compare the two GraphSchema instances, deep equality is implemented by each schema def class
        assertTrue(fromYaml2.equals(fromYaml));
    }

    @Test
    public void testYamlSeralization() throws IOException {
        final File schemaFile = IOUtil.copyFromResourcesIntoNewTempFile("example_schema.yaml");
        final Configuration config = new MapConfiguration(new HashMap<>() {{
            put(YAMLSchemaParser.Config.Keys.YAML_FILE_PATH, schemaFile.getAbsolutePath());
            put(TinkerPopSchemaTraversalParser.Config.Keys.GRAPH_PROVIDER, SharedEmptyTinkerGraphGraphProvider.class.getName());
        }});

        //parse the yaml file to a GraphSchema
        GraphSchema fromYaml = YAMLSchemaParser.open(config).parse();

        final String yamlString = YAMLSchemaParser.dumpSchema(fromYaml);

        Path tempFile = Files.createTempFile("yaml", "test");
        Files.write(tempFile, yamlString.getBytes());
        GraphSchema fromYaml2 = YAMLSchemaParser.from(tempFile).parse();


        //compare the two GraphSchema instances, deep equality is implemented by each schema def class
        assertTrue(fromYaml2.equals(fromYaml));
    }

    @Test
    public void testGraphYamlEquality() throws Exception {
        final File schemaFile = IOUtil.copyFromResourcesIntoNewTempFile("example_schema.yaml");
        final Graph graph = SharedEmptyTinkerGraphGraphProvider.open().getProvided(GraphProvider.GraphProviderContext.OUTPUT);
        graph.traversal().V().drop().iterate();
        final Configuration config = new MapConfiguration(new HashMap<>() {{
            put(YAMLSchemaParser.Config.Keys.YAML_FILE_PATH, schemaFile.getAbsolutePath());
            put(TinkerPopSchemaTraversalParser.Config.Keys.GRAPH_PROVIDER, SharedEmptyTinkerGraphGraphProvider.class.getName());
        }});

        //parse the yaml file to a GraphSchema
        GraphSchema fromYaml = YAMLSchemaParser.open(config).parse();

        //write that loaded schema into a graph instance
        SchemaGraphUtil.writeToTraversalSource(graph.traversal(), fromYaml);

        //parse the graph instance back into a GraphSchema
        GraphSchema fromGraph = TinkerPopSchemaTraversalParser.open(config).parse();
        graph.close();
        //compare the two GraphSchema instances, deep equality is implemented by each schema def class
        assertTrue(fromGraph.equals(fromYaml));
        assertTrue(fromYaml.equals(fromGraph));

    }

    @Test
    public void testGraphSONSeralization() throws Exception {
        final File schemaFile = IOUtil.copyFromResourcesIntoNewTempFile("example_schema.yaml");
        final Graph graph = SharedEmptyTinkerGraphGraphProvider.open().getProvided(GraphProvider.GraphProviderContext.OUTPUT);
        graph.traversal().V().drop().iterate();
        final Configuration config = new MapConfiguration(new HashMap<>() {{
            put(YAMLSchemaParser.Config.Keys.YAML_FILE_PATH, schemaFile.getAbsolutePath());
            put(TinkerPopSchemaTraversalParser.Config.Keys.GRAPH_PROVIDER, SharedEmptyTinkerGraphGraphProvider.class.getName());
        }});

        //parse the yaml file to a GraphSchema
        GraphSchema fromYaml = YAMLSchemaParser.open(config).parse();

        //write that loaded schema into a graph instance
        SchemaGraphUtil.writeToTraversalSource(graph.traversal(), fromYaml);
        graph.traversal().io("target/example_schema.json").write().iterate();
        graph.traversal().V().drop().iterate();
        graph.traversal().io("target/example_schema.json").read().iterate();
        graph.close();
        final Configuration GraphSONConfig = new MapConfiguration(new HashMap<>() {{
            put(TinkerPopSchemaTraversalParser.Config.Keys.GRAPHSON_FILE, "target/example_schema.json");
        }});


        //parse the graph instance back into a GraphSchema
        GraphSchema fromGraph = TinkerPopSchemaTraversalParser.open(GraphSONConfig).parse();

        //compare the two GraphSchema instances, deep equality is implemented by each schema def class
        assertTrue(fromGraph.equals(fromYaml));
    }


}