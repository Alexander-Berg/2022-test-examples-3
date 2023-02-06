package ru.yandex.market.marketId.config;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import ru.yandex.market.common.postgres.test.PGConfigBuilder;
import ru.yandex.market.common.postgres.test.PGEmbeddedDatabase;
import ru.yandex.market.common.postgres.test.PGEmbeddedDatasource;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;

@Configuration
public class EmbeddedPostgresConfiguration {

    private static final String POSTGRES_VERSION = "10.5-1";

    @Value("market_id")
    private String username;

    @Value("market_id")
    private String password;

    @Bean
    public PostgresConfig postgresConfig() {
        try {
            return new PGConfigBuilder()
                    .setUser(username)
                    .setPassword(password)
                    .setVersion(POSTGRES_VERSION)
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

    @Bean
    @DependsOn("pgEmbeddedDatabase")
    public PGEmbeddedDatasource dataSource() {
        return new PGEmbeddedDatasource(postgresConfig());
    }
}
