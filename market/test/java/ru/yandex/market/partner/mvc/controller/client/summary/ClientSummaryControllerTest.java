package ru.yandex.market.partner.mvc.controller.client.summary;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.MoreMbiMatchers;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.Mockito.when;

/**
 * Тесты для {@link ClientSummaryController}.
 *
 * @author otedikova
 */
@DbUnitDataSet(before = "ClientControllerClientSummaryTest.csv")
class ClientSummaryControllerTest extends FunctionalTest {
    @Autowired
    private BalanceContactService balanceContactService;

    @Test
    void getAgencyClientSummaryTest() {
        when(balanceContactService.getClientIdByUid(3001)).thenReturn(1001L);

        ResponseEntity<String> response = FunctionalTestHelper.get(getClientSummaryUrl(3001));
        Assert.assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers
                .jsonPropertyEquals("result",
                        "{\"clientId\":1001,\"campaignsSummary\":" +
                                "{\"types\":[\"SHOP\",\"SUPPLIER\"]}}")));
    }

    @Test
    void getAgencyWithDbsOnClientSummaryTest() {
        when(balanceContactService.getClientIdByUid(4001)).thenReturn(1002L);

        ResponseEntity<String> response = FunctionalTestHelper.get(getClientSummaryUrl(4001));
        Assert.assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers
                .jsonPropertyEquals("result",
                        "{\"clientId\":1002,\"campaignsSummary\":" +
                                "{\"types\":[\"SHOP\",\"SUPPLIER\"]}}")));
    }

    @Test
    void getAgencyWithOnlyCpcOnClientSummaryTest() {
        when(balanceContactService.getClientIdByUid(5001)).thenReturn(1003L);

        ResponseEntity<String> response = FunctionalTestHelper.get(getClientSummaryUrl(5001));
        Assert.assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers
                .jsonPropertyEquals("result",
                        "{\"clientId\":1003,\"campaignsSummary\":" +
                                "{\"types\":[\"SHOP\"]}}")));
    }

    @Test
    void getClientSummaryNotAllCampaignsTest() {
        when(balanceContactService.getClientIdByUid(3002)).thenReturn(1113L);

        ResponseEntity<String> response = FunctionalTestHelper.get(getClientSummaryUrl(3002));
        Assert.assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers
                .jsonPropertyEquals("result",
                        "{\"clientId\":1113,\"campaignsSummary\":{\"" +
                                "types\":[]}}")));
    }

    @Test
    void getAgencyClientWithNoCompanies() {
        when(balanceContactService.getClientIdByUid(3003)).thenReturn(1114L);

        ResponseEntity<String> response = FunctionalTestHelper.get(getClientSummaryUrl(3003));
        Assert.assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers
                .jsonPropertyEquals("result",
                        "{\"clientId\":1114,\"campaignsSummary\":{\"" +
                                "types\":[]}}")));
    }

    @Test
    void getClientSummaryPermittedByContactRoleTest() {
        when(balanceContactService.getClientIdByUid(3005)).thenReturn(1115L);

        ResponseEntity<String> response = FunctionalTestHelper.get(getClientSummaryUrl(3005));
        Assert.assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers
                .jsonPropertyEquals("result",
                        "{\"clientId\":1115,\"campaignsSummary\":{\"" +
                                "types\":[\"SHOP\"]}}")));
    }

    @Test
    void getClientSummaryNoClientFound() {
        ResponseEntity<String> response = FunctionalTestHelper.get(getClientSummaryUrl(321432));
        Assert.assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers
                .jsonPropertyEquals("result",
                        "{\"clientId\":-1,\"campaignsSummary\":{" +
                                "\"types\":[]}}")));
    }

    private String getClientSummaryUrl(long euid) {
        return baseUrl + "client/summary?euid=" + euid;
    }
}
