package ru.yandex.direct.core.entity.strategy.converter

import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel
import ru.yandex.direct.core.entity.campaign.model.CampaignWithAttributionModel
import ru.yandex.direct.core.entity.campaign.model.CampaignWithDayBudget
import ru.yandex.direct.core.entity.campaign.model.CampaignWithEnableCpcHoldForbidden
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMeaningfulGoals
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMetrikaCounters
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPackageStrategy
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.model.WithDayBudget
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
import ru.yandex.direct.core.entity.strategy.model.CommonStrategy
import ru.yandex.direct.core.entity.strategy.model.CpmDefault
import ru.yandex.direct.core.entity.strategy.model.DefaultManualStrategy
import ru.yandex.direct.core.entity.strategy.model.PeriodFixBid
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel
import ru.yandex.direct.core.entity.strategy.model.StrategyDayBudgetShowMode
import ru.yandex.direct.core.entity.strategy.model.StrategyName
import ru.yandex.direct.core.entity.strategy.model.StrategyWithDayBudget
import ru.yandex.direct.core.entity.strategy.model.StrategyWithMeaningfulGoals
import ru.yandex.direct.core.entity.strategy.model.StrategyWithMetrikaCounters
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy
import ru.yandex.direct.currency.Currency
import ru.yandex.direct.model.ModelChanges
import java.math.BigDecimal
import java.time.LocalDateTime

class ConverterTestUtils(
    val clientId: Long,
    val strategyId: Long,
    val cid: Long,
    val walletId: Long,
    val currency: Currency,
    val metrikaCounters: List<Long>,
    val meaningfulGoals: List<MeaningfulGoal>
) {

    fun periodFixBid(now: LocalDateTime) =
        fillStrategyCommonFields(PeriodFixBid(), StrategyName.PERIOD_FIX_BID)
            .withMetrikaCounters(metrikaCounters)
            .withStart(now.toLocalDate())
            .withFinish(now.toLocalDate().plusDays(1))
            .withAutoProlongation(true)
            .withBudget(currency.minAutobudget.multiply(2L.toBigDecimal()))

    fun periodFixBidModelChanges(
        now: LocalDateTime,
        campaignClass: Class<out BaseCampaign>,
        strategyClass: Class<out CommonStrategy>
    ): ModelChanges<CampaignWithPackageStrategy> {

        return fillCampaignModelChangesCommonFields(campaignClass, strategyClass, now)
            .process(TestCampaignsStrategy.defaultPeriodFixBid(now), CampaignWithPackageStrategy.STRATEGY)
    }

    fun defaultManualStrategy(now: LocalDateTime?) =
        fillStrategyCommonFields(DefaultManualStrategy(), StrategyName.DEFAULT_)
            .withDayBudget(BigDecimal.TEN)
            .withEnableCpcHold(true)
            .withDayBudget(BigDecimal.TEN)
            .withDayBudgetLastChange(now)
            .withDayBudgetShowMode(StrategyDayBudgetShowMode.DEFAULT_)
            .withDayBudgetDailyChangeCount(1)
            .withMetrikaCounters(metrikaCounters)
            .withMeaningfulGoals(meaningfulGoals)

    fun defaultManualStrategyModelChanges(
        now: LocalDateTime,
        campaignClass: Class<out BaseCampaign>,
        strategyClass: Class<out CommonStrategy>
    ): ModelChanges<CampaignWithPackageStrategy> {

        return fillCampaignModelChangesCommonFields(campaignClass, strategyClass, now)
            .process(TestCampaignsStrategy.defaultDefaultManualStrategy(now), CampaignWithPackageStrategy.STRATEGY)
            .process(true, CampaignWithPackageStrategy.ENABLE_CPC_HOLD)
    }

    fun cpmDefault(now: LocalDateTime?) =
        fillStrategyCommonFields(CpmDefault(), StrategyName.CPM_DEFAULT)
            .withDayBudget(BigDecimal.TEN)
            .withDayBudgetDailyChangeCount(1)
            .withDayBudgetLastChange(now)
            .withDayBudgetShowMode(StrategyDayBudgetShowMode.DEFAULT_)
            .withMetrikaCounters(metrikaCounters)

    fun cpmDefaultModelChanges(
        now: LocalDateTime,
        campaignClass: Class<out BaseCampaign>,
        strategyClass: Class<out CommonStrategy>
    ): ModelChanges<CampaignWithPackageStrategy> {

        return fillCampaignModelChangesCommonFields(campaignClass, strategyClass, now)
            .process(TestCampaignsStrategy.defaultCpmDefault(now), CampaignWithPackageStrategy.STRATEGY)
    }

    fun autobudgetWeekSum(now: LocalDateTime) =
        fillStrategyCommonFields(AutobudgetWeekSum(), StrategyName.AUTOBUDGET)
            .withMetrikaCounters(metrikaCounters)
            .withMeaningfulGoals(meaningfulGoals)
            .withLastBidderRestartTime(now)
            .withSum(currency.minAutobudget)
            .withBid(BigDecimal.TEN)

    fun autobudgetWeekSumModelChanges(
        now: LocalDateTime,
        campaignClass: Class<out BaseCampaign>,
        strategyClass: Class<out CommonStrategy>
    ): ModelChanges<CampaignWithPackageStrategy> {

        return fillCampaignModelChangesCommonFields(campaignClass, strategyClass, now)
            .process(TestCampaignsStrategy.defaultAutobudgetWeekSum(now), CampaignWithPackageStrategy.STRATEGY)
    }

    fun autobudgetRoi() =
        fillStrategyCommonFields(AutobudgetRoi(), StrategyName.AUTOBUDGET_ROI)
            .withMeaningfulGoals(meaningfulGoals)
            .withMetrikaCounters(metrikaCounters)
            .withSum(currency.minAutobudget)
            .withGoalId(12345L)
            .withBid(BigDecimal.TEN)
            .withProfitability(BigDecimal.TEN)
            .withRoiCoef(BigDecimal.ONE)
            .withReserveReturn(10L)

    fun autobudgetRoiModelChanges(
        now: LocalDateTime,
        campaignClass: Class<out BaseCampaign>,
        strategyClass: Class<out CommonStrategy>
    ): ModelChanges<CampaignWithPackageStrategy> {

        return fillCampaignModelChangesCommonFields(campaignClass, strategyClass, now)
            .process(TestCampaignsStrategy.defaultAutobudgetRoi(), CampaignWithPackageStrategy.STRATEGY)
    }

    fun autobudgetMedia() =
        fillStrategyCommonFields(AutobudgetMedia(), StrategyName.AUTOBUDGET_MEDIA)
            .withMetrikaCounters(metrikaCounters)

    fun autobudgetMediaModelChanges(
        now: LocalDateTime,
        campaignClass: Class<out BaseCampaign>,
        strategyClass: Class<out CommonStrategy>
    ): ModelChanges<CampaignWithPackageStrategy> {

        return fillCampaignModelChangesCommonFields(campaignClass, strategyClass, now)
            .process(TestCampaignsStrategy.defaultAutobudgetMedia(now), CampaignWithPackageStrategy.STRATEGY)
    }

    fun autobudgetMaxReachCustomPeriod(now: LocalDateTime) =
        fillStrategyCommonFields(
            AutobudgetMaxReachCustomPeriod(),
            StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD
        )
            .withMetrikaCounters(metrikaCounters)
            .withAvgCpm(BigDecimal.valueOf(22))
            .withStart(now.toLocalDate())
            .withFinish(now.toLocalDate().plusDays(1))
            .withLastUpdateTime(now)
            .withDailyChangeCount(1)
            .withBudget(currency.minAutobudget.multiply(2L.toBigDecimal()))
            .withAutoProlongation(true)

    fun autobudgetMaxReachCustomPeriodModelChanges(
        now: LocalDateTime,
        campaignClass: Class<out BaseCampaign>,
        strategyClass: Class<out CommonStrategy>
    ): ModelChanges<CampaignWithPackageStrategy> {

        return fillCampaignModelChangesCommonFields(campaignClass, strategyClass, now)
            .process(
                TestCampaignsStrategy.defaultAutobudgetMaxReachCustomPeriod(now),
                CampaignWithPackageStrategy.STRATEGY
            )
    }

    fun autobudgetMaxReach() =
        fillStrategyCommonFields(AutobudgetMaxReach(), StrategyName.AUTOBUDGET_MAX_REACH)
            .withMetrikaCounters(metrikaCounters)
            .withAvgCpm(BigDecimal.valueOf(22))
            .withSum(currency.minAutobudget.multiply(7L.toBigDecimal()))

    fun autobudgetMaxReachModelChanges(
        now: LocalDateTime,
        campaignClass: Class<out BaseCampaign>,
        strategyClass: Class<out CommonStrategy>
    ): ModelChanges<CampaignWithPackageStrategy> {

        return fillCampaignModelChangesCommonFields(campaignClass, strategyClass, now)
            .process(TestCampaignsStrategy.defaultAutobudgetMaxReach(), CampaignWithPackageStrategy.STRATEGY)
    }

    fun autobudgetMaxImpressionsCustomPeriod(now: LocalDateTime) =
        fillStrategyCommonFields(
            AutobudgetMaxImpressionsCustomPeriod(),
            StrategyName.AUTOBUDGET_MAX_IMPRESSIONS_CUSTOM_PERIOD
        )
            .withMetrikaCounters(metrikaCounters)
            .withStart(now.toLocalDate())
            .withFinish(now.toLocalDate().plusDays(1))
            .withLastUpdateTime(now)
            .withDailyChangeCount(1)
            .withAutoProlongation(true)
            .withBudget(currency.minAutobudget.multiply(2L.toBigDecimal()))
            .withAvgCpm(BigDecimal.valueOf(22))

    fun autobudgetMaxImpressionsCustomPeriodModelChanges(
        now: LocalDateTime,
        campaignClass: Class<out BaseCampaign>,
        strategyClass: Class<out CommonStrategy>
    ): ModelChanges<CampaignWithPackageStrategy> {

        return fillCampaignModelChangesCommonFields(campaignClass, strategyClass, now)
            .process(
                TestCampaignsStrategy.defaultAutobudgetMaxImpressionsCustomPeriod(now),
                CampaignWithPackageStrategy.STRATEGY
            )
    }

    fun autobudgetMaxImpressions() =
        fillStrategyCommonFields(AutobudgetMaxImpressions(), StrategyName.AUTOBUDGET_MAX_IMPRESSIONS)
            .withMetrikaCounters(metrikaCounters)
            .withAvgCpm(BigDecimal.valueOf(22))
            .withSum(currency.minAutobudget.multiply(7L.toBigDecimal()))

    fun autobudgetMaxImpressionsModelChanges(
        now: LocalDateTime,
        campaignClass: Class<out BaseCampaign>,
        strategyClass: Class<out CommonStrategy>
    ): ModelChanges<CampaignWithPackageStrategy> {

        return fillCampaignModelChangesCommonFields(campaignClass, strategyClass, now)
            .process(TestCampaignsStrategy.defaultAutobudgetMaxImpressions(), CampaignWithPackageStrategy.STRATEGY)
    }

    fun autobudgetCrr(now: LocalDateTime?) =
        fillStrategyCommonFields(AutobudgetCrr(), StrategyName.AUTOBUDGET_CRR)
            .withMeaningfulGoals(meaningfulGoals)
            .withMetrikaCounters(metrikaCounters)
            .withGoalId(12345L)
            .withLastBidderRestartTime(now)
            .withCrr(12)
            .withIsPayForConversionEnabled(false)
            .withSum(currency.minAutobudget)

    fun autobudgetCrrModelChanges(
        now: LocalDateTime,
        campaignClass: Class<out BaseCampaign>,
        strategyClass: Class<out CommonStrategy>
    ): ModelChanges<CampaignWithPackageStrategy> {

        return fillCampaignModelChangesCommonFields(campaignClass, strategyClass, now)
            .process(TestCampaignsStrategy.defaultAutobudgetCrr(now), CampaignWithPackageStrategy.STRATEGY)
    }

    fun autobudgetAvgCpvCustomPeriod(now: LocalDateTime) =
        fillStrategyCommonFields(AutobudgetAvgCpvCustomPeriod(), StrategyName.AUTOBUDGET_AVG_CPV_CUSTOM_PERIOD)
            .withMetrikaCounters(metrikaCounters)
            .withAvgCpv(currency.maxAvgCpv)
            .withStart(now.toLocalDate())
            .withFinish(now.toLocalDate().plusDays(1))
            .withLastUpdateTime(now)
            .withDailyChangeCount(1)
            .withBudget(currency.minAutobudget.multiply(2L.toBigDecimal()))
            .withAutoProlongation(false)

    fun autobudgetAvgCpvCustomPeriodModelChanges(
        now: LocalDateTime,
        campaignClass: Class<out BaseCampaign>,
        strategyClass: Class<out CommonStrategy>
    ): ModelChanges<CampaignWithPackageStrategy> {

        return fillCampaignModelChangesCommonFields(campaignClass, strategyClass, now)
            .process(
                TestCampaignsStrategy.defaultAutobudgetAvgCpvCustomPeriod(now),
                CampaignWithPackageStrategy.STRATEGY
            )
    }

    fun autobudgetAvgCpv() =
        fillStrategyCommonFields(AutobudgetAvgCpv(), StrategyName.AUTOBUDGET_AVG_CPV)
            .withMetrikaCounters(metrikaCounters)
            .withAvgCpv(currency.maxAvgCpv)
            .withSum(currency.minAutobudget.multiply(7L.toBigDecimal()))

    fun autobudgetAvgCpvModelChanges(
        now: LocalDateTime,
        campaignClass: Class<out BaseCampaign>,
        strategyClass: Class<out CommonStrategy>
    ): ModelChanges<CampaignWithPackageStrategy> {

        return fillCampaignModelChangesCommonFields(campaignClass, strategyClass, now)
            .process(TestCampaignsStrategy.defaultAutobudgetAvgCpv(), CampaignWithPackageStrategy.STRATEGY)
    }

    fun autobudgetAvgCpi(now: LocalDateTime) =
        fillStrategyCommonFields(AutobudgetAvgCpi(), StrategyName.AUTOBUDGET_AVG_CPI)
            .withSum(BigDecimal.valueOf(10000))
            .withGoalId(12345L)
            .withAvgCpi(BigDecimal.valueOf(55))
            .withLastBidderRestartTime(now)
            .withBid(null)

    fun autobudgetAvgCpiModelChanges(
        now: LocalDateTime,
        campaignClass: Class<out BaseCampaign>,
        strategyClass: Class<out CommonStrategy>
    ): ModelChanges<CampaignWithPackageStrategy> {

        return fillCampaignModelChangesCommonFields(campaignClass, strategyClass, now)
            .process(TestCampaignsStrategy.defaultAutobudgetAvgCpiStrategy(now), CampaignWithPackageStrategy.STRATEGY)
    }

    fun autobudgetAvgCpcPerFilter() =
        fillStrategyCommonFields(AutobudgetAvgCpcPerFilter(), StrategyName.AUTOBUDGET_AVG_CPC_PER_FILTER)
            .withMeaningfulGoals(meaningfulGoals)
            .withMetrikaCounters(metrikaCounters)
            .withFilterAvgBid(BigDecimal.TEN)
            .withSum(currency.minAutobudget)
            .withBid(BigDecimal.TEN)

    fun autobudgetAvgCpcPerFilterModelChanges(
        now: LocalDateTime,
        campaignClass: Class<out BaseCampaign>,
        strategyClass: Class<out CommonStrategy>
    ): ModelChanges<CampaignWithPackageStrategy> {

        return fillCampaignModelChangesCommonFields(campaignClass, strategyClass, now)
            .process(TestCampaignsStrategy.defaultAutobudgetAvgCpcPerFilter(), CampaignWithPackageStrategy.STRATEGY)
    }

    fun autobudgetAvgCpcPerCamp() =
        fillStrategyCommonFields(AutobudgetAvgCpcPerCamp(), StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP)
            .withMeaningfulGoals(meaningfulGoals)
            .withMetrikaCounters(metrikaCounters)
            .withSum(currency.minAutobudget)
            .withBid(BigDecimal.TEN)
            .withAvgBid(BigDecimal.valueOf(5))

    fun autobudgetAvgCpcPerCampModelChanges(
        now: LocalDateTime,
        campaignClass: Class<out BaseCampaign>,
        strategyClass: Class<out CommonStrategy>
    ): ModelChanges<CampaignWithPackageStrategy> {

        return fillCampaignModelChangesCommonFields(campaignClass, strategyClass, now)
            .process(TestCampaignsStrategy.defaultAutobudgetAvgCpcPerCamp(), CampaignWithPackageStrategy.STRATEGY)
    }

    fun autobudgetAvgCpaPerFilter(now: LocalDateTime) =
        fillStrategyCommonFields(AutobudgetAvgCpaPerFilter(), StrategyName.AUTOBUDGET_AVG_CPA_PER_FILTER)
            .withMeaningfulGoals(meaningfulGoals)
            .withMetrikaCounters(metrikaCounters)
            .withSum(currency.minAutobudget)
            .withBid(BigDecimal.TEN)
            .withGoalId(12345L)
            .withFilterAvgCpa(BigDecimal.valueOf(19))
            .withIsPayForConversionEnabled(false)
            .withLastBidderRestartTime(now)

    fun autobudgetAvgCpaPerFilterModelChanges(
        now: LocalDateTime,
        campaignClass: Class<out BaseCampaign>,
        strategyClass: Class<out CommonStrategy>
    ): ModelChanges<CampaignWithPackageStrategy> {

        return fillCampaignModelChangesCommonFields(campaignClass, strategyClass, now)
            .process(TestCampaignsStrategy.defaultAutobudgetAvgCpaPerFilter(now), CampaignWithPackageStrategy.STRATEGY)
    }

    fun autobudgetAvgCpaPerCamp(now: LocalDateTime) =
        fillStrategyCommonFields(AutobudgetAvgCpaPerCamp(), StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP)
            .withMeaningfulGoals(meaningfulGoals)
            .withMetrikaCounters(metrikaCounters)
            .withSum(currency.minAutobudget)
            .withBid(BigDecimal.TEN)
            .withGoalId(12345L)
            .withAvgCpa(BigDecimal.valueOf(19))
            .withIsPayForConversionEnabled(false)
            .withLastBidderRestartTime(now)

    fun autobudgetAvgCpaPerCampModelChanges(
        now: LocalDateTime,
        campaignClass: Class<out BaseCampaign>,
        strategyClass: Class<out CommonStrategy>
    ): ModelChanges<CampaignWithPackageStrategy> {

        return fillCampaignModelChangesCommonFields(campaignClass, strategyClass, now)
            .process(TestCampaignsStrategy.defaultAutobudgetAvgCpaPerCamp(now), CampaignWithPackageStrategy.STRATEGY)
    }

    fun autobudgetAvgCpa(now: LocalDateTime?) =
        fillStrategyCommonFields(AutobudgetAvgCpa(), StrategyName.AUTOBUDGET_AVG_CPA)
            .withMeaningfulGoals(meaningfulGoals)
            .withMetrikaCounters(metrikaCounters)
            .withSum(currency.minAutobudget)
            .withGoalId(12345L)
            .withAvgCpa(BigDecimal.valueOf(19))
            .withIsPayForConversionEnabled(true)
            .withLastBidderRestartTime(now)

    fun autobudgetAvgCpaModelChanges(
        now: LocalDateTime,
        campaignClass: Class<out BaseCampaign>,
        strategyClass: Class<out CommonStrategy>
    ): ModelChanges<CampaignWithPackageStrategy> {

        return fillCampaignModelChangesCommonFields(campaignClass, strategyClass, now)
            .process(TestCampaignsStrategy.defaultAutobudgetAvgCpa(now), CampaignWithPackageStrategy.STRATEGY)
    }

    fun autobudgetAvgClick() =
        fillStrategyCommonFields(AutobudgetAvgClick(), StrategyName.AUTOBUDGET_AVG_CLICK)
            .withMeaningfulGoals(meaningfulGoals)
            .withMetrikaCounters(metrikaCounters)
            .withSum(currency.minAutobudget)
            .withAvgBid(BigDecimal.valueOf(19))

    fun autobudgetAvgClickModelChanges(
        now: LocalDateTime,
        campaignClass: Class<out BaseCampaign>,
        strategyClass: Class<out CommonStrategy>
    ): ModelChanges<CampaignWithPackageStrategy> {
        return fillCampaignModelChangesCommonFields(campaignClass, strategyClass, now)
            .process(TestCampaignsStrategy.defaultAutobudgetAvgClick(), CampaignWithPackageStrategy.STRATEGY)
    }

    fun autobudgetWeekBundle() =
        fillStrategyCommonFields(AutobudgetWeekBundle(), StrategyName.AUTOBUDGET_WEEK_BUNDLE)
            .withMetrikaCounters(metrikaCounters)
            .withMeaningfulGoals(meaningfulGoals)
            .withAvgBid(BigDecimal.valueOf(18))
            .withLimitClicks(currency.minAutobudgetClicksBundle.toLong())

    fun autobudgetWeekBundleModelChanges(
        now: LocalDateTime,
        campaignClass: Class<out BaseCampaign>,
        strategyClass: Class<out CommonStrategy>
    ): ModelChanges<CampaignWithPackageStrategy> {

        return fillCampaignModelChangesCommonFields(campaignClass, strategyClass, now)
            .process(TestCampaignsStrategy.defaultAutobudgetWeekBundle(), CampaignWithPackageStrategy.STRATEGY)
    }

    fun textCampaign(metrikaCounters: List<Long>? = null, meaningfulGoals: List<MeaningfulGoal>? = null) =
        TextCampaign()
            .withId(cid)
            .withStatusArchived(false)
            .withClientId(clientId)
            .withWalletId(walletId)
            .withMetrikaCounters(metrikaCounters)
            .withMeaningfulGoals(meaningfulGoals)
            .withAttributionModel(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK)
            .withDayBudget(BigDecimal.ZERO)
            .withDayBudgetDailyChangeCount(0)
            .withDayBudgetShowMode(DayBudgetShowMode.DEFAULT_)
            .withStrategyId(strategyId)

    fun mobileCampaign() =
        MobileContentCampaign()
            .withId(cid)
            .withStatusArchived(false)
            .withClientId(clientId)
            .withWalletId(walletId)
            .withAttributionModel(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK)
            .withDayBudget(BigDecimal.ZERO)
            .withDayBudgetDailyChangeCount(0)
            .withDayBudgetShowMode(DayBudgetShowMode.DEFAULT_)
            .withStrategyId(strategyId)

    private fun <T : CommonStrategy> fillStrategyCommonFields(strategy: T, type: StrategyName): T =
        strategy
            .withId(strategyId)
            .withType(type)
            .withClientId(clientId)
            .withWalletId(walletId)
            .withIsPublic(false)
            .withAttributionModel(StrategyAttributionModel.LAST_YANDEX_DIRECT_CLICK)
            .withStatusArchived(false) as T

    private fun fillCampaignModelChangesCommonFields(
        campaignClass: Class<out BaseCampaign>,
        strategyClass: Class<out CommonStrategy>,
        now: LocalDateTime
    ): ModelChanges<CampaignWithPackageStrategy> {
        val mc = ModelChanges(cid, CampaignWithPackageStrategy::class.java)
            .process(strategyId, CampaignWithPackageStrategy.STRATEGY_ID)
            .process(null, CommonCampaign.LAST_CHANGE)

        processAttributionModel(mc, campaignClass)
        processMetrikaCounters(mc, campaignClass, strategyClass)
        processDayBudget(mc, campaignClass, strategyClass, now)
        processMeaningfulGoals(mc, strategyClass, campaignClass)
        processEnableCpcHold(mc, strategyClass, campaignClass)

        return mc
    }

    private fun processMeaningfulGoals(
        mc: ModelChanges<CampaignWithPackageStrategy>,
        strategyClass: Class<out CommonStrategy>,
        campaignClass: Class<out BaseCampaign>
    ) {
        val campaignSupportsMeaningfulGoals = CampaignWithMeaningfulGoals::class.java.isAssignableFrom(campaignClass)
        val strategySupportsMeaningfulGoals = StrategyWithMeaningfulGoals::class.java.isAssignableFrom(strategyClass)
        if (!strategySupportsMeaningfulGoals && campaignSupportsMeaningfulGoals) {
            mc.castModel(CampaignWithMeaningfulGoals::class.java)
                .process(
                    null,
                    CampaignWithMeaningfulGoals.MEANINGFUL_GOALS
                )
        } else if (campaignSupportsMeaningfulGoals) {
            mc.castModel(CampaignWithMeaningfulGoals::class.java)
                .process(
                    meaningfulGoals,
                    CampaignWithMeaningfulGoals.MEANINGFUL_GOALS
                )
        }
    }

    private fun processEnableCpcHold(
        mc: ModelChanges<CampaignWithPackageStrategy>,
        strategyClass: Class<out CommonStrategy>,
        campaignClass: Class<out BaseCampaign>
    ) {
        val isCampaignWithEnableCpcHoldForbidden =
            CampaignWithEnableCpcHoldForbidden::class.java.isAssignableFrom(campaignClass)
        val isStrategySupportsEnableCpcHold = DefaultManualStrategy::class.java.isAssignableFrom(strategyClass)
        if (!isStrategySupportsEnableCpcHold && !isCampaignWithEnableCpcHoldForbidden) {
            mc.castModel(CommonCampaign::class.java)
                .process(
                    false,
                    CommonCampaign.ENABLE_CPC_HOLD
                )
        } else if (!isCampaignWithEnableCpcHoldForbidden) {
            mc.castModel(CommonCampaign::class.java)
                .process(
                    true,
                    CommonCampaign.ENABLE_CPC_HOLD
                )
        }
    }

    private fun processAttributionModel(
        mc: ModelChanges<CampaignWithPackageStrategy>,
        campaignClass: Class<out BaseCampaign>
    ) {
        if (CampaignWithAttributionModel::class.java.isAssignableFrom(campaignClass)) {
            mc.castModel(CampaignWithAttributionModel::class.java)
                .process(
                    CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK,
                    CampaignWithAttributionModel.ATTRIBUTION_MODEL
                )
        }
    }

    private fun processMetrikaCounters(
        mc: ModelChanges<CampaignWithPackageStrategy>,
        campaignClass: Class<out BaseCampaign>,
        strategyClass: Class<out CommonStrategy>
    ) {
        val campaignIsAssignableFrom = CampaignWithMetrikaCounters::class.java.isAssignableFrom(campaignClass)
        val strategyIsAssignableFrom = StrategyWithMetrikaCounters::class.java.isAssignableFrom(strategyClass)
        if (!strategyIsAssignableFrom && campaignIsAssignableFrom) {
            mc.castModel(CampaignWithMetrikaCounters::class.java)
                .process(
                    null,
                    CampaignWithMetrikaCounters.METRIKA_COUNTERS
                )
        } else if (campaignIsAssignableFrom) {
            mc.castModel(CampaignWithMetrikaCounters::class.java)
                .process(
                    metrikaCounters,
                    CampaignWithMetrikaCounters.METRIKA_COUNTERS
                )
        }
    }

    private fun processDayBudget(
        mc: ModelChanges<CampaignWithPackageStrategy>,
        campaignClass: Class<out BaseCampaign>,
        strategyClass: Class<out CommonStrategy>,
        now: LocalDateTime
    ) {
        val isCampaignAssignableFrom = CampaignWithDayBudget::class.java.isAssignableFrom(campaignClass)
        val isStrategyAssignableFrom = StrategyWithDayBudget::class.java.isAssignableFrom(strategyClass)
        if (!isStrategyAssignableFrom && isCampaignAssignableFrom) {
            mc.castModel(CampaignWithDayBudget::class.java)
                .process(BigDecimal.ZERO, WithDayBudget.DAY_BUDGET)
                .process(DayBudgetShowMode.DEFAULT_, WithDayBudget.DAY_BUDGET_SHOW_MODE)
        } else if (isCampaignAssignableFrom) {
            mc.castModel(CampaignWithDayBudget::class.java)
                .process(BigDecimal.TEN, WithDayBudget.DAY_BUDGET)
                .process(1, WithDayBudget.DAY_BUDGET_DAILY_CHANGE_COUNT)
                .process(now, WithDayBudget.DAY_BUDGET_LAST_CHANGE)
                .process(DayBudgetShowMode.DEFAULT_, WithDayBudget.DAY_BUDGET_SHOW_MODE)
        }
    }
}
