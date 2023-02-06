package ru.yandex.market.copy.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.copy.CopyExecutor;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static ru.yandex.market.copy.AbstractCopyService.ENV_COPY_ENABLED;
import static ru.yandex.market.copy.AbstractCopyService.ENV_READ_CHUNK_SIZE;
import static ru.yandex.market.copy.AbstractCopyService.ENV_WRITE_CHUNK_SIZE;

public class MarketPpGroupsCopyExecutorTest extends FunctionalTest {

    @Autowired
    CopyExecutor marketPpGroupsCopyExecutor;

    @Autowired
    EnvironmentService environmentService;

    @BeforeEach
    public void beforeEach() {
        environmentService.setValue(String.format(ENV_COPY_ENABLED, "market_pp_groups"), "true");
    }

    @Test
    @DbUnitDataSet(
            before = "csv/MarketPpGroupsCopyExecutorTest.copy.before.csv",
            after = "csv/MarketPpGroupsCopyExecutorTest.copy.after.csv"
    )
    @DisplayName("Проверка копирования таблицы shops_web.market_pp_groups из шопсового PG в Oracle")
    void copy() {
        marketPpGroupsCopyExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "csv/MarketPpGroupsCopyExecutorTest.copy.before.csv",
            after = "csv/MarketPpGroupsCopyExecutorTest.copy.before.csv"
    )
    @DisplayName("Копирование таблицы shops_web.market_pp_groups из шопсового PG в Oracle не работает," +
            " когда enabled != true")
    void doesntWorkWhenDisabled() {
        environmentService.setValue(String.format(ENV_COPY_ENABLED, "market_pp_groups"), "false");
        marketPpGroupsCopyExecutor.doJob(null);
        environmentService.removeAllValues(String.format(ENV_COPY_ENABLED, "market_pp_groups"));
        marketPpGroupsCopyExecutor.doJob(null);
    }
}
