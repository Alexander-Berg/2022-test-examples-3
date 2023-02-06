package ru.yandex.market.mbi.tariffs.config;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import net.ttddyy.dsproxy.listener.logging.OutputParameterLogEntryCreator;
import net.ttddyy.dsproxy.listener.logging.SystemOutQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.common.postgres.test.PGConfigBuilder;
import ru.yandex.market.common.postgres.test.PGEmbeddedDatabase;
import ru.yandex.market.common.postgres.test.PGEmbeddedDatasource;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;

/**
 * Конфиг для постгреса
 */
@Configuration
public class EmbeddedPostgresConfig {
    private static final String POSTGRES_VERSION = "10.5-1";

    @Bean
    public PostgresConfig postgresConfig() {
        try {
            return new PGConfigBuilder()
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

    @Bean(name = {"postgresDataSource", "dataSource"})
    @DependsOn("pgEmbeddedDatabase")
    public DataSource dataSource() {
        SystemOutQueryLoggingListener listener = new SystemOutQueryLoggingListener();
        OutputParameterLogEntryCreator queryLogEntryCreator = new OutputParameterLogEntryCreator();
        queryLogEntryCreator.setMultiline(true);
        listener.setQueryLogEntryCreator(queryLogEntryCreator);

        return new ProxyDataSourceBuilder()
                .dataSource(new PGEmbeddedDatasource(postgresConfig()))
                .name("EmbeddedDatabase")
                .listener(listener)
                .build();
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(dataSource());
    }

    @Bean
    public SpringLiquibase liquibase() {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource());
        liquibase.setChangeLog("classpath:changelog_without_data.xml");
        return liquibase;
    }
}
