package ru.yandex.autotests.directintapi.tests.fakeintapi;

import java.util.List;
import java.util.stream.Collectors;

import com.yandex.direct.api.v5.campaigns.CampaignFieldEnum;
import com.yandex.direct.api.v5.campaigns.CampaignGetItem;
import com.yandex.direct.api.v5.campaigns.CampaignStateEnum;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.ClientsWorkCurrency;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.ClientsRecord;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.common.api45mng.CreateNewSubclientResponse;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.ConvertType;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.model.api5.campaigns.CampaignsSelectionCriteriaMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.GetRequestMap;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.ClientStepsHelper;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertTrue;

/**
 * Author: xy6er
 */
@Issues({
        @Issue("https://st.yandex-team.ru/TESTIRT-9649"),
        @Issue("https://st.yandex-team.ru/TESTIRT-3506"),
        @Issue("https://st.yandex-team.ru/TESTIRT-3757")
})
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.FAKE_METHODS)
public class FakeConvertCurrencyClientTest {
    protected LogSteps log = LogSteps.getLogger(this.getClass());

    private DarkSideSteps darkSideSteps;
    private CreateNewSubclientResponse clientData;
    private Long campaignId;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_MNGR).wsdl(APIPort_PortType.class);

    @Before
    public void initSteps() {
        darkSideSteps = api.userSteps.getDarkSideSteps();
        ClientStepsHelper clientStepsHelper = new ClientStepsHelper(api.userSteps.clientSteps());
        clientData = clientStepsHelper.createServicedClient("intapi-servClient21-", Logins.LOGIN_MNGR);
        campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign(clientData.getLogin());
    }


    @Test
    @Title("Конвертация клиента методом COPY")
    public void fakeCopyConvertCurrencyClientTest() {
        Currency currency = Currency.values()[RandomUtils.getNextInt(Currency.values().length - 1) + 1];
        boolean result = darkSideSteps.getClientFakeSteps().
                convertCurrency(clientData.getLogin(), currency.value(), ConvertType.COPY);
        assertTrue("Method FakeConvertCurrencyClient returned false", result);

        ClientsRecord clients = darkSideSteps.getDirectJooqDbSteps().useShard(api.userSteps.clientFakeSteps()
                .getUserShard(clientData.getClientID())).clientsSteps().getClients((long) clientData.getClientID());
        assertThat("Валюта клиента не изменилась", clients.getWorkCurrency(),
                equalTo(ClientsWorkCurrency.valueOf(currency.value())));

        List<Long> cids = api.userSteps.campaignSteps().getCampaigns(clientData.getLogin(),
                new GetRequestMap()
                        .withSelectionCriteria(
                                new CampaignsSelectionCriteriaMap().withStates(CampaignStateEnum.values()))
                        .withFieldNames(CampaignFieldEnum.ID)).stream().map(CampaignGetItem::getId)
                .collect(Collectors.toList());
        assertThat("У клиента должно быть две кампании", cids, hasSize(2));
    }

    @Test
    @Title("Конвертация клиента методом MODIFY")
    public void fakeModifyConvertCurrencyClientTest() {
        Currency currency = Currency.RUB;
        boolean result = darkSideSteps.getClientFakeSteps().
                convertCurrency(clientData.getLogin(), currency.value(), ConvertType.MODIFY);
        assertTrue("Method FakeConvertCurrencyClient returned false", result);

        ClientsRecord clients = darkSideSteps.getDirectJooqDbSteps().useShard(api.userSteps.clientFakeSteps()
                .getUserShard(clientData.getClientID())).clientsSteps().getClients((long) clientData.getClientID());
        assertThat("Валюта клиента не изменилась", clients.getWorkCurrency(),
                equalTo(ClientsWorkCurrency.valueOf(currency.value())));

        List<Long> cids = api.userSteps.campaignSteps().getAllCampaignIds(clientData.getLogin());
        assertThat("У клиента должно быть одна кампания", cids, hasSize(1));
        assertThat("ID кампании не должен был измениться", cids.get(0), equalTo(campaignId));
    }
}
