package ru.yandex.autotests.directintapi.tests.autobudgetoptimizedby;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

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
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;

/**
 * Created by pavryabov on 19.08.15.
 * https://st.yandex-team.ru/TESTIRT-6740
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.AUTOBUDGET_OPTIMIZED_BY)
@Description("Вызов AutobudgetOptimizedBy.setOrder с массивом объектов")
@Issue("https://st.yandex-team.ru/DIRECT-44545")
public class AutobudgetOptimizedBySetOrderArrayTest {

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104).as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    private Long campaignId;
    private Long orderId;
    private Long campaignIdElse;
    private Long orderIdElse;
    public static int shard;

    @BeforeClass
    public static void getShard() {
        shard = api.userSteps.getDarkSideSteps().getClientFakeSteps().getUserShard(Logins.LOGIN_MAIN);
    }

    @Before
    public void createData() {
        campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign();
        orderId = Long.valueOf(api.userSteps.getDarkSideSteps().getCampaignFakeSteps().setRandomOrderID(campaignId));
        campaignIdElse = api.userSteps.campaignSteps().addDefaultTextCampaign();
        orderIdElse =
                Long.valueOf(api.userSteps.getDarkSideSteps().getCampaignFakeSteps().setRandomOrderID(campaignIdElse));
        api.userSteps.getDirectJooqDbSteps().useShard(shard).campaignsSteps().setType(campaignId,
                CampaignsType.performance);
        api.userSteps.getDirectJooqDbSteps().useShard(shard).campaignsSteps().setType(campaignIdElse,
                CampaignsType.performance);
    }

    @Test
    public void twoValidElementsInRequest() {
        api.userSteps.getDirectJooqDbSteps().campaignsSteps().addCampaignsPerformanceWithNowOptimizingBy(Long.valueOf(campaignId),
                CampaignsPerformanceNowOptimizingBy.CPA);

        api.userSteps.getDirectJooqDbSteps().campaignsSteps().addCampaignsPerformanceWithNowOptimizingBy(Long.valueOf(campaignIdElse),
                CampaignsPerformanceNowOptimizingBy.CPC);

        List<AutobudgetOptimizedByResponse> response =
                api.userSteps.getDarkSideSteps().getAutobudgetOptimizedBySteps().setOrder(
                        new AutobudgetOptimizedByRequest()
                                .withOrderID(orderId)
                                .withOptimizedBy(OptimizedBy.CPC.toString()),
                        new AutobudgetOptimizedByRequest()
                                .withOrderID(orderIdElse)
                                .withOptimizedBy(OptimizedBy.CPA.toString())
                );
        assertThat("в ответе нет ошибок", response, emptyIterable());
        CampaignsPerformanceRecord campPerformance =
                api.userSteps.getDirectJooqDbSteps().campaignsSteps().getCampaignsPerformance(campaignId);
        assertThat("значение поля now_optimizing_by в таблице campaigns_perf изменилось",
                campPerformance.getNowOptimizingBy(), equalTo(CampaignsPerformanceNowOptimizingBy.CPC));
        campPerformance =
                api.userSteps.getDirectJooqDbSteps().campaignsSteps().getCampaignsPerformance(campaignIdElse);
        assertThat("значение поля now_optimizing_by в таблице campaigns_perf изменилось",
                campPerformance.getNowOptimizingBy(), equalTo(CampaignsPerformanceNowOptimizingBy.CPA));
    }

    @Test
    public void twoInvalidElementsInRequest() {
        List<AutobudgetOptimizedByResponse> response =
                api.userSteps.getDarkSideSteps().getAutobudgetOptimizedBySteps().setOrder(
                        new AutobudgetOptimizedByRequest()
                                .withOrderID(Long.valueOf(campaignId))
                                .withOptimizedBy(OptimizedBy.CPC.toString()),
                        new AutobudgetOptimizedByRequest()
                                .withOrderID(Long.valueOf(campaignIdElse))
                                .withOptimizedBy(OptimizedBy.CPC.toString())
                );
        AutobudgetOptimizedByResponse expectedResponse = new AutobudgetOptimizedByResponse()
                .withOrderID(Long.valueOf(campaignId))
                .withCode(AutobudgetOptimizedByResponse.CODE_UNKNOWN_SHARD)
                .withMsg(AutobudgetOptimizedByResponse.MSG_UNKNOWN_SHARD);
        AutobudgetOptimizedByResponse expectedResponseElse = new AutobudgetOptimizedByResponse()
                .withOrderID(Long.valueOf(campaignIdElse))
                .withCode(AutobudgetOptimizedByResponse.CODE_UNKNOWN_SHARD)
                .withMsg(AutobudgetOptimizedByResponse.MSG_UNKNOWN_SHARD);
        assertThat("вернулся правильный набор ошибок",
                response, beanDiffer(Arrays.asList(expectedResponse, expectedResponseElse)));
    }

    @Test
    public void validAndInvalidElementsInRequest() {
        api.userSteps.getDirectJooqDbSteps().campaignsSteps()
                .addCampaignsPerformanceWithNowOptimizingBy(Long.valueOf(campaignId), CampaignsPerformanceNowOptimizingBy.CPA);
        List<AutobudgetOptimizedByResponse> response =
                api.userSteps.getDarkSideSteps().getAutobudgetOptimizedBySteps().setOrder(
                        new AutobudgetOptimizedByRequest()
                                .withOrderID(orderId)
                                .withOptimizedBy(OptimizedBy.CPC.toString()),
                        new AutobudgetOptimizedByRequest()
                                .withOrderID(Long.valueOf(campaignIdElse))
                                .withOptimizedBy(OptimizedBy.CPC.toString())
                );
        AutobudgetOptimizedByResponse expectedResponse = new AutobudgetOptimizedByResponse()
                .withOrderID(Long.valueOf(campaignIdElse))
                .withCode(AutobudgetOptimizedByResponse.CODE_UNKNOWN_SHARD)
                .withMsg(AutobudgetOptimizedByResponse.MSG_UNKNOWN_SHARD);
        assertThat("вернулся правильный набор ошибок",
                response, beanDiffer(Arrays.asList(expectedResponse)));
        CampaignsPerformanceRecord campPerformance =
                api.userSteps.getDirectJooqDbSteps().campaignsSteps().getCampaignsPerformance(campaignId);
        assertThat("значение поля now_optimizing_by в таблице campaigns_perf изменилось",
                campPerformance.getNowOptimizingBy(), equalTo(CampaignsPerformanceNowOptimizingBy.CPC));
    }

    @Test
    public void invalidAndValidElementsInRequest() {
        api.userSteps.getDirectJooqDbSteps().campaignsSteps()
                .addCampaignsPerformanceWithNowOptimizingBy(Long.valueOf(campaignId), CampaignsPerformanceNowOptimizingBy.CPA);
        List<AutobudgetOptimizedByResponse> response =
                api.userSteps.getDarkSideSteps().getAutobudgetOptimizedBySteps().setOrder(
                        new AutobudgetOptimizedByRequest()
                                .withOrderID(Long.valueOf(campaignIdElse))
                                .withOptimizedBy(OptimizedBy.CPC.toString()),
                        new AutobudgetOptimizedByRequest()
                                .withOrderID(orderId)
                                .withOptimizedBy(OptimizedBy.CPC.toString())
                );
        AutobudgetOptimizedByResponse expectedResponse = new AutobudgetOptimizedByResponse()
                .withOrderID(Long.valueOf(campaignIdElse))
                .withCode(AutobudgetOptimizedByResponse.CODE_UNKNOWN_SHARD)
                .withMsg(AutobudgetOptimizedByResponse.MSG_UNKNOWN_SHARD);
        assertThat("вернулся правильный набор ошибок",
                response, beanDiffer(Arrays.asList(expectedResponse)));
        CampaignsPerformanceRecord campPerformance =
                api.userSteps.getDirectJooqDbSteps().campaignsSteps().getCampaignsPerformance(campaignId);
        assertThat("значение поля now_optimizing_by в таблице campaigns_perf изменилось",
                campPerformance.getNowOptimizingBy(), equalTo(CampaignsPerformanceNowOptimizingBy.CPC));
    }

    @Test
    public void twoSameOrdersInRequest() {
        api.userSteps.getDirectJooqDbSteps().campaignsSteps().addCampaignsPerformanceWithNowOptimizingBy(
                (Long.valueOf(campaignId)), CampaignsPerformanceNowOptimizingBy.CPA);
        List<AutobudgetOptimizedByResponse> response =
                api.userSteps.getDarkSideSteps().getAutobudgetOptimizedBySteps().setOrder(
                        new AutobudgetOptimizedByRequest()
                                .withOrderID(orderId)
                                .withOptimizedBy(OptimizedBy.CPA.toString()),
                        new AutobudgetOptimizedByRequest()
                                .withOrderID(orderId)
                                .withOptimizedBy(OptimizedBy.CPC.toString())
                );
        assertThat("в ответе нет ошибок", response, emptyIterable());
        CampaignsPerformanceRecord campPerformance =
                api.userSteps.getDirectJooqDbSteps().campaignsSteps().getCampaignsPerformance(campaignId);
        assertThat("значение поля now_optimizing_by в таблице campaigns_perf изменилось",
                campPerformance.getNowOptimizingBy(), equalTo(CampaignsPerformanceNowOptimizingBy.CPC));
    }
}
