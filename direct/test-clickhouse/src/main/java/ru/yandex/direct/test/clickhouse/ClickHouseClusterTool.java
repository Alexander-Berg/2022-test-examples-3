package ru.yandex.direct.test.clickhouse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.lang3.StringUtils;

import ru.yandex.direct.clickhouse.ClickHouseCluster;
import ru.yandex.direct.clickhouse.ClickHouseClusterBuilder;
import ru.yandex.direct.process.Docker;
import ru.yandex.direct.process.DockerContainer;
import ru.yandex.direct.process.DockerHostUserEntryPoint;
import ru.yandex.direct.utils.GracefulShutdownHook;
import ru.yandex.direct.utils.io.TempDirectory;

@ParametersAreNonnullByDefault
@SuppressWarnings("squid:S106") // Умышленное использование System.err
public class ClickHouseClusterTool {
    public static void main(String[] args) throws IOException {
        CommonArgs commonArgs = new CommonArgs();
        ConfigArgs configArgs = new ConfigArgs();
        RunArgs runArgs = new RunArgs();
        JCommander jcommander = new JCommander();
        jcommander.addObject(commonArgs);
        jcommander.addCommand("config", configArgs);
        jcommander.addCommand("run", runArgs);
        jcommander.parse(args);

        if (commonArgs.help) {
            jcommander.usage();
            System.exit(1);
        }

        String s = jcommander.getParsedCommand();
        if (Objects.equals(s, "config")) {
            System.exit(doConfig(configArgs));
        } else if (Objects.equals(s, "run")) {
            System.exit(doRun(commonArgs, runArgs));
        } else {
            jcommander.usage();
            System.exit(1);
        }
    }

    private static int doConfig(ConfigArgs configArgs) throws IOException {
        Path configPath = Paths.get(configArgs.configPath);
        if (configPath.toFile().exists()) {
            System.err.printf("File %s already exists. Will not rewrite it.%n",
                    configPath.normalize().toString());
            System.exit(1);
        }

        Config config = Config.fromArgs(configArgs);

        config.builder
                .withZooKeeper(1, "zookeeper01")
                .withZooKeeper(2, "zookeeper02")
                .withZooKeeper(3, "zookeeper03")
                .withClickHouse("clickhouse01")
                .withClickHouse("clickhouse02")
                .withClickHouse("clickhouse03")
                .withClickHouse("clickhouse04");

        config.builder.shardGroup()
                .withShard(1, "clickhouse01", "clickhouse02")
                .withShard(2, "clickhouse03", "clickhouse04");

        try (Writer writer = new BufferedWriter(new FileWriter(configArgs.configPath))) {
            writer.write(new ObjectMapper()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .writeValueAsString(config));
            writer.write('\n');
        }

        System.err.printf("Sample config written into %s%nEdit it and run the cluster.%n",
                Paths.get(configArgs.configPath).normalize().toAbsolutePath().toString());
        return 0;
    }

    @SuppressWarnings("squid:S1148")
    private static int doRun(CommonArgs commonArgs, RunArgs runArgs) throws IOException {
        int exitCode = 0;
        Config config;
        try (Reader reader = new BufferedReader(new FileReader(runArgs.configPath))) {
            config = new ObjectMapper()
                    .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                    .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                    .readValue(reader, Config.class);
        }
        config.builder.forceIpv6(runArgs.forceIpv6);

        Path tmpDirPath = Paths.get(commonArgs.tmpVolumePath);
        Files.createDirectories(tmpDirPath);
        try (TempDirectory tmpdir = new TempDirectory(tmpDirPath, config.clusterName);
             GracefulShutdownHook ignored = new GracefulShutdownHook(Duration.ofHours(1));
             ClickHouseCluster cluster = config.builder.build(tmpdir, new Docker(), config.clusterName,
                     config.dataRootPath != null ? Paths.get(config.dataRootPath) : null)) {
            try {
                System.err.println("Cluster name: " + cluster.getClusterName());
                cluster.getClickHouseJdbcUrls().entrySet().stream()
                        .sorted(Comparator.comparing(Map.Entry::getKey))
                        .forEachOrdered(e -> System.out.printf("%s: %s%n", e.getKey(), e.getValue()));
                createDbConfigs(config, cluster, runArgs.dbConfigHostName);
                observeInLoopThatItWorks(cluster);
            } catch (InterruptedException e) {
                System.err.println("Terminating...");
                throw e;
            } catch (RuntimeException e) {
                e.printStackTrace(System.err);
                exitCode = 1;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return exitCode;
    }

    private static void createDbConfigs(Config config, ClickHouseCluster cluster, @Nullable String dbConfigHostName)
            throws IOException {
        for (Config.DbConfig dbConfigGenInfo : config.dbConfigs) {
            if (StringUtils.isEmpty(dbConfigGenInfo.srcPath)) {
                throw new IllegalStateException("Empty db_configs.?.src_path");
            }
            if (StringUtils.isEmpty(dbConfigGenInfo.injectTo)) {
                throw new IllegalStateException("Empty db_configs.?.inject_to");
            }
            if (StringUtils.isEmpty(dbConfigGenInfo.destPath)) {
                throw new IllegalStateException("Empty db_configs.?.dest_path");
            }
            try (Reader reader = new BufferedReader(new FileReader(dbConfigGenInfo.srcPath));
                 Writer writer = new BufferedWriter(new FileWriter(dbConfigGenInfo.destPath))) {
                ClickHouseClusterDbConfig.generateDbConfig(
                        config.builder, cluster, reader, dbConfigGenInfo.injectTo,
                        dbConfigHostName, "default", writer);
            }
            System.err.printf("Db config generation:%n  %s%n  ->%n  %s%n",
                    Paths.get(dbConfigGenInfo.srcPath).normalize().toAbsolutePath().toString(),
                    Paths.get(dbConfigGenInfo.destPath).normalize().toAbsolutePath().toString());
        }
    }

    private static void observeInLoopThatItWorks(ClickHouseCluster cluster) throws InterruptedException {
        try {
            cluster.awaitConnectivity(Duration.ofSeconds(20));
            System.err.println("Cluster is ready to operate. CTRL-C to terminate.");
            while (!Thread.interrupted()) {
                Thread.sleep(3000);
                cluster.awaitConnectivity(Duration.ofSeconds(10));
            }
        } catch (RuntimeException e) {
            System.err.println("***********\n"
                    + "Sudden error.\n"
                    + "See https://a.yandex-team.ru/arc/trunk/arcadia/direct/libs/test-clickhouse"
                    + " for troubleshooting.\n"
                    + "Tail of every container log:");
            cluster.readContainersStderr(DockerContainer.Tail.lines(30), Duration.ofSeconds(20)).entrySet().stream()
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .forEachOrdered(entry -> System.err.printf("*** %s ***%n%s%n%n",
                            entry.getKey(), entry.getValue().orElse("<empty log>")));
            throw e;
        }
    }

    static class Config {
        @JsonProperty("cluster_name")
        String clusterName;

        @JsonProperty("data_root_path")
        String dataRootPath;

        @JsonProperty("db_configs")
        Collection<DbConfig> dbConfigs = Collections.emptyList();

        @JsonProperty("config")
        ClickHouseClusterBuilder builder;

        static Config fromArgs(ConfigArgs args) {
            Config result = new Config();
            result.clusterName = args.clusterName.get(0);
            result.dataRootPath = args.dataRootPath;
            result.builder = new ClickHouseClusterBuilder();
            return result;
        }

        static class DbConfig {
            /**
             * Путь к json-файлу с исходным dbconfig
             */
            @JsonProperty("src_path")
            String srcPath;

            /**
             * dbconfig-путь, в который вставить информацию о кластере. Например, "ppchouse:user_action_log".
             */
            @JsonProperty("inject_to")
            String injectTo;

            /**
             * Путь, куда записать новый dbconfig.json
             */
            @JsonProperty("dest_path")
            String destPath;
        }
    }

    @Parameters
    static class CommonArgs {
        @Parameter(names = {"-h", "--help"}, help = true)
        boolean help;

        // С настройками по умолчанию Docker for Mac не может примонтировать volume из /tmp, только из /Users
        @Parameter(names = {"-T", "--tmp-volume-dir"},
                description = "Host directory used for generating temporary files")
        String tmpVolumePath = DockerHostUserEntryPoint.IS_MACOS
                ? Paths.get("./tmp").normalize().toAbsolutePath().toString()
                : "/tmp";
    }

    @Parameters(separators = "=", commandDescription = "Generate ClickHouse cluster config file")
    static class ConfigArgs {
        static final String DEFAULT_CONFIG_PATH = "clickhouse_cluster.json";

        @Parameter(names = {"-c", "--config-path"}, description = "Path to file where config will be written")
        String configPath = DEFAULT_CONFIG_PATH;

        // Баг в используемой версии jcommander: для одного main-аргумента приходится указывать список вместо строки.
        @Parameter(description = "<cluster name>", required = true, arity = 1)
        List<String> clusterName;

        @Parameter(names = {"-d", "--data-dir"},
                description = "Host directory where containers will store their data")
        String dataRootPath = null;
    }

    @Parameters(separators = "=", commandDescription = "Run ClickHouse cluster in Docker")
    static class RunArgs {
        @Parameter(names = {"-c", "--config-path"}, description = "Path to config file")
        String configPath = ConfigArgs.DEFAULT_CONFIG_PATH;

        @Nullable
        @Parameter(names = {"--ipv6"},
                description = "true/false: Force use or no use of IPv6. Auto detection by default",
                arity = 1)
        Boolean forceIpv6 = null;

        @Nullable
        @Parameter(names = {"--dbconfig-hostname"},
                description = "Specify hostname for injecting dbconfig. Localhost by default.")
        String dbConfigHostName;
    }
}
