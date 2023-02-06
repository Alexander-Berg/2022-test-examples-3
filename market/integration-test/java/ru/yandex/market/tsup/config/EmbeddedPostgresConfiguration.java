package ru.yandex.market.tsup.config;

import java.sql.SQLException;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

import ru.yandex.market.logistics.test.integration.db.zonky.ZonkyEmbeddedPostgresConfiguration;


@Slf4j
@Configuration
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class })
public class EmbeddedPostgresConfiguration {

    @Bean
    @Order(0)
    @Primary
    @ConditionalOnProperty(name = "embedded.postgres", havingValue = "docker")
    public DataSource dataSource(@Value("${embedded.postgres.docker.port}") String pgPort,
                                 @Value("${embedded.postgres.docker.host}") String host,
                                 @Value("${embedded.postgres.docker.username}") String username,
                                 @Value("${embedded.postgres.docker.password}") String password) throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%s/postgres", host, pgPort));
        config.setDriverClassName("org.postgresql.Driver");
        config.setUsername(username);
        config.setPassword(password);

        var dataSource = new HikariDataSource(config);
        try (var conn = dataSource.getConnection()) {
            log.info("Dropping schemas...");
            conn.setAutoCommit(false);
            conn.prepareStatement("DROP SCHEMA public CASCADE;").execute();
            conn.prepareStatement("CREATE SCHEMA public;").execute();
            conn.prepareStatement("DROP SCHEMA dbqueue CASCADE;").execute();
            conn.prepareStatement("CREATE SCHEMA dbqueue;").execute();
            conn.prepareStatement("DROP SCHEMA qrtz CASCADE;").execute();
            conn.prepareStatement("CREATE SCHEMA qrtz;").execute();
            conn.commit();
        }

        return new HikariDataSource(config);
    }

    @Configuration
    @Import(ZonkyEmbeddedPostgresConfiguration.class)
    @ConditionalOnProperty(name = "embedded.postgres", havingValue = "zonky", matchIfMissing = true)
    class EmbeddedZonkyConfiguration {

    }
}

