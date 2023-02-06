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
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by pavryabov on 04.12.15.
 * https://st.yandex-team.ru/TESTIRT-7911
 * https://st.yandex-team.ru/TESTIRT-8691
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.GET_CAMPAIGN_INFO)
@Description("Проверка CampaignInfo.getCampaignInfo с различными ошибками в запросе")
@Issues({@Issue("https://st.yandex-team.ru/DIRECT-48543"),
        @Issue("https://st.yandex-team.ru/DIRECT-51358")})
public class GetCampaignInfoErrorsTest {

    private static final String CLIENT = Logins.LOGIN_MAIN;

    private static final String INCORRECT_FIELDS = "Argument fields is incorrect or absent";
    private static final String INCORRECT_FIELD = "Argument fields is incorrect: unsupported fields: %s";
    private static final String CIDS_OR_BIDS_CAN_NOT_GO_TOGETHER = "Both bids and cids cannot be used in one request or omitted";
    private static final String NO_CIDS_OR_BIDS = "Both bids and cids cannot be omitted";

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    private static String clientId;
    private static Integer orderId;

    private static Long cid;

    @BeforeClass
    public static void createObjects() {
        clientId = api.userSteps.getDarkSideSteps().getClientFakeSteps().getClientData(CLIENT).getClientID();
        cid = api.as(CLIENT).userSteps.campaignSteps().addDefaultTextCampaign();
        orderId = api.userSteps.campaignFakeSteps().setRandomOrderID(cid.intValue());
    }

    @Test
    public void oneNonExistentCid() {
        Long errorCid = cid + 100000;
        GetCampaignInfoRequest request = new GetCampaignInfoRequest()
                .withCids(errorCid)
                .withFields(Fields.ORDER_ID.toString(), Fields.CLIENT_ID.toString());
        GetCampaignInfoResponse response =
                api.userSteps.getDarkSideSteps().getCampaignInfoSteps().getCampaignInfo(request);
        GetCampaignInfoResponse expectedResponse = new GetCampaignInfoResponse()
                .withResult(new GetCampaignInfoItem()
                        .withCid(errorCid.toString())
                        .withError(GetCampaignInfoItem.NOT_FOUND));
        assertThat("ручка вернула ожидаемый результат", response, beanDiffer(expectedResponse));
    }

    @Test
    public void existentAndNonExistentCids() {
        Long errorCid = cid + 100000;
        GetCampaignInfoRequest request = new GetCampaignInfoRequest()
                .withCids(cid, errorCid)
                .withFields(Fields.ORDER_ID.toString(), Fields.CLIENT_ID.toString());
        GetCampaignInfoResponse response =
                api.userSteps.getDarkSideSteps().getCampaignInfoSteps().getCampaignInfo(request);
        GetCampaignInfoResponse expectedResponse = new GetCampaignInfoResponse()
                .withResult(new GetCampaignInfoItem()
                                .withCid(cid.toString())
                                .withOrderID(orderId.toString())
                                .withClientID(clientId),
                        new GetCampaignInfoItem()
                                .withCid(errorCid.toString())
                                .withError(GetCampaignInfoItem.NOT_FOUND));
        assertThat("ручка вернула ожидаемый результат",
                response.sortResultByCid(), beanDiffer(expectedResponse.sortResultByCid()));
    }

    @Test
    public void requestWithoutCidsOrBids() {
        GetCampaignInfoRequest request = new GetCampaignInfoRequest()
                .withFields(Fields.ORDER_ID.toString(), Fields.CLIENT_ID.toString());
        GetCampaignInfoResponse response =
                api.userSteps.getDarkSideSteps().getCampaignInfoSteps().getCampaignInfo(request);
        GetCampaignInfoResponse expectedResponse = new GetCampaignInfoResponse()
                .withError(new GetCampaignInfoItem()
                        .withCode(GetCampaignInfoItem.INCORRECT_ARGS)
                        .withDescription(NO_CIDS_OR_BIDS));
        assertThat("ручка вернула ожидаемый результат", response, beanDiffer(expectedResponse));
    }

    @Test
    public void requestWithCidsAndBids() {
        GetCampaignInfoRequest request = new GetCampaignInfoRequest()
                .withCids(cid)
                .withBids(cid)
                .withFields(Fields.ORDER_ID.toString(), Fields.CLIENT_ID.toString());
        GetCampaignInfoResponse response =
                api.userSteps.getDarkSideSteps().getCampaignInfoSteps().getCampaignInfo(request);
        GetCampaignInfoResponse expectedResponse = new GetCampaignInfoResponse()
                .withError(new GetCampaignInfoItem()
                        .withCode(GetCampaignInfoItem.INCORRECT_ARGS)
                        .withDescription(CIDS_OR_BIDS_CAN_NOT_GO_TOGETHER));
        assertThat("ручка вернула ожидаемый результат", response, beanDiffer(expectedResponse));
    }

    @Test
    public void requestWithoutFields() {
        GetCampaignInfoRequest request = new GetCampaignInfoRequest()
                .withCids(cid);
        GetCampaignInfoResponse response =
                api.userSteps.getDarkSideSteps().getCampaignInfoSteps().getCampaignInfo(request);
        GetCampaignInfoResponse expectedResponse = new GetCampaignInfoResponse()
                .withError(new GetCampaignInfoItem()
                        .withCode(GetCampaignInfoItem.INCORRECT_ARGS)
                        .withDescription(INCORRECT_FIELDS));
        assertThat("ручка вернула ожидаемый результат", response, beanDiffer(expectedResponse));
    }

    @Test
    public void requestWithInvalidFieldInFields() {
        String incorrectField = "uid";
        GetCampaignInfoRequest request = new GetCampaignInfoRequest()
                .withCids(cid)
                .withFields(incorrectField);
        GetCampaignInfoResponse response =
                api.userSteps.getDarkSideSteps().getCampaignInfoSteps().getCampaignInfo(request);
        GetCampaignInfoResponse expectedResponse = new GetCampaignInfoResponse()
                .withError(new GetCampaignInfoItem()
                        .withCode(GetCampaignInfoItem.INCORRECT_ARGS)
                        .withDescription(String.format(INCORRECT_FIELD, incorrectField)));
        assertThat("ручка вернула ожидаемый результат", response, beanDiffer(expectedResponse));
    }
}
