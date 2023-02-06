package ru.yandex.market.promo.dyn;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.supplier.promo.service.PromoService;
import ru.yandex.market.shop.FunctionalTest;

public class PostponedPromoStepEventExecutorTest extends FunctionalTest {
    @Autowired
    private PostponedPromoStepEventExecutor postponedPromoStepEventExecutor;

    @Autowired
    private PromoService promoService;

    @DbUnitDataSet(
            before = "testDoJob_before.csv"
    )
    @Test
    public void testJob() {
        Assertions.assertEquals(Optional.of(1234L), promoService.getMaxTableVersionForUnsentStepEvent());
        postponedPromoStepEventExecutor.doJob(null);
        Assertions.assertEquals(Optional.empty(), promoService.getMaxTableVersionForUnsentStepEvent());
    }
}
