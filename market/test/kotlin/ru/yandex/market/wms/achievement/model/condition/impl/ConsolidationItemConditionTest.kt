package ru.yandex.market.wms.achievement.model.condition.impl

import org.junit.jupiter.api.Assertions
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
import ru.yandex.market.wms.achievement.model.metric.ConsolidationItemMetric
import java.math.BigDecimal
import java.time.Instant

internal class ConsolidationItemConditionTest {

    private val shiftId_1 = "SHIFT_1"
    private val shiftId_2 = "SHIFT_2"
    private val condition = ConsolidationItemCondition()
    private val config = ConsolidationItemConditionConfig(10, false)
    private val value = ConsolidationItemConditionValue(8, currentShiftId = shiftId_1)
    private val user = UserEntity(1, "test", "172", shiftId_2)
    private val state = ConditionState(1, condition.code, user, config, value, Instant.now(), 1, 1)
    private val event = MetricEvent(ConsolidationItemMetric(1), user, Instant.now())

    @Test
    fun getParams() {
        Assertions.assertEquals(expectedParams(), condition.getParams())
        Assertions.assertEquals(expectedParams(config), condition.getParams(config))
    }

    @Test
    fun getStatistic() {
        Assertions.assertEquals(expectedStatistic(), condition.getStatistic(state))
    }

    @Test
    fun getLevel() {
        Assertions.assertEquals(expectedLevel(), condition.getLevel(state))
    }

    @Test
    fun onConsolidationItemMetric() {
        Assertions.assertEquals(
            ConsolidationItemConditionValue(value.currentCount + event.metric.count, shiftId_1),
            condition.onMetric(state, event).value
        )
        Assertions.assertEquals(
            ConsolidationItemConditionValue(event.metric.count, shiftId_2),
            condition.onMetric(state.copy(config = config.copy(isShiftOn = true)), event).value
        )
    }

    private fun expectedParams(config: ConsolidationItemConditionConfig? = null) = listOf(
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
                code = ConditionStatisticCode(condition.code, "CONSOLIDATION_ITEM_COUNT"),
                requiredValue = config.requiredCount,
                currentValue = value.currentCount
            )
        )
    )

    private fun expectedLevel() = ConditionLevel(state.conditionId, condition.code, BigDecimal(0.8))
}
