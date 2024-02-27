package com.aerospike.graph.synth.cli;

import com.aerospike.graph.synth.process.tasks.generator.Generate;
import com.aerospike.movement.config.core.ConfigurationBase;
import com.aerospike.movement.encoding.files.csv.GraphCSVEncoder;
import com.aerospike.movement.encoding.tinkerpop.TinkerPopTraversalEncoder;
import com.aerospike.movement.output.files.DirectoryOutput;
import com.aerospike.movement.plugin.Plugin;
import com.aerospike.movement.plugin.tinkerpop.PluginServiceFactory;
import com.aerospike.movement.process.core.Task;
import com.aerospike.movement.runtime.core.local.LocalParallelStreamRuntime;
import com.aerospike.movement.tinkerpop.common.RemoteGraphTraversalProvider;
import com.aerospike.movement.util.core.Pair;
import com.aerospike.movement.util.core.configuration.ConfigUtil;
import com.aerospike.movement.util.core.iterator.ext.IteratorUtils;
import com.aerospike.movement.util.core.runtime.RuntimeUtil;
import com.aerospike.movement.util.files.FileUtil;
import org.apache.commons.configuration2.Configuration;
import org.apache.tinkerpop.gremlin.structure.Graph;

import java.net.URI;
import java.nio.file.Path;
import java.util.*;

import static com.aerospike.movement.config.core.ConfigurationBase.Keys.ENCODER;
import static com.aerospike.movement.config.core.ConfigurationBase.Keys.OUTPUT;


public class GraphSynthCLIPlugin extends Plugin {

    private Task task;

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
            return ConfigUtil.getKeysFromClass(Keys.class);
        }


        public static class Keys {
        }

        private static final Map<String, String> DEFAULTS = new HashMap<>() {{
        }};
    }


    private final CLI.GraphSynthCLI cli;

    public CLI.GraphSynthCLI getCommandLine() {
        return cli;
    }

    public static Plugin open(final Configuration configuration) {
        return new GraphSynthCLIPlugin(CLI.GraphSynthCLI.fromConfig(configuration), configuration);
    }

    public Iterator<Object> call() {


        if (cli.overrides().isPresent() && (!cli.overrides().get().isEmpty())) {
            final Map<String, String> overrides = new HashMap<>(cli.overrides().get());
            overrides.forEach(config::setProperty);
        }
        List.of(Generate.class).forEach(it -> RuntimeUtil.registerTaskAlias(it.getSimpleName(), it));

        String outputScheme = cli.outputUri().get().getScheme();
        if (outputScheme.equals("ws") || outputScheme.equals("wss")) {
            final String host = cli.outputUri().get().getHost();
            final Integer port = cli.outputUri().get().getPort();
            config.setProperty(TinkerPopTraversalEncoder.Config.Keys.TRAVERSAL_PROVIDER, RemoteGraphTraversalProvider.class.getName());
            config.setProperty(RemoteGraphTraversalProvider.Config.Keys.HOST, host);
            config.setProperty(RemoteGraphTraversalProvider.Config.Keys.PORT, port);
        } else if (outputScheme.equals("file")) {
            config.setProperty(ENCODER, GraphCSVEncoder.class.getName());
            config.setProperty(OUTPUT, DirectoryOutput.class.getName());
            config.setProperty(DirectoryOutput.Config.Keys.OUTPUT_DIRECTORY, cli.outputUri().get().toString());
        } else {
            throw new RuntimeException("output uri must have scheme file for csv output or ws/wss for Gremlin Server websocket connection");
        }

        final URI inputURI = cli.inputUri().orElseThrow(() -> new RuntimeException("Schema URI not set"));
        final URI outputURI = cli.outputUri().orElseThrow(() -> new RuntimeException("Output URI not set"));

        final Path inputPath = Path.of(inputURI);
        if (!inputPath.toFile().exists()) {
            throw new RuntimeException(inputPath + " does not exist");
        }

        final Path outputPath = Path.of(outputURI);
        if (outputPath.toFile().exists()) {
            FileUtil.recursiveDelete(outputPath);
        }
        if (!outputPath.toFile().mkdirs())
            throw new RuntimeException("could not create directory " + outputPath);


        if (cli.scaleFactor().isEmpty()){
            throw new RuntimeException("you must set a scale factor or provide a list of scale factors to batch");
        }

        Configuration cliconfig = CLI.GraphSynthCLI.taskConfig(cli,config);
        cliconfig.getKeys().forEachRemaining(key -> {
            config.setProperty(key, cliconfig.getString(key));
        });

        Configuration taskConfig = ConfigUtil.withOverrides(config, CLI.GraphSynthCLI.taskConfig(cli,config));
        task = (Task) RuntimeUtil.openClassRef(RuntimeUtil.getTaskClassByAlias(Generate.class.getSimpleName()).getName(), taskConfig);
        Pair<LocalParallelStreamRuntime, Iterator<Object>> x = runTask(task, taskConfig).orElseThrow(() -> new RuntimeException("Failed to run task: " + Generate.class.getSimpleName()));
        Iterator<Object> resultIterator = (Iterator<Object>) x.right;
        return resultIterator;
    }


    protected GraphSynthCLIPlugin(final CLI.GraphSynthCLI cli, Configuration configuration) {
        super(Config.INSTANCE, configuration);
        this.cli = cli;
    }

    @Override
    public Map<String, List<String>> api() {
        return IteratorUtils.consolidateToMap(Arrays.stream(CLI.GraphSynthCLI.ArgNames.class.getDeclaredFields()).map(f -> new AbstractMap.SimpleEntry<>(f.getName(), List.of(String.class.getSimpleName()))));
    }

    @Override
    public void plugInto(final Object system) {
        Graph graph = (Graph) system;
        final PluginServiceFactory psf = PluginServiceFactory.create(task, this, graph, config);
        graph.getServiceRegistry().registerService(psf);
    }

    @Override
    public void init(final Configuration config) {

    }

    @Override
    public void close() throws Exception {

    }
}


