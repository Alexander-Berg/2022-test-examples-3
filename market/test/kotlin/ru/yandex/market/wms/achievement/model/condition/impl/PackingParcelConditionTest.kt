package ru.yandex.market.wms.achievement.model.condition.impl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
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
import ru.yandex.market.wms.achievement.model.metric.PackingParcelMetric
import java.math.BigDecimal
import java.time.Instant

internal class PackingParcelConditionTest {
    private val shiftId_1 = "SHIFT_1"
    private val shiftId_2 = "SHIFT_2"
    private val condition = PackingParcelCondition()
    private val config = PackingParcelConditionConfig(10, false)
    private val value = PackingParcelConditionValue(8, currentShiftId = shiftId_1)
    private val user = UserEntity(1, "test", "172", shiftId_2)
    private val state = ConditionState(1, condition.code, user, config, value, Instant.now(), 1, 1)
    private val event = MetricEvent(PackingParcelMetric(1), user, Instant.now())

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
    fun onPackingParcelMetric() {
        assertEquals(
            PackingParcelConditionValue(value.currentCount + event.metric.count, shiftId_1),
            condition.onMetric(state, event).value
        )
        assertEquals(
            PackingParcelConditionValue(event.metric.count, shiftId_2),
            condition.onMetric(state.copy(config = config.copy(isShiftOn = true)), event).value
        )
    }

    private fun expectedParams(config: PackingParcelConditionConfig? = null) = listOf(
        ConditionParam(
            code = ConditionParamCode(condition.code, "ISSHIFTON"),
            type = ConditionParamType.BOOLEAN,
            required = true,
            value = config?.isShiftOn
        ),
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
                code = ConditionStatisticCode(condition.code, "PACKING_PARCEL_COUNT"),
                requiredValue = config.requiredCount,
                currentValue = value.currentCount
            )
        )
    )

    private fun expectedLevel() = ConditionLevel(state.conditionId, condition.code, BigDecimal(0.8))
}
