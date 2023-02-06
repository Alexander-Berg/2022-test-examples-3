package ru.yandex.market.mbi.api.controller.marketing;

import java.util.List;
import java.util.Map;

import one.util.streamex.EntryStream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.core.marketing.MarketingCampaignType;
import ru.yandex.market.mbi.api.client.entity.marketing.MarketingCampaignParamsDTO;
import ru.yandex.market.mbi.api.client.entity.marketing.MarketingCampaignTypeParamDTO;
import ru.yandex.market.mbi.api.config.FunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MarketingCampaignsControllerTest extends FunctionalTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MarketingCampaignsControllerTest.class);

    @Test
    @DisplayName("Проверка общего количества типов кампаний")
    void getMarketingCampaignCountTest() {
        MarketingCampaignParamsDTO marketingCampaignParams = mbiApiClient.getMarketingCampaignParams();
        Map<MarketingCampaignType, MarketingCampaignTypeParamDTO> campaignTypes =
                marketingCampaignParams.getCampaignTypes();
        assertEquals(21, campaignTypes.keySet().size());
    }

    @Test
    @DisplayName("Проверка количества и типов промокампаний")
    void getPromoMarketingCampaign() {
        List<MarketingCampaignType> promoCampaignTypes = EntryStream.of(
                mbiApiClient.getMarketingCampaignParams().getCampaignTypes()
        )
                .filterValues(MarketingCampaignTypeParamDTO::isAnaplanIdRequired)
                .keys()
                .toList();
        assertEquals(8, promoCampaignTypes.size());

        assertThat(
                promoCampaignTypes,
                Matchers.containsInAnyOrder(
                        MarketingCampaignType.GENERIC_BUNDLE,
                        MarketingCampaignType.BLUE_SET,
                        MarketingCampaignType.CHEAPEST_AS_GIFT,
                        MarketingCampaignType.MARKET_COUPON,
                        MarketingCampaignType.MARKET_COIN,
                        MarketingCampaignType.CASHBACK,
                        MarketingCampaignType.FEDERAL_PROMO,
                        MarketingCampaignType.UNIVERSAL_PROMO
                )
        );
    }

    @Test
    @DisplayName("Редактировать типы кампаний нельзя")
    void testUnmodifiedCampaignTypes() {
        Map<MarketingCampaignType, MarketingCampaignTypeParamDTO> campaignTypes =
                mbiApiClient.getMarketingCampaignParams().getCampaignTypes();

        assertThrows(UnsupportedOperationException.class, campaignTypes::clear);

        MarketingCampaignTypeParamDTO marketingCampaignTypeParam = new MarketingCampaignTypeParamDTO(true);
        assertThrows(UnsupportedOperationException.class,
                () -> campaignTypes.put(MarketingCampaignType.FEDERAL_PROMO, marketingCampaignTypeParam)
        );
    }
}
