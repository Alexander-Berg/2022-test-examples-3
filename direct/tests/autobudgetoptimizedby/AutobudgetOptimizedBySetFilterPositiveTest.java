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
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BidsPerformanceNowOptimizingBy;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BidsPerformanceRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudgetoptimizedby.AutobudgetOptimizedByRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudgetoptimizedby.AutobudgetOptimizedByResponse;
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
 * Created by pavryabov on 21.08.15.
 * https://st.yandex-team.ru/TESTIRT-6740
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.AUTOBUDGET_OPTIMIZED_BY)
@Description("Вызов AutobudgetOptimizedBy.setFilter с одним объектом")
@Issue("https://st.yandex-team.ru/DIRECT-44545")
@RunWith(Parameterized.class)
public class AutobudgetOptimizedBySetFilterPositiveTest {

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    public Long campaignId;
    public static DirectJooqDbSteps jooqDbSteps;
    public Long orderId;
    public Long pid;

    @Parameterized.Parameter(value = 0)
    public BidsPerformanceNowOptimizingBy optimizedByBefore;

    @Parameterized.Parameter(value = 1)
    public BidsPerformanceNowOptimizingBy optimizedByInRequest;

    @Parameterized.Parameters(name = "before = {0}, in setOrder = {1}")
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {BidsPerformanceNowOptimizingBy.CPA, BidsPerformanceNowOptimizingBy.CPC},
                {BidsPerformanceNowOptimizingBy.CPC, BidsPerformanceNowOptimizingBy.CPA},
                {BidsPerformanceNowOptimizingBy.CPA, BidsPerformanceNowOptimizingBy.CPA},
                {BidsPerformanceNowOptimizingBy.CPC, BidsPerformanceNowOptimizingBy.CPC}
        };
        return Arrays.asList(data);
    }

    @BeforeClass
    public static void initJooqDbSteps() {
        jooqDbSteps = api.userSteps.getDirectJooqDbSteps().useShardForLogin(Logins.LOGIN_MAIN);
    }

    @Before
    public void createData() {
        campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign();
        orderId = (long) api.userSteps.getDarkSideSteps().getCampaignFakeSteps().setRandomOrderID(campaignId);
        pid = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId);
    }

    @Test
    public void setOptimizedBy() {
        Long id = jooqDbSteps.bidsPerformanceSteps().saveDefaultBidsPerformance(pid, optimizedByBefore);
        AutobudgetOptimizedByRequest request = new AutobudgetOptimizedByRequest()
                .withOrderID(orderId)
                .withGroupExportID(pid)
                .withPhraseID(id)
                .withOptimizedBy(optimizedByInRequest.toString());
        List<AutobudgetOptimizedByResponse> response =
                api.userSteps.getDarkSideSteps().getAutobudgetOptimizedBySteps().setFilter(request);
        assertThat("в ответе нет ошибок", response, emptyIterable());
        BidsPerformanceRecord bidsPerformance = jooqDbSteps.bidsPerformanceSteps().getBidsPerformance(id);
        assertThat("значение поля now_optimizing_by в таблице bid_performance изменилось",
                bidsPerformance.getNowOptimizingBy(), equalTo(optimizedByInRequest));
    }
}
