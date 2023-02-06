package ru.yandex.market.supplier.promo;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.supplier.promo.service.ValidationMdsService;
import ru.yandex.market.shop.FunctionalTest;

public class PromoOfferHistoryRemovalExecutorTest extends FunctionalTest {
    @Autowired
    PromoOfferHistoryRemovalExecutor promoOfferHistoryRemovalExecutor;
    @Autowired
    ValidationMdsService validationMdsService;

    @Test
    @DbUnitDataSet(before = "removeHistory.before.csv", after = "removeHistory.after.csv")
    public void test() {
        promoOfferHistoryRemovalExecutor.doJob(null);
        Mockito.verify(validationMdsService, Mockito.times(2)).delete("key");
        Mockito.verify(validationMdsService, Mockito.times(2)).delete("eligible_s3_key");
    }
}
