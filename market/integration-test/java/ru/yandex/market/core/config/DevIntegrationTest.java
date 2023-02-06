package ru.yandex.market.core.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.junit.JupiterDbUnitTest;
import ru.yandex.market.core.favicon.FaviconClientConfig;

@SpringJUnitConfig({
        DevJdbcConfig.class,
        FaviconClientConfig.class,
})
public abstract class DevIntegrationTest extends JupiterDbUnitTest {
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    protected TransactionTemplate transactionTemplate;
}
