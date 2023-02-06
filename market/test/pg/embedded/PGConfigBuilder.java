package ru.yandex.market.test.pg.embedded;

import java.io.IOException;
import java.util.Arrays;

import de.flapdoodle.embed.process.distribution.IVersion;

import ru.yandex.qatools.embed.postgresql.Command;
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;
import ru.yandex.qatools.embed.postgresql.distribution.Version;

/**
 * Created by antipov93@yndx-team.ru
 */
public class PGConfigBuilder {

    private final IVersion version;
    private String databaseName = "embedded";
    private String user = "postgres";
    private String password = "postgres";

    public PGConfigBuilder() {
        this(Version.Main.PRODUCTION);
    }

    public PGConfigBuilder(String version) {
        this(() -> version);
    }

    public PGConfigBuilder(IVersion version) {
        this.version = version;
    }

    public PostgresConfig build() throws IOException {
        PostgresConfig config = new PostgresConfig(
                version,
                new AbstractPostgresConfig.Net(),
                new AbstractPostgresConfig.Storage(databaseName + System.currentTimeMillis()),
                new AbstractPostgresConfig.Timeout(),
                new AbstractPostgresConfig.Credentials(user, password),
                Command.PgCtl);
        // pass info regarding encoding, locale, collate, ctype, instead of setting global environment settings
        config.getAdditionalInitDbParams().addAll(Arrays.asList(
                "-E", "UTF-8",
                "--locale=en_US.UTF-8",
                "--lc-collate=en_US.UTF-8",
                "--lc-ctype=en_US.UTF-8"
        ));
        return config;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

