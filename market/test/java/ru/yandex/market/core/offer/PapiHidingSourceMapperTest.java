package ru.yandex.market.core.offer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ru.yandex.market.core.campaign.model.CampaignType;

/**
 * Тесты {@link PapiHidingSourceMapper}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class PapiHidingSourceMapperTest {

    @ParameterizedTest
    @DisplayName("Тест маппинга в источник скрытия")
    @CsvSource({
            "SHOP,false,false,PULL_PARTNER_API",
            "SHOP,true,false,PULL_PARTNER_API",
            "SHOP,false,true,PUSH_PARTNER_API",
            "SHOP,true,true,MARKET_PRICELABS",
            "SUPPLIER,false,true,PUSH_PARTNER_API",
            "SUPPLIER,true,true,PUSH_PARTNER_API",
    })
    void testMapping(CampaignType campaignType, boolean priority, boolean usePushScheme, PapiHidingSource expected) {
        PapiHidingSource actual = PapiHidingSourceMapper.get(campaignType, priority, usePushScheme);
        Assertions.assertEquals(expected, actual);
    }
}
