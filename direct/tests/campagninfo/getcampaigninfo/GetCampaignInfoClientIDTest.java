package ru.yandex.autotests.directintapi.tests.campagninfo.getcampaigninfo;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.campaigninfo.Fields;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.campaigninfo.GetCampaignInfoItem;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.campaigninfo.GetCampaignInfoRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.campaigninfo.GetCampaignInfoResponse;
import ru.yandex.autotests.directapi.model.Logins;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by pavryabov on 02.12.15.
 * https://st.yandex-team.ru/TESTIRT-7911
 * https://st.yandex-team.ru/TESTIRT-8691
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.GET_CAMPAIGN_INFO)
@Description("Проверка поля ClientID в ответе CampaignInfo.getCampaignInfo")
@Issues({@Issue("https://st.yandex-team.ru/DIRECT-48543"),
        @Issue("https://st.yandex-team.ru/DIRECT-51358")})
@RunWith(Parameterized.class)
public class GetCampaignInfoClientIDTest {

    private static final String LOGIN_FOR_USD = ru.yandex.autotests.directapi.darkside.Logins.LOGIN_USD; //используем свой логин, чтобы тесты апи не мешали при создании кампании

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    @Parameterized.Parameter(value = 0)
    public String description;

    @Parameterized.Parameter(value = 1)
    public String creator;

    @Parameterized.Parameter(value = 2)
    public String client;

    @Parameterized.Parameters(name = "{0}")
    public static Collection strategies() {
        Object[][] data = new Object[][]{
                {"самостоятельный клиент",
                        Logins.CLIENT_SELF, Logins.CLIENT_SELF},
                {"самостоятельный клиент, кампания создана супером",
                        Logins.SUPER_LOGIN, Logins.CLIENT_SELF},
                {"сервисируемый клиент, кампания создана клиентом",
                        LOGIN_FOR_USD, LOGIN_FOR_USD},
                {"сервисируемый клиент, кампания создана супером",
                        Logins.SUPER_LOGIN, LOGIN_FOR_USD},
                {"сервисируемый клиент, кампания создана менеджером",
                        Logins.MANAGER_DEFAULT, LOGIN_FOR_USD},
                {"субклиент",
                        Logins.AGENCY_TMONEY, Logins.TMONEY_CLIENT0},
                {"at-direct-api-test, кампанию создает менеджер",
                        Logins.MANAGER_DEFAULT, Logins.CLIENT_FREE_YE_DEFAULT},
                {"at-direct-api-test, кампанию создает агентство",
                        Logins.AGENCY_YE_DEFAULT, Logins.CLIENT_FREE_YE_DEFAULT},
        };
        return Arrays.asList(data);
    }

    private Long cid;
    private String clientId;

    @Before
    @Step("Подготовка тестовых данных")
    public void createObjects() {
        cid = api.as(creator).userSteps.campaignSteps().addDefaultTextCampaign(client);
        clientId = api.userSteps.getDarkSideSteps().getClientFakeSteps().getClientData(client).getClientID();
    }

    @Test
    public void checkClientIDByCid() {
        GetCampaignInfoRequest request = new GetCampaignInfoRequest()
                .withCids(cid)
                .withFields(Fields.CLIENT_ID.toString());
        GetCampaignInfoResponse response =
                api.userSteps.getDarkSideSteps().getCampaignInfoSteps().getCampaignInfo(request);
        GetCampaignInfoResponse expectedResponse = new GetCampaignInfoResponse()
                .withResult(new GetCampaignInfoItem()
                        .withCid(cid.toString())
                        .withClientID(clientId));
        assertThat("ручка вернула ожидаемый результат", response, beanDiffer(expectedResponse));
    }

    @Test
    public void checkClientIDByBid() {
        Long pid = api.userSteps.adGroupsSteps().addDefaultGroup(cid, client);
        Long bid = api.userSteps.adsSteps().addDefaultTextAd(pid, client);
        GetCampaignInfoRequest request = new GetCampaignInfoRequest()
                .withBids(bid)
                .withFields(Fields.CLIENT_ID.toString());
        GetCampaignInfoResponse response =
                api.userSteps.getDarkSideSteps().getCampaignInfoSteps().getCampaignInfo(request);
        GetCampaignInfoResponse expectedResponse = new GetCampaignInfoResponse()
                .withResult(new GetCampaignInfoItem()
                        .withBids(bid.toString())
                        .withCid(cid.toString())
                        .withClientID(clientId));
        assertThat("ручка вернула ожидаемый результат", response, beanDiffer(expectedResponse));
    }
}
