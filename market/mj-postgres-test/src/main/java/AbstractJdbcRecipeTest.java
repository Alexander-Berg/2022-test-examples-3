package ru.yandex.market.javaframework.postgres.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.starter.postgres.config.PostgresAutoConfiguration;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = PostgresTestConfig.class)
@ImportAutoConfiguration(PostgresAutoConfiguration.class)
public abstract class AbstractJdbcRecipeTest {

    @Autowired
    protected JdbcTemplate jdbcTemplate;
}
