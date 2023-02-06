package ru.yandex.direct.core.testing.data.campaign

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform
import ru.yandex.direct.core.entity.campaign.model.DbStrategy
import ru.yandex.direct.core.entity.campaign.model.StrategyData
import ru.yandex.direct.core.entity.campaign.model.StrategyName
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStrategyName

object TestCampaignsStrategy {

    private val CURRENCY = Currencies.getCurrency(CurrencyCode.RUB)

    @JvmStatic
    fun defaultStrategy(): DbStrategy {
        return defaultStrategy(differentPlaces = true)
    }

    @JvmStatic
    fun defaultStrategy(differentPlaces: Boolean): DbStrategy {
        val strategy = DbStrategy()
        strategy.strategyName = StrategyName.DEFAULT_
        strategy.autobudget = CampaignsAutobudget.NO
        strategy.platform = CampaignsPlatform.BOTH
        strategy.strategyData = StrategyData()
            .withName(CampaignsStrategyName.default_.literal)
            .withVersion(1L)
        if (differentPlaces) {
            strategy.strategy = CampOptionsStrategy.DIFFERENT_PLACES
        }
        return strategy
    }

    @JvmStatic
    fun defaultCpmStrategyData(): DbStrategy {
        val cpmDefault = "cpm_default"
        val strategyData = StrategyData()
            .withVersion(1L)
            .withName(cpmDefault)
        return DbStrategy()
            .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
            .withAutobudget(CampaignsAutobudget.NO)
            .withStrategyName(StrategyName.CPM_DEFAULT)
            .withPlatform(CampaignsPlatform.CONTEXT)
            .withStrategyData(strategyData) as DbStrategy
    }

    @JvmStatic
    fun defaultAutobudgetRoiStrategy(goalId: Long): DbStrategy {
        return defaultAutobudgetRoiStrategy(goalId, true)
    }

    @JvmStatic
    fun defaultAutobudgetRoiStrategy(goalId: Long, differentPlaces: Boolean): DbStrategy {
        val strategy = DbStrategy()
            .withStrategyName(StrategyName.AUTOBUDGET_ROI)
            .withPlatform(CampaignsPlatform.BOTH)
            .withAutobudget(CampaignsAutobudget.YES)
            .withStrategyData(StrategyData()
                .withName(CampaignsStrategyName.autobudget_roi.getLiteral())
                .withSum(null)
                .withBid(null)
                .withRoiCoef(BigDecimal("1"))
                .withReserveReturn(20L)
                .withProfitability(BigDecimal("20"))
                .withGoalId(goalId)
                .withVersion(1)
            ) as DbStrategy
        if (differentPlaces) {
            strategy.withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
        }
        return strategy
    }

    @JvmStatic
    fun defaultAutobudgetAvgCpcPerFilter(goalId: Long): DbStrategy {
        val strategy = DbStrategy()
            .withStrategyName(StrategyName.AUTOBUDGET_AVG_CPC_PER_FILTER)
            .withStrategy(CampOptionsStrategy.AUTOBUDGET_AVG_CPC_PER_FILTER)
            .withPlatform(CampaignsPlatform.BOTH)
            .withAutobudget(CampaignsAutobudget.YES)
            .withStrategyData(StrategyData()
                .withName(CampaignsStrategyName.autobudget_avg_cpc_per_filter.getLiteral())
                .withFilterAvgBid(BigDecimal("20"))
                .withSum(BigDecimal("10000"))
                .withBid(null)
                .withGoalId(goalId)
                .withVersion(1)
            ) as DbStrategy
        return strategy
    }

    @JvmStatic
    fun defaultAutobudgetStrategy(): DbStrategy {
        return defaultAutobudgetStrategy(null)
    }

    @JvmStatic
    fun defaultAutobudgetStrategy(goalId: Long?): DbStrategy {
        return DbStrategy()
            .withStrategyName(StrategyName.AUTOBUDGET)
            .withAutobudget(CampaignsAutobudget.YES)
            .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
            .withPlatform(CampaignsPlatform.BOTH)
            .withStrategyData(StrategyData()
                .withName(CampaignsStrategyName.autobudget.getLiteral())
                .withSum(BigDecimal("10000"))
                .withBid(null)
                .withGoalId(goalId)
                .withVersion(1)
            ) as DbStrategy
    }

    @JvmStatic
    fun averageBidStrategy(): DbStrategy {
        return DbStrategy()
            .withStrategyName(StrategyName.AUTOBUDGET_AVG_CLICK)
            .withAutobudget(CampaignsAutobudget.YES)
            .withPlatform(CampaignsPlatform.BOTH)
            .withDayBudget(BigDecimal.valueOf(100).setScale(2, RoundingMode.DOWN))
            .withStrategyData(StrategyData()
                .withName(CampaignsStrategyName.autobudget.getLiteral())
                .withAvgBid(BigDecimal("55"))
                .withSum(BigDecimal("9000"))
                .withVersion(1)
            ) as DbStrategy
    }

    @JvmStatic
    fun defaultAutobudgetAvgCpiStrategy(goalId: Long?): DbStrategy {
        return DbStrategy()
            .withStrategyName(StrategyName.AUTOBUDGET_AVG_CPI)
            .withAutobudget(CampaignsAutobudget.YES)
            .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
            .withPlatform(CampaignsPlatform.BOTH)
            .withStrategyData(StrategyData()
                .withName(CampaignsStrategyName.autobudget_avg_cpi.getLiteral())
                .withAvgCpi(BigDecimal("55"))
                .withSum(BigDecimal("10000"))
                .withBid(null)
                .withGoalId(goalId)
                .withVersion(1)
            ) as DbStrategy
    }

    @JvmStatic
    fun defaultAutobudgetAvgCpiStrategy(goalId: Long?, now: LocalDateTime): DbStrategy {
        return DbStrategy()
            .withStrategyName(StrategyName.AUTOBUDGET_AVG_CPI)
            .withAutobudget(CampaignsAutobudget.YES)
            .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
            .withPlatform(CampaignsPlatform.BOTH)
            .withStrategyData(
                StrategyData()
                    .withName(CampaignsStrategyName.autobudget_avg_cpi.getLiteral())
                    .withAvgCpi(BigDecimal("55"))
                    .withSum(BigDecimal("10000"))
                    .withLastBidderRestartTime(now)
                    .withBid(null)
                    .withGoalId(goalId)
                    .withVersion(1)
            ) as DbStrategy
    }

    @JvmStatic
    fun defaultAutobudgetAvgCpiStrategy(now: LocalDateTime): DbStrategy {
        return DbStrategy()
            .withStrategyName(StrategyName.AUTOBUDGET_AVG_CPI)
            .withAutobudget(CampaignsAutobudget.YES)
            .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
            .withPlatform(CampaignsPlatform.BOTH)
            .withStrategyData(
                StrategyData()
                    .withName(CampaignsStrategyName.autobudget_avg_cpi.getLiteral())
                    .withGoalId(12345L) // Случайный идентификатор цели
                    .withAvgCpi(BigDecimal("55"))
                    .withSum(BigDecimal("10000"))
                    .withLastBidderRestartTime(now)
                    .withBid(null)
                    .withVersion(1)
            ) as DbStrategy
    }

    @JvmStatic
    fun autobudgetCrrStrategy(sum: BigDecimal?, crr: Long?, goalId: Long): DbStrategy {
        return DbStrategy()
            .withStrategyName(StrategyName.AUTOBUDGET_CRR)
            .withPlatform(CampaignsPlatform.BOTH)
            .withAutobudget(CampaignsAutobudget.YES)
            .withStrategyData(
                StrategyData()
                    .withName(CampaignsStrategyName.autobudget_crr.getLiteral())
                    .withSum(sum)
                    .withCrr(crr)
                    .withGoalId(goalId)
                    .withVersion(1)
            ) as DbStrategy
    }

    @JvmStatic
    fun weekBundleStrategy(limitClicks: Long, maxBid: Long? = null, avgBid: Long? = null): DbStrategy =
        DbStrategy()
            .withStrategyName(StrategyName.AUTOBUDGET_WEEK_BUNDLE)
            .withPlatform(CampaignsPlatform.BOTH)
            .withAutobudget(CampaignsAutobudget.YES)
            .withStrategyData(
                StrategyData()
                    .withName(CampaignsStrategyName.autobudget_week_bundle.literal)
                    .withBid(maxBid?.toBigDecimal())
                    .withAvgBid(avgBid?.toBigDecimal())
                    .withLimitClicks(limitClicks)
                    .withVersion(1)
            ) as DbStrategy

    @JvmStatic
    fun defaultAutobudgetAvgClick() =
        DbStrategy()
            .withStrategyName(StrategyName.AUTOBUDGET_AVG_CLICK)
            .withPlatform(CampaignsPlatform.BOTH)
            .withAutobudget(CampaignsAutobudget.YES)
            .withStrategyData(
                StrategyData()
                    .withName(CampaignsStrategyName.autobudget_avg_click.literal)
                    .withAvgBid(BigDecimal.valueOf(19))
                    .withSum(CURRENCY.minAutobudget)
                    .withVersion(1)
            ) as DbStrategy

    @JvmStatic
    fun defaultAutobudgetAvgCpa(time: LocalDateTime) =
        DbStrategy()
            .withStrategyName(StrategyName.AUTOBUDGET_AVG_CPA)
            .withPlatform(CampaignsPlatform.BOTH)
            .withAutobudget(CampaignsAutobudget.YES)
            .withStrategyData(
                StrategyData()
                    .withName(CampaignsStrategyName.autobudget_avg_cpa.literal)
                    .withSum(CURRENCY.minAutobudget)
                    .withGoalId(12345L) // Случайный идентификатор цели
                    .withAvgCpa(BigDecimal.valueOf(19))
                    .withLastBidderRestartTime(time)
                    .withPayForConversion(true)
                    .withVersion(1)
            ) as DbStrategy

    @JvmStatic
    fun defaultAutobudgetAvgCpaPerCamp(time: LocalDateTime) =
        DbStrategy()
            .withStrategyName(StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP)
            .withPlatform(CampaignsPlatform.BOTH)
            .withAutobudget(CampaignsAutobudget.YES)
            .withStrategyData(
                StrategyData()
                    .withName(CampaignsStrategyName.autobudget_avg_cpa_per_camp.literal)
                    .withSum(CURRENCY.minAutobudget)
                    .withBid(BigDecimal.TEN)
                    .withGoalId(12345L) // Случайный идентификатор цели
                    .withAvgCpa(BigDecimal.valueOf(19))
                    .withPayForConversion(false)
                    .withLastBidderRestartTime(time)
                    .withVersion(1)
            ) as DbStrategy

    @JvmStatic
    fun defaultAutobudgetAvgCpaPerFilter(time: LocalDateTime) =
        DbStrategy()
            .withStrategyName(StrategyName.AUTOBUDGET_AVG_CPA_PER_FILTER)
            .withPlatform(CampaignsPlatform.BOTH)
            .withAutobudget(CampaignsAutobudget.YES)
            .withStrategyData(
                StrategyData()
                    .withName(CampaignsStrategyName.autobudget_avg_cpa_per_filter.literal)
                    .withSum(CURRENCY.minAutobudget)
                    .withBid(BigDecimal.TEN)
                    .withGoalId(12345L) // Случайный идентификатор цели
                    .withFilterAvgCpa(BigDecimal.valueOf(19))
                    .withPayForConversion(false)
                    .withLastBidderRestartTime(time)
                    .withVersion(1)
            ) as DbStrategy

    @JvmStatic
    fun defaultAutobudgetAvgCpcPerCamp() =
        DbStrategy()
            .withStrategyName(StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP)
            .withPlatform(CampaignsPlatform.BOTH)
            .withAutobudget(CampaignsAutobudget.YES)
            .withStrategyData(
                StrategyData()
                    .withName(CampaignsStrategyName.autobudget_avg_cpc_per_camp.literal)
                    .withSum(CURRENCY.minAutobudget)
                    .withBid(BigDecimal.TEN)
                    .withAvgBid(BigDecimal.valueOf(5))
                    .withVersion(1)
            ) as DbStrategy


    @JvmStatic
    fun defaultAutobudgetAvgCpcPerFilter() =
        DbStrategy()
            .withStrategyName(StrategyName.AUTOBUDGET_AVG_CPC_PER_FILTER)
            .withPlatform(CampaignsPlatform.BOTH)
            .withAutobudget(CampaignsAutobudget.YES)
            .withStrategyData(
                StrategyData()
                    .withName(CampaignsStrategyName.autobudget_avg_cpc_per_filter.literal)
                    .withFilterAvgBid(BigDecimal.TEN)
                    .withSum(CURRENCY.minAutobudget)
                    .withBid(BigDecimal.TEN)
                    .withVersion(1)
            ) as DbStrategy

    @JvmStatic
    fun defaultAutobudgetAvgCpv() =
        DbStrategy()
            .withStrategyName(StrategyName.AUTOBUDGET_AVG_CPV)
            .withPlatform(CampaignsPlatform.BOTH)
            .withAutobudget(CampaignsAutobudget.YES)
            .withStrategyData(
                StrategyData()
                    .withName(CampaignsStrategyName.autobudget_avg_cpv.literal)
                    .withSum(CURRENCY.minAutobudget.multiply(7L.toBigDecimal()))
                    .withAvgCpv(CURRENCY.maxAvgCpv)
                    .withPayForConversion(true)
                    .withVersion(1)
            ) as DbStrategy

    @JvmStatic
    fun defaultAutobudgetAvgCpvCustomPeriod(time: LocalDateTime) =
        DbStrategy()
            .withStrategyName(StrategyName.AUTOBUDGET_AVG_CPV_CUSTOM_PERIOD)
            .withPlatform(CampaignsPlatform.BOTH)
            .withAutobudget(CampaignsAutobudget.YES)
            .withStrategyData(
                StrategyData()
                    .withName(CampaignsStrategyName.autobudget_avg_cpv_custom_period.literal)
                    .withAvgCpv(CURRENCY.maxAvgCpv)
                    .withPayForConversion(true)
                    .withBudget(CURRENCY.minDailyBudgetForPeriod.multiply(2L.toBigDecimal()))
                    .withStart(time.toLocalDate())
                    .withFinish(time.toLocalDate().plusDays(1))
                    .withLastUpdateTime(time)
                    .withDailyChangeCount(1)
                    .withAutoProlongation(0)
                    .withVersion(1)
            ) as DbStrategy

    @JvmStatic
    fun defaultAutobudgetCrr(time: LocalDateTime) =
        DbStrategy()
            .withStrategyName(StrategyName.AUTOBUDGET_CRR)
            .withPlatform(CampaignsPlatform.BOTH)
            .withAutobudget(CampaignsAutobudget.YES)
            .withStrategyData(
                StrategyData()
                    .withName(CampaignsStrategyName.autobudget_crr.literal)
                    .withGoalId(12345L) // случайный идентификатор цели
                    .withLastBidderRestartTime(time)
                    .withCrr(12)
                    .withPayForConversion(false)
                    .withSum(CURRENCY.minAutobudget)
                    .withVersion(1)
            ) as DbStrategy

    @JvmStatic
    fun defaultAutobudgetMaxImpressions() =
        DbStrategy()
            .withStrategyName(StrategyName.AUTOBUDGET_MAX_IMPRESSIONS)
            .withPlatform(CampaignsPlatform.BOTH)
            .withAutobudget(CampaignsAutobudget.YES)
            .withStrategyData(
                StrategyData()
                    .withName(CampaignsStrategyName.autobudget_max_impressions.literal)
                    .withAvgCpm(BigDecimal.valueOf(22))
                    .withSum(CURRENCY.minAutobudget.multiply(7L.toBigDecimal()))
                    .withVersion(1)
            ) as DbStrategy

    @JvmStatic
    fun defaultAutobudgetMaxImpressionsCustomPeriod(time: LocalDateTime) =
        DbStrategy()
            .withStrategyName(StrategyName.AUTOBUDGET_MAX_IMPRESSIONS_CUSTOM_PERIOD)
            .withPlatform(CampaignsPlatform.BOTH)
            .withAutobudget(CampaignsAutobudget.YES)
            .withStrategyData(
                StrategyData()
                    .withName(CampaignsStrategyName.autobudget_max_impressions_custom_period.literal)
                    .withStart(time.toLocalDate())
                    .withFinish(time.toLocalDate().plusDays(1))
                    .withLastUpdateTime(time)
                    .withDailyChangeCount(1)
                    .withAutoProlongation(1)
                    .withBudget(CURRENCY.minDailyBudgetForPeriod.multiply(2L.toBigDecimal()))
                    .withAvgCpm(BigDecimal.valueOf(22))
                    .withVersion(1)
            ) as DbStrategy

    @JvmStatic
    fun defaultAutobudgetMaxReach() =
        DbStrategy()
            .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH)
            .withPlatform(CampaignsPlatform.BOTH)
            .withAutobudget(CampaignsAutobudget.YES)
            .withStrategyData(
                StrategyData()
                    .withName(CampaignsStrategyName.autobudget_max_reach.literal)
                    .withAvgCpm(BigDecimal.valueOf(22))
                    .withSum(CURRENCY.minAutobudget.multiply(7L.toBigDecimal()))
                    .withVersion(1)
            ) as DbStrategy

    @JvmStatic
    fun defaultAutobudgetMedia(time: LocalDateTime) =
        DbStrategy()
            .withStrategyName(StrategyName.AUTOBUDGET_MEDIA)
            .withPlatform(CampaignsPlatform.BOTH)
            .withAutobudget(CampaignsAutobudget.YES)
            .withStrategyData(
                StrategyData()
                    .withName(CampaignsStrategyName.autobudget_media.literal)
                    .withVersion(1)
            ) as DbStrategy

    @JvmStatic
    fun defaultAutobudgetRoi() =
        DbStrategy()
            .withStrategyName(StrategyName.AUTOBUDGET_ROI)
            .withPlatform(CampaignsPlatform.BOTH)
            .withAutobudget(CampaignsAutobudget.YES)
            .withStrategyData(
                StrategyData()
                    .withName(CampaignsStrategyName.autobudget_roi.literal)
                    .withSum(CURRENCY.minAutobudget)
                    .withGoalId(12345L) // Случайный идентификатор цели
                    .withProfitability(BigDecimal.TEN)
                    .withRoiCoef(BigDecimal.ONE)
                    .withReserveReturn(10L)
                    .withBid(BigDecimal.TEN)
                    .withVersion(1)
            ) as DbStrategy

    @JvmStatic
    fun defaultAutobudgetWeekBundle() =
        DbStrategy()
            .withStrategyName(StrategyName.AUTOBUDGET_WEEK_BUNDLE)
            .withPlatform(CampaignsPlatform.BOTH)
            .withAutobudget(CampaignsAutobudget.YES)
            .withStrategyData(
                StrategyData()
                    .withName(CampaignsStrategyName.autobudget_week_bundle.literal)
                    .withAvgBid(BigDecimal.valueOf(18))
                    .withLimitClicks(CURRENCY.minAutobudgetClicksBundle.toLong())
                    .withVersion(1)
            ) as DbStrategy

    @JvmStatic
    fun defaultAutobudgetWeekSum(time: LocalDateTime) =
        DbStrategy()
            .withStrategyName(StrategyName.AUTOBUDGET)
            .withPlatform(CampaignsPlatform.BOTH)
            .withAutobudget(CampaignsAutobudget.YES)
            .withStrategyData(
                StrategyData()
                    .withName(CampaignsStrategyName.autobudget.literal)
                    .withLastBidderRestartTime(time)
                    .withSum(CURRENCY.minAutobudget)
                    .withBid(BigDecimal.TEN)
                    .withVersion(1)
            ) as DbStrategy

    @JvmStatic
    fun defaultCpmDefault(time: LocalDateTime) =
        DbStrategy()
            .withStrategyName(StrategyName.CPM_DEFAULT)
            .withPlatform(CampaignsPlatform.BOTH)
            .withAutobudget(CampaignsAutobudget.NO)
            .withStrategyData(
                StrategyData()
                    .withName(CampaignsStrategyName.cpm_default.literal)
                    .withVersion(1)
            ) as DbStrategy

    @JvmStatic
    fun defaultDefaultManualStrategy(time: LocalDateTime) =
        DbStrategy()
            .withStrategyName(StrategyName.DEFAULT_)
            .withPlatform(CampaignsPlatform.BOTH)
            .withAutobudget(CampaignsAutobudget.NO)
            .withStrategyData(
                StrategyData()
                    .withName(CampaignsStrategyName.default_.literal)
                    .withVersion(1)
            ) as DbStrategy

    @JvmStatic
    fun defaultAutobudgetMaxReachCustomPeriod(time: LocalDateTime) =
        DbStrategy()
            .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD)
            .withPlatform(CampaignsPlatform.BOTH)
            .withAutobudget(CampaignsAutobudget.YES)
            .withStrategyData(
                StrategyData()
                    .withName(CampaignsStrategyName.autobudget_max_reach_custom_period.literal)
                    .withAvgCpm(BigDecimal.valueOf(22))
                    .withStart(time.toLocalDate())
                    .withFinish(time.toLocalDate().plusDays(1))
                    .withLastUpdateTime(time)
                    .withBudget(CURRENCY.minDailyBudgetForPeriod.multiply(2L.toBigDecimal()))
                    .withDailyChangeCount(1)
                    .withAutoProlongation(1)
                    .withVersion(1)
            ) as DbStrategy

    @JvmStatic
    fun defaultPeriodFixBid(time: LocalDateTime) =
        DbStrategy()
            .withStrategyName(StrategyName.PERIOD_FIX_BID)
            .withPlatform(CampaignsPlatform.BOTH)
            .withAutobudget(CampaignsAutobudget.NO)
            .withStrategyData(
                StrategyData()
                    .withName(CampaignsStrategyName.period_fix_bid.literal)
                    .withStart(time.toLocalDate())
                    .withFinish(time.toLocalDate().plusDays(1))
                    .withAutoProlongation(1)
                    .withBudget(CURRENCY.minDailyBudgetForPeriod.multiply(2L.toBigDecimal()))
                    .withVersion(1)
            ) as DbStrategy
}
