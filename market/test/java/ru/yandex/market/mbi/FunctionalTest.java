package ru.yandex.market.mbi;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.junit.JupiterDbUnitTest;

@SpringJUnitConfig(classes = FunctionalTestConfig.class)
public abstract class FunctionalTest extends JupiterDbUnitTest {
    @Autowired
    protected DataSource dataSource;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected TransactionTemplate transactionTemplate;
}
