package ru.yandex.direct.core.entity.strategy.converter

import java.math.BigDecimal
import java.time.LocalDateTime.now
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPackageStrategy
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal
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
import ru.yandex.direct.core.entity.strategy.service.converter.CampaignToStrategyConverterService
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetAvgClick
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetAvgCpa
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetAvgCpaPerCamp
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetAvgCpaPerFilter
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetAvgCpcPerCamp
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetAvgCpcPerFilter
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetAvgCpiStrategy
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetAvgCpv
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetAvgCpvCustomPeriod
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetCrr
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetMaxImpressions
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetMaxImpressionsCustomPeriod
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetMaxReach
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetMaxReachCustomPeriod
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetMedia
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetRoi
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetWeekBundle
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetWeekSum
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultCpmDefault
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultDefaultManualStrategy
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultPeriodFixBid
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.test.utils.RandomNumberUtils

@CoreTest
@RunWith(Parameterized::class)
class CampaignToStrategyConverterServiceTest(
    private val className: String,
    private val expectedStrategy: BaseStrategy,
    private val campaign: CampaignWithPackageStrategy
) {
    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    lateinit var campaignToStrategyConverterService: CampaignToStrategyConverterService

    @Autowired
    lateinit var steps: Steps

    @Autowired
    lateinit var campaignTypedRepository: CampaignTypedRepository

    lateinit var defaultUser: UserInfo
    lateinit var campaignInfo: TextCampaignInfo

    val now = now()

    companion object {
        private val currency = Currencies.getCurrency(CurrencyCode.RUB)

        val strategyId = RandomNumberUtils.nextPositiveLong()
        val clientId = RandomNumberUtils.nextPositiveLong()
        val metrikaCounters = listOf(RandomNumberUtils.nextPositiveLong())
        val meaningfulGoals = listOf(
            MeaningfulGoal()
                .withGoalId(RandomNumberUtils.nextPositiveLong())
                .withConversionValue(BigDecimal.TEN)
        )
        val walletId = RandomNumberUtils.nextPositiveLong()
        val cid = RandomNumberUtils.nextPositiveLong()

        val converterTestUtils = ConverterTestUtils(
            clientId,
            strategyId,
            cid,
            walletId,
            currency,
            metrikaCounters,
            meaningfulGoals
        )

        @JvmStatic
        @Parameterized.Parameters(name = "Convert campaign to strategy with type {0}")
        fun params(): Collection<Array<Any?>> {

            val now = now()
            return listOf(

                arrayOf(
                    AutobudgetAvgClick::class.java.name,
                    converterTestUtils.autobudgetAvgClick(),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(defaultAutobudgetAvgClick())
                ),

                arrayOf(
                    AutobudgetAvgCpa::class.java.name,
                    converterTestUtils.autobudgetAvgCpa(now),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(defaultAutobudgetAvgCpa(now))
                ),

                arrayOf(
                    AutobudgetAvgCpaPerCamp::class.java.name,
                    converterTestUtils.autobudgetAvgCpaPerCamp(now),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(defaultAutobudgetAvgCpaPerCamp(now))
                ),

                arrayOf(
                    AutobudgetAvgCpaPerFilter::class.java.name,
                    converterTestUtils.autobudgetAvgCpaPerFilter(now),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(defaultAutobudgetAvgCpaPerFilter(now))
                ),

                arrayOf(
                    AutobudgetAvgCpcPerCamp::class.java.name,
                    converterTestUtils.autobudgetAvgCpcPerCamp(),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(defaultAutobudgetAvgCpcPerCamp())
                ),

                arrayOf(
                    AutobudgetAvgCpcPerFilter::class.java.name,
                    converterTestUtils.autobudgetAvgCpcPerFilter(),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(defaultAutobudgetAvgCpcPerFilter())
                ),

                arrayOf(
                    AutobudgetAvgCpi::class.java.name,
                    converterTestUtils.autobudgetAvgCpi(now)
                        .withLastBidderRestartTime(now),
                    converterTestUtils.mobileCampaign()
                        .withStrategy(defaultAutobudgetAvgCpiStrategy(now))
                ),

                arrayOf(
                    AutobudgetAvgCpv::class.java.name,
                    converterTestUtils.autobudgetAvgCpv(),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(defaultAutobudgetAvgCpv())
                ),

                arrayOf(
                    AutobudgetAvgCpvCustomPeriod::class.java.name,
                    converterTestUtils.autobudgetAvgCpvCustomPeriod(now),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(defaultAutobudgetAvgCpvCustomPeriod(now))
                ),

                arrayOf(
                    AutobudgetCrr::class.java.name,
                    converterTestUtils.autobudgetCrr(now),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(defaultAutobudgetCrr(now))
                ),

                arrayOf(
                    AutobudgetMaxImpressions::class.java.name,
                    converterTestUtils.autobudgetMaxImpressions(),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(defaultAutobudgetMaxImpressions())
                ),

                arrayOf(
                    AutobudgetMaxImpressionsCustomPeriod::class.java.name,
                    converterTestUtils.autobudgetMaxImpressionsCustomPeriod(now),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(defaultAutobudgetMaxImpressionsCustomPeriod(now))
                ),

                arrayOf(
                    AutobudgetMaxReach::class.java.name,
                    converterTestUtils.autobudgetMaxReach(),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(defaultAutobudgetMaxReach())
                ),

                arrayOf(
                    AutobudgetMaxReachCustomPeriod::class.java.name,
                    converterTestUtils.autobudgetMaxReachCustomPeriod(now),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(defaultAutobudgetMaxReachCustomPeriod(now))
                ),

                arrayOf(
                    AutobudgetMedia::class.java.name,
                    converterTestUtils.autobudgetMedia(),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(defaultAutobudgetMedia(now))
                ),

                arrayOf(
                    AutobudgetRoi::class.java.name,
                    converterTestUtils.autobudgetRoi(),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(defaultAutobudgetRoi())
                ),

                arrayOf(
                    AutobudgetWeekBundle::class.java.name,
                    converterTestUtils.autobudgetWeekBundle(),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(defaultAutobudgetWeekBundle())
                ),

                arrayOf(
                    AutobudgetWeekSum::class.java.name,
                    converterTestUtils.autobudgetWeekSum(now),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(defaultAutobudgetWeekSum(now))
                ),

                arrayOf(
                    CpmDefault::class.java.name,
                    converterTestUtils.cpmDefault(now),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withDayBudget(BigDecimal.TEN)
                        .withDayBudgetLastChange(now)
                        .withDayBudgetShowMode(DayBudgetShowMode.DEFAULT_)
                        .withDayBudgetDailyChangeCount(1)
                        .withStrategy(defaultCpmDefault(now))
                ),

                arrayOf(
                    DefaultManualStrategy::class.java.name,
                    converterTestUtils.defaultManualStrategy(now),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withEnableCpcHold(true)
                        .withDayBudget(BigDecimal.TEN)
                        .withDayBudgetDailyChangeCount(1)
                        .withDayBudgetLastChange(now)
                        .withDayBudgetShowMode(DayBudgetShowMode.DEFAULT_)
                        .withDayBudgetDailyChangeCount(1)
                        .withStrategy(defaultDefaultManualStrategy(now))
                ),

                arrayOf(
                    PeriodFixBid::class.java.name,
                    converterTestUtils.periodFixBid(now),
                    converterTestUtils.textCampaign(metrikaCounters, meaningfulGoals)
                        .withStrategy(defaultPeriodFixBid(now))
                )
            )
        }

        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Before
    fun before() {
        defaultUser = steps.userSteps().createDefaultUser()
    }

    @Test
    fun convertWithIdEqualToOrderId() {
        val strategy = campaignToStrategyConverterService.toStrategyWithIdEqualToCidWithOffset(
            ClientId.fromLong(clientId),
            now,
            campaign
        )
        checkStrategyFilledCorrectly(strategy, campaign.id + BsOrderIdCalculator.ORDER_ID_OFFSET)
    }

    @Test
    fun convertWithIdEqualToCampaignStrategyId() {
        val strategy = campaignToStrategyConverterService.toStrategyWithIdEqualToCampaignStrategyId(
            ClientId.fromLong(clientId),
            now,
            campaign
        )
        checkStrategyFilledCorrectly(strategy, campaign.strategyId)
    }

    @Test
    fun convertWithNotFilledId() {
        val strategy = campaignToStrategyConverterService.toStrategyWithNotFilledId(
            ClientId.fromLong(clientId),
            now,
            campaign
        )
        checkStrategyFilledCorrectly(strategy, null)
    }

    private fun checkStrategyFilledCorrectly(strategy: BaseStrategy, expectedStrategyId: Long?) {
        assertThat((strategy as CommonStrategy).lastChange).isNotNull
        strategy.lastChange = null

        expectedStrategy.withId(expectedStrategyId)
        (expectedStrategy as CommonStrategy).withCids(listOf(campaign.id))
        assertThat(strategy).usingRecursiveComparison().isEqualTo(expectedStrategy)
    }
}
