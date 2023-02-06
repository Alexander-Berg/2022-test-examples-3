package ru.yandex.market.api.partner.distribution;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.distribution.model.DistributionPartner;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Тесты для {@link DistributionPartnerService}.
 */
class DistributionPartnerServiceTest extends FunctionalTest {

    @Autowired
    private DistributionPartnerService distributionPartnerService;

    @Test
    @DisplayName("Возращаем null, если нет партнера нужного типа")
    @DbUnitDataSet(before = "csv/DistributionPartnerService.campaign_not_present.csv")
    void testNoPartner() {
        final DistributionPartner result = distributionPartnerService.getDistributionPartner(2001L);
        Assertions.assertNull(result);
    }

    @Test
    @DisplayName("Возвращаем модель, если есть партнер нужного типа")
    @DbUnitDataSet(before = "csv/DistributionPartnerService.campaign_present.csv")
    void testCampaignPresent() {
        final DistributionPartner expectedResult = new DistributionPartner(1001L, "test1", 2001L);
        final DistributionPartner actualResult = distributionPartnerService.getDistributionPartner(2001L);

        Assertions.assertEquals(expectedResult, actualResult);
    }

}
