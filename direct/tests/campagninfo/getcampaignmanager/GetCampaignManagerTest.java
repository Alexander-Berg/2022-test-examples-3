package ru.yandex.autotests.directintapi.tests.campagninfo.getcampaignmanager;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.common.api45mng.CreateNewSubclientResponse;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.campaigninfo.GetCampaignManagerResponse;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.ClientFakeInfo;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.ClientStepsHelper;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.emptyArray;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;

/**
 * Author: xy6er
 * Date: 23.01.15
 * https://st.yandex-team.ru/TESTIRT-4048
 */
@Aqua.Test(title = "GetCampaignManager - Ручка, отдающая логин менеджера в модерацию")
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.GET_CAMPAIGN_MANAGER)
public class GetCampaignManagerTest {
    public static Long invalidCid = -1L;
    public static Long agencyCid;
    public static Long managerCid;
    public static Long clientCid;
    public static GetCampaignManagerResponse.ManagerInfo managerInfo;

    private GetCampaignManagerResponse[] responses;

    @ClassRule
    public static ApiSteps api = new ApiSteps().wsdl(APIPort_PortType.class);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @BeforeClass
    public static void initTestData() {
        api.userSteps.clientFakeSteps().enableToCreateSubClients(Logins.LOGIN_AGENCY);
        api.as(Logins.LOGIN_AGENCY);
        ClientStepsHelper clientStepsHelper = new ClientStepsHelper(api.userSteps.clientSteps());
        CreateNewSubclientResponse clientInfo = clientStepsHelper
                .createNewAgencySubClient("at-intapi-agencySub1-", Logins.LOGIN_AGENCY, Currency.RUB);
        agencyCid = api.userSteps.campaignSteps().addDefaultTextCampaign(clientInfo.getLogin());

        api.as(Logins.LOGIN_MNGR);
        clientStepsHelper = new ClientStepsHelper(api.userSteps.clientSteps());
        clientInfo = clientStepsHelper.createServicedClient("intapi-servClient30-", Logins.LOGIN_MNGR);
        managerCid = api.userSteps.campaignSteps().addDefaultTextCampaign(clientInfo.getLogin());

        api.as(Logins.LOGIN_MAIN);
        clientCid = api.userSteps.campaignSteps().addDefaultTextCampaign();

        ClientFakeInfo clientFakeInfo = api.userSteps.clientFakeSteps()
                .getClientData(Logins.LOGIN_MNGR, new String[]{"email", "fio"});
        managerInfo = new GetCampaignManagerResponse.ManagerInfo();
        managerInfo.setLogin(Logins.LOGIN_MNGR);
        managerInfo.setUid(Long.parseLong(clientFakeInfo.getPassportID()));
        managerInfo.setEmail(clientFakeInfo.getEmail());
        managerInfo.setFio(clientFakeInfo.getFio());
    }


    @Test
    public void getCampaignManagerWithAgencyCidTest() {
        responses = api.userSteps.getDarkSideSteps().getCampaignInfoSteps().getCampaignManager(agencyCid);

        GetCampaignManagerResponse expectedResponse = new GetCampaignManagerResponse();
        expectedResponse.setCid(agencyCid);
        expectedResponse.setAgManager(managerInfo);
        assertThat("В ответе метода должно быть одна запись", responses, arrayWithSize(1));
        assertThat("Неверная информация о менеджере кампании", responses[0], beanDiffer(expectedResponse));
    }

    @Test
    public void getCampaignManagerWithManagerCidTest() {
        responses = api.userSteps.getDarkSideSteps().getCampaignInfoSteps().getCampaignManager(managerCid);

        GetCampaignManagerResponse expectedResponse = new GetCampaignManagerResponse();
        expectedResponse.setCid(managerCid);
        expectedResponse.setManager(managerInfo);
        assertThat("В ответе метода должно быть одна запись", responses, arrayWithSize(1));
        assertThat("Неверная информация о менеджере кампании", responses[0], beanDiffer(expectedResponse));
    }

    @Test
    public void getCampaignManagerWithMassCidsTest() {
        responses = api.userSteps.getDarkSideSteps().getCampaignInfoSteps()
                .getCampaignManager(agencyCid, clientCid, invalidCid, managerCid);

        assertThat("В ответе метода должно быть две записи", responses, arrayWithSize(2));
    }

    @Test
    public void getCampaignManagerWithClientCidTest() {
        responses = api.userSteps.getDarkSideSteps().getCampaignInfoSteps().getCampaignManager(clientCid);
        assertThat("Ответ метода должен быть пустым, т.к. у кампании нету менеджера", responses, emptyArray());
    }

    @Test
    public void getCampaignManagerWithInvalidCidTest() {
        responses = api.userSteps.getDarkSideSteps().getCampaignInfoSteps().getCampaignManager(invalidCid);
        assertThat("Ответ метода должен быть пустым", responses, emptyArray());
    }

}
