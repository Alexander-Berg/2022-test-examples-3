package ru.yandex.direct.intapi.entity.clients.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.data.Percentage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Percent;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.intapi.configuration.IntApiTest;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeWalletCampaign;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IntapiClientServiceGetClientInfoByIdsTest {
    @Autowired
    private IntapiClientService intapiClientService;

    @Autowired
    private Steps steps;

    private ClientInfo clientInfo;


    @Before
    public void setUp() throws Exception {
        CampaignInfo walletInfo = steps.campaignSteps().createCampaign(activeWalletCampaign(null, null));
        clientInfo = walletInfo.getClientInfo();
    }

    @Test
    public void getClientInfoByIds() {
        List<ClientId> clientIds = Arrays.asList(clientInfo.getClientId());
        var clientInfos = intapiClientService.getClientInfoByIds(clientIds, false);
        assertThat(clientInfos.size()).isEqualTo(1);
        assertThat(clientInfos.get(0).getClientId()).isEqualTo(clientInfo.getClientId().asLong());
        assertThat(clientInfos.get(0).getBalance()).isCloseTo(
                BigDecimal.valueOf(90_000), Percentage.withPercentage(0.01));
        assertThat(clientInfos.get(0).getNds()).isCloseTo(
                BigDecimal.valueOf(20), Percentage.withPercentage(0.01));
        assertThat(clientInfos.get(0).getCurrencyCode()).isEqualTo(CurrencyCode.RUB);
    }

    @Test
    public void getClientInfoWithAgencyNdsByIds() {
        ClientId clientId = clientInfo.getClientId();
        steps.clientSteps().deleteClientNds(clientInfo.getShard(), clientId.asLong());
        steps.clientSteps().initAgencyNds(
                clientInfo.getShard(), clientId.asLong(), Percent.fromPercent(BigDecimal.valueOf(42)));

        List<ClientId> clientIds = Arrays.asList(clientInfo.getClientId());
        var clientInfos = intapiClientService.getClientInfoByIds(clientIds, false);
        assertThat(clientInfos.size()).isEqualTo(1);
        assertThat(clientInfos.get(0).getClientId()).isEqualTo(clientInfo.getClientId().asLong());
        assertThat(clientInfos.get(0).getBalance()).isCloseTo(
                BigDecimal.valueOf(90_000), Percentage.withPercentage(0.01));
        assertThat(clientInfos.get(0).getNds()).isCloseTo(
                BigDecimal.valueOf(42), Percentage.withPercentage(0.01));
        assertThat(clientInfos.get(0).getCurrencyCode()).isEqualTo(CurrencyCode.RUB);
    }

    @Test
    public void getClientInfoByIdsWithoutMoneyFields() {
        List<ClientId> clientIds = Arrays.asList(clientInfo.getClientId());
        var clientInfos = intapiClientService.getClientInfoByIds(clientIds, true);
        assertThat(clientInfos.size()).isEqualTo(1);
        assertThat(clientInfos.get(0).getClientId()).isEqualTo(clientInfo.getClientId().asLong());
        assertThat(clientInfos.get(0).getBalance()).isNull();
        assertThat(clientInfos.get(0).getNds()).isNull();
        assertThat(clientInfos.get(0).getCurrencyCode()).isEqualTo(CurrencyCode.RUB);
    }

    @Test
    public void getClientInfoByInvalidIds() {
        List<ClientId> clientIds = Arrays.asList(ClientId.fromLong(Long.MAX_VALUE));
        var clientInfos = intapiClientService.getClientInfoByIds(clientIds, null);
        assertThat(clientInfos).isEmpty();
    }

    @Test
    public void getClientInfoByIdsEmpty() {
        var clientInfos = intapiClientService.getClientInfoByIds(emptyList(), null);
        assertThat(clientInfos).isEmpty();
    }
}
