package ru.yandex.autotests.directintapi.tests.autobudgetoptimizedby;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.CampaignsPerformanceNowOptimizingBy;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.CampaignsType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsPerformanceRecord;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudgetoptimizedby.AutobudgetOptimizedByRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudgetoptimizedby.AutobudgetOptimizedByResponse;
import ru.yandex.autotests.directapi.darkside.model.OptimizedBy;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by pavryabov on 19.08.15.
 * https://st.yandex-team.ru/TESTIRT-6740
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.AUTOBUDGET_OPTIMIZED_BY)
@Description("Вызов AutobudgetOptimizedBy.setOrder с одним объектом")
@Issue("https://st.yandex-team.ru/DIRECT-44545")
@RunWith(Parameterized.class)
public class AutobudgetOptimizedBySetOrderPositiveTest {

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    public Long campaignId;
    public Long orderId;
    public static int shard;

    @Parameterized.Parameter(value = 0)
    public String optimizedByBefore;

    @Parameterized.Parameter(value = 1)
    public String optimizedByInRequest;

    @Parameterized.Parameters(name = "before = {0}, in setOrder = {1}")
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {OptimizedBy.CPA.toString(), OptimizedBy.CPC.toString()},
                {OptimizedBy.CPC.toString(), OptimizedBy.CPA.toString()},
                {OptimizedBy.CPA.toString(), OptimizedBy.CPA.toString()},
                {OptimizedBy.CPC.toString(), OptimizedBy.CPC.toString()}
        };
        return Arrays.asList(data);
    }

    @BeforeClass
    public static void getShard() {
        shard = api.userSteps.getDarkSideSteps().getClientFakeSteps().getUserShard(Logins.LOGIN_MAIN);
    }

    @Before
    public void createData() {
        campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign();
        orderId = Long.valueOf(api.userSteps.getDarkSideSteps().getCampaignFakeSteps().setRandomOrderID(campaignId));
        api.userSteps.getDirectJooqDbSteps().useShard(shard).campaignsSteps().setType(campaignId,
                CampaignsType.performance);
    }

    @Test
    public void setOptimizedBy() {
        api.userSteps.getDirectJooqDbSteps().campaignsSteps()
                .addCampaignsPerformanceWithNowOptimizingBy(Long.valueOf(campaignId), CampaignsPerformanceNowOptimizingBy.valueOf(optimizedByBefore));
        AutobudgetOptimizedByRequest request = new AutobudgetOptimizedByRequest()
                .withOrderID(orderId)
                .withOptimizedBy(optimizedByInRequest);
        List<AutobudgetOptimizedByResponse> response =
                api.userSteps.getDarkSideSteps().getAutobudgetOptimizedBySteps().setOrder(request);
        assertThat("в ответе нет ошибок", response, emptyIterable());
        CampaignsPerformanceRecord campPerformance =
                api.userSteps.getDirectJooqDbSteps().campaignsSteps().getCampaignsPerformance(campaignId);
        assertThat("значение поля now_optimizing_by в таблице campaigns_perf изменилось",
                campPerformance.getNowOptimizingBy(), equalTo(CampaignsPerformanceNowOptimizingBy.valueOf(optimizedByInRequest)));
    }
}
