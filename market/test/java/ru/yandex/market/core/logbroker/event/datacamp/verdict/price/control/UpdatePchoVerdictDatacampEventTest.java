package ru.yandex.market.core.logbroker.event.datacamp.verdict.price.control;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.logbroker.event.datacamp.AbstractDataCampEventTest;
import ru.yandex.market.core.offer.IndexerOfferKey;

/**
 * Date: 02.04.2021
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
class UpdatePchoVerdictDatacampEventTest extends AbstractDataCampEventTest {

    @DisplayName("Корректность заполнения всех полей DataCampOffer.Offer")
    @Test
    void convertToDataCampOffer_allField_correctMapping() {
        UpdatePchoVerdictDatacampEvent datacampEvent = new UpdatePchoVerdictDatacampEvent(
                PartnerId.partnerId(6669L, CampaignType.SUPPLIER),
                IndexerOfferKey.anyMarketOrShopSkuUsedAndOtherIgnored(423L, 123456789L, "shop-sku-blue"),
                9403L,
                Instant.ofEpochSecond(1610602214L, 480271000L)
        );

        assertDataCampEvent("updatePchoVerdict.json", datacampEvent);
    }
}
