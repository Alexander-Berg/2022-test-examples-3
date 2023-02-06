package ru.yandex.market.core.marketing;

import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.EntryStream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.marketing.model.MarketingCampaignParams;
import ru.yandex.market.core.marketing.model.MarketingCampaignTypeParam;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ParametersAreNonnullByDefault
class MarketingCampaignServiceTest extends FunctionalTest {

    @Autowired
    MarketingCampaignsService marketingCampaignsService;

    @Test
    @DisplayName("Проверка общего количества типов кампаний")
    void getMarketingCampaignCountTest() {
        MarketingCampaignParams marketingCampaignParams = marketingCampaignsService.getMarketingCampaignParams();
        assertNotNull(marketingCampaignParams);

        Map<MarketingCampaignType, MarketingCampaignTypeParam> campaignTypes =
                marketingCampaignParams.getCampaignTypes();
        assertNotNull(campaignTypes);

        assertEquals(21, campaignTypes.size());
    }

    @Test
    @DisplayName("Проверка количества и типов промокампаний")
    void getPromoMarketingCampaign() {
        List<MarketingCampaignType> promoCampaignTypes = EntryStream.of(
                marketingCampaignsService.getMarketingCampaignParams().getCampaignTypes()
        )
                .filterValues(MarketingCampaignTypeParam::isAnaplanIdRequired)
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
        Map<MarketingCampaignType, MarketingCampaignTypeParam> campaignTypes =
                marketingCampaignsService.getMarketingCampaignParams().getCampaignTypes();

        assertThrows(UnsupportedOperationException.class, campaignTypes::clear);

        MarketingCampaignTypeParam marketingCampaignTypeParam = new MarketingCampaignTypeParam(true);
        assertThrows(UnsupportedOperationException.class,
                () -> campaignTypes.put(MarketingCampaignType.FEDERAL_PROMO, marketingCampaignTypeParam)
        );
    }
}
