package ru.yandex.market.billing.fulfillment.billing.shortage;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

class SupplyShortageBillingServiceTest extends FunctionalTest {

    private static final LocalDate BILLING_DATE = LocalDate.of(2021, 2, 10);

    @Autowired
    private SupplyShortageBillingService supplyShortageBillingService;

    @Test
    @DbUnitDataSet(
            before = "db/SupplyShortageBillingServiceTest.testShortageBilling.before.csv",
            after = "db/SupplyShortageBillingServiceTest.testShortageBilling.after.csv"
    )
    void testShortageBilling() {
        supplyShortageBillingService.process(BILLING_DATE);
    }
}
