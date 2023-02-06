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
 * Created by buhter on 03.03.16.
 * https://st.yandex-team.ru/TESTIRT-8691
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.GET_CAMPAIGN_INFO)
@Description("Проверка CampaignInfo.getCampaignInfo с несколькими bid'ами в запросе")
@Issue("https://st.yandex-team.ru/DIRECT-51358")
public class GetCampaignInfoSeveralBidsTest {

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

    private static Long clientCid;
    private static Long clientBid;
    private static Long clientOtherBid;
    private static Long clientOtherCid;
    private static Long clientOtherCampaignBid;

    private static Long otherClientCid;
    private static Long otherClientBid;

    private static Integer clientOrderId;
    private static Integer clientOtherOrderId;
    private static Integer otherClientOrderId;

    @BeforeClass
    public static void createObjects() {
        clientId = api.userSteps.getDarkSideSteps().getClientFakeSteps().getClientData(CLIENT).getClientID();
        otherClientId = api.userSteps.getDarkSideSteps().getClientFakeSteps().getClientData(CLIENT_ELSE).getClientID();

        clientCid = api.as(CLIENT).userSteps.campaignSteps().addDefaultTextCampaign();
        clientOtherCid = api.as(CLIENT).userSteps.campaignSteps().addDefaultTextCampaign();
        otherClientCid = api.as(CLIENT_ELSE).userSteps.campaignSteps().addDefaultTextCampaign();

        clientOrderId = api.userSteps.campaignFakeSteps().setRandomOrderID(clientCid.intValue());
        clientOtherOrderId = api.userSteps.campaignFakeSteps().setRandomOrderID(clientOtherCid.intValue());
        otherClientOrderId = api.userSteps.campaignFakeSteps().setRandomOrderID(otherClientCid.intValue());

        Long clientPid = api.as(CLIENT).userSteps.adGroupsSteps().addDefaultGroup(clientCid);
        Long clientOtherPid = api.as(CLIENT).userSteps.adGroupsSteps().addDefaultGroup(clientOtherCid);
        Long otherClientPid = api.as(CLIENT_ELSE).userSteps.adGroupsSteps().addDefaultGroup(otherClientCid);

        clientBid = api.as(CLIENT).userSteps.adsSteps().addDefaultTextAd(clientPid);
        clientOtherBid = api.as(CLIENT).userSteps.adsSteps().addDefaultTextAd(clientPid);
        clientOtherCampaignBid = api.as(CLIENT).userSteps.adsSteps().addDefaultTextAd(clientOtherPid);
        otherClientBid = api.as(CLIENT_ELSE).userSteps.adsSteps().addDefaultTextAd(otherClientPid);
    }

    @Test
    public void twoSameBids() {
        GetCampaignInfoRequest request = new GetCampaignInfoRequest()
                .withBids(clientBid, clientBid)
                .withFields(Fields.ORDER_ID.toString(), Fields.CLIENT_ID.toString());
        GetCampaignInfoResponse response =
                api.userSteps.getDarkSideSteps().getCampaignInfoSteps().getCampaignInfo(request);
        GetCampaignInfoResponse expectedResponse = new GetCampaignInfoResponse()
                .withResult(new GetCampaignInfoItem()
                        .withBids(clientBid.toString())
                        .withCid(clientCid.toString())
                        .withOrderID(clientOrderId.toString())
                        .withClientID(clientId));
        assertThat("ручка вернула ожидаемый результат", response, beanDiffer(expectedResponse));
    }

    @Test
    public void twoBidsOfOneClientCampaign() {
        GetCampaignInfoRequest request = new GetCampaignInfoRequest()
                .withBids(clientBid, clientOtherBid)
                .withFields(Fields.ORDER_ID.toString(), Fields.CLIENT_ID.toString());
        GetCampaignInfoResponse response =
                api.userSteps.getDarkSideSteps().getCampaignInfoSteps().getCampaignInfo(request);
        GetCampaignInfoResponse expectedResponse = new GetCampaignInfoResponse()
                .withResult(new GetCampaignInfoItem()
                        .withCid(clientCid.toString())
                        .withBids(clientBid.toString(), clientOtherBid.toString())
                        .withOrderID(clientOrderId.toString())
                        .withClientID(clientId));
        assertThat("ручка вернула ожидаемый результат", response, beanDiffer(expectedResponse));
    }

    @Test
    public void twoBidsOfOneClientTwoCampaigns() {
        GetCampaignInfoRequest request = new GetCampaignInfoRequest()
                .withBids(clientBid, clientOtherCampaignBid)
                .withFields(Fields.ORDER_ID.toString(), Fields.CLIENT_ID.toString());
        GetCampaignInfoResponse response =
                api.userSteps.getDarkSideSteps().getCampaignInfoSteps().getCampaignInfo(request);
        GetCampaignInfoResponse expectedResponse = new GetCampaignInfoResponse()
                .withResult(new GetCampaignInfoItem()
                                .withCid(clientCid.toString())
                                .withBids(clientBid.toString())
                                .withOrderID(clientOrderId.toString())
                                .withClientID(clientId),
                        new GetCampaignInfoItem()
                                .withBids(clientOtherCampaignBid.toString())
                                .withCid(clientOtherCid.toString())
                                .withOrderID(clientOtherOrderId.toString())
                                .withClientID(clientId));
        assertThat("ручка вернула ожидаемый результат",
                response.sortResultByCid(), beanDiffer(expectedResponse.sortResultByCid()));
    }


    @Test
    public void twoBidsOfTwoClientsCampaigns() {
        GetCampaignInfoRequest request = new GetCampaignInfoRequest()
                .withBids(clientBid, otherClientBid)
                .withFields(Fields.ORDER_ID.toString(), Fields.CLIENT_ID.toString());
        GetCampaignInfoResponse response =
                api.userSteps.getDarkSideSteps().getCampaignInfoSteps().getCampaignInfo(request);
        GetCampaignInfoResponse expectedResponse = new GetCampaignInfoResponse()
                .withResult(new GetCampaignInfoItem()
                                .withCid(clientCid.toString())
                                .withBids(clientBid.toString())
                                .withOrderID(clientOrderId.toString())
                                .withClientID(clientId),
                        new GetCampaignInfoItem()
                                .withBids(otherClientBid.toString())
                                .withCid(otherClientCid.toString())
                                .withOrderID(otherClientOrderId.toString())
                                .withClientID(otherClientId));
        assertThat("ручка вернула ожидаемый результат",
                response.sortResultByCid(), beanDiffer(expectedResponse.sortResultByCid()));
    }
}
