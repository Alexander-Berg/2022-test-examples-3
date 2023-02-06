package ru.yandex.market.logistics.lom.converter.mbi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.converter.EnumConverter;
import ru.yandex.market.logistics.lom.entity.enums.CampaignType;

@DisplayName("Тест на конвертацию CampaignType")
class CampaignTypeTest extends AbstractTest {
    private final EnumConverter enumConverter = new EnumConverter();

    @ParameterizedTest
    @EnumSource(ru.yandex.market.core.campaign.model.CampaignType.class)
    @DisplayName("MBI -> LOM")
    void campaignTypeFromExternal(ru.yandex.market.core.campaign.model.CampaignType campaignType) {
        softly.assertThat(enumConverter.convert(campaignType, CampaignType.class)).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(CampaignType.class)
    @DisplayName("LOM -> MBI")
    void campaignTypeToExternal(CampaignType campaignType) {
        softly.assertThat(enumConverter.convert(campaignType, ru.yandex.market.core.campaign.model.CampaignType.class))
            .isNotNull();
    }
}
