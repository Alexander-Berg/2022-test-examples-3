package ru.yandex.market.vendors.analytics.tms.jobs.offer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.tms.BalanceFunctionalTest;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "OfferVendors.common.before.csv")
class OfferVendorsBalanceCheckExecutorTest extends BalanceFunctionalTest {

    @Autowired
    private OfferVendorsBalanceCheckExecutor offerVendorsBalanceCheckExecutor;

    @Test
    @DisplayName("Проверка баланса на текущий месяц")
    @DbUnitDataSet(after = "OfferVendorsBalanceCheckExecutorTest.after.csv")
    void startOfMonth() {
        mockBalance(1001, 26, 2);
        mockChangeDynamicCost(1001, 0, 19);
        mockBalance(1002, 9, 2);
        mockChangeDynamicCost(1002, 0, 9);
        offerVendorsBalanceCheckExecutor.doJob(null);
    }
}