package ru.yandex.market.ff.configuration;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;

import com.google.common.collect.ImmutableMap;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.ff.config.properties.JpaMasterConfigProperties;
import ru.yandex.market.ff.config.properties.JpaReplicaConfigProperties;
import ru.yandex.market.ff.util.query.count.QueriesCountInspector;
import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration;
import ru.yandex.market.logistics.test.integration.db.LiquibaseTestConfiguration;
import ru.yandex.market.logistics.test.integration.db.zonky.EnablePooledZonkyEmbeddedPostgres;


@Configuration
@EnablePooledZonkyEmbeddedPostgres
@Import({
    LiquibaseTestConfiguration.class,
    DbUnitTestConfiguration.class
})
public class PostgreSQLContainerConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PostgreSQLContainerConfiguration.class);
    private static final String POSTGRESQL_DRIVER = "org.postgresql.Driver";
    private static final Duration STARTUP_WAIT = Duration.ofSeconds(30);
    private static final int EMBEDDED_POSTGRES_START_RETRIES_COUNT = 3;

    @Bean
    public EmbeddedPostgres postgres() throws IOException {
        int currentRetry = 0;
        EmbeddedPostgres embeddedPostgres = null;
        while (currentRetry < EMBEDDED_POSTGRES_START_RETRIES_COUNT) {
            try {
                embeddedPostgres = EmbeddedPostgres.builder().setPGStartupWait(STARTUP_WAIT).start();
                log.info("Embedded postgres started at port: {}", embeddedPostgres.getPort());
                break;
            } catch (Exception e) {
                currentRetry++;
                if (currentRetry == EMBEDDED_POSTGRES_START_RETRIES_COUNT) {
                    throw e;
                }
            }
        }

        try (Connection c = embeddedPostgres.getPostgresDatabase().getConnection()) {

            Statement s = c.createStatement();
            s.execute("CREATE ROLE market_ff_workflow WITH LOGIN SUPERUSER PASSWORD 'market_ff_workflow'");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return embeddedPostgres;
    }

    @Bean
    public JpaMasterConfigProperties jpaMasterConfigProperties(EmbeddedPostgres postgres) {
        return new JpaMasterConfigProperties(
                postgres.getJdbcUrl("postgres", "postgres"),
                "postgres",
                "postgres",
                POSTGRESQL_DRIVER,
                ImmutableMap.of(
                        "hibernate.session_factory.statement_inspector",
                        QueriesCountInspector.class.getName()
                ));
    }

    @Bean
    public JpaReplicaConfigProperties jpaReplicaConfigProperties(EmbeddedPostgres postgres) {
        return new JpaReplicaConfigProperties(
                postgres.getJdbcUrl("postgres", "postgres"),
                "postgres",
                "postgres",
                POSTGRESQL_DRIVER,
                ImmutableMap.of(
                        "hibernate.session_factory.statement_inspector",
                        QueriesCountInspector.class.getName()
                ));
    }
}
