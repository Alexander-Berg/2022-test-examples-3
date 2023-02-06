package ru.yandex.direct.grid.processing.service.strategy

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgClick
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaPerCampStrategy
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaPerFilterStrategy
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaStrategy
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpcPerCampStrategy
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpcPerFilterStrategy
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpiStrategy
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpv
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpvCustomPeriodStrategy
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetCrrStrategy
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxImpressionsCustomPeriodStrategy
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxImpressionsStrategy
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxReachCustomPeriodStrategies
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxReachStrategy
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetRoiStrategy
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetWeekBundleStrategy
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetWeekSumStrategy
import ru.yandex.direct.core.testing.data.strategy.TestCpmDefaultStrategy
import ru.yandex.direct.core.testing.data.strategy.TestDefaultManualStrategy
import ru.yandex.direct.core.testing.data.strategy.TestPeriodFixBidStrategy
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.utils.json.LocalDateTimeDeserializer
import java.time.LocalDateTime

interface AbstractPackageStrategyGraphQlServiceTest {

    companion object {
        val goalId = RandomNumberUtils.nextPositiveInteger()
        val meaningfulGoalId = RandomNumberUtils.nextPositiveInteger()
        val counterId = RandomNumberUtils.nextPositiveInteger()
        val metrikaCounters = listOf(counterId.toLong())
        val meaningfulGoals = listOf(
            MeaningfulGoal()
                .withGoalId(meaningfulGoalId.toLong())
                .withConversionValue(0.5.toBigDecimal())
        )

        internal class LocalDateTimeSerializer : JsonSerializer<LocalDateTime>() {
            override fun serialize(value: LocalDateTime, gen: JsonGenerator, serializers: SerializerProvider) {
                gen.writeString(value.toString())
            }
        }

        private fun createLocalDateTimeModule(): Module {
            val module = SimpleModule("LocalDateTimeModule")
            module.addSerializer(LocalDateTime::class.java, LocalDateTimeSerializer())
            module.addDeserializer(LocalDateTime::class.java, LocalDateTimeDeserializer())
            return module
        }

        fun objectMapper(): ObjectMapper {
            return ObjectMapper()
                .registerModule(createLocalDateTimeModule())
                .registerModule(JsonUtils.createLocalDateModule())
        }
    }

    fun autobudgetAvgCpaPerCamp() =
        TestAutobudgetAvgCpaPerCampStrategy.autobudgetAvgCpaPerCamp()
            .withGoalId(goalId.toLong())
            .withMetrikaCounters(metrikaCounters)
            .withMeaningfulGoals(meaningfulGoals)

    fun autobudgetCrr() =
        TestAutobudgetCrrStrategy.autobudgetCrr()
            .withGoalId(goalId.toLong())
            .withMetrikaCounters(metrikaCounters)
            .withIsPayForConversionEnabled(true)
            .withMeaningfulGoals(meaningfulGoals)

    fun autobudgetAvgClick() =
        TestAutobudgetAvgClick.autobudgetAvgClick()
            .withMeaningfulGoals(meaningfulGoals)
            .withMetrikaCounters(metrikaCounters)

    fun autobudgetAvgCpa() =
        TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
            .withGoalId(goalId.toLong())
            .withMetrikaCounters(metrikaCounters)
            .withMeaningfulGoals(meaningfulGoals)

    fun autobudgetAvgCpaPerFilter() =
        TestAutobudgetAvgCpaPerFilterStrategy.autobudgetAvgCpaPerFilter()
            .withGoalId(goalId.toLong())
            .withMetrikaCounters(metrikaCounters)
            .withMeaningfulGoals(meaningfulGoals)

    fun autobudgetAvgCpcPerFilter() =
        TestAutobudgetAvgCpcPerFilterStrategy.autobudgetAvgCpcPerFilter()
            .withMetrikaCounters(metrikaCounters)
            .withMeaningfulGoals(meaningfulGoals)

    fun autobudgetAvgCpcPerCamp() =
        TestAutobudgetAvgCpcPerCampStrategy.autobudgetAvgCpcPerCamp()
            .withMetrikaCounters(metrikaCounters)
            .withMeaningfulGoals(meaningfulGoals)

    fun autobudgetAvgCpi() =
        TestAutobudgetAvgCpiStrategy.autobudgetAvgCpi()

    fun autobudgetAvgCpv() =
        TestAutobudgetAvgCpv.autobudgetAvgCpv()
            .withMetrikaCounters(metrikaCounters)

    fun clientAutobudgetAvgCpvCustomPeriodStrategy() =
        TestAutobudgetAvgCpvCustomPeriodStrategy.clientAutobudgetAvgCpvCustomPeriodStrategy()
            .withMetrikaCounters(metrikaCounters)

    fun clientAutobudgetMaxImpressions() =
        TestAutobudgetMaxImpressionsStrategy.clientAutobudgetMaxImpressionsStrategy()
            .withMetrikaCounters(metrikaCounters)

    fun clientAutobudgetMaxImpressionsCustomPeriodStrategy() =
        TestAutobudgetMaxImpressionsCustomPeriodStrategy.clientAutobudgetMaxImpressionsCustomPeriodStrategy()
            .withMetrikaCounters(metrikaCounters)

    fun clientAutobudgetReachStrategy() =
        TestAutobudgetMaxReachStrategy.clientAutobudgetReachStrategy()
            .withMetrikaCounters(metrikaCounters)

    fun clientAutobudgetMaxReachCustomPeriodStrategy() =
        TestAutobudgetMaxReachCustomPeriodStrategies.clientAutobudgetMaxReachCustomPeriodStrategy()
            .withMetrikaCounters(metrikaCounters)

    fun autobudgetRoi() =
        TestAutobudgetRoiStrategy.autobudgetRoi()
            .withGoalId(goalId.toLong())
            .withMeaningfulGoals(meaningfulGoals)
            .withMetrikaCounters(metrikaCounters)

    fun autobudgetWeekBundle() =
        TestAutobudgetWeekBundleStrategy.autobudgetWeekBundle()
            .withMeaningfulGoals(meaningfulGoals)
            .withMetrikaCounters(metrikaCounters)

    fun autobudget() =
        TestAutobudgetWeekSumStrategy.autobudget()
            .withMetrikaCounters(metrikaCounters)
            .withMeaningfulGoals(meaningfulGoals)
            .withGoalId(null)

    fun clientCpmDefaultStrategy() =
        TestCpmDefaultStrategy.clientCpmDefaultStrategy()
            .withMetrikaCounters(metrikaCounters)

    fun clientDefaultManualStrategy() =
        TestDefaultManualStrategy.clientDefaultManualStrategy()
            .withMeaningfulGoals(meaningfulGoals)
            .withMetrikaCounters(metrikaCounters)

    fun clientPeriodFixBidStrategy() =
        TestPeriodFixBidStrategy.clientPeriodFixBidStrategy()
            .withMetrikaCounters(metrikaCounters)
}
