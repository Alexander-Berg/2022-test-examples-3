package ru.yandex.market.billing.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;

public class SqlJobsConfigTest extends FunctionalTest {

    @Autowired
    private SQLJobsConfig sqlJobsConfig;

    @Test
    void smokeSaveShopManagersIntoHistoryExecutorTest() {
        sqlJobsConfig.saveShopManagersIntoHistoryExecutor().doJob(null);
    }
}
