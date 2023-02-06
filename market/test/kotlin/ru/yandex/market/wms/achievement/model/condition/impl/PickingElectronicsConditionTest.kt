package ru.yandex.market.wms.achievement.model.condition.impl

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import ru.yandex.market.wms.achievement.model.Statistic
import ru.yandex.market.wms.achievement.model.StatisticValue
import ru.yandex.market.wms.achievement.model.condition.ConditionLevel
import ru.yandex.market.wms.achievement.model.condition.ConditionParam
import ru.yandex.market.wms.achievement.model.condition.ConditionParamCode
import ru.yandex.market.wms.achievement.model.condition.ConditionParamType
import ru.yandex.market.wms.achievement.model.condition.ConditionState
import ru.yandex.market.wms.achievement.model.condition.ConditionStatisticCode
import ru.yandex.market.wms.achievement.model.entity.UserEntity
import ru.yandex.market.wms.achievement.model.metric.MetricEvent
import ru.yandex.market.wms.achievement.model.metric.PickingElectronicsMetric
import ru.yandex.market.wms.achievement.model.metric.PickingItemMetric
import java.math.BigDecimal
import java.time.Instant

internal class PickingElectronicsConditionTest {

    private val condition = PickingElectronicsCondition()
    private val config = PickingElectronicsConditionConfig(10)
    private val value = PickingElectronicsConditionValue(8)
    private val user = UserEntity(1, "test", "sof")
    private val state = ConditionState(1, condition.code, user, config, value, Instant.now(), 1, 1)
    private val event = MetricEvent(PickingElectronicsMetric(1), user, Instant.now())

    @Test
    fun getParams() {
        assertEquals(expectedParams(), condition.getParams())
        assertEquals(expectedParams(config), condition.getParams(config))
    }

    @Test
    fun getStatistic() {
        assertEquals(expectedStatistic(), condition.getStatistic(state))
    }

    @Test
    fun getLevel() {
        assertEquals(expectedLevel(), condition.getLevel(state))
    }

    @Test
    fun onPickingElectronicsMetric() {
        assertEquals(expectedValue(), condition.onPickingElectronicsMetric(config, value, event))
    }

    private fun expectedParams(config: PickingElectronicsConditionConfig? = null) = listOf(
        ConditionParam(
            code = ConditionParamCode(condition.code, "REQUIREDCOUNT"),
            type = ConditionParamType.INTEGER,
            required = true,
            value = config?.requiredCount
        ),
    )

    private fun expectedStatistic() = Statistic(
        state.conditionId, condition.code, listOf(
            StatisticValue(
                code = ConditionStatisticCode(condition.code, "PICKING_ELECTRONICS_COUNT"),
                requiredValue = config.requiredCount,
                currentValue = value.currentCount
            )
        )
    )

    private fun expectedValue() =
        PickingElectronicsConditionValue(value.currentCount + event.metric.count)

    private fun expectedLevel() = ConditionLevel(state.conditionId, condition.code, BigDecimal(0.8))
}
