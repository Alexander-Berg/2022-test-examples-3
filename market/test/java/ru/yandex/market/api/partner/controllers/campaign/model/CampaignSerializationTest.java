package ru.yandex.market.api.partner.controllers.campaign.model;

import org.junit.jupiter.api.Test;

import ru.yandex.market.api.partner.controllers.serialization.BaseOldSerializationTest;

/**
 * @author zoom
 */
class CampaignSerializationTest extends BaseOldSerializationTest {

    @Test
    void shouldSerializeNullNewCampaign() {
        Campaign campaign = new Campaign();
        testSerialization(campaign,
                "{\"id\":0,\"clientId\": 0}",
                "<campaign id=\"0\" client-id=\"0\"/>");
    }

    @Test
    void shouldSerializeNotNullNewCampaign() {
        Campaign campaign = new Campaign();
        campaign.setId(1L);
        campaign.setClientId(2L);
        campaign.setState(3);
        testSerialization(campaign,
                "{\"id\":1,\"clientId\":2,\"state\":3}",
                "<campaign id=\"1\" client-id=\"2\" state=\"3\"/>");
    }

}
