package ru.yandex.direct.core.entity.strategy.converter

import org.assertj.core.api.Assertions.assertThat
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPackageStrategy
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgClick
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpa
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpaPerCamp
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpaPerFilter
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpcPerCamp
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpcPerFilter
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpi
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpv
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpvCustomPeriod
import ru.yandex.direct.core.entity.strategy.model.AutobudgetCrr
import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxImpressions
import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxImpressionsCustomPeriod
import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxReach
import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxReachCustomPeriod
import ru.yandex.direct.core.entity.strategy.model.AutobudgetMedia
import ru.yandex.direct.core.entity.strategy.model.AutobudgetRoi
import ru.yandex.direct.core.entity.strategy.model.AutobudgetWeekBundle
import ru.yandex.direct.core.entity.strategy.model.AutobudgetWeekSum
import ru.yandex.direct.core.entity.strategy.model.BaseStrategy
import ru.yandex.direct.core.entity.strategy.model.CommonStrategy
import ru.yandex.direct.core.entity.strategy.model.CpmDefault
import ru.yandex.direct.core.entity.strategy.model.DefaultManualStrategy
import ru.yandex.direct.core.entity.strategy.model.PeriodFixBid
import ru.yandex.direct.core.entity.strategy.service.converter.StrategyToCampaignConverterFacade
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.test.utils.RandomNumberUtils
import java.math.BigDecimal
import java.time.LocalDateTime

@CoreTest
@RunWith(Parameterized::class)
class StrategyToCampaignConverterTest(
    private val className: String,
    private val strategy: BaseStrategy,
    private val campaign: CampaignWithPackageStrategy,
    private val expectedCampaign: CampaignWithPackageStrategy,
    private val modelChanges: ModelChanges<CampaignWithPackageStrategy>,
    private val expectedModelChanges: ModelChanges<CampaignWithPackageStrategy>,
) {
    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    lateinit var facade: StrategyToCampaignConverterFacade

    @Autowired
    lateinit var campaignTypedRepository: CampaignTypedRepository

    companion object {
        private const val STRATEGY_ID = 17483L
        private const val CAMPAIGN_ID = 238L

        private val currency = Currencies.getCurrency(CurrencyCode.RUB)
        val clientId = RandomNumberUtils.nextPositiveLong()
        val metrikaCounters = listOf(RandomNumberUtils.nextPositiveLong())
        val meaningfulGoals = listOf(
            MeaningfulGoal()
                .withGoalId(RandomNumberUtils.nextPositiveLong())
                .withConversionValue(BigDecimal.TEN)
        )
        val walletId = RandomNumberUtils.nextPositiveLong()
        val converterTestUtils =
            ConverterTestUtils(clientId, STRATEGY_ID, CAMPAIGN_ID, walletId, currency, metrikaCounters, meaningfulGoals)

        @JvmStatic
        @Parameterized.Parameters(name = "Convert strategy with type {0} to campaign")
        fun params(): Collection<Array<Any?>> {
            val now = LocalDateTime.now()
            return listOf(

                arrayOf(
                    AutobudgetAvgClick::class.java.name,
                    converterTestUtils.autobudgetAvgClick(),
                    converterTestUtils.textCampaign(),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(TestCampaignsStrategy.defaultAutobudgetAvgClick()),
                    ModelChanges(CAMPAIGN_ID, CampaignWithPackageStrategy::class.java),
                    converterTestUtils.autobudgetAvgClickModelChanges(
                        now,
                        TextCampaign::class.java,
                        AutobudgetAvgClick::class.java
                    )
                ),

                arrayOf(
                    AutobudgetAvgCpa::class.java.name,
                    converterTestUtils.autobudgetAvgCpa(now),
                    converterTestUtils.textCampaign(),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(TestCampaignsStrategy.defaultAutobudgetAvgCpa(now)),
                    ModelChanges(CAMPAIGN_ID, CampaignWithPackageStrategy::class.java),
                    converterTestUtils.autobudgetAvgCpaModelChanges(
                        now,
                        TextCampaign::class.java,
                        AutobudgetAvgCpa::class.java
                    )
                ),

                arrayOf(
                    AutobudgetAvgCpaPerCamp::class.java.name,
                    converterTestUtils.autobudgetAvgCpaPerCamp(now),
                    converterTestUtils.textCampaign(),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(TestCampaignsStrategy.defaultAutobudgetAvgCpaPerCamp(now)),
                    ModelChanges(CAMPAIGN_ID, CampaignWithPackageStrategy::class.java),
                    converterTestUtils.autobudgetAvgCpaPerCampModelChanges(
                        now,
                        TextCampaign::class.java,
                        AutobudgetAvgCpaPerCamp::class.java
                    )
                ),

                arrayOf(
                    AutobudgetAvgCpaPerFilter::class.java.name,
                    converterTestUtils.autobudgetAvgCpaPerFilter(now),
                    converterTestUtils.textCampaign(),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(TestCampaignsStrategy.defaultAutobudgetAvgCpaPerFilter(now)),
                    ModelChanges(CAMPAIGN_ID, CampaignWithPackageStrategy::class.java),
                    converterTestUtils.autobudgetAvgCpaPerFilterModelChanges(
                        now,
                        TextCampaign::class.java,
                        AutobudgetAvgCpaPerFilter::class.java
                    )
                ),

                arrayOf(
                    AutobudgetAvgCpcPerCamp::class.java.name,
                    converterTestUtils.autobudgetAvgCpcPerCamp(),
                    converterTestUtils.textCampaign(),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(TestCampaignsStrategy.defaultAutobudgetAvgCpcPerCamp()),
                    ModelChanges(CAMPAIGN_ID, CampaignWithPackageStrategy::class.java),
                    converterTestUtils.autobudgetAvgCpcPerCampModelChanges(
                        now,
                        TextCampaign::class.java,
                        AutobudgetAvgCpcPerCamp::class.java
                    )
                ),

                arrayOf(
                    AutobudgetAvgCpcPerFilter::class.java.name,
                    converterTestUtils.autobudgetAvgCpcPerFilter(),
                    converterTestUtils.textCampaign(),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(TestCampaignsStrategy.defaultAutobudgetAvgCpcPerFilter()),
                    ModelChanges(CAMPAIGN_ID, CampaignWithPackageStrategy::class.java),
                    converterTestUtils.autobudgetAvgCpcPerFilterModelChanges(
                        now,
                        TextCampaign::class.java,
                        AutobudgetAvgCpcPerFilter::class.java
                    )
                ),

                arrayOf(
                    AutobudgetAvgCpi::class.java.name,
                    converterTestUtils.autobudgetAvgCpi(now),
                    converterTestUtils.mobileCampaign(),
                    converterTestUtils.mobileCampaign()
                        .withStrategy(TestCampaignsStrategy.defaultAutobudgetAvgCpiStrategy(now)),
                    ModelChanges(CAMPAIGN_ID, CampaignWithPackageStrategy::class.java),
                    converterTestUtils.autobudgetAvgCpiModelChanges(
                        now,
                        MobileContentCampaign::class.java,
                        AutobudgetAvgCpi::class.java
                    )
                ),

                arrayOf(
                    AutobudgetAvgCpv::class.java.name,
                    converterTestUtils.autobudgetAvgCpv(),
                    converterTestUtils.textCampaign(),
                    converterTestUtils.textCampaign(metrikaCounters)
                        .withStrategy(TestCampaignsStrategy.defaultAutobudgetAvgCpv()),
                    ModelChanges(CAMPAIGN_ID, CampaignWithPackageStrategy::class.java),
                    converterTestUtils.autobudgetAvgCpvModelChanges(
                        now,
                        TextCampaign::class.java,
                        AutobudgetAvgCpv::class.java
                    )
                ),

                arrayOf(
                    AutobudgetAvgCpvCustomPeriod::class.java.name,
                    converterTestUtils.autobudgetAvgCpvCustomPeriod(now),
                    converterTestUtils.textCampaign(),
                    converterTestUtils.textCampaign(metrikaCounters)
                        .withStrategy(TestCampaignsStrategy.defaultAutobudgetAvgCpvCustomPeriod(now)),
                    ModelChanges(CAMPAIGN_ID, CampaignWithPackageStrategy::class.java),
                    converterTestUtils.autobudgetAvgCpvCustomPeriodModelChanges(
                        now,
                        TextCampaign::class.java,
                        AutobudgetAvgCpvCustomPeriod::class.java
                    )
                ),

                arrayOf(
                    AutobudgetCrr::class.java.name,
                    converterTestUtils.autobudgetCrr(now),
                    converterTestUtils.textCampaign(),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(TestCampaignsStrategy.defaultAutobudgetCrr(now)),
                    ModelChanges(CAMPAIGN_ID, CampaignWithPackageStrategy::class.java),
                    converterTestUtils.autobudgetCrrModelChanges(
                        now,
                        TextCampaign::class.java,
                        AutobudgetCrr::class.java
                    )
                ),

                arrayOf(
                    AutobudgetMaxImpressions::class.java.name,
                    converterTestUtils.autobudgetMaxImpressions(),
                    converterTestUtils.textCampaign(),
                    converterTestUtils.textCampaign(metrikaCounters)
                        .withStrategy(TestCampaignsStrategy.defaultAutobudgetMaxImpressions()),
                    ModelChanges(CAMPAIGN_ID, CampaignWithPackageStrategy::class.java),
                    converterTestUtils.autobudgetMaxImpressionsModelChanges(
                        now,
                        TextCampaign::class.java,
                        AutobudgetMaxImpressions::class.java
                    )
                ),

                arrayOf(
                    AutobudgetMaxImpressionsCustomPeriod::class.java.name,
                    converterTestUtils.autobudgetMaxImpressionsCustomPeriod(now),
                    converterTestUtils.textCampaign(),
                    converterTestUtils.textCampaign(metrikaCounters)
                        .withStrategy(TestCampaignsStrategy.defaultAutobudgetMaxImpressionsCustomPeriod(now)),
                    ModelChanges(CAMPAIGN_ID, CampaignWithPackageStrategy::class.java),
                    converterTestUtils.autobudgetMaxImpressionsCustomPeriodModelChanges(
                        now,
                        TextCampaign::class.java,
                        AutobudgetMaxImpressionsCustomPeriod::class.java
                    )
                ),

                arrayOf(
                    AutobudgetMaxReach::class.java.name,
                    converterTestUtils.autobudgetMaxReach(),
                    converterTestUtils.textCampaign(),
                    converterTestUtils.textCampaign(metrikaCounters)
                        .withStrategy(TestCampaignsStrategy.defaultAutobudgetMaxReach()),
                    ModelChanges(CAMPAIGN_ID, CampaignWithPackageStrategy::class.java),
                    converterTestUtils.autobudgetMaxReachModelChanges(
                        now,
                        TextCampaign::class.java,
                        AutobudgetMaxReach::class.java
                    )
                ),

                arrayOf(
                    AutobudgetMaxReachCustomPeriod::class.java.name,
                    converterTestUtils.autobudgetMaxReachCustomPeriod(now),
                    converterTestUtils.textCampaign(),
                    converterTestUtils.textCampaign(metrikaCounters)
                        .withStrategy(TestCampaignsStrategy.defaultAutobudgetMaxReachCustomPeriod(now)),
                    ModelChanges(CAMPAIGN_ID, CampaignWithPackageStrategy::class.java),
                    converterTestUtils.autobudgetMaxReachCustomPeriodModelChanges(
                        now,
                        TextCampaign::class.java,
                        AutobudgetMaxReachCustomPeriod::class.java
                    )
                ),

                arrayOf(
                    AutobudgetMedia::class.java.name,
                    converterTestUtils.autobudgetMedia(),
                    converterTestUtils.textCampaign(),
                    converterTestUtils.textCampaign(metrikaCounters)
                        .withStrategy(TestCampaignsStrategy.defaultAutobudgetMedia(now)),
                    ModelChanges(CAMPAIGN_ID, CampaignWithPackageStrategy::class.java),
                    converterTestUtils.autobudgetMediaModelChanges(
                        now,
                        TextCampaign::class.java,
                        AutobudgetMedia::class.java
                    )
                ),

                arrayOf(
                    AutobudgetRoi::class.java.name,
                    converterTestUtils.autobudgetRoi(),
                    converterTestUtils.textCampaign(),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(TestCampaignsStrategy.defaultAutobudgetRoi()),
                    ModelChanges(CAMPAIGN_ID, CampaignWithPackageStrategy::class.java),
                    converterTestUtils.autobudgetRoiModelChanges(
                        now,
                        TextCampaign::class.java,
                        AutobudgetRoi::class.java
                    )
                ),

                arrayOf(
                    AutobudgetWeekBundle::class.java.name,
                    converterTestUtils.autobudgetWeekBundle(),
                    converterTestUtils.textCampaign(),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(TestCampaignsStrategy.defaultAutobudgetWeekBundle()),
                    ModelChanges(CAMPAIGN_ID, CampaignWithPackageStrategy::class.java),
                    converterTestUtils.autobudgetWeekBundleModelChanges(
                        now,
                        TextCampaign::class.java,
                        AutobudgetWeekBundle::class.java
                    )
                ),

                arrayOf(
                    AutobudgetWeekSum::class.java.name,
                    converterTestUtils.autobudgetWeekSum(now),
                    converterTestUtils.textCampaign(),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(TestCampaignsStrategy.defaultAutobudgetWeekSum(now)),
                    ModelChanges(CAMPAIGN_ID, CampaignWithPackageStrategy::class.java),
                    converterTestUtils.autobudgetWeekSumModelChanges(
                        now,
                        TextCampaign::class.java,
                        AutobudgetWeekSum::class.java
                    )
                ),

                arrayOf(
                    CpmDefault::class.java.name,
                    converterTestUtils.cpmDefault(now),
                    converterTestUtils.textCampaign(),
                    converterTestUtils.textCampaign(metrikaCounters)
                        .withDayBudget(BigDecimal.TEN)
                        .withDayBudgetLastChange(now)
                        .withDayBudgetShowMode(DayBudgetShowMode.DEFAULT_)
                        .withDayBudgetDailyChangeCount(1)
                        .withStrategy(TestCampaignsStrategy.defaultCpmDefault(now)),
                    ModelChanges(CAMPAIGN_ID, CampaignWithPackageStrategy::class.java),
                    converterTestUtils.cpmDefaultModelChanges(now, TextCampaign::class.java, CpmDefault::class.java)
                ),

                arrayOf(
                    DefaultManualStrategy::class.java.name,
                    converterTestUtils.defaultManualStrategy(now),
                    converterTestUtils.textCampaign(),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withEnableCpcHold(true)
                        .withDayBudget(BigDecimal.TEN)
                        .withDayBudgetDailyChangeCount(1)
                        .withDayBudgetLastChange(now)
                        .withDayBudgetShowMode(DayBudgetShowMode.DEFAULT_)
                        .withDayBudgetDailyChangeCount(1)
                        .withStrategy(TestCampaignsStrategy.defaultDefaultManualStrategy(now)),
                    ModelChanges(CAMPAIGN_ID, CampaignWithPackageStrategy::class.java),
                    converterTestUtils.defaultManualStrategyModelChanges(
                        now,
                        TextCampaign::class.java,
                        DefaultManualStrategy::class.java
                    )
                ),

                arrayOf(
                    PeriodFixBid::class.java.name,
                    converterTestUtils.periodFixBid(now),
                    converterTestUtils.textCampaign(),
                    converterTestUtils.textCampaign(metrikaCounters)
                        .withStrategy(TestCampaignsStrategy.defaultPeriodFixBid(now)),
                    ModelChanges(CAMPAIGN_ID, CampaignWithPackageStrategy::class.java),
                    converterTestUtils.periodFixBidModelChanges(now, TextCampaign::class.java, PeriodFixBid::class.java)
                ),

                arrayOf(
                    CpmDefault::class.java.name,
                    converterTestUtils.cpmDefault(now),
                    converterTestUtils.textCampaign()
                        .withDayBudgetLastChange(now.minusDays(1))
                        .withStrategy(TestCampaignsStrategy.defaultCpmDefault(now.minusDays(1))),
                    converterTestUtils.textCampaign(metrikaCounters)
                        .withDayBudget(BigDecimal.TEN)
                        .withDayBudgetLastChange(now)
                        .withDayBudgetShowMode(DayBudgetShowMode.DEFAULT_)
                        .withDayBudgetDailyChangeCount(1)
                        .withStrategy(TestCampaignsStrategy.defaultCpmDefault(now)),
                    ModelChanges(CAMPAIGN_ID, CampaignWithPackageStrategy::class.java),
                    converterTestUtils.cpmDefaultModelChanges(now, TextCampaign::class.java, CpmDefault::class.java)
                ),
            )
        }

        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Test
    fun convert() {
        val now = LocalDateTime.now()
        (strategy as CommonStrategy).withCids(listOf(campaign.id))
        facade.copyStrategyToCampaign(now, strategy, campaign)
        assertThat(campaign.lastChange).isNotNull
        campaign.lastChange = null

        facade.copyStrategyToCampaignModelChanges(now, strategy, modelChanges, campaign.javaClass)
        assertThat(modelChanges.getPropIfChanged(CampaignWithPackageStrategy.LAST_CHANGE)).isNotNull
        modelChanges.process(null, CampaignWithPackageStrategy.LAST_CHANGE)

        expectedCampaign.strategy.platform = null
        expectedCampaign.strategy.strategy = null

        expectedModelChanges.getChangedProp(CampaignWithPackageStrategy.STRATEGY)?.platform = null
        expectedModelChanges.getChangedProp(CampaignWithPackageStrategy.STRATEGY)?.strategy = null

        assertThat(campaign).usingRecursiveComparison().isEqualTo(expectedCampaign)
        assertThat(modelChanges).usingRecursiveComparison().isEqualTo(expectedModelChanges)
    }
}
