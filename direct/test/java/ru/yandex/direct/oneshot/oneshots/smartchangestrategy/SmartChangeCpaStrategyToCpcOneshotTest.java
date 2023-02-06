package ru.yandex.direct.oneshot.oneshots.smartchangestrategy;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithCustomStrategy;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.oneshot.configuration.OneshotTest;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAverageCpcPerCamprStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAverageCpcPerFilterStrategy;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@OneshotTest
@RunWith(Parameterized.class)
public class SmartChangeCpaStrategyToCpcOneshotTest extends SmartChangeStrategyBaseTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private SmartChangeCpaStrategyToCpcOneshot oneshot;
    @Autowired
    private CampaignModifyRepository campaignModifyRepository;

    private CampaignInfo campaignInfo;
    private SmartCampaign campaign;

    @Parameterized.Parameter
    public String testDescription;
    @Parameterized.Parameter(1)
    public DbStrategy dbStrategy;
    @Parameterized.Parameter(2)
    public DbStrategy dbStrategyExpect;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> params() {
        return asList(
                // AUTOBUDGET_AVG_CPA_PER_FILTER стратегия с filterAvgBid и без filterAvgCpa ->
                // переходит в AUTOBUDGET_AVG_CPC_PER_FILTER
                new Object[]{
                        "AVG_CPA_PER_FILTER стратегия только с filterAvgBid -> изменяется в AVG_CPС_PER_FILTER",
                        averageCpaPerFilterStrategy(null, FILTER_AVG_BID, null, null),
                        defaultAverageCpcPerFilterStrategy(FILTER_AVG_BID, null, null)
                },
                new Object[]{
                        "AVG_CPA_PER_FILTER стратегия с filterAvgBid и sum -> изменяется в AVG_CPС_PER_FILTER",
                        averageCpaPerFilterStrategy(null, FILTER_AVG_BID, null, SUM),
                        defaultAverageCpcPerFilterStrategy(FILTER_AVG_BID, null, SUM)
                },
                new Object[]{
                        "AVG_CPA_PER_FILTER стратегия с filterAvgBid и bid -> изменяется в AVG_CPС_PER_FILTER",
                        averageCpaPerFilterStrategy(null, FILTER_AVG_BID, BID, null),
                        defaultAverageCpcPerFilterStrategy(FILTER_AVG_BID, BID, null)
                },
                new Object[]{
                        "AVG_CPA_PER_FILTER стратегия с filterAvgBid, bid и sum -> изменяется в AVG_CPС_PER_FILTER",
                        averageCpaPerFilterStrategy(null, FILTER_AVG_BID, BID, SUM),
                        defaultAverageCpcPerFilterStrategy(FILTER_AVG_BID, BID, SUM)
                },
                // AUTOBUDGET_AVG_CPA_PER_FILTER стратегия с filterAvgBid и с filterAvgCpa -> не изменяется
                new Object[]{
                        "AVG_CPA_PER_FILTER стратегия с filterAvgCpa и filterAvgBid -> не изменяется",
                        averageCpaPerFilterStrategy(FILTER_AVG_CPA, FILTER_AVG_BID, null, null),
                        averageCpaPerFilterStrategy(FILTER_AVG_CPA, FILTER_AVG_BID, null, null)
                },
                new Object[]{
                        "AVG_CPA_PER_FILTER стратегия с filterAvgCpa, filterAvgBid и sum -> не изменяется",
                        averageCpaPerFilterStrategy(FILTER_AVG_CPA, FILTER_AVG_BID, null, SUM),
                        averageCpaPerFilterStrategy(FILTER_AVG_CPA, FILTER_AVG_BID, null, SUM)
                },
                new Object[]{
                        "AVG_CPA_PER_FILTER стратегия с filterAvgCpa, filterAvgBid и bid -> не изменяется",
                        averageCpaPerFilterStrategy(FILTER_AVG_CPA, FILTER_AVG_BID, BID, null),
                        averageCpaPerFilterStrategy(FILTER_AVG_CPA, FILTER_AVG_BID, BID, null)
                },
                new Object[]{
                        "AVG_CPA_PER_FILTER стратегия с filterAvgCpa, filterAvgBid, sum и bid -> не изменяется",
                        averageCpaPerFilterStrategy(FILTER_AVG_CPA, FILTER_AVG_BID, BID, SUM),
                        averageCpaPerFilterStrategy(FILTER_AVG_CPA, FILTER_AVG_BID, BID, SUM)
                },
                // AUTOBUDGET_AVG_CPA_PER_CAMP стратегия с AvgBid и без AvgCpa ->
                // переходит в AUTOBUDGET_AVG_CPC_PER_CAMP
                new Object[]{
                        "AVG_CPA_PER_CAMP стратегия только с AvgBid -> изменяется в AVG_CPС_PER_CAMP",
                        averageCpaPerCampStrategy(null, AVG_BID, null, null),
                        defaultAverageCpcPerCamprStrategy(AVG_BID, null, null)
                },
                new Object[]{
                        "AVG_CPA_PER_CAMP стратегия с AvgBid и sum -> изменяется в AVG_CPС_PER_CAMP",
                        averageCpaPerCampStrategy(null, AVG_BID, null, SUM),
                        defaultAverageCpcPerCamprStrategy(AVG_BID, null, SUM)
                },
                new Object[]{
                        "AVG_CPA_PER_CAMP стратегия с AvgBid и bid -> изменяется в AVG_CPС_PER_CAMP",
                        averageCpaPerCampStrategy(null, AVG_BID, BID, null),
                        defaultAverageCpcPerCamprStrategy(AVG_BID, BID, null)
                },
                new Object[]{
                        "AVG_CPA_PER_CAMP стратегия с AvgBid, bid и sum -> изменяется в AVG_CPС_PER_CAMP",
                        averageCpaPerCampStrategy(null, AVG_BID, BID, SUM),
                        defaultAverageCpcPerCamprStrategy(AVG_BID, BID, SUM)
                },
                // AUTOBUDGET_AVG_CPA_PER_CAMP стратегия с AvgBid и с AvgCpa -> не изменяется
                new Object[]{
                        "AVG_CPA_PER_FILTER стратегия с AvgCpa и AvgBid -> не изменяется",
                        averageCpaPerCampStrategy(AVG_CPA, AVG_BID, null, null),
                        averageCpaPerCampStrategy(AVG_CPA, AVG_BID, null, null)
                },
                new Object[]{
                        "AVG_CPA_PER_CAMP стратегия с AvgCpa, AvgBid и sum -> не изменяется",
                        averageCpaPerCampStrategy(AVG_CPA, AVG_BID, null, SUM),
                        averageCpaPerCampStrategy(AVG_CPA, AVG_BID, null, SUM)
                },
                new Object[]{
                        "AVG_CPA_PER_CAMP стратегия с AvgCpa, AvgBid и bid -> не изменяется",
                        averageCpaPerCampStrategy(AVG_CPA, AVG_BID, BID, null),
                        averageCpaPerCampStrategy(AVG_CPA, AVG_BID, BID, null)
                },
                new Object[]{
                        "AVG_CPA_PER_CAMP стратегия с AvgCpa, AvgBid, sum и bid -> не изменяется",
                        averageCpaPerCampStrategy(AVG_CPA, AVG_BID, BID, SUM),
                        averageCpaPerCampStrategy(AVG_CPA, AVG_BID, BID, SUM)
                },
                // AUTOBUDGET_AVG_CPC_PER_FILTER стратегия с filterAvgBid -> не изменяется
                new Object[]{
                        "AVG_CPС_PER_FILTER стратегия -> не изменяется",
                        defaultAverageCpcPerFilterStrategy(FILTER_AVG_BID, BID, SUM),
                        defaultAverageCpcPerFilterStrategy(FILTER_AVG_BID, BID, SUM)
                },
                // AUTOBUDGET_AVG_CPC_PER_CAMP стратегия с AvgBid -> не изменяется
                new Object[]{
                        "AVG_CPС_PER_CAMP стратегия -> не изменяется",
                        defaultAverageCpcPerCamprStrategy(AVG_BID, BID, SUM),
                        defaultAverageCpcPerCamprStrategy(AVG_BID, BID, SUM)
                }
        );
    }

    @Before
    public void before() {
        campaignInfo = steps.campaignSteps().createActivePerformanceCampaign();
        campaign = ((SmartCampaign) campaignTypedRepository
                .getTypedCampaigns(campaignInfo.getShard(), singletonList(campaignInfo.getCampaignId())).get(0));
    }

    @Test
    public void test() {
        ModelChanges<CampaignWithCustomStrategy> campModelChanges
                = ModelChanges.build(campaign, CampaignWithCustomStrategy.STRATEGY, dbStrategy);
        var appliedChangesList = List.of(campModelChanges.applyTo(campaign));
        RestrictedCampaignsUpdateOperationContainer updateParameters =
                RestrictedCampaignsUpdateOperationContainer.create(campaignInfo.getShard(), campaignInfo.getUid(),
                        campaignInfo.getClientId(), campaignInfo.getUid(), campaignInfo.getUid());
        campaignModifyRepository.updateCampaigns(updateParameters, appliedChangesList);

        oneshot.execute(null, null, campaignInfo.getShard());

        SmartCampaign campaignResult = ((SmartCampaign) campaignTypedRepository
                .getTypedCampaigns(campaignInfo.getShard(), singletonList(campaignInfo.getCampaignId())).get(0));

        assertThat(campaignResult.getStrategy())
                .is(matchedBy(beanDiffer(dbStrategyExpect)
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }
}
