package ru.yandex.direct.grid.processing.service.strategy

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
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddAutobudgetAvgClick
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddAutobudgetAvgCpa
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddAutobudgetAvgCpaPerCamp
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddAutobudgetAvgCpaPerFilter
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddAutobudgetAvgCpcPerCamp
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddAutobudgetAvgCpcPerFilter
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddAutobudgetAvgCpi
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddAutobudgetAvgCpv
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddAutobudgetAvgCpvCustomPeriod
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddAutobudgetCrr
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddAutobudgetMaxImpressions
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddAutobudgetMaxImpressionsCustomPeriod
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddAutobudgetMaxReach
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddAutobudgetMaxReachCustomPeriod
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddAutobudgetMedia
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddAutobudgetRoi
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddAutobudgetWeekBundle
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddAutobudgetWeekSum
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddCpmDefault
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddDefaultManualStrategy
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddPackageStrategy
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddPackageStrategyUnion
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddPeriodFixBid
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddStrategyWithAvgBid
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddStrategyWithAvgCpa
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddStrategyWithAvgCpm
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddStrategyWithAvgCpv
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddStrategyWithBid
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddStrategyWithConversion
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddStrategyWithCustomPeriodBudget
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddStrategyWithDayBudget
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddStrategyWithMeaningfulGoals
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddStrategyWithMetrikaCounters
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddStrategyWithPayForConversion
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddStrategyWithWeeklyBudget
import ru.yandex.direct.grid.processing.service.strategy.PackageStrategyUpdateConverters.toGdMeaningFulGoals
import ru.yandex.direct.grid.processing.service.strategy.query.GdPackageConverter

/*
* Конвертер нужен исключительно для тестов.
* Если по каким-то причинам он нужен не только для тестов, то можно вынести в `main` часть.
* */
object PackageStrategyAddConverters {

    fun toGdAddPackageStrategyUnion(strategy: CommonStrategy): GdAddPackageStrategyUnion {
        val union = GdAddPackageStrategyUnion()
        when (strategy) {
            is AutobudgetWeekSum -> {
                val update = GdAddAutobudgetWeekSum()
                union.withAutobudgetWeekSum(fillCommon(strategy, update))
            }
            is DefaultManualStrategy -> {
                val update = GdAddDefaultManualStrategy()
                    .withEnableCpcHold(strategy.enableCpcHold)
                union.withDefaultManualStrategy(fillCommon(strategy, update))
            }
            is CpmDefault -> {
                val update = GdAddCpmDefault()
                union.withCpmDefault(fillCommon(strategy, update))
            }
            is AutobudgetAvgCpvCustomPeriod -> {
                val update = GdAddAutobudgetAvgCpvCustomPeriod()
                union.withAutobudgetAvgCpvCustomPeriod(fillCommon(strategy, update))
            }
            is AutobudgetAvgCpv -> {
                val update = GdAddAutobudgetAvgCpv()
                union.withAutobudgetAvgCpv(fillCommon(strategy, update))
            }
            is AutobudgetCrr -> {
                val update = GdAddAutobudgetCrr()
                    .withCrr(strategy.crr)
                union.withAutobudgetCrr(fillCommon(strategy, update))
            }
            is AutobudgetAvgCpaPerCamp -> {
                val update = GdAddAutobudgetAvgCpaPerCamp()
                union.withAutobudgetAvgCpaPerCamp(fillCommon(strategy, update))
            }
            is AutobudgetAvgCpaPerFilter -> {
                val update = GdAddAutobudgetAvgCpaPerFilter()
                    .withFilterAvgCpa(strategy.filterAvgCpa)
                union.withAutobudgetAvgCpaPerFilter(fillCommon(strategy, update))
            }
            is AutobudgetAvgCpa -> {
                val update = GdAddAutobudgetAvgCpa()
                union.withAutobudgetAvgCpa(fillCommon(strategy, update))
            }
            is AutobudgetAvgCpi -> {
                val update = GdAddAutobudgetAvgCpi()
                    .withAvgCpi(strategy.avgCpi)
                union.withAutobudgetAvgCpi(fillCommon(strategy, update))
            }
            is AutobudgetRoi -> {
                val update = GdAddAutobudgetRoi()
                    .withRoiCoef(strategy.roiCoef)
                    .withReserveReturn(strategy.reserveReturn)
                    .withProfitability(strategy.profitability)
                union.withAutobudgetRoi(fillCommon(strategy, update))
            }
            is AutobudgetAvgClick -> {
                val update = GdAddAutobudgetAvgClick()
                union.withAutobudgetAvgClick(fillCommon(strategy, update))
            }
            is AutobudgetAvgCpcPerCamp -> {
                val update = GdAddAutobudgetAvgCpcPerCamp()
                union.withAutobudgetAvgCpcPerCamp(fillCommon(strategy, update))
            }
            is AutobudgetAvgCpcPerFilter -> {
                val update = GdAddAutobudgetAvgCpcPerFilter()
                    .withFilterAvgBid(strategy.filterAvgBid)
                union.withAutobudgetAvgCpcPerFilter(fillCommon(strategy, update))
            }
            is AutobudgetMaxImpressions -> {
                val update = GdAddAutobudgetMaxImpressions()
                union.withAutobudgetMaxImpressions(fillCommon(strategy, update))
            }
            is AutobudgetMaxImpressionsCustomPeriod -> {
                val update = GdAddAutobudgetMaxImpressionsCustomPeriod()
                union.withAutobudgetMaxImpressionsCustomPeriod(fillCommon(strategy, update))
            }
            is AutobudgetMaxReach -> {
                val update = GdAddAutobudgetMaxReach()
                union.withAutobudgetMaxReach(fillCommon(strategy, update))
            }
            is AutobudgetMaxReachCustomPeriod -> {
                val update = GdAddAutobudgetMaxReachCustomPeriod()
                union.withAutobudgetMaxReachCustomPeriod(fillCommon(strategy, update))
            }
            is AutobudgetMedia -> {
                val update = GdAddAutobudgetMedia()
                    .withDate(strategy.date)
                union.withAutobudgetMedia(fillCommon(strategy, update))
            }
            is AutobudgetWeekBundle -> {
                val update = GdAddAutobudgetWeekBundle()
                    .withLimitClicks(strategy.limitClicks)
                union.withAutobudgetWeekBundle(fillCommon(strategy, update))
            }
            is PeriodFixBid -> {
                val update = GdAddPeriodFixBid()
                union.withPeriodFixBid(fillCommon(strategy, update))
            }
            else -> {}
        }
        return union
    }

    private fun <T : GdAddPackageStrategy> fillCommon(strategy: CommonStrategy, instance: T): T {
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

    private fun <T : GdAddPackageStrategy> fillMetrikaCounters(commonStrategy: CommonStrategy, instance: T): T {
        if (commonStrategy is StrategyWithMetrikaCounters && instance is GdAddStrategyWithMetrikaCounters) {
            instance.metrikaCounters = commonStrategy.metrikaCounters
        }
        return instance
    }

    private fun <T : GdAddPackageStrategy> fillAvgBid(commonStrategy: CommonStrategy, instance: T): T {
        if (commonStrategy is StrategyWithAvgBid && instance is GdAddStrategyWithAvgBid) {
            instance.avgBid = commonStrategy.avgBid
        }
        return instance
    }

    private fun <T : GdAddPackageStrategy> fillAvgCpa(commonStrategy: CommonStrategy, instance: T): T {
        if (commonStrategy is StrategyWithAvgCpa && instance is GdAddStrategyWithAvgCpa) {
            instance.avgCpa = commonStrategy.avgCpa
        }
        return instance
    }

    private fun <T : GdAddPackageStrategy> fillAvgCpm(commonStrategy: CommonStrategy, instance: T): T {
        if (commonStrategy is StrategyWithAvgCpm && instance is GdAddStrategyWithAvgCpm) {
            instance.avgCpm = commonStrategy.avgCpm
        }
        return instance
    }

    private fun <T : GdAddPackageStrategy> fillAvgCpv(commonStrategy: CommonStrategy, instance: T): T {
        if (commonStrategy is StrategyWithAvgCpv && instance is GdAddStrategyWithAvgCpv) {
            instance.avgCpv = commonStrategy.avgCpv
        }
        return instance
    }

    private fun <T : GdAddPackageStrategy> fillBid(commonStrategy: CommonStrategy, instance: T): T {
        if (commonStrategy is StrategyWithBid && instance is GdAddStrategyWithBid) {
            instance.bid = commonStrategy.bid
        }
        return instance
    }

    private fun <T : GdAddPackageStrategy> fillStrategyWithConversion(
        commonStrategy: CommonStrategy,
        instance: T
    ): T {
        if (commonStrategy is StrategyWithConversion && instance is GdAddStrategyWithConversion) {
            instance.goalId = commonStrategy.goalId
        }
        return instance
    }

    private fun <T : GdAddPackageStrategy> fillCustomPeriodBudget(commonStrategy: CommonStrategy, instance: T): T {
        if (commonStrategy is StrategyWithCustomPeriodBudget && instance is GdAddStrategyWithCustomPeriodBudget) {
            instance.start = commonStrategy.start
            instance.finish = commonStrategy.finish
            instance.autoProlongation = commonStrategy.autoProlongation
            instance.budget = commonStrategy.budget
        }
        return instance
    }

    private fun <T : GdAddPackageStrategy> fillDayBudget(commonStrategy: CommonStrategy, instance: T): T {
        if (commonStrategy is StrategyWithDayBudget && instance is GdAddStrategyWithDayBudget) {
            instance.dayBudget = commonStrategy.dayBudget
            instance.dayBudgetShowMode = GdPackageConverter.toGdDayBudgetShowMode(commonStrategy.dayBudgetShowMode)
        }
        return instance
    }

    private fun <T : GdAddPackageStrategy> fillMeaningfulGoals(commonStrategy: CommonStrategy, instance: T): T {
        if (commonStrategy is StrategyWithMeaningfulGoals && instance is GdAddStrategyWithMeaningfulGoals) {
            instance.meaningfulGoals = commonStrategy.meaningfulGoals?.map { toGdMeaningFulGoals(it) }
        }
        return instance
    }

    private fun <T : GdAddPackageStrategy> fillPayForConversion(commonStrategy: CommonStrategy, instance: T): T {
        if (commonStrategy is StrategyWithPayForConversion && instance is GdAddStrategyWithPayForConversion) {
            instance.isPayForConversionEnabled = commonStrategy.isPayForConversionEnabled
        }
        return instance
    }

    private fun <T : GdAddPackageStrategy> fillWeeklyBudget(commonStrategy: CommonStrategy, instance: T): T {
        if (commonStrategy is StrategyWithWeeklyBudget && instance is GdAddStrategyWithWeeklyBudget) {
            instance.sum = commonStrategy.sum
        }
        return instance
    }
}
