package com.aerospike.graph.synth.cli;

import com.aerospike.graph.synth.emitter.generator.Generator;
import com.aerospike.graph.synth.emitter.generator.schema.seralization.TinkerPopSchemaTraversalParser;
import com.aerospike.graph.synth.emitter.generator.schema.seralization.YAMLSchemaParser;
import com.aerospike.graph.synth.process.tasks.generator.Generate;
import com.aerospike.movement.output.files.DirectoryOutput;
import com.aerospike.movement.process.core.Task;
import com.aerospike.movement.runtime.core.local.LocalParallelStreamRuntime;
import com.aerospike.movement.runtime.core.local.RunningPhase;
import com.aerospike.movement.tinkerpop.common.RemoteGraphTraversalProvider;
import com.aerospike.movement.util.core.configuration.ConfigUtil;
import com.aerospike.movement.util.core.iterator.ext.IteratorUtils;
import com.aerospike.movement.util.core.runtime.RuntimeUtil;
import com.aerospike.synth.emitter.generator.schema.ExampleSchemas;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.MapConfiguration;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class CLI {
    private static final String CLEAR_MESSAGE = "Will only write into an empty graph, use --clear to force and erase your existing graph";

    public static void main(String[] args) throws Exception {
        RuntimeUtil.registerTaskAlias(Generate.class.getSimpleName(), Generate.class);
        RuntimeUtil.openClass(Generate.class, ConfigUtil.empty());
        GraphSynthCLI cli = parseCLI(args);
        if (cli.help().isPresent()) {
            CommandLine.usage(new CLI.GraphSynthCLI(), System.out);
            System.exit(0);
        }
        if (GraphSynthCLI.ioutil(cli)) {
            System.exit(0);
        }
        final List<Long> scales;
        scales = cli.scaleFactor().orElseThrow(() -> {
            CommandLine.usage(new CLI.GraphSynthCLI(), System.out);
            return new RuntimeException("no scale factors provided");
        });
        Map<String, String> scaleResults = new HashMap<>();
        if (!cli.outputUri().get().getScheme().equals("file") && scales.size() > 1) {
            throw new RuntimeException("multiple scale factors only supported for file:// output");
        }
        scales.forEach(scaleFactor -> {
            try {
                RuntimeUtil.unload(Generate.class);
            } catch (Exception e) {
                System.out.println(e);
            }
            cli.scaleFactor = String.valueOf(scaleFactor);
            Configuration config = GraphSynthCLI.toConfig(cli);
            config.setProperty(GraphSynthCLI.Argument.SCALE_FACTOR.getConfigKey(), scaleFactor);
            if (cli.outputUri().get().getScheme().equals("file")) {
                Path scalePath = Path.of(cli.outputUri().get()).resolve(String.valueOf(scaleFactor));
                config.setProperty(GraphSynthCLI.Argument.OUTPUT_URI_LONG.getConfigKey(), scalePath.toUri().toString());
            }
            if (!cli.inputUri().get().getScheme().equals("file")) {
                config.setProperty(Generator.Config.Keys.SCHEMA_PARSER, TinkerPopSchemaTraversalParser.class.getName());
                config.setProperty(TinkerPopSchemaTraversalParser.Config.Keys.GRAPH_PROVIDER, RemoteGraphTraversalProvider.class.getName());
                config.setProperty(RemoteGraphTraversalProvider.Config.Keys.INPUT_URI, cli.inputUri().get().toString());
            }
            if (cli.outputUri().get().getScheme().startsWith("ws")) {
                RemoteGraphTraversalProvider.URIConnectionInfo info = RemoteGraphTraversalProvider.URIConnectionInfo.from(cli.outputUri().get());
                GraphTraversalSource g = AnonymousTraversalSource
                        .traversal()
                        .withRemote(DriverRemoteConnection.using(info.host, info.port, info.traversalSourceName));
                if (g.V().hasNext() && (cli.clear().isEmpty() || !cli.clear().get())) {
                    System.err.println(CLEAR_MESSAGE);
                    System.exit(1);
                } else {
                    g.V().drop().iterate();
                    try {
                        g.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            Optional<GraphSynthCLIPlugin> plugin = loadPlugin(config);
            System.out.println("will generate scale factor: " + scaleFactor);
            final UUID taskId = (UUID) plugin.orElseThrow(() -> new RuntimeException("Could not load CLI")).call().next();
            final Task.StatusMonitor taskMonitor = Task.StatusMonitor.from(taskId);
            while (taskMonitor.isRunning()) {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println(YAMLSchemaParser.dump(taskMonitor.status(true)));
            }

            LocalParallelStreamRuntime runtime = RuntimeUtil.runtimeForTask(taskId);
            Optional<RunningPhase> rp = RuntimeUtil.runningPhaseForTask(taskId);

            if (rp.isPresent() && cli.debug().isPresent()) {
                Configuration runningConfig = rp.get().config;
                System.out.println(runningConfig);
            }
            RuntimeUtil.waitTask(taskId);
            runtime.close();
            long fileCount = 0;
            if (cli.outputUri().get().getScheme().equals("file")) {
                try {
                    Path scalePath = Path.of(cli.outputUri().get()).resolve(String.valueOf(scaleFactor));
                    fileCount = Files.walk(scalePath).filter(it -> it.toFile().isFile()).count();
                    System.out.printf("file count for %s: %d\n", scalePath.toString(), fileCount);
                    scaleResults.put("Files generated at Scale Factor: " + scaleFactor, String.valueOf(fileCount));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            System.out.println("done scaleFactor " + scaleFactor);
        });
        scaleResults.forEach((k, v) -> System.out.println(String.join(" ", k, v)));

        if (!cli.testMode) System.exit(0);
    }

    public static GraphSynthCLI parseCLI(final String[] args) {
        try {
            return CommandLine.populateCommand(new GraphSynthCLI(), args);
        } catch (CommandLine.ParameterException pxe) {
            CommandLine.usage(new CLI.GraphSynthCLI(), System.out);
            throw new RuntimeException("Error parsing command line arguments", pxe);
        }
    }

    public boolean checkCLI(GraphSynthCLI cli) {
        if (cli.help().isPresent()) {
            CommandLine.usage(new CLI.GraphSynthCLI(), System.out);
            return false;
        }
        if (cli.outputUri().isEmpty()) {
            System.out.println("You must set an output URI\n");
            CommandLine.usage(new CLI.GraphSynthCLI(), System.out);
            return false;
        }
        if (cli.inputUri().isEmpty()) {
            System.out.println("You must set an input URI\n");
            CommandLine.usage(new CLI.GraphSynthCLI(), System.out);
            return false;
        }
        return true;
    }

    protected static Optional<GraphSynthCLIPlugin> loadPlugin(Configuration config) {
        return Optional.of((GraphSynthCLIPlugin) GraphSynthCLIPlugin.open(config));
    }

    @CommandLine.Command(name = "GraphSynth", header = "Graph Synth, by Aerospike.", sortSynopsis = false, sortOptions = false)
    public static class GraphSynthCLI {

        public static class ArgNames {
            public static final String SET_LONG = "--set";
            public static final String HELP_LONG = "--help";
            public static final String DEBUG_LONG = "--debug";
            public static final String TEST_MODE = "--test-mode";
            public static final String OUTPUT_URI_LONG = "--output-uri";
            public static final String INPUT_URI_LONG = "--input-uri";
            public static final String SCALE_FACTOR = "--scale-factor";
            public static final String LIST_SAMPLES = "--list-sample-schemas";
            public static final String LOAD_SAMPLE = "--load-sample";
            public static final String DUMP_SAMPLE = "--dump-sample";
            public static final String EXPORT_SCHEMA = "--export-schema";
            public static final String LOAD_SCHEMA = "--load-schema";
            public static final String CLEAR = "--clear";
        }


        public enum Argument {
            SET_LONG(ArgNames.SET_LONG),
            HELP_LONG(ArgNames.HELP_LONG),
            DEBUG_LONG(ArgNames.DEBUG_LONG),
            TEST_MODE(ArgNames.TEST_MODE),
            OUTPUT_URI_LONG(ArgNames.OUTPUT_URI_LONG),
            INPUT_URI_LONG(ArgNames.INPUT_URI_LONG),
            SCALE_FACTOR(ArgNames.SCALE_FACTOR);
            private final String cliArgument;

            Argument(String cliArgument) {
                this.cliArgument = cliArgument;
            }

            public String getConfigKey() {
                return cliToConfigKey(cliArgument);
            }

            public String getCLIKey() {
                return cliArgument;
            }

            public String toString() {
                return getConfigKey() + " = " + getCLIKey();
            }

            public static Optional<Argument> configKeyToArgument(final String configKey) {
                return Arrays.stream(Argument.values()).filter(arg -> arg.getConfigKey().equals(configKey)).findFirst();
            }

            public static Optional<Argument> cliToArgument(final String cliKey) {
                return Arrays.stream(Argument.values()).filter(arg -> arg.getCLIKey().equals(cliKey)).findFirst();
            }
        }

        public static String cliToConfigKey(String cli) {
            return cli.replace("--", "").replace("-", ".");
        }

        public static Configuration toConfig(GraphSynthCLI cli) {
            final Map<String, String> results = new HashMap<>();
            cli.overrides().ifPresent(results::putAll);
            cli.outputUri().ifPresent(it -> results.put(Argument.OUTPUT_URI_LONG.getConfigKey(), it.toString()));
            cli.inputUri().ifPresent(it -> results.put(Argument.INPUT_URI_LONG.getConfigKey(), it.toString()));
            cli.testMode().ifPresent(it -> results.put(Argument.TEST_MODE.getConfigKey(), it.toString()));
            cli.debug().ifPresent(it -> results.put(Argument.DEBUG_LONG.getConfigKey(), it.toString()));
            cli.help().ifPresent(it -> results.put(Argument.HELP_LONG.getConfigKey(), it.toString()));
            return new MapConfiguration(results);
        }

        public static Configuration taskConfig(GraphSynthCLI cli, Configuration config) {
            final Map<String, String> results = new HashMap<>();
            cli.overrides().ifPresent(results::putAll);
            cli.scaleFactor().ifPresent(it -> results.put(Generator.Config.Keys.SCALE_FACTOR, Optional.ofNullable(config.getString(Argument.SCALE_FACTOR.getConfigKey())).orElseThrow(() -> new RuntimeException("no scale factor configured"))));
            cli.testMode().ifPresent(it -> results.put(Argument.TEST_MODE.getConfigKey(), it.toString()));
            cli.debug().ifPresent(it -> results.put(Argument.DEBUG_LONG.getConfigKey(), it.toString()));
            cli.help().ifPresent(it -> results.put(Argument.HELP_LONG.getConfigKey(), it.toString()));
            if (cli.outputUri().get().getScheme().equals("file")) {
                cli.outputUri().ifPresent(it -> results.put(DirectoryOutput.Config.Keys.OUTPUT_DIRECTORY, Path.of(URI.create(it.toString())).toAbsolutePath().toString()));
            }
            if (cli.inputUri().get().getScheme().equals("file")) {
                cli.inputUri().ifPresent(it -> results.put(YAMLSchemaParser.Config.Keys.YAML_FILE_URI, it.toString()));
            }
            return new MapConfiguration(results);
        }

        public static GraphSynthCLI fromArguments(List<String> args) {

            GraphSynthCLI cli = CommandLine.populateCommand(new GraphSynthCLI(), args.toArray(new String[]{}));
            return cli;
        }

        public static GraphSynthCLI fromConfig(Configuration config) {
            List<String> args = IteratorUtils.stream(config.getKeys()).filter(key -> Argument.configKeyToArgument(key).isPresent()).map(key -> Argument.configKeyToArgument(key).get().getCLIKey() + "=" + config.getString(key)).collect(Collectors.toList());
            return fromArguments(args);
        }


        @CommandLine.Option(names = {ArgNames.HELP_LONG}, description = "Help", order = 0)
        protected Boolean help;

        private Optional<Boolean> help() {
            return Optional.ofNullable(help);
        }


        @CommandLine.Option(names = {ArgNames.SCALE_FACTOR}, description = "Comma delimited list of scale factors", order = 1)
        protected String scaleFactor;
        public Optional<List<Long>> scaleFactor() {
            return Optional.ofNullable(scaleFactor).map(scalesString -> Arrays.stream(scalesString.split(",")).map(Long::valueOf).collect(Collectors.toList()));
        }

        @CommandLine.Option(names = {ArgNames.INPUT_URI_LONG}, description = "File or Gremlin Server URI for schema, supported schemes:\n file:// \n ws:// \n wss:// ", order = 2)
        protected String inputUri;

        public Optional<URI> inputUri() {
            return Optional.ofNullable(inputUri).map(URI::create);
        }
        @CommandLine.Option(names = {ArgNames.OUTPUT_URI_LONG}, description = "File or Gremlin Server URI for output, supported schemes:\n file:// \n ws:// \n wss://", order = 3)
        protected String outputUri;
        public Optional<URI> outputUri() {
            return Optional.ofNullable(outputUri).map(URI::create);
        }


        @CommandLine.Option(names = {ArgNames.LIST_SAMPLES}, description = "List Sample Schemas", order = 4)
        protected Boolean listSamples;

        public Optional<Boolean> listSamples() {
            return Optional.ofNullable(listSamples);
        }

        @CommandLine.Option(names = {ArgNames.LOAD_SAMPLE}, description = "Load Sample to Gremlin Server", order = 5)
        protected String loadSample;

        public Optional<String> loadSample() {
            return Optional.ofNullable(loadSample);
        }

        @CommandLine.Option(names = {ArgNames.DUMP_SAMPLE}, description = "Dump Sample Schema to YAML", order = 6)
        protected String dumpSample;

        public Optional<String> dumpSample() {
            return Optional.ofNullable(dumpSample);
        }

        @CommandLine.Option(names = {ArgNames.EXPORT_SCHEMA}, description = "Export Schema from Gremlin Server to YAML file", order = 7)
        protected boolean exportSchema;

        public Optional<Boolean> exportSchema() {
            return exportSchema ? Optional.of(true) : Optional.empty();
        }

        @CommandLine.Option(names = {ArgNames.LOAD_SCHEMA}, description = "Load YAML Schema to Gremlin Server", order = 8)
        protected boolean loadSchema;

        public Optional<Boolean> loadSchema() {
            return loadSchema ? Optional.of(true) : Optional.empty();
        }

        @CommandLine.Option(names = {ArgNames.CLEAR}, description = "Delete and overwrite existing remote graph", order = 9)
        protected Boolean clear;

        private Optional<Boolean> clear() {
            return Optional.ofNullable(clear);
        }

        @CommandLine.Option(names = {ArgNames.SET_LONG}, description = "Set or override configuration key",order = 10)
        protected Map<String, String> overrides;

        public Optional<Map<String, String>> overrides() {
            return Optional.ofNullable(overrides);
        }
        @CommandLine.Option(names = {ArgNames.DEBUG_LONG}, description = "Show Debug Output", order = 11)
        protected boolean debug;

        private Optional<Boolean> debug() {
            return debug ? Optional.of(true) : Optional.empty();
        }


        @CommandLine.Option(names = {ArgNames.TEST_MODE}, description = "Test Mode", hidden = true, order = 12)
        protected Boolean testMode = false;

        private Optional<Boolean> testMode() {
            return Optional.ofNullable(testMode);
        }



        public static boolean ioutil(GraphSynthCLI cli) {
            if (!(cli.listSamples().isPresent() ||
                    cli.loadSample().isPresent() ||
                    cli.dumpSample().isPresent() ||
                    cli.loadSchema().isPresent() ||
                    cli.exportSchema().isPresent())) {
                return false;
            }
            if (cli.listSamples().isPresent()) {
                ExampleSchemas.samples.stream().map(it -> it.getClass().getSimpleName()).collect(Collectors.toList()).forEach(System.out::println);
                return true;
            }
            if (cli.loadSample().isPresent()) {
                String sampleName = cli.loadSample().get();
                ExampleSchemas sample = ExampleSchemas.samples.stream().filter(it -> it.getClass().getSimpleName().equals(sampleName)).findFirst().orElseThrow(() -> new RuntimeException("sample schema: " + sampleName + " not found"));
                if (!cli.outputUri().isPresent() || !cli.outputUri().get().getScheme().startsWith("ws"))
                    throw new RuntimeException("must load into a ws:// or wss:// outputURI");
                RemoteGraphTraversalProvider.URIConnectionInfo info = RemoteGraphTraversalProvider.URIConnectionInfo.from(cli.outputUri().get());
                GraphTraversalSource g = AnonymousTraversalSource
                        .traversal()
                        .withRemote(DriverRemoteConnection.using(info.host, info.port, info.traversalSourceName));
                if (g.V().hasNext() && (cli.clear().isEmpty() || !cli.clear().get())) {
                    System.err.println(CLEAR_MESSAGE);
                    System.exit(1);
                } else {
                    g.V().drop().iterate();
                }
                sample.writeToTraversalSource(g);
                return true;
            }
            if (cli.dumpSample().isPresent()) {
                String sampleName = cli.dumpSample().get();
                ExampleSchemas sample = ExampleSchemas.samples.stream().filter(it -> it.getClass().getSimpleName().equals(sampleName)).findFirst().orElseThrow();
                if (!cli.outputUri().isPresent() || !cli.outputUri().get().getScheme().startsWith("file"))
                    throw new RuntimeException("must dump into a yaml file:// outputURI");
                sample.writeToYAML(Path.of(cli.outputUri().get()));
                return true;
            }
            if (cli.exportSchema().isPresent()) {
                if (!cli.inputUri().isPresent() || !cli.inputUri().get().getScheme().startsWith("ws"))
                    throw new RuntimeException("must export from a ws:// or wss:// inputURI");
                if (!cli.outputUri().isPresent() || !cli.outputUri().get().getScheme().startsWith("file"))
                    throw new RuntimeException("must export into a yaml file:// outputURI");

                RemoteGraphTraversalProvider.URIConnectionInfo info = RemoteGraphTraversalProvider.URIConnectionInfo.from(cli.inputUri().get());
                GraphTraversalSource g = AnonymousTraversalSource
                        .traversal()
                        .withRemote(DriverRemoteConnection.using(info.host, info.port, info.traversalSourceName));
                try {
                    Files.write(Path.of(cli.outputUri().get()), YAMLSchemaParser.dumpSchema(TinkerPopSchemaTraversalParser.fromTraversal(g)).getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return true;
            }
            if (cli.loadSchema().isPresent()) {
                if (!cli.outputUri().isPresent() || !cli.inputUri().get().getScheme().startsWith("file"))
                    throw new RuntimeException("must load from a yaml file:// inputURI");
                if (!cli.inputUri().isPresent() || !cli.outputUri().get().getScheme().startsWith("ws"))
                    throw new RuntimeException("must load to a ws:// or wss:// outputURI");

                RemoteGraphTraversalProvider.URIConnectionInfo info = RemoteGraphTraversalProvider.URIConnectionInfo.from(cli.outputUri().get());
                GraphTraversalSource g = AnonymousTraversalSource
                        .traversal()
                        .withRemote(DriverRemoteConnection.using(info.host, info.port, info.traversalSourceName));
                if (g.V().hasNext() && (cli.clear().isEmpty() || !cli.clear().get())) {
                    System.err.println(CLEAR_MESSAGE);
                    System.exit(1);
                } else {
                    g.V().drop().iterate();
                }
                TinkerPopSchemaTraversalParser.writeTraversalSource(g, YAMLSchemaParser.from(cli.inputUri().get()).parse());
                return true;
            }
            return true;

        }


    }
}
