package com.aerospike.graph.synth.cli;

import com.aerospike.graph.synth.emitter.generator.Generator;
import com.aerospike.graph.synth.emitter.generator.schema.seralization.YAMLSchemaParser;
import com.aerospike.graph.synth.process.tasks.generator.Generate;
import com.aerospike.movement.output.files.DirectoryOutput;
import com.aerospike.movement.process.core.Task;
import com.aerospike.movement.runtime.core.local.LocalParallelStreamRuntime;
import com.aerospike.movement.runtime.core.local.RunningPhase;
import com.aerospike.movement.util.core.configuration.ConfigUtil;
import com.aerospike.movement.util.core.iterator.ext.IteratorUtils;
import com.aerospike.movement.util.core.runtime.RuntimeUtil;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.MapConfiguration;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static com.aerospike.graph.synth.emitter.generator.Generator.Config.Keys.SCALE_FACTOR;

public class CLI {
    public static void main(String[] args) throws Exception {
        RuntimeUtil.registerTaskAlias(Generate.class.getSimpleName(), Generate.class);
        RuntimeUtil.openClass(Generate.class, ConfigUtil.empty());
        GraphSynthCLI cli = parseCLI(args);
        final List<Long> scales;
        if (cli.batchScales().isPresent())
            scales = cli.batchScales().get();
        else
            scales = List.of(cli.scaleFactor().get());
        Map<String, String> scaleResults = new HashMap<>();
        scales.forEach(scaleFactor -> {
            try {
                RuntimeUtil.unload(Generate.class);
            } catch (Exception e) {
                System.out.println(e);
            }
            cli.scaleFactor = String.valueOf(scaleFactor);
            Path scalePath = Path.of(cli.outputUri().get()).resolve(String.valueOf(scaleFactor));
            Configuration config = GraphSynthCLI.toConfig(cli);
            config.setProperty(GraphSynthCLI.Argument.SCALE_FACTOR.getConfigKey(), scaleFactor);
            config.setProperty(GraphSynthCLI.Argument.OUTPUT_URI_LONG.getConfigKey(), scalePath.toUri().toString());
            Optional<GraphSynthCLIPlugin> plugin = loadPlugin(config);
            System.out.println("will generate scale factor: " + scaleFactor);
            final UUID taskId = (UUID) plugin.orElseThrow(() -> new RuntimeException("Could not load CLI")).call().next();
            final Task.StatusMonitor taskMonitor = Task.StatusMonitor.from(taskId);
            while (taskMonitor.isRunning()) {
                System.out.println(YAMLSchemaParser.dump(taskMonitor.status(true)));
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            LocalParallelStreamRuntime runtime = RuntimeUtil.runtimeForTask(taskId);
            Optional<RunningPhase> rp = RuntimeUtil.runningPhaseForTask(taskId);

            if (rp.isPresent()) {
                Configuration runningConfig = rp.get().config;
                System.out.println(runningConfig);
            }
            RuntimeUtil.waitTask(taskId);
            runtime.close();
            long fileCount = 0;
            try {
                fileCount = Files.walk(scalePath).filter(it -> it.toFile().isFile()).count();
                System.out.printf("file count for %s: %d\n", scalePath.toString(), fileCount);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            scaleResults.put("Files generated at Scale Factor: " + scaleFactor, String.valueOf(fileCount));
            System.out.println("done scaleFactor " + scaleFactor);
        });
        scaleResults.forEach((k, v) -> System.out.println(String.join(" ", k, v)));

        if (!cli.testMode)
            System.exit(0);
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

    @CommandLine.Command(name = "GraphSynth", header = "Graph Synth, by Aerospike.")
    public static class GraphSynthCLI {

        public static class ArgNames {
            public static final String SET_LONG = "--set";
            public static final String HELP_LONG = "--help";
            public static final String DEBUG_LONG = "--debug";
            public static final String TEST_MODE = "--test-mode";
            public static final String OUTPUT_URI_LONG = "--output-uri";
            public static final String INPUT_URI_LONG = "--input-uri";
            public static final String SCALE_FACTOR = "--scale-factor";
            public static final String BATCH_SCALES = "--batch-scales";
        }


        public enum Argument {
            SET_LONG(ArgNames.SET_LONG),
            HELP_LONG(ArgNames.HELP_LONG),
            DEBUG_LONG(ArgNames.DEBUG_LONG),
            TEST_MODE(ArgNames.TEST_MODE),
            OUTPUT_URI_LONG(ArgNames.OUTPUT_URI_LONG),
            INPUT_URI_LONG(ArgNames.INPUT_URI_LONG),
            SCALE_FACTOR(ArgNames.SCALE_FACTOR),
            BATCH_SCALES(ArgNames.BATCH_SCALES);

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
            cli.scaleFactor().ifPresent(it -> results.put(Argument.SCALE_FACTOR.getConfigKey(), it.toString()));
            cli.outputUri().ifPresent(it -> results.put(Argument.OUTPUT_URI_LONG.getConfigKey(), it.toString()));
            cli.inputUri().ifPresent(it -> results.put(Argument.INPUT_URI_LONG.getConfigKey(), it.toString()));
            cli.testMode().ifPresent(it -> results.put(Argument.TEST_MODE.getConfigKey(), it.toString()));
            cli.debug().ifPresent(it -> results.put(Argument.DEBUG_LONG.getConfigKey(), it.toString()));
            cli.help().ifPresent(it -> results.put(Argument.HELP_LONG.getConfigKey(), it.toString()));
            return new MapConfiguration(results);
        }

        public static Configuration taskConfig(GraphSynthCLI cli) {
            final Map<String, String> results = new HashMap<>();
            cli.overrides().ifPresent(results::putAll);
            cli.scaleFactor().ifPresent(it -> results.put(Generator.Config.Keys.SCALE_FACTOR, it.toString()));
            cli.outputUri().ifPresent(it -> results.put(DirectoryOutput.Config.Keys.OUTPUT_DIRECTORY, Path.of(URI.create(it.toString())).toAbsolutePath().toString()));
            cli.inputUri().ifPresent(it -> results.put(YAMLSchemaParser.Config.Keys.YAML_FILE_URI, it.toString()));
            cli.testMode().ifPresent(it -> results.put(Argument.TEST_MODE.getConfigKey(), it.toString()));
            cli.debug().ifPresent(it -> results.put(Argument.DEBUG_LONG.getConfigKey(), it.toString()));
            cli.help().ifPresent(it -> results.put(Argument.HELP_LONG.getConfigKey(), it.toString()));
            return new MapConfiguration(results);
        }

        public static GraphSynthCLI fromArguments(List<String> args) {

            GraphSynthCLI cli = CommandLine.populateCommand(new GraphSynthCLI(), args.toArray(new String[]{}));
            return cli;
        }

        public static GraphSynthCLI fromConfig(Configuration config) {
            List<String> args = IteratorUtils
                    .stream(config.getKeys())
                    .filter(key -> Argument.configKeyToArgument(key).isPresent())
                    .map(key -> Argument.configKeyToArgument(key).get().getCLIKey() + "=" + config.getString(key))
                    .collect(Collectors.toList());
            return fromArguments(args);
        }

        private Optional<Boolean> help() {
            return Optional.ofNullable(help);
        }

        private Optional<Boolean> debug() {
            return Optional.ofNullable(debug);
        }

        private Optional<Boolean> testMode() {
            return Optional.ofNullable(testMode);
        }

        @CommandLine.Option(names = {ArgNames.OUTPUT_URI_LONG}, description = "Set the output URI. Supported URI schemes are \n ws:// \n wss://")
        protected String outputUri;
        @CommandLine.Option(names = {ArgNames.INPUT_URI_LONG}, description = "Directory URI for source files, supported schemes file:// ")
        protected String inputUri;
        @CommandLine.Option(names = {ArgNames.SCALE_FACTOR}, description = "scale factor")
        protected String scaleFactor;
        @CommandLine.Option(names = {ArgNames.BATCH_SCALES}, description = "Comma delimited list of scale factors to generate datasets for")
        protected String batchScales;
        @CommandLine.Option(names = {ArgNames.HELP_LONG}, description = "Help")
        protected Boolean help;
        @CommandLine.Option(names = {ArgNames.SET_LONG}, description = "Set or override configuration key")
        protected Map<String, String> overrides;
        @CommandLine.Option(names = {ArgNames.TEST_MODE}, description = "Test Mode")
        protected Boolean testMode = false;

        @CommandLine.Option(names = {ArgNames.DEBUG_LONG}, description = "Show Debug Output")
        protected Boolean debug = false;

        public Optional<Map<String, String>> overrides() {
            return Optional.ofNullable(overrides);
        }

        public Optional<URI> outputUri() {
            return Optional.ofNullable(outputUri).map(URI::create);
        }

        public Optional<URI> inputUri() {
            return Optional.ofNullable(inputUri).map(URI::create);
        }

        public Optional<Long> scaleFactor() {
            return Optional.ofNullable(scaleFactor).map(Long::valueOf);
        }

        public Optional<List<Long>> batchScales() {
            return Optional.ofNullable(batchScales).map(scalesString -> Arrays.stream(scalesString.split(",")).map(Long::valueOf).collect(Collectors.toList()));
        }
    }
}
