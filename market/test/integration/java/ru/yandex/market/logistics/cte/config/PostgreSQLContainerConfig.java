package ru.yandex.market.logistics.cte.config;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.logistics.cte.base.QueriesCountInspector;
import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration;
import ru.yandex.market.logistics.test.integration.db.zonky.EnablePooledZonkyEmbeddedPostgres;

@Configuration
@Slf4j
@EnablePooledZonkyEmbeddedPostgres
@Import({
        DbUnitTestConfiguration.class
})
public class PostgreSQLContainerConfig {

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
            s.execute("CREATE ROLE market_fulfillment_cte WITH LOGIN SUPERUSER PASSWORD 'market_fulfillment_cte'");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return embeddedPostgres;
    }


    @Bean
    public DbConnectionProperties containerPostgresDbConnectionProperties(EmbeddedPostgres postgres) {
        return new DbConnectionProperties(
                postgres.getJdbcUrl("postgres", "postgres"),
                "postgres",
                "postgres",
                POSTGRESQL_DRIVER,
                "hibernate.session_factory.statement_inspector=" + QueriesCountInspector.class.getName()
        );
    }
}
