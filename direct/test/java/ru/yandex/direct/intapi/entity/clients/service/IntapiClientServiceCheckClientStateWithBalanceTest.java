package ru.yandex.direct.intapi.entity.clients.service;

import org.assertj.core.data.Percentage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.LoginOrUid;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.clients.model.CheckClientStateResponse;
import ru.yandex.direct.intapi.entity.clients.model.ClientRole;
import ru.yandex.direct.intapi.entity.clients.model.ClientState;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeWalletCampaign;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IntapiClientServiceCheckClientStateWithBalanceTest {
    @Autowired
    private IntapiClientService intapiClientService;

    @Autowired
    private Steps steps;

    private ClientInfo clientInfo;

    @Before
    public void setUp() throws Exception {
        var uidAndClientId = steps.clientSteps().generateNewUidAndClientId();
        clientInfo = steps.campaignSteps().createCampaign(activeWalletCampaign(uidAndClientId.getClientId(),
                uidAndClientId.getUid())).getClientInfo();
    }

    @Test
    public void checkExistingUserWithBalance() {
        CheckClientStateResponse response =
                intapiClientService.checkClientState(LoginOrUid.of(clientInfo.getUid()), false, true);
        assertThat(response.getCanNotBeCreatedReason()).isNullOrEmpty();
        assertThat(response.getClientState()).isEqualTo(ClientState.API_DISABLED);
        assertThat(response.getClientRole()).isEqualTo(ClientRole.CLIENT);
        assertThat(response.getHasSharedWallet()).isTrue();
        assertThat(response.getBalance()).isCloseTo(
                TestCampaigns.MONEY_ON_ACTIVE_BALANCE, Percentage.withPercentage(0.01));
    }
}
