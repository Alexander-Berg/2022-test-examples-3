package ru.yandex.market.billing.api.service;


import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.EntryStream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.api.FunctionalTest;
import ru.yandex.market.billing.api.controller.type.MarketingCampaignType;
import ru.yandex.market.billing.api.model.MarketingCampaignTypeParam;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ParametersAreNonnullByDefault
class MarketingCampaignsServiceTest extends FunctionalTest {

    @Autowired
    MarketingCampaignsService marketingCampaignsService;

    @Test
    @DisplayName("Проверка общего количества типов кампаний")
    void getMarketingCampaignCountTest() {
        Map<String, MarketingCampaignTypeParam> campaignTypes = marketingCampaignsService.getMarketingCampaignParams();

        assertNotNull(campaignTypes);

        assertEquals(21, campaignTypes.size());
    }

    @Test
    @DisplayName("Проверка количества и типов промокампаний")
    void getPromoMarketingCampaign() {
        List<String> promoCampaignTypes = EntryStream.of(
                        marketingCampaignsService.getMarketingCampaignParams()
                )
                .filterValues(MarketingCampaignTypeParam::getIsAnaplanIdRequired)
                .keys()
                .toList();
        assertEquals(8, promoCampaignTypes.size());

        assertThat(
                promoCampaignTypes,
                Matchers.containsInAnyOrder(
                        MarketingCampaignType.GENERIC_BUNDLE.getId(),
                        MarketingCampaignType.BLUE_SET.getId(),
                        MarketingCampaignType.CHEAPEST_AS_GIFT.getId(),
                        MarketingCampaignType.MARKET_COUPON.getId(),
                        MarketingCampaignType.MARKET_COIN.getId(),
                        MarketingCampaignType.CASHBACK.getId(),
                        MarketingCampaignType.FEDERAL_PROMO.getId(),
                        MarketingCampaignType.UNIVERSAL_PROMO.getId()
                )
        );
    }

    @Test
    @DisplayName("Редактировать типы кампаний нельзя")
    void testUnmodifiedCampaignTypes() {
        Map<String, MarketingCampaignTypeParam> campaignTypes =
                marketingCampaignsService.getMarketingCampaignParams();

        assertThrows(UnsupportedOperationException.class, campaignTypes::clear);

        MarketingCampaignTypeParam marketingCampaignTypeParam = new MarketingCampaignTypeParam()
                .isAnaplanIdRequired(true);
        assertThrows(UnsupportedOperationException.class,
                () -> campaignTypes.put(MarketingCampaignType.FEDERAL_PROMO.getId(), marketingCampaignTypeParam)
        );
    }
}
