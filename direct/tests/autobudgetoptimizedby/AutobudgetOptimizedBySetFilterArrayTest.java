package ru.yandex.autotests.directintapi.tests.autobudgetoptimizedby;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BidsPerformanceNowOptimizingBy;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BidsPerformanceRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
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
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;

/**
 * Created by pavryabov on 21.08.15.
 * https://st.yandex-team.ru/TESTIRT-6740
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.AUTOBUDGET_OPTIMIZED_BY)
@Description("Вызов AutobudgetOptimizedBy.setFilter с массивом объектов")
@Issue("https://st.yandex-team.ru/DIRECT-44545")
public class AutobudgetOptimizedBySetFilterArrayTest {

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
    public Long campaignIdElse;
    public Long orderIdElse;
    public Long pidElse;

    @BeforeClass
    public static void getShard() {
        jooqDbSteps = api.userSteps.getDirectJooqDbSteps().useShardForLogin(Logins.LOGIN_MAIN);
    }

    @Before
    public void createData() {
        campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign();
        orderId = (long) api.userSteps.getDarkSideSteps().getCampaignFakeSteps().setRandomOrderID(campaignId);
        pid = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId);
        campaignIdElse = api.userSteps.campaignSteps().addDefaultTextCampaign();
        orderIdElse =
                (long) api.userSteps.getDarkSideSteps().getCampaignFakeSteps().setRandomOrderID(campaignIdElse);
        pidElse = api.userSteps.adGroupsSteps().addDefaultGroup(campaignIdElse);
    }

    @Test
    public void twoValidElementsInRequest() {
        Long id =
                jooqDbSteps.bidsPerformanceSteps().saveDefaultBidsPerformance(pid, BidsPerformanceNowOptimizingBy.CPA);
        Long idElse = jooqDbSteps.bidsPerformanceSteps()
                .saveDefaultBidsPerformance(pidElse, BidsPerformanceNowOptimizingBy.CPC);
        List<AutobudgetOptimizedByResponse> response =
                api.userSteps.getDarkSideSteps().getAutobudgetOptimizedBySteps().setFilter(
                        new AutobudgetOptimizedByRequest()
                                .withOrderID(orderId)
                                .withGroupExportID(pid)
                                .withPhraseID(id)
                                .withOptimizedBy(OptimizedBy.CPC.toString()),
                        new AutobudgetOptimizedByRequest()
                                .withOrderID(orderIdElse)
                                .withGroupExportID(pidElse)
                                .withPhraseID(idElse)
                                .withOptimizedBy(OptimizedBy.CPA.toString())
                );
        assertThat("в ответе нет ошибок", response, emptyIterable());
        BidsPerformanceRecord bidsPerformance = jooqDbSteps.bidsPerformanceSteps().getBidsPerformance(id);
        assertThat("значение поля now_optimizing_by в таблице bid_performance изменилось",
                bidsPerformance.getNowOptimizingBy(), equalTo(BidsPerformanceNowOptimizingBy.CPC));

        bidsPerformance = jooqDbSteps.bidsPerformanceSteps().getBidsPerformance(idElse);
        assertThat("значение поля now_optimizing_by в таблице bid_performance изменилось",
                bidsPerformance.getNowOptimizingBy(), equalTo(BidsPerformanceNowOptimizingBy.CPA));
    }

    @Test
    public void twoInvalidElementsInRequest() {
        List<AutobudgetOptimizedByResponse> response =
                api.userSteps.getDarkSideSteps().getAutobudgetOptimizedBySteps().setFilter(
                        new AutobudgetOptimizedByRequest()
                                .withOrderID(pid)
                                .withGroupExportID(pid)
                                .withPhraseID(pid)
                                .withOptimizedBy(OptimizedBy.CPC.toString()),
                        new AutobudgetOptimizedByRequest()
                                .withOrderID(pid)
                                .withGroupExportID(pid)
                                .withPhraseID(pid)
                                .withOptimizedBy(OptimizedBy.CPC.toString())
                );
        AutobudgetOptimizedByResponse expectedResponse = new AutobudgetOptimizedByResponse()
                .withOrderID(pid)
                .withGroupExportID(pid)
                .withPhraseID(pid)
                .withCode(AutobudgetOptimizedByResponse.CODE_UNKNOWN_SHARD)
                .withMsg(AutobudgetOptimizedByResponse.MSG_UNKNOWN_SHARD);
        AutobudgetOptimizedByResponse expectedResponseElse = new AutobudgetOptimizedByResponse()
                .withOrderID(pid)
                .withGroupExportID(pid)
                .withPhraseID(pid)
                .withCode(AutobudgetOptimizedByResponse.CODE_UNKNOWN_SHARD)
                .withMsg(AutobudgetOptimizedByResponse.MSG_UNKNOWN_SHARD);
        assertThat("вернулся правильный набор ошибок",
                response, beanDiffer(Arrays.asList(expectedResponse, expectedResponseElse)));
    }

    @Test
    public void validAndInvalidElementsInRequest() {
        Long id =
                jooqDbSteps.bidsPerformanceSteps().saveDefaultBidsPerformance(pid, BidsPerformanceNowOptimizingBy.CPA);
        List<AutobudgetOptimizedByResponse> response =
                api.userSteps.getDarkSideSteps().getAutobudgetOptimizedBySteps().setFilter(
                        new AutobudgetOptimizedByRequest()
                                .withOrderID(orderId)
                                .withGroupExportID(pid)
                                .withPhraseID(id)
                                .withOptimizedBy(OptimizedBy.CPC.toString()),
                        new AutobudgetOptimizedByRequest()
                                .withOrderID(pid)
                                .withGroupExportID(pid)
                                .withPhraseID(pid)
                                .withOptimizedBy(OptimizedBy.CPC.toString())
                );
        AutobudgetOptimizedByResponse expectedResponse = new AutobudgetOptimizedByResponse()
                .withOrderID(pid)
                .withGroupExportID(pid)
                .withPhraseID(pid)
                .withCode(AutobudgetOptimizedByResponse.CODE_UNKNOWN_SHARD)
                .withMsg(AutobudgetOptimizedByResponse.MSG_UNKNOWN_SHARD);
        assertThat("вернулся правильный набор ошибок",
                response, beanDiffer(Arrays.asList(expectedResponse)));
        BidsPerformanceRecord bidsPerformance = jooqDbSteps.bidsPerformanceSteps().getBidsPerformance(id);
        assertThat("значение поля now_optimizing_by в таблице bid_performance изменилось",
                bidsPerformance.getNowOptimizingBy(), equalTo(BidsPerformanceNowOptimizingBy.CPC));
    }

    @Test
    public void invalidAndValidElementsInRequest() {
        Long id =
                jooqDbSteps.bidsPerformanceSteps().saveDefaultBidsPerformance(pid, BidsPerformanceNowOptimizingBy.CPA);
        List<AutobudgetOptimizedByResponse> response =
                api.userSteps.getDarkSideSteps().getAutobudgetOptimizedBySteps().setFilter(
                        new AutobudgetOptimizedByRequest()
                                .withOrderID(pid)
                                .withGroupExportID(pid)
                                .withPhraseID(pid)
                                .withOptimizedBy(OptimizedBy.CPC.toString()),
                        new AutobudgetOptimizedByRequest()
                                .withOrderID(orderId)
                                .withGroupExportID(pid)
                                .withPhraseID(id)
                                .withOptimizedBy(OptimizedBy.CPC.toString())
                );
        AutobudgetOptimizedByResponse expectedResponse = new AutobudgetOptimizedByResponse()
                .withOrderID(pid)
                .withGroupExportID(pid)
                .withPhraseID(pid)
                .withCode(AutobudgetOptimizedByResponse.CODE_UNKNOWN_SHARD)
                .withMsg(AutobudgetOptimizedByResponse.MSG_UNKNOWN_SHARD);
        assertThat("вернулся правильный набор ошибок",
                response, beanDiffer(Arrays.asList(expectedResponse)));
        BidsPerformanceRecord bidsPerformance = jooqDbSteps.bidsPerformanceSteps().getBidsPerformance(id);
        assertThat("значение поля now_optimizing_by в таблице bid_performance изменилось",
                bidsPerformance.getNowOptimizingBy(), equalTo(BidsPerformanceNowOptimizingBy.CPC));
    }

    @Test
    public void twoSameOrdersInRequest() {
        Long id =
                jooqDbSteps.bidsPerformanceSteps().saveDefaultBidsPerformance(pid, BidsPerformanceNowOptimizingBy.CPA);
        List<AutobudgetOptimizedByResponse> response =
                api.userSteps.getDarkSideSteps().getAutobudgetOptimizedBySteps().setFilter(
                        new AutobudgetOptimizedByRequest()
                                .withOrderID(orderId)
                                .withGroupExportID(pid)
                                .withPhraseID(id)
                                .withOptimizedBy(OptimizedBy.CPA.toString()),
                        new AutobudgetOptimizedByRequest()
                                .withOrderID(orderId)
                                .withGroupExportID(pid)
                                .withPhraseID(id)
                                .withOptimizedBy(OptimizedBy.CPC.toString())
                );
        assertThat("в ответе нет ошибок", response, emptyIterable());
        BidsPerformanceRecord bidsPerformance = jooqDbSteps.bidsPerformanceSteps().getBidsPerformance(id);
        assertThat("значение поля now_optimizing_by в таблице bid_performance изменилось",
                bidsPerformance.getNowOptimizingBy(), equalTo(BidsPerformanceNowOptimizingBy.CPC));
    }
}
