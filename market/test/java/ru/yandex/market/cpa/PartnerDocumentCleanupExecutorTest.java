package ru.yandex.market.cpa;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Тесты для {@link PartnerDocumentCleanupExecutor}.
 *
 * @author Vadim Lyalin
 */
public class PartnerDocumentCleanupExecutorTest extends FunctionalTest {
    @Autowired
    private PartnerDocumentCleanupExecutor executor;

    @Test
    @DbUnitDataSet(before = "PrepayRequestCleanupExecutorTest.before.csv",
            after = "PrepayRequestCleanupExecutorTest.after.csv")
    void testDoJob() {
        executor.doJob(null);
    }
}
