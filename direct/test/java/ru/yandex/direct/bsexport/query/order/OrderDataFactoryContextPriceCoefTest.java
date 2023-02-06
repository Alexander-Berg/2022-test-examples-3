package ru.yandex.direct.bsexport.query.order;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.bsexport.snapshot.BsExportSnapshot;
import ru.yandex.direct.core.entity.campaign.model.InternalAutobudgetCampaign;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OrderDataFactoryContextPriceCoefTest {
    private OrderDataFactory orderDataFactory = new OrderDataFactory(mock(BsExportSnapshot.class));

    @Test
    void campaignWithNetworkSettingsTest() {
        var campaignsWithNetworkSettings = new SmartCampaign()
                .withId(1L)
                .withContextPriceCoef(2);
        var coef = orderDataFactory.getContextPriceCoef(campaignsWithNetworkSettings);
        assertThat(coef).isEqualTo(2);
    }

    @Test
    void campaignWithoutNetworkSettingsTest() {
        var campaignsWithNetworkSettings = new InternalAutobudgetCampaign()
                .withId(1L);
        var coef = orderDataFactory.getContextPriceCoef(campaignsWithNetworkSettings);
        assertThat(coef).isEqualTo(100);
    }
}
