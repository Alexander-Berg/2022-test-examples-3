package ru.yandex.market.adv.promo.service.promo.statistic;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static ru.yandex.market.adv.promo.service.promo.statistic.constant.PromoStatisticConstants.ALL_PROMOS;

public class CachedPromoStatisticServiceTest extends FunctionalTest {

    @Autowired
    private CachedPromoStatisticService cachedPromoStatisticService;

    @Test
    @DbUnitDataSet
    @DisplayName("Обновление значение существующей статистики")
    void putValue_updateExistingStatistic() {
        Assertions.assertEquals(Optional.empty(), cachedPromoStatisticService.getStatisticValue(ALL_PROMOS));
        cachedPromoStatisticService.putStatistic(ALL_PROMOS, 123);
        Assertions.assertEquals(123, cachedPromoStatisticService.getStatisticValue(ALL_PROMOS).get());
        cachedPromoStatisticService.putStatistic(ALL_PROMOS, 567);
        Assertions.assertEquals(567, cachedPromoStatisticService.getStatisticValue(ALL_PROMOS).get());
    }
}
