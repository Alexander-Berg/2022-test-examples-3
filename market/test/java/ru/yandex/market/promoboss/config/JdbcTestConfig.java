package ru.yandex.market.promoboss.config;

import javax.sql.DataSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

@TestConfiguration
public class JdbcTestConfig {
    @Bean
    public SimpleJdbcInsert testSimpleJdbcInsert(DataSource dataSource) {
        return new SimpleJdbcInsert(dataSource);
    }

}
