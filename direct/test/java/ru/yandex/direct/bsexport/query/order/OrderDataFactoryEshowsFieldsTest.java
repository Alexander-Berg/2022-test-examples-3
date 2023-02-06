package ru.yandex.direct.bsexport.query.order;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.bsexport.model.Order;
import ru.yandex.direct.bsexport.snapshot.BsExportSnapshot;
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.EshowsRate;
import ru.yandex.direct.core.entity.campaign.model.EshowsSettings;
import ru.yandex.direct.core.entity.campaign.model.EshowsVideoType;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OrderDataFactoryEshowsFieldsTest {
    private OrderDataFactory orderDataFactory = new OrderDataFactory(mock(BsExportSnapshot.class));

    @Test
    void campaignWithoutEshowsSettingsTest() {
        var campaignWithoutEshowsSettings = new TextCampaign()
                .withId(1L);
        var orderBuilder = Order.newBuilder();
        orderDataFactory.addEshowsFields(orderBuilder, campaignWithoutEshowsSettings);
        assertThat(orderBuilder.hasEshowsBannerRate()).isFalse();
        assertThat(orderBuilder.hasEshowsVideoRate()).isFalse();
        assertThat(orderBuilder.hasEshowsVideoType()).isFalse();
    }

    @Test
    void campaignWithEshowsSettingsTest() {
        var campaignWithEshowsSettings = new CpmBannerCampaign()
                .withId(1L)
                .withEshowsSettings(new EshowsSettings()
                        .withBannerRate(EshowsRate.ON)
                        .withVideoRate(EshowsRate.OFF)
                        .withVideoType(EshowsVideoType.COMPLETES));
        var orderBuilder = Order.newBuilder();
        orderDataFactory.addEshowsFields(orderBuilder, campaignWithEshowsSettings);
        assertThat(orderBuilder.getEshowsBannerRate()).isEqualTo(1.0);
        assertThat(orderBuilder.getEshowsVideoRate()).isEqualTo(0.0);
        assertThat(orderBuilder.getEshowsVideoType())
                .isEqualTo(ru.yandex.direct.bsexport.model.EshowsVideoType.Completes);
    }

    @Test
    void campaignWithEshowsSettings_SomeFieldsNullTest() {
        var campaignWithEshowsSettings = new CpmBannerCampaign()
                .withId(1L)
                .withEshowsSettings(new EshowsSettings()
                        .withVideoRate(EshowsRate.ON));
        var orderBuilder = Order.newBuilder();
        orderDataFactory.addEshowsFields(orderBuilder, campaignWithEshowsSettings);
        assertThat(orderBuilder.hasEshowsBannerRate()).isFalse();
        assertThat(orderBuilder.getEshowsVideoRate()).isEqualTo(1.0);
        assertThat(orderBuilder.hasEshowsVideoType()).isFalse();
    }
}
