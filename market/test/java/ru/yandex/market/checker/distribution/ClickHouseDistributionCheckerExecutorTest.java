package ru.yandex.market.checker.distribution;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.environment.EnvironmentService;

/**
 * Тесты для {@link ClickHouseDistributionCheckerExecutor}.
 */
class ClickHouseDistributionCheckerExecutorTest extends FunctionalTest {

    @Autowired
    private ClickHouseDistributionCheckerDao realCheckerDao;

    @Autowired
    private EnvironmentService environmentService;

    private ClickHouseDistributionCheckerExecutor executor;

    @BeforeEach
    void setUp() {
        var spyDao = Mockito.spy(realCheckerDao);
        Mockito.doReturn(20)
                .when(spyDao)
                .getClickhouseOrderCreatedDelay();

        this.executor = new ClickHouseDistributionCheckerExecutor(spyDao, environmentService);
    }

    @Test
    @DisplayName("Тест технических обновлений данных для мониторинга")
    @DbUnitDataSet(
            before = "ClickHouseDistributionCheckerExecutorTest.update.before.csv",
            after = "ClickHouseDistributionCheckerExecutorTest.update.after.csv"
    )
    void updateDelayTest() {
        executor.doJob(null);
    }
}
