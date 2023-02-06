package ru.yandex.market.supportwizard.config;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.common.postgres.test.PGConfigBuilder;
import ru.yandex.market.common.postgres.test.PGEmbeddedDatabase;
import ru.yandex.market.common.postgres.test.PGEmbeddedDatasource;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;

@Configuration
public class EmbeddedPostgresConfiguration {

    private static final String POSTGRES_VERSION = "10.5-1";

    @Value("supplier_wizard")
    private String username;

    @Value("supplier_wizard")
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

    @Bean(name = "pgEmbeddedDatabase")
    public PGEmbeddedDatabase pgEmbeddedDatabase() {
        return new PGEmbeddedDatabase(
                postgresConfig(),
                new File("").getAbsolutePath()
        );
    }

    @Bean
    @Primary
    @DependsOn("pgEmbeddedDatabase")
    public DataSource dataSource() {
        return new PGEmbeddedDatasource(postgresConfig());
    }

    @Bean(name = "tmsDataSource")
    @DependsOn("dataSource")
    public DataSource tmsDataSource() {
        return dataSource();
    }
}
