package ru.yandex.market.adv.promo.tms.job.promos.clear_multi_details.executor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

class ClearAssortmentProcessingDetailsExecutorTest extends FunctionalTest {

    @Autowired
    private ClearAssortmentProcessingDetailsExecutor executor;

    @Test
    @DbUnitDataSet(
            before = "ClearAssortmentProcessingDetailsExecutorTest/before.csv",
            after = "ClearAssortmentProcessingDetailsExecutorTest/after.csv"
    )
    void test() {
        executor.doJob(null);
    }
}
