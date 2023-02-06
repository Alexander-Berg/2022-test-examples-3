package ru.yandex.market.common.postgres.test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.io.Slf4jLevel;
import de.flapdoodle.embed.process.io.Slf4jStreamProcessor;
import de.flapdoodle.embed.process.io.directories.FixedPath;
import de.flapdoodle.embed.process.io.directories.IDirectory;
import de.flapdoodle.embed.process.io.directories.UserHome;
import de.flapdoodle.embed.process.store.PostgresArtifactStoreBuilder;
import org.slf4j.LoggerFactory;

import ru.yandex.qatools.embed.postgresql.Command;
import ru.yandex.qatools.embed.postgresql.PostgresExecutable;
import ru.yandex.qatools.embed.postgresql.PostgresProcess;
import ru.yandex.qatools.embed.postgresql.PostgresStarter;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;
import ru.yandex.qatools.embed.postgresql.config.PostgresDownloadConfigBuilder;
import ru.yandex.qatools.embed.postgresql.config.RuntimeConfigBuilder;
import ru.yandex.qatools.embed.postgresql.ext.LogWatchStreamProcessor;

/**
 * Created by antipov93@yndx-team.ru
 */
public class PGEmbeddedDatabase {
    public static final UserHome USER_HOME = new UserHome(".embedpostgresql");

    private PostgresProcess process;

    private final PostgresConfig config;
    private final IDirectory artifactStorePath;

    public PGEmbeddedDatabase(PostgresConfig config) {
        this(config, USER_HOME);
    }

    public PGEmbeddedDatabase(PostgresConfig config, String artifactStorePath) {
        this(config, new FixedPath(artifactStorePath));
    }

    public PGEmbeddedDatabase(PostgresConfig config, IDirectory artifactStorePath) {
        this.config = config;
        this.artifactStorePath = artifactStorePath;
    }

    @PostConstruct
    public void init() throws IOException {
        PostgresStarter<PostgresExecutable, PostgresProcess> runtime = PostgresStarter.getInstance(runtimeConfig());
        PostgresExecutable exec = runtime.prepare(config);
        process = exec.start();
    }

    @PreDestroy
    public void close() {
        if (process != null) {
            process.stop();
        }
    }

    private IRuntimeConfig runtimeConfig() {
        Command cmd = Command.PgCtl;
        LogWatchStreamProcessor logWatch = new LogWatchStreamProcessor(
                "started", new HashSet<>(Collections.singletonList("failed")),
                new Slf4jStreamProcessor(LoggerFactory.getLogger("postgres"), Slf4jLevel.TRACE));
        return new RuntimeConfigBuilder()
                .defaults(cmd)
                .artifactStore(new PostgresArtifactStoreBuilder()
                        .defaults(cmd)
                        .download(new PostgresDownloadConfigBuilder()
                                .defaultsForCommand(cmd)
                                .artifactStorePath(artifactStorePath)
                                .build()))
                .processOutput(new ProcessOutput(logWatch, logWatch, logWatch))
                .build();
    }
}
