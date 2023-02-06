package ru.yandex.autotests.directintapi.tests.campagninfo.getcampaigninfo;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.campaigninfo.Fields;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.campaigninfo.GetCampaignInfoItem;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.campaigninfo.GetCampaignInfoRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.campaigninfo.GetCampaignInfoResponse;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by pavryabov on 03.12.15.
 * https://st.yandex-team.ru/TESTIRT-7911
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.GET_CAMPAIGN_INFO)
@Description("Проверка CampaignInfo.getCampaignInfo с несколькими cid'ами в запросе")
@Issue("https://st.yandex-team.ru/DIRECT-48543")
public class GetCampaignInfoSeveralCidsTest {

    private static final String CLIENT = Logins.LOGIN_MAIN;
    private static final String CLIENT_ELSE = Logins.LOGIN_EUR;

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    private static String clientId;
    private static String otherClientId;

    private static Long clientCid1;
    private static Long clientCid2;

    private static Long otherClientCid;

    private static Integer clientOrderId1;
    private static Integer clientOrderId2;

    private static Integer otherClientOrderId;

    @BeforeClass
    public static void createObjects() {
        clientId = api.userSteps.getDarkSideSteps().getClientFakeSteps().getClientData(CLIENT).getClientID();
        otherClientId = api.userSteps.getDarkSideSteps().getClientFakeSteps().getClientData(CLIENT_ELSE).getClientID();
        clientCid1 = api.as(CLIENT).userSteps.campaignSteps().addDefaultTextCampaign();
        clientCid2 = api.as(CLIENT).userSteps.campaignSteps().addDefaultTextCampaign();
        otherClientCid = api.as(CLIENT_ELSE).userSteps.campaignSteps().addDefaultTextCampaign();
        clientOrderId1 = api.userSteps.campaignFakeSteps().setRandomOrderID(clientCid1.intValue());
        clientOrderId2 = api.userSteps.campaignFakeSteps().setRandomOrderID(clientCid2.intValue());
        otherClientOrderId = api.userSteps.campaignFakeSteps().setRandomOrderID(otherClientCid.intValue());
    }

    @Test
    public void twoSameCids() {
        GetCampaignInfoRequest request = new GetCampaignInfoRequest()
                .withCids(clientCid1, clientCid1)
                .withFields(Fields.ORDER_ID.toString(), Fields.CLIENT_ID.toString());
        GetCampaignInfoResponse response =
                api.userSteps.getDarkSideSteps().getCampaignInfoSteps().getCampaignInfo(request);
        GetCampaignInfoResponse expectedResponse = new GetCampaignInfoResponse()
                .withResult(new GetCampaignInfoItem()
                        .withCid(clientCid1.toString())
                        .withOrderID(clientOrderId1.toString())
                        .withClientID(clientId));
        assertThat("ручка вернула ожидаемый результат", response, beanDiffer(expectedResponse));
    }

    @Test
    public void twoCidsOfOneClient() {
        GetCampaignInfoRequest request = new GetCampaignInfoRequest()
                .withCids(clientCid1, clientCid2)
                .withFields(Fields.ORDER_ID.toString(), Fields.CLIENT_ID.toString());
        GetCampaignInfoResponse response =
                api.userSteps.getDarkSideSteps().getCampaignInfoSteps().getCampaignInfo(request);
        GetCampaignInfoResponse expectedResponse = new GetCampaignInfoResponse()
                .withResult(new GetCampaignInfoItem()
                                .withCid(clientCid1.toString())
                                .withOrderID(clientOrderId1.toString())
                                .withClientID(clientId),
                        new GetCampaignInfoItem()
                                .withCid(clientCid2.toString())
                                .withOrderID(clientOrderId2.toString())
                                .withClientID(clientId));
        assertThat("ручка вернула ожидаемый результат",
                response.sortResultByCid(), beanDiffer(expectedResponse.sortResultByCid()));
    }

    @Test
    public void twoCidsOfTwoClients() {
        GetCampaignInfoRequest request = new GetCampaignInfoRequest()
                .withCids(clientCid1, otherClientCid)
                .withFields(Fields.ORDER_ID.toString(), Fields.CLIENT_ID.toString());
        GetCampaignInfoResponse response =
                api.userSteps.getDarkSideSteps().getCampaignInfoSteps().getCampaignInfo(request);
        GetCampaignInfoResponse expectedResponse = new GetCampaignInfoResponse()
                .withResult(new GetCampaignInfoItem()
                                .withCid(clientCid1.toString())
                                .withOrderID(clientOrderId1.toString())
                                .withClientID(clientId),
                        new GetCampaignInfoItem()
                                .withCid(otherClientCid.toString())
                                .withOrderID(otherClientOrderId.toString())
                                .withClientID(otherClientId));
        assertThat("ручка вернула ожидаемый результат",
                response.sortResultByCid(), beanDiffer(expectedResponse.sortResultByCid()));
    }
}
