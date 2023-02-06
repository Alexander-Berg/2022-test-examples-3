package ru.yandex.market.axapta.revenue;

import java.time.Clock;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.mockito.Mockito.mock;

class AxaptaRevenueImportExecutorTest extends FunctionalTest {

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    final AxaptaRevenueImportService axaptaRevenueImportService = mock(AxaptaRevenueImportService.class);

    @Test
    @DbUnitDataSet(
            before = "AxaptaRevenueImportExecutorTest.testDoNotShiftImportDate.before.csv",
            after = "AxaptaRevenueImportExecutorTest.testDoNotShiftImportDate.after.csv"
    )
    void testDoNotShiftImportDate() {
        final Clock clock = Clock.fixed(
                DateTimes.toInstantAtDefaultTz(2020, 5, 6, 15, 0, 0),
                ZoneId.systemDefault()
        );
        AxaptaRevenueImportExecutor axaptaRevenueImportExecutor = new AxaptaRevenueImportExecutor(clock,
                environmentService, transactionTemplate, axaptaRevenueImportService);

        axaptaRevenueImportExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "AxaptaRevenueImportExecutorTest.testShiftImportDate.before.csv",
            after = "AxaptaRevenueImportExecutorTest.testShiftImportDate.after.csv"
    )
    void testShiftImportDate() {
        final Clock clock = Clock.fixed(
                DateTimes.toInstantAtDefaultTz(2020, 5, 10, 15, 0, 0),
                ZoneId.systemDefault()
        );
        AxaptaRevenueImportExecutor axaptaRevenueImportExecutor = new AxaptaRevenueImportExecutor(clock,
                environmentService, transactionTemplate, axaptaRevenueImportService);

        axaptaRevenueImportExecutor.doJob(null);
    }
}
