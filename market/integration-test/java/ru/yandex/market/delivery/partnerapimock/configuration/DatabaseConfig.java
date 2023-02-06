package ru.yandex.market.delivery.partnerapimock.configuration;


import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres;

@ConditionalOnProperty(prefix = "liquibase", name = "enabled", matchIfMissing = true)
@Configuration
public class DatabaseConfig {

    @Bean
    public DataSource dataSource(EmbeddedPostgres postgres) {
        return DataSourceBuilder
            .create()
            .url(postgres.getConnectionUrl().orElseThrow(RuntimeException::new))
            .build();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public EmbeddedPostgres embeddedPostgres() {
        return new EmbeddedPostgres();
    }
}
