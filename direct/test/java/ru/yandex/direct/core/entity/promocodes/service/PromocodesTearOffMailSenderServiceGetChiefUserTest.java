package ru.yandex.direct.core.entity.promocodes.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.junit.Assert.assertEquals;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PromocodesTearOffMailSenderServiceGetChiefUserTest {
    @Autowired
    private Steps steps;

    @Autowired
    private PromocodesTearOffMailSenderService senderService;

    private long campaignId;
    private Long expectedUserUid;

    @Before
    public void prepareData() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
        campaignId = campaignInfo.getCampaignId();

        expectedUserUid = clientInfo.getUid();
    }

    @Test
    public void sendPromocodesTearOffMailTest() {
        User user = senderService.getChiefByCampaignId(campaignId);
        assertEquals(expectedUserUid, user.getUid());
    }
}
