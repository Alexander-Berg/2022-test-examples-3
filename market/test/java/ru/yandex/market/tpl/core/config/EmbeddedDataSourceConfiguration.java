package ru.yandex.market.tpl.core.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;

import javax.sql.DataSource;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author ungomma
 */
@EnableTransactionManagement(proxyTargetClass = true)
@Configuration
@Slf4j
public class EmbeddedDataSourceConfiguration {

    private static final String DEFAULT_PG_DB_NAME = "postgres";
    private static final String DEFAULT_PG_USERNAME = "postgres";

    private final String schema;

    public EmbeddedDataSourceConfiguration(@Value("${tpl.jdbc.schema}") final String schema) {
        this.schema = schema;
    }

    @Bean
    @SneakyThrows
    public DataSource dataSource(
            @Value("${tpl.postgresql.use.recipe:false}") boolean useRecipe
    ) {
        String jdbcUrl;
        String userName = DEFAULT_PG_USERNAME;
        String password = DEFAULT_PG_USERNAME;
        if (useRecipe) {
            int port = Integer.parseInt(System.getenv("PG_LOCAL_PORT"));
            String database = System.getenv("PG_LOCAL_DATABASE");
            userName = System.getenv("PG_LOCAL_USER");
            password = System.getenv("PG_LOCAL_PASSWORD");

            jdbcUrl = String.format(
                    "jdbc:postgresql://localhost:%s/%s?user=%s",
                    port,
                    database,
                    userName
            );
        } else {
            EmbeddedPostgres embeddedPostgres =
                    EmbeddedPostgres.builder()
                            .setServerConfig("jit","off")
                            .setPGStartupWait(Duration.ofSeconds(30)).start();
            jdbcUrl = embeddedPostgres.getJdbcUrl(DEFAULT_PG_DB_NAME, DEFAULT_PG_DB_NAME);
        }

        var dataSource = new BasicDataSource();
        dataSource.setUsername(userName);
        dataSource.setPassword(password);
        dataSource.setDriverClassName("org.postgresql.Driver");

        dataSource.setUrl(jdbcUrl);
        log.info("Test database connection URL: {}", jdbcUrl);

        dataSource.setDefaultSchema(schema);
        try (var connection = dataSource.getConnection()) {
            createSchema(connection, schema);
        }

        return dataSource;
    }

    private void createSchema(Connection connection, String schemaName) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("CREATE SCHEMA IF NOT EXISTS " + schemaName);
        ps.executeUpdate();
        ps.close();
    }

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

}
