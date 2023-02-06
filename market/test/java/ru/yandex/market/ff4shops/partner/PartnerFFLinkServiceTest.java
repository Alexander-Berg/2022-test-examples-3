package ru.yandex.market.ff4shops.partner;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.ff4shops.PartnerFulfillmentLinkForFF4Shops;
import ru.yandex.market.ff4shops.config.FunctionalTest;

/**
 * Тесты для PartnerFFLinkService
 */
class PartnerFFLinkServiceTest  extends FunctionalTest {

    @Autowired
    private PartnerFFLinkService partnerFFLinkService;

    /**
     * Проверяем, что при обновлении ФФ-линки не затираем partnerWarehouseId
     */
    @Test
    @DbUnitDataSet(before = "PartnerFFLinkServiceTest.before.csv",
                   after = "PartnerFFLinkServiceTest.after.csv")
    void updateFFLinksNotRewritePartnerWarehouseId() {
        partnerFFLinkService.updateFFLinks(1,
                List.of(PartnerFulfillmentLinkForFF4Shops.newBuilder()
                        .withServiceId(2L)
                        .withFeedId(4L)
                        .build()
                ));
    }
}
