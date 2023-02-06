package ru.yandex.market.shop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.billing.FunctionalTest;

class ShopWorkingHoursScheduleExportExecutorTest extends FunctionalTest {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ShopWorkingHoursScheduleExportExecutor executor;

    @Test
    void getLoadQuery() {
        // smoke test
        jdbcTemplate.execute(      executor.getLoadQuery());
    }
}
