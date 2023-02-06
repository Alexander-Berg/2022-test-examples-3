package ru.yandex.market.billing.tasks.cutoff;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.finance.FinanceCutoffExecutor;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.notification.service.NotificationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class FinanceCutoffExecutorTest extends FunctionalTest {
    @Autowired
    private NotificationService notificationService; // spy

    @Autowired
    private FinanceCutoffExecutor financeCutoffExecutor;

    @Test
    @DbUnitDataSet(
            before = "financeCutoffExecutorTest.before.csv",
            after = "financeCutoffExecutorTest.after.csv"
    )
    void generateInDb() {
        financeCutoffExecutor.doJobLocked(null);
        verify(notificationService).send(eq(33), eq(45L), any());
        verify(notificationService).send(eq(33), eq(47L), any());
    }
}
