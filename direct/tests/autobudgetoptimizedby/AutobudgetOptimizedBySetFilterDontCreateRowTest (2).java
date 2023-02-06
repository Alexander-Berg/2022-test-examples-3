package ru.yandex.autotests.directintapi.tests.autobudgetoptimizedby;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BidsPerformanceRecord;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudgetoptimizedby.AutobudgetOptimizedByRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudgetoptimizedby.AutobudgetOptimizedByResponse;
import ru.yandex.autotests.directapi.darkside.model.OptimizedBy;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by pavryabov on 21.08.15.
 * https://st.yandex-team.ru/TESTIRT-6740
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.AUTOBUDGET_OPTIMIZED_BY)
@Description("Проверка того, что AutobudgetOptimizedBy.setFilter не умеет создавать новые записи в таблице campaigns_perf")
@Issues({
        @Issue("https://st.yandex-team.ru/DIRECT-52059"),
        @Issue("https://st.yandex-team.ru/DIRECT-44545")
})
public class AutobudgetOptimizedBySetFilterDontCreateRowTest {

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    public Long campaignId;
    public Long orderId;
    public Long pid;
    public static int shard;

    @BeforeClass
    public static void getShard() {
        shard = api.userSteps.getDarkSideSteps().getClientFakeSteps().getUserShard(Logins.LOGIN_MAIN);
    }

    @Before
    public void createData() {
        campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign();
        orderId = Long.valueOf(api.userSteps.getDarkSideSteps().getCampaignFakeSteps().setRandomOrderID(campaignId));
        pid = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId);
    }

    @Test
    public void callSetOrderForCampaignWithoutRowInCampaignsPerformance() {
        long id = RandomUtils.getNextInt(2000000);
        List<AutobudgetOptimizedByResponse> response =
                api.userSteps.getDarkSideSteps().getAutobudgetOptimizedBySteps().setFilter(
                        new AutobudgetOptimizedByRequest()
                                .withOrderID(orderId)
                                .withGroupExportID(pid)
                                .withPhraseID(id)
                                .withOptimizedBy(OptimizedBy.CPC.toString())
                );

        AutobudgetOptimizedByResponse expectedResponse = new AutobudgetOptimizedByResponse()
                .withOrderID(orderId)
                .withGroupExportID(pid)
                .withPhraseID(id)
                .withCode(AutobudgetOptimizedByResponse.CODE_UNKNOWN_ID)
                .withMsg(AutobudgetOptimizedByResponse.MSG_UNKNOWN_IDS);
        assertThat("вернулся правильный набор ошибок", response, beanDiffer(Arrays.asList(expectedResponse)));

        BidsPerformanceRecord bidsPerformance =
                api.userSteps.getDirectJooqDbSteps().useShard(shard).bidsPerformanceSteps().getBidsPerformance(id);
        assertThat("для id не появилось записи в таблице bids_performance", bidsPerformance, nullValue());
    }
}
