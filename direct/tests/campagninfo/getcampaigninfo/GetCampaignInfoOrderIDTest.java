package ru.yandex.autotests.directintapi.tests.campagninfo.getcampaigninfo;

import org.junit.Before;
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
@Description("Проверка поля OrderID в ответе CampaignInfo.getCampaignInfo")
@Issues({@Issue("https://st.yandex-team.ru/DIRECT-48543"),
        @Issue("https://st.yandex-team.ru/DIRECT-51358")})
public class GetCampaignInfoOrderIDTest {

    private static final String CLIENT = Logins.LOGIN_MAIN;

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    private Long cid;

    @Before
    @Step("Подготовка тестовых данных")
    public void createObjects() {
        cid = api.as(CLIENT).userSteps.campaignSteps().addDefaultTextCampaign();
    }

    @Test
    public void campaignWithOrderIDByCid() {
        Integer orderID = api.userSteps.campaignFakeSteps().setRandomOrderID(cid.intValue());
        GetCampaignInfoRequest request = new GetCampaignInfoRequest()
                .withCids(cid)
                .withFields(Fields.ORDER_ID.toString());
        GetCampaignInfoResponse response =
                api.userSteps.getDarkSideSteps().getCampaignInfoSteps().getCampaignInfo(request);
        GetCampaignInfoResponse expectedResponse = new GetCampaignInfoResponse()
                .withResult(new GetCampaignInfoItem()
                        .withCid(cid.toString())
                        .withOrderID(orderID.toString()));
        assertThat("ручка вернула ожидаемый результат", response, beanDiffer(expectedResponse));
    }

    @Test
    public void campaignWithoutOrderIDByCid() {
        GetCampaignInfoRequest request = new GetCampaignInfoRequest()
                .withCids(cid)
                .withFields(Fields.ORDER_ID.toString());
        GetCampaignInfoResponse response =
                api.userSteps.getDarkSideSteps().getCampaignInfoSteps().getCampaignInfo(request);
        GetCampaignInfoResponse expectedResponse = new GetCampaignInfoResponse()
                .withResult(new GetCampaignInfoItem()
                        .withCid(cid.toString())
                        .withOrderID(null));
        assertThat("ручка вернула ожидаемый результат", response, beanDiffer(expectedResponse));
    }

    @Test
    public void campaignWithOrderIDByBid() {
        Long pid = api.userSteps.adGroupsSteps().addDefaultGroup(cid);
        Long bid = api.userSteps.adsSteps().addDefaultTextAd(pid);
        Integer orderID = api.userSteps.campaignFakeSteps().setRandomOrderID(cid.intValue());
        GetCampaignInfoRequest request = new GetCampaignInfoRequest()
                .withBids(bid)
                .withFields(Fields.ORDER_ID.toString());
        GetCampaignInfoResponse response =
                api.userSteps.getDarkSideSteps().getCampaignInfoSteps().getCampaignInfo(request);
        GetCampaignInfoResponse expectedResponse = new GetCampaignInfoResponse()
                .withResult(new GetCampaignInfoItem()
                        .withCid(cid.toString())
                        .withBids(bid.toString())
                        .withOrderID(orderID.toString()));
        assertThat("ручка вернула ожидаемый результат", response, beanDiffer(expectedResponse));
    }

    @Test
    public void campaignWithoutOrderIDByBid() {
        Long pid = api.userSteps.adGroupsSteps().addDefaultGroup(cid);
        Long bid = api.userSteps.adsSteps().addDefaultTextAd(pid);
        GetCampaignInfoRequest request = new GetCampaignInfoRequest()
                .withBids(bid)
                .withFields(Fields.ORDER_ID.toString());
        GetCampaignInfoResponse response =
                api.userSteps.getDarkSideSteps().getCampaignInfoSteps().getCampaignInfo(request);
        GetCampaignInfoResponse expectedResponse = new GetCampaignInfoResponse()
                .withResult(new GetCampaignInfoItem()
                        .withCid(cid.toString())
                        .withBids(bid.toString())
                        .withOrderID(null));
        assertThat("ручка вернула ожидаемый результат", response, beanDiffer(expectedResponse));
    }
}
