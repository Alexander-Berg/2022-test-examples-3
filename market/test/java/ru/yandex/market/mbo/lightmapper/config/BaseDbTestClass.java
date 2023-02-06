package ru.yandex.market.mbo.lightmapper.config;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;

/**
 * @author yuramalinov
 * @created 10.10.18
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    initializers = PGaaSZonkyInitializer.class,
    classes = {DbTestConfiguration.class}
)
@Transactional
public abstract class BaseDbTestClass {
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    protected NamedParameterJdbcTemplate namedJdbcTemplate;
    @Autowired
    protected TransactionHelper transactionHelper;
    @Autowired
    protected TransactionTemplate transactionTemplate;
}
