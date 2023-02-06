package ru.yandex.autotests.directintapi.tests.autobudgetoptimizedby;

import java.util.Arrays;
import java.util.List;

import com.googlecode.jsonrpc4j.JsonRpcClientException;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudgetoptimizedby.AutobudgetOptimizedByRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudgetoptimizedby.AutobudgetOptimizedByResponse;
import ru.yandex.autotests.directapi.darkside.exceptions.DarkSideException;
import ru.yandex.autotests.directapi.darkside.model.OptimizedBy;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.apache.commons.lang.StringUtils.uncapitalize;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;

/**
 * Created by pavryabov on 21.08.15.
 * https://st.yandex-team.ru/TESTIRT-6740
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.AUTOBUDGET_OPTIMIZED_BY)
@Description("Вызов AutobudgetOptimizedBy.setFilter с ошибками")
@Issue("https://st.yandex-team.ru/DIRECT-44545")
public class AutobudgetOptimizedBySetFilterNegativeTest {

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    public static Long campaignId;
    public static Long orderId;
    public static Long pid;
    public static int shard;

    @BeforeClass
    public static void prepareData() {
        shard = api.userSteps.getDarkSideSteps().getClientFakeSteps().getUserShard(Logins.LOGIN_MAIN);
        campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign();
        orderId = Long.valueOf(api.userSteps.getDarkSideSteps().getCampaignFakeSteps().setRandomOrderID(campaignId));
        pid = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId);
    }

    @Test
    public void campaignIdInsteadOrderId() {
        AutobudgetOptimizedByRequest request = new AutobudgetOptimizedByRequest()
                .withOrderID(Long.valueOf(campaignId))
                .withGroupExportID(pid)
                .withPhraseID(1l)
                .withOptimizedBy(OptimizedBy.CPC.toString());
        List<AutobudgetOptimizedByResponse> response =
                api.userSteps.getDarkSideSteps().getAutobudgetOptimizedBySteps().setFilter(request);
        AutobudgetOptimizedByResponse expectedResponse = new AutobudgetOptimizedByResponse()
                .withOrderID(Long.valueOf(campaignId))
                .withGroupExportID(pid)
                .withPhraseID(1l)
                .withCode(AutobudgetOptimizedByResponse.CODE_UNKNOWN_SHARD)
                .withMsg(AutobudgetOptimizedByResponse.MSG_UNKNOWN_SHARD);
        assertThat("вернулся правильный набор ошибок", response, beanDiffer(Arrays.asList(expectedResponse)));
    }

    @Test
    public void negativeIds() {
        AutobudgetOptimizedByRequest request = new AutobudgetOptimizedByRequest()
                .withOrderID(-1l)
                .withGroupExportID(pid)
                .withPhraseID(1l)
                .withOptimizedBy(OptimizedBy.CPC.toString());
        List<AutobudgetOptimizedByResponse> response =
                api.userSteps.getDarkSideSteps().getAutobudgetOptimizedBySteps().setFilter(request);
        AutobudgetOptimizedByResponse expectedResponse = new AutobudgetOptimizedByResponse()
                .withOrderID(-1l)
                .withGroupExportID(pid)
                .withPhraseID(1l)
                .withCode(AutobudgetOptimizedByResponse.CODE_MUST_BE_GREATER_ZERO)
                .withMsg(AutobudgetOptimizedByResponse.MSG_IDS_MUST_BE_GREATER_ZERO);
        assertThat("вернулся правильный набор ошибок", response, beanDiffer(Arrays.asList(expectedResponse)));
    }

    @Test
    public void zeroOrderId() {
        AutobudgetOptimizedByRequest request = new AutobudgetOptimizedByRequest()
                .withOrderID(0l)
                .withGroupExportID(pid)
                .withPhraseID(1l)
                .withOptimizedBy(OptimizedBy.CPC.toString());
        List<AutobudgetOptimizedByResponse> response =
                api.userSteps.getDarkSideSteps().getAutobudgetOptimizedBySteps().setFilter(request);
        AutobudgetOptimizedByResponse expectedResponse = new AutobudgetOptimizedByResponse()
                .withOrderID(0l)
                .withGroupExportID(pid)
                .withPhraseID(1l)
                .withCode(AutobudgetOptimizedByResponse.CODE_MUST_BE_GREATER_ZERO)
                .withMsg(AutobudgetOptimizedByResponse.MSG_IDS_MUST_BE_GREATER_ZERO);
        assertThat("вернулся правильный набор ошибок", response, beanDiffer(Arrays.asList(expectedResponse)));
    }

    @Test
    public void invalidOptimizedBy() {
        AutobudgetOptimizedByRequest request = new AutobudgetOptimizedByRequest()
                .withOrderID(Long.valueOf(campaignId))
                .withGroupExportID(pid)
                .withPhraseID(1l)
                .withOptimizedBy(uncapitalize(OptimizedBy.CPC.toString()));
        List<AutobudgetOptimizedByResponse> response =
                api.userSteps.getDarkSideSteps().getAutobudgetOptimizedBySteps().setFilter(request);
        AutobudgetOptimizedByResponse expectedResponse = new AutobudgetOptimizedByResponse()
                .withOrderID(Long.valueOf(campaignId))
                .withGroupExportID(pid)
                .withPhraseID(1l)
                .withCode(AutobudgetOptimizedByResponse.CODE_INVALID_OPTIMIZED_BY)
                .withMsg(AutobudgetOptimizedByResponse.MSG_INVALID_OPTIMIZED_BY);
        assertThat("вернулся правильный набор ошибок", response, beanDiffer(Arrays.asList(expectedResponse)));
    }

    @Test
    public void invalidRequest() {
        //объект вместо массива
        AutobudgetOptimizedByRequest request = new AutobudgetOptimizedByRequest()
                .withOrderID(Long.valueOf(campaignId))
                .withOptimizedBy(OptimizedBy.CPC.toString());
        try {
            api.userSteps.getDarkSideSteps().getAutobudgetOptimizedBySteps().setFilter(request.toString());
        } catch (DarkSideException e) {
            assertThat("вернулось правильное исключение",
                    e.getCause().getClass().toString(), equalTo(JsonRpcClientException.class.toString()));
        }
    }

    @Test
    public void notExistentPid() {
        AutobudgetOptimizedByRequest request = new AutobudgetOptimizedByRequest()
                .withOrderID(orderId)
                .withGroupExportID(pid + 10000)
                .withPhraseID(1l)
                .withOptimizedBy(OptimizedBy.CPC.toString());
        List<AutobudgetOptimizedByResponse> response =
                api.userSteps.getDarkSideSteps().getAutobudgetOptimizedBySteps().setFilter(request);
        AutobudgetOptimizedByResponse expectedResponse = new AutobudgetOptimizedByResponse()
                .withOrderID(orderId)
                .withGroupExportID(pid + 10000)
                .withPhraseID(1l)
                .withCode(AutobudgetOptimizedByResponse.CODE_UNKNOWN_ID)
                .withMsg(AutobudgetOptimizedByResponse.MSG_UNKNOWN_IDS);
        assertThat("вернулся правильный набор ошибок", response, beanDiffer(Arrays.asList(expectedResponse)));
    }

    @Test
    public void anotherPid() {
        Long anotherCampaignId = api.userSteps.campaignSteps().addDefaultTextCampaign();
        long anotherPid = api.userSteps.adGroupsSteps().addDefaultGroup(anotherCampaignId);
        AutobudgetOptimizedByRequest request = new AutobudgetOptimizedByRequest()
                .withOrderID(orderId)
                .withGroupExportID(anotherPid)
                .withPhraseID(1l)
                .withOptimizedBy(OptimizedBy.CPC.toString());
        List<AutobudgetOptimizedByResponse> response =
                api.userSteps.getDarkSideSteps().getAutobudgetOptimizedBySteps().setFilter(request);
        AutobudgetOptimizedByResponse expectedResponse = new AutobudgetOptimizedByResponse()
                .withOrderID(orderId)
                .withGroupExportID(anotherPid)
                .withPhraseID(1l)
                .withCode(AutobudgetOptimizedByResponse.CODE_UNKNOWN_ID)
                .withMsg(AutobudgetOptimizedByResponse.MSG_UNKNOWN_IDS);
        assertThat("вернулся правильный набор ошибок", response, beanDiffer(Arrays.asList(expectedResponse)));
    }
}
