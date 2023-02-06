package ru.yandex.direct.bsexport.query.order;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.model.WalletTypedCampaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.bsexport.query.order.OrderDataFactory.attributionTypeMapper;
import static ru.yandex.direct.bsexport.query.order.OrderDataFactory.getAttributionType;
import static ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel.FIRST_CLICK;
import static ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel.FIRST_CLICK_CROSS_DEVICE;
import static ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel.LAST_CLICK;
import static ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel.LAST_SIGNIFICANT_CLICK;
import static ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel.LAST_SIGNIFICANT_CLICK_CROSS_DEVICE;
import static ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK;
import static ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK_CROSS_DEVICE;

class OrderDataFactoryAttributionTypeTests {

    /**
     * Проверяем что любая модель атрибуции кампании имеет отправляемое в БК значение
     * При появлении новых значений - следует добавить (ниже) отдельный тест на это значение
     */
    @ParameterizedTest
    @EnumSource(value = CampaignAttributionModel.class)
    void allAttributionModelHasMappedValue(CampaignAttributionModel campaignAttributionModel) {
        assertDoesNotThrow(() -> attributionTypeMapper(campaignAttributionModel));
    }

    @Test
    void lastClickMappedValue() {
        assertThat(attributionTypeMapper(LAST_CLICK)).isEqualTo(1);
    }

    @Test
    void lastSignificantClickMappedValue() {
        assertThat(attributionTypeMapper(LAST_SIGNIFICANT_CLICK)).isEqualTo(2);
    }

    @Test
    void firstClickMappedValue() {
        assertThat(attributionTypeMapper(FIRST_CLICK)).isEqualTo(3);
    }

    @Test
    void lastYandexDirectClickMappedValue() {
        assertThat(attributionTypeMapper(LAST_YANDEX_DIRECT_CLICK)).isEqualTo(4);
    }

    @Test
    void lastSignificantClickCrossDeviceMappedValue() {
        assertThat(attributionTypeMapper(LAST_SIGNIFICANT_CLICK_CROSS_DEVICE)).isEqualTo(5);
    }

    @Test
    void firstClickCrossDeviceMappedValue() {
        assertThat(attributionTypeMapper(FIRST_CLICK_CROSS_DEVICE)).isEqualTo(6);
    }

    @Test
    void lastYandexDirectClickCrossDeviceMappedValue() {
        assertThat(attributionTypeMapper(LAST_YANDEX_DIRECT_CLICK_CROSS_DEVICE)).isEqualTo(7);
    }

    @Test
    void textCampaignWithLastClick_AttributionTypeMappedFromAttributionModel() {
        var campaign = new TextCampaign().withAttributionModel(LAST_CLICK);
        assertThat(getAttributionType(campaign)).isEqualTo(1);
    }

    @Test
    void textCampaignWithLastYandexDirectClick_AttributionTypeMappedFromAttributionModel() {
        var campaign = new TextCampaign().withAttributionModel(LAST_YANDEX_DIRECT_CLICK);
        assertThat(getAttributionType(campaign)).isEqualTo(4);
    }

    @Test
    void dynamicCampaignWithFirstClick_AttributionTypeMappedFromAttributionModel() {
        var campaign = new DynamicCampaign().withAttributionModel(FIRST_CLICK);
        assertThat(getAttributionType(campaign)).isEqualTo(3);
    }

    @Test
    void dynamicCampaignWithLastSignificantClick_AttributionTypeMappedFromAttributionModel() {
        var campaign = new DynamicCampaign().withAttributionModel(LAST_SIGNIFICANT_CLICK);
        assertThat(getAttributionType(campaign)).isEqualTo(2);
    }

    @Test
    void dynamicCampaignWithLastSignificantClickCrossDevice_AttributionTypeMappedFromAttributionModel() {
        var campaign = new DynamicCampaign().withAttributionModel(LAST_SIGNIFICANT_CLICK_CROSS_DEVICE);
        assertThat(getAttributionType(campaign)).isEqualTo(5);
    }

    @Test
    void dynamicCampaignWithFirstClickCrossDevice_AttributionTypeMappedFromAttributionModel() {
        var campaign = new DynamicCampaign().withAttributionModel(FIRST_CLICK_CROSS_DEVICE);
        assertThat(getAttributionType(campaign)).isEqualTo(6);
    }

    @Test
    void textCampaignWithLastYandexDirectClickCrossDevice_AttributionTypeMappedFromAttributionModel() {
        var campaign = new TextCampaign().withAttributionModel(LAST_YANDEX_DIRECT_CLICK_CROSS_DEVICE);
        assertThat(getAttributionType(campaign)).isEqualTo(7);
    }


    // TODO: раскомментировать после включения проперти CROSS_DEVICE_DEFAULT_ATTRIBUTION_TYPE_ENABLED
    //@Test
    void walletCampaign_DefaultAttributionType() {
        var campaign = new WalletTypedCampaign();
        assertThat(getAttributionType(campaign)).isEqualTo(7);
    }

    // TODO: раскомментировать после включения проперти CROSS_DEVICE_DEFAULT_ATTRIBUTION_TYPE_ENABLED
    //@Test
    void campaignWithoutAttributionModel_DefaultAttributionType() {
        var campaign = mock(CommonCampaign.class);
        assertThat(getAttributionType(campaign)).isEqualTo(7);
    }
}
