package ru.yandex.market.common.postgres.test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import de.flapdoodle.embed.process.distribution.IVersion;
import org.apache.commons.lang3.SystemUtils;

import ru.yandex.qatools.embed.postgresql.Command;
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;
import ru.yandex.qatools.embed.postgresql.distribution.Version;

/**
 * Created by antipov93@yndx-team.ru
 */
public class PGConfigBuilder {

    private static final IVersion DEFAULT_VERSION = Version.Main.PRODUCTION;
    private static final String DEFAULT_DB_PROPERTY = "postgres";
    private static final List<String> DEFAULT_PARAMS = Arrays.asList(
            "-E", "UTF-8",
            "--locale=en_US.UTF-8",
            "--lc-collate=en_US.UTF-8",
            "--lc-ctype=en_US.UTF-8"
    );
    private static final List<String> WINDOWS_PARAMS = Arrays.asList(
            "-E", "UTF-8",
            "--locale=en-US",
            "--lc-collate=en-US",
            "--lc-ctype=en-US"
    );

    /**
     * Версия Postgres. По умолчанию будет взята вресия 10.3.
     */
    private IVersion version;
    /**
     * Название базы данных, по умолчанию "postgres".
     */
    private String databaseName;
    /**
     * Имя пользоватля, по умолчанию "postgres".
     */
    private String user;
    /**
     * Пароль пользоватля, по умолчанию "postgres".
     */
    private String password;

    public PostgresConfig build() throws IOException {
        PostgresConfig config = new PostgresConfig(
                Optional.ofNullable(version).orElse(DEFAULT_VERSION),
                new AbstractPostgresConfig.Net(),
                new AbstractPostgresConfig.Storage(
                        Optional.ofNullable(databaseName).orElse(DEFAULT_DB_PROPERTY) +
                        System.currentTimeMillis()),
                new AbstractPostgresConfig.Timeout(),
                new AbstractPostgresConfig.Credentials(
                        Optional.ofNullable(user).orElse(DEFAULT_DB_PROPERTY),
                        Optional.ofNullable(password).orElse(DEFAULT_DB_PROPERTY)),
                Command.PgCtl);
        // pass info regarding encoding, locale, collate, ctype, instead of setting global environment settings
        config.getAdditionalInitDbParams().addAll(SystemUtils.IS_OS_WINDOWS ? WINDOWS_PARAMS : DEFAULT_PARAMS);
        return config;
    }

    public PGConfigBuilder setVersion(IVersion version) {
        this.version = version;
        return this;
    }

    public PGConfigBuilder setVersion(String version) {
        this.version = () -> version;
        return this;
    }

    public PGConfigBuilder setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
        return this;
    }

    public PGConfigBuilder setUser(String user) {
        this.user = user;
        return this;
    }

    public PGConfigBuilder setPassword(String password) {
        this.password = password;
        return this;
    }
}
