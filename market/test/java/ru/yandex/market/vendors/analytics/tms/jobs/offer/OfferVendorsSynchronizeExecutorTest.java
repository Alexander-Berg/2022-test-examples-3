package ru.yandex.market.vendors.analytics.tms.jobs.offer;

import one.util.streamex.LongStreamEx;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.tms.BalanceFunctionalTest;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "OfferVendors.common.before.csv")
class OfferVendorsSynchronizeExecutorTest extends BalanceFunctionalTest {

    @Autowired
    private OfferVendorsSynchronizeExecutor offerVendorsSynchronizeExecutor;

    @Test
    @DbUnitDataSet(after = "OfferVendorsSynchronizeExecutorTest.after.csv")
    void synchronize() {
        mockBalance(1001, 27, 1);
        mockChangeDynamicCost(1001, 0, 27);
        mockBalance(1002, 9, 1);
        mockChangeDynamicCost(1002, 0, 9);
        LongStreamEx.rangeClosed(1003, 1005).forEach(datasourceId -> {
            mockBalance(datasourceId, 0, 1);
            mockChangeDynamicCost(datasourceId, 0, 0);
        });
        offerVendorsSynchronizeExecutor.doJob(null);
    }
}
