package ru.yandex.market.wms.achievement.model.condition.impl

import ru.yandex.market.wms.achievement.annotation.ConditionParameter
import ru.yandex.market.wms.achievement.model.StatisticValue
import ru.yandex.market.wms.achievement.model.condition.BaseCondition
import ru.yandex.market.wms.achievement.model.condition.ConditionCode
import ru.yandex.market.wms.achievement.model.condition.ConditionConfig
import ru.yandex.market.wms.achievement.model.condition.ConditionParamType.DATETIME
import ru.yandex.market.wms.achievement.model.condition.ConditionParamType.INTEGER
import ru.yandex.market.wms.achievement.model.condition.ConditionParamType.STRING
import ru.yandex.market.wms.achievement.model.condition.ConditionStatisticCode
import ru.yandex.market.wms.achievement.model.condition.ConditionValue
import ru.yandex.market.wms.achievement.model.entity.UserEntity
import ru.yandex.market.wms.achievement.model.metric.MetricEvent
import ru.yandex.market.wms.achievement.model.metric.MetricType
import ru.yandex.market.wms.achievement.model.metric.PickingItemMetric
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Условие для тестов
 */
class TestCondition : BaseCondition<TestConditionConfig, TestConditionValue>(
    ConditionCode("TEST_CONDITION"),
    supportedMetrics = setOf(MetricType.PICKING_ITEM)
) {
    override fun statistic(config: TestConditionConfig, value: TestConditionValue): List<StatisticValue> = listOf(
        StatisticValue(ConditionStatisticCode(code, "TEST"), config.param1, value.value1)
    )

    override fun initialValue(config: TestConditionConfig): TestConditionValue {
        return TestConditionValue(1, "2")
    }

    override fun level(config: TestConditionConfig, value: TestConditionValue): BigDecimal = BigDecimal(1.3)

    override fun onPickingItemMetric(
        config: TestConditionConfig,
        value: TestConditionValue,
        event: MetricEvent<PickingItemMetric>
    ): TestConditionValue = valueChanged()

    override fun achievementGrantedCallback(
        config: TestConditionConfig,
        value: TestConditionValue,
        grantedLevel: Int,
        user: UserEntity
    ): Pair<TestConditionConfig, TestConditionValue> =
        TestConditionConfig(0, "update config", LocalDateTime.parse("2022-06-01T02:00:00")) to
            TestConditionValue(0, "got achievement")

    fun config(): TestConditionConfig = TestConditionConfig(1, "param2", LocalDateTime.parse("2022-04-17T02:00:00"))
    fun value(): TestConditionValue = TestConditionValue(1, "value2")
    fun valueChanged(): TestConditionValue = TestConditionValue(2, "value2 changed")
}

data class TestConditionConfig(
    @ConditionParameter(INTEGER)
    val param1: Int,
    @ConditionParameter(STRING)
    val param2: String,
    @ConditionParameter(DATETIME)
    val param3: LocalDateTime
) : ConditionConfig

data class TestConditionValue(
    val value1: Int,
    val value2: String
) : ConditionValue
