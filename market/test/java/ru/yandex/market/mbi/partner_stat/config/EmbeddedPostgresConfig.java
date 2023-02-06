package ru.yandex.market.mbi.partner_stat.config;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.common.postgres.test.PGConfigBuilder;
import ru.yandex.market.common.postgres.test.PGEmbeddedDatabase;
import ru.yandex.market.common.postgres.test.PGEmbeddedDatasource;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;

/**
 * Конфиг для EmbeddedPostgres.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@Configuration
@ParametersAreNonnullByDefault
public class EmbeddedPostgresConfig {
    public static final String DATA_SOURCE = "postgresDataSource";
    private static final String POSTGRES_VERSION = "10.5-1";

    private final String username;
    private final String password;
    private final String schema;

    public EmbeddedPostgresConfig(@Value("${mbi.partner_stat.db.username}") final String username,
                                  @Value("${mbi.partner_stat.db.password}") final String password,
                                  @Value("${mbi.partner_stat.db.schema}") final String schema) {
        this.username = username;
        this.password = password;
        this.schema = schema;
    }

    @Bean
    public PostgresConfig postgresConfig() {
        try {
            return new PGConfigBuilder()
                    .setVersion(POSTGRES_VERSION)
                    .setUser(username)
                    .setPassword(password)
                    .build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Bean
    public PGEmbeddedDatabase pgEmbeddedDatabase() {
        return new PGEmbeddedDatabase(
                postgresConfig(),
                new File("").getAbsolutePath()
        );
    }

    @Primary
    @Bean(name = {DATA_SOURCE, "dataSource"})
    @DependsOn("pgEmbeddedDatabase")
    public PGEmbeddedDatasource postgresDataSource() {
        return new PGEmbeddedDatasource(postgresConfig(), schema);
    }
}
