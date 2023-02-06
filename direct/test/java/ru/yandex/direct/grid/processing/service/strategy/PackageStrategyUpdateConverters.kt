package ru.yandex.direct.grid.processing.service.strategy

import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal
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
import ru.yandex.direct.core.entity.strategy.model.StrategyWithAvgBid
import ru.yandex.direct.core.entity.strategy.model.StrategyWithAvgCpa
import ru.yandex.direct.core.entity.strategy.model.StrategyWithAvgCpm
import ru.yandex.direct.core.entity.strategy.model.StrategyWithAvgCpv
import ru.yandex.direct.core.entity.strategy.model.StrategyWithBid
import ru.yandex.direct.core.entity.strategy.model.StrategyWithConversion
import ru.yandex.direct.core.entity.strategy.model.StrategyWithCustomPeriodBudget
import ru.yandex.direct.core.entity.strategy.model.StrategyWithDayBudget
import ru.yandex.direct.core.entity.strategy.model.StrategyWithMeaningfulGoals
import ru.yandex.direct.core.entity.strategy.model.StrategyWithMetrikaCounters
import ru.yandex.direct.core.entity.strategy.model.StrategyWithPayForConversion
import ru.yandex.direct.core.entity.strategy.model.StrategyWithWeeklyBudget
import ru.yandex.direct.grid.model.campaign.GdMeaningfulGoal
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateAutobudgetAvgClick
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateAutobudgetAvgCpa
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateAutobudgetAvgCpaPerCamp
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateAutobudgetAvgCpaPerFilter
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateAutobudgetAvgCpcPerCamp
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateAutobudgetAvgCpcPerFilter
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateAutobudgetAvgCpi
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateAutobudgetAvgCpv
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateAutobudgetAvgCpvCustomPeriod
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateAutobudgetCrr
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateAutobudgetMaxImpressions
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateAutobudgetMaxImpressionsCustomPeriod
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateAutobudgetMaxReach
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateAutobudgetMaxReachCustomPeriod
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateAutobudgetMedia
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateAutobudgetRoi
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateAutobudgetWeekBundle
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateAutobudgetWeekSum
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateCpmDefault
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateDefaultManualStrategy
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdatePackageStrategy
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdatePackageStrategyUnion
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdatePeriodFixBid
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateStrategyWithAvgBid
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateStrategyWithAvgCpa
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateStrategyWithAvgCpm
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateStrategyWithAvgCpv
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateStrategyWithBid
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateStrategyWithConversion
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateStrategyWithCustomPeriodBudget
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateStrategyWithDayBudget
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateStrategyWithMeaningfulGoals
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateStrategyWithMetrikaCounters
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateStrategyWithPayForConversion
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateStrategyWithWeeklyBudget
import ru.yandex.direct.grid.processing.service.strategy.query.GdPackageConverter

/*
* Конвертер нужен исключительно для тестов.
* Если по каким-то причинам он нужен не только для тестов, то можно вынести в `main` часть.
* */
object PackageStrategyUpdateConverters {

    fun toGdUpdatePackageStrategyUnion(strategy: CommonStrategy): GdUpdatePackageStrategyUnion {
        val union = GdUpdatePackageStrategyUnion()
        when (strategy) {
            is AutobudgetWeekSum -> {
                val update = GdUpdateAutobudgetWeekSum()
                union.withAutobudgetWeekSum(fillCommon(strategy, update))
            }
            is DefaultManualStrategy -> {
                val update = GdUpdateDefaultManualStrategy()
                    .withEnableCpcHold(strategy.enableCpcHold)
                union.withDefaultManualStrategy(fillCommon(strategy, update))
            }
            is CpmDefault -> {
                val update = GdUpdateCpmDefault()
                union.withCpmDefault(fillCommon(strategy, update))
            }
            is AutobudgetAvgCpvCustomPeriod -> {
                val update = GdUpdateAutobudgetAvgCpvCustomPeriod()
                union.withAutobudgetAvgCpvCustomPeriod(fillCommon(strategy, update))
            }
            is AutobudgetAvgCpv -> {
                val update = GdUpdateAutobudgetAvgCpv()
                union.withAutobudgetAvgCpv(fillCommon(strategy, update))
            }
            is AutobudgetCrr -> {
                val update = GdUpdateAutobudgetCrr()
                    .withCrr(strategy.crr)
                union.withAutobudgetCrr(fillCommon(strategy, update))
            }
            is AutobudgetAvgCpaPerCamp -> {
                val update = GdUpdateAutobudgetAvgCpaPerCamp()
                union.withAutobudgetAvgCpaPerCamp(fillCommon(strategy, update))
            }
            is AutobudgetAvgCpaPerFilter -> {
                val update = GdUpdateAutobudgetAvgCpaPerFilter()
                    .withFilterAvgCpa(strategy.filterAvgCpa)
                union.withAutobudgetAvgCpaPerFilter(fillCommon(strategy, update))
            }
            is AutobudgetAvgCpa -> {
                val update = GdUpdateAutobudgetAvgCpa()
                union.withAutobudgetAvgCpa(fillCommon(strategy, update))
            }
            is AutobudgetAvgCpi -> {
                val update = GdUpdateAutobudgetAvgCpi()
                    .withAvgCpi(strategy.avgCpi)
                union.withAutobudgetAvgCpi(fillCommon(strategy, update))
            }
            is AutobudgetRoi -> {
                val update = GdUpdateAutobudgetRoi()
                    .withRoiCoef(strategy.roiCoef)
                    .withReserveReturn(strategy.reserveReturn)
                    .withProfitability(strategy.profitability)
                union.withAutobudgetRoi(fillCommon(strategy, update))
            }
            is AutobudgetAvgClick -> {
                val update = GdUpdateAutobudgetAvgClick()
                union.withAutobudgetAvgClick(fillCommon(strategy, update))
            }
            is AutobudgetAvgCpcPerCamp -> {
                val update = GdUpdateAutobudgetAvgCpcPerCamp()
                union.withAutobudgetAvgCpcPerCamp(fillCommon(strategy, update))
            }
            is AutobudgetAvgCpcPerFilter -> {
                val update = GdUpdateAutobudgetAvgCpcPerFilter()
                    .withFilterAvgBid(strategy.filterAvgBid)
                union.withAutobudgetAvgCpcPerFilter(fillCommon(strategy, update))
            }
            is AutobudgetMaxImpressions -> {
                val update = GdUpdateAutobudgetMaxImpressions()
                union.withAutobudgetMaxImpressions(fillCommon(strategy, update))
            }
            is AutobudgetMaxImpressionsCustomPeriod -> {
                val update = GdUpdateAutobudgetMaxImpressionsCustomPeriod()
                union.withAutobudgetMaxImpressionsCustomPeriod(fillCommon(strategy, update))
            }
            is AutobudgetMaxReach -> {
                val update = GdUpdateAutobudgetMaxReach()
                union.withAutobudgetMaxReach(fillCommon(strategy, update))
            }
            is AutobudgetMaxReachCustomPeriod -> {
                val update = GdUpdateAutobudgetMaxReachCustomPeriod()
                union.withAutobudgetMaxReachCustomPeriod(fillCommon(strategy, update))
            }
            is AutobudgetMedia -> {
                val update = GdUpdateAutobudgetMedia()
                    .withDate(strategy.date)
                union.withAutobudgetMedia(fillCommon(strategy, update))
            }
            is AutobudgetWeekBundle -> {
                val update = GdUpdateAutobudgetWeekBundle()
                    .withLimitClicks(strategy.limitClicks)
                union.withAutobudgetWeekBundle(fillCommon(strategy, update))
            }
            is PeriodFixBid -> {
                val update = GdUpdatePeriodFixBid()
                union.withPeriodFixBid(fillCommon(strategy, update))
            }
            else -> {}
        }
        return union
    }

    private fun <T : GdUpdatePackageStrategy> fillCommon(strategy: CommonStrategy, instance: T): T {
        instance.id = strategy.id
        instance.type = GdPackageConverter.toGdStrategyName(strategy.type)
        instance.attributionModel = GdPackageConverter.toGdAttributionModel(strategy.attributionModel)
        instance.cids = strategy.cids
        instance.isPublic = strategy.isPublic
        instance.name = strategy.name
        fillMetrikaCounters(strategy, instance)
        fillAvgBid(strategy, instance)
        fillAvgCpa(strategy, instance)
        fillAvgCpm(strategy, instance)
        fillAvgCpv(strategy, instance)
        fillBid(strategy, instance)
        fillStrategyWithConversion(strategy, instance)
        fillCustomPeriodBudget(strategy, instance)
        fillDayBudget(strategy, instance)
        fillMeaningfulGoals(strategy, instance)
        fillPayForConversion(strategy, instance)
        fillWeeklyBudget(strategy, instance)
        return instance
    }

    private fun <T : GdUpdatePackageStrategy> fillMetrikaCounters(commonStrategy: CommonStrategy, instance: T): T {
        if (commonStrategy is StrategyWithMetrikaCounters && instance is GdUpdateStrategyWithMetrikaCounters) {
            instance.metrikaCounters = commonStrategy.metrikaCounters
        }
        return instance
    }

    private fun <T : GdUpdatePackageStrategy> fillAvgBid(commonStrategy: CommonStrategy, instance: T): T {
        if (commonStrategy is StrategyWithAvgBid && instance is GdUpdateStrategyWithAvgBid) {
            instance.avgBid = commonStrategy.avgBid
        }
        return instance
    }

    private fun <T : GdUpdatePackageStrategy> fillAvgCpa(commonStrategy: CommonStrategy, instance: T): T {
        if (commonStrategy is StrategyWithAvgCpa && instance is GdUpdateStrategyWithAvgCpa) {
            instance.avgCpa = commonStrategy.avgCpa
        }
        return instance
    }

    private fun <T : GdUpdatePackageStrategy> fillAvgCpm(commonStrategy: CommonStrategy, instance: T): T {
        if (commonStrategy is StrategyWithAvgCpm && instance is GdUpdateStrategyWithAvgCpm) {
            instance.avgCpm = commonStrategy.avgCpm
        }
        return instance
    }

    private fun <T : GdUpdatePackageStrategy> fillAvgCpv(commonStrategy: CommonStrategy, instance: T): T {
        if (commonStrategy is StrategyWithAvgCpv && instance is GdUpdateStrategyWithAvgCpv) {
            instance.avgCpv = commonStrategy.avgCpv
        }
        return instance
    }

    private fun <T : GdUpdatePackageStrategy> fillBid(commonStrategy: CommonStrategy, instance: T): T {
        if (commonStrategy is StrategyWithBid && instance is GdUpdateStrategyWithBid) {
            instance.bid = commonStrategy.bid
        }
        return instance
    }

    private fun <T : GdUpdatePackageStrategy> fillStrategyWithConversion(
        commonStrategy: CommonStrategy,
        instance: T
    ): T {
        if (commonStrategy is StrategyWithConversion && instance is GdUpdateStrategyWithConversion) {
            instance.goalId = commonStrategy.goalId
        }
        return instance
    }

    private fun <T : GdUpdatePackageStrategy> fillCustomPeriodBudget(commonStrategy: CommonStrategy, instance: T): T {
        if (commonStrategy is StrategyWithCustomPeriodBudget && instance is GdUpdateStrategyWithCustomPeriodBudget) {
            instance.start = commonStrategy.start
            instance.finish = commonStrategy.finish
            instance.autoProlongation = commonStrategy.autoProlongation
            instance.budget = commonStrategy.budget
        }
        return instance
    }

    private fun <T : GdUpdatePackageStrategy> fillDayBudget(commonStrategy: CommonStrategy, instance: T): T {
        if (commonStrategy is StrategyWithDayBudget && instance is GdUpdateStrategyWithDayBudget) {
            instance.dayBudget = commonStrategy.dayBudget
            instance.dayBudgetShowMode = GdPackageConverter.toGdDayBudgetShowMode(commonStrategy.dayBudgetShowMode)
        }
        return instance
    }

    private fun <T : GdUpdatePackageStrategy> fillMeaningfulGoals(commonStrategy: CommonStrategy, instance: T): T {
        if (commonStrategy is StrategyWithMeaningfulGoals && instance is GdUpdateStrategyWithMeaningfulGoals) {
            instance.meaningfulGoals = commonStrategy.meaningfulGoals?.map { toGdMeaningFulGoals(it) }
        }
        return instance
    }

    private fun <T : GdUpdatePackageStrategy> fillPayForConversion(commonStrategy: CommonStrategy, instance: T): T {
        if (commonStrategy is StrategyWithPayForConversion && instance is GdUpdateStrategyWithPayForConversion) {
            instance.isPayForConversionEnabled = commonStrategy.isPayForConversionEnabled
        }
        return instance
    }

    private fun <T : GdUpdatePackageStrategy> fillWeeklyBudget(commonStrategy: CommonStrategy, instance: T): T {
        if (commonStrategy is StrategyWithWeeklyBudget && instance is GdUpdateStrategyWithWeeklyBudget) {
            instance.sum = commonStrategy.sum
        }
        return instance
    }

    fun toGdMeaningFulGoals(goal: MeaningfulGoal): GdMeaningfulGoal {
        return GdMeaningfulGoal()
            .withGoalId(goal.goalId)
            .withConversionValue(goal.conversionValue)
            .withIsMetrikaSourceOfValue(goal.isMetrikaSourceOfValue)
    }
}
