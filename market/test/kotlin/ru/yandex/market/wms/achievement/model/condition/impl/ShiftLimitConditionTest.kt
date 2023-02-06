package ru.yandex.market.wms.achievement.model.condition.impl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import ru.yandex.market.wms.achievement.model.Condition
import ru.yandex.market.wms.achievement.model.Settings
import ru.yandex.market.wms.achievement.model.Statistic
import ru.yandex.market.wms.achievement.model.StatisticValue
import ru.yandex.market.wms.achievement.model.condition.ConditionLevel
import ru.yandex.market.wms.achievement.model.condition.ConditionState
import ru.yandex.market.wms.achievement.model.condition.ConditionStatisticCode
import ru.yandex.market.wms.achievement.model.entity.UserEntity
import ru.yandex.market.wms.achievement.model.metric.MetricEvent
import ru.yandex.market.wms.achievement.model.metric.PickingItemMetric
import ru.yandex.market.wms.achievement.utils.mapToSet
import ru.yandex.market.wms.shared.libs.its.settings.provider.MockSettingsProvider
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

internal class ShiftLimitConditionTest {
    private val limit = 1
    private val condition = ShiftLimitCondition()
    private val shiftId_1 = "SHIFT_1"
    private val shiftId_2 = "SHIFT_2"
    private val shiftId_3 = "SHIFT_3"
    private val shiftDetails: HashSet<ShiftDetail> = hashSetOf(
        ShiftDetail(countOfUsers = limit, shiftId = shiftId_1, Instant.parse("2000-01-01T00:00:00.00Z")),
        ShiftDetail(countOfUsers = limit, shiftId = null),
        ShiftDetail(countOfUsers = 0, shiftId = shiftId_2),
    )
    private val config = ShiftLimitConditionConfig(userLimit = limit, shiftDetails = shiftDetails)
    private val value = ShiftLimitConditionValue(currentShiftId = null)
    private val user = UserEntity(1, "test", "sof", null)
    private val state = ConditionState(1, condition.code, user, config, value, Instant.now(), 1, 1)

    private val settingsProvider = MockSettingsProvider(Settings(condition = Condition(Duration.ofDays(1))))

    init {
        ReflectionTestUtils.setField(condition, "settingsProvider", settingsProvider)
    }

    @Test
    fun statistic() {
        assertEquals(expectedStatistic(false), condition.getStatistic(state))
        val event = MetricEvent(PickingItemMetric(1), user.copy(shiftId = shiftId_3), Instant.now())
        val newState = condition.onMetric(state, event)
        assertEquals(expectedStatistic(true), condition.getStatistic(newState))
    }

    @Test
    fun level() {
        assertEquals(expectedLevel(false), condition.getLevel(state))
        val event = MetricEvent(PickingItemMetric(1), user.copy(shiftId = shiftId_3), Instant.now())
        val stateAfterShiftChanged = condition.onMetric(state, event)
        assertEquals(expectedLevel(true), condition.getLevel(stateAfterShiftChanged))
        val stateAfterAchievementGranted = condition.getStateAfterAchievementLevelChanged(stateAfterShiftChanged, limit)
        assertEquals(expectedLevel(false), condition.getLevel(stateAfterAchievementGranted))
    }

    @Test
    fun getStateAfterAchievementLevelChanged() {
        val state = state.copy(user = user.copy(shiftId = shiftId_3), value = value.copy(currentShiftId = shiftId_3))
        val newState = condition.getStateAfterAchievementLevelChanged(state, 1)
        val value = newState.value as ShiftLimitConditionValue
        val config = newState.config as ShiftLimitConditionConfig
        assertNotEquals(state, newState)
        assertEquals(3, config.shiftDetails.size)
        val expectedShifts = setOf(null, shiftId_2, shiftId_3)
        assertEquals(expectedShifts, config.shiftDetails.mapToSet { it.shiftId })
    }

    @Test
    fun onMetric() {
        val event = MetricEvent(PickingItemMetric(1), user.copy(shiftId = shiftId_3), Instant.now())
        val newState = condition.onMetric(state, event)
        val value = newState.value as ShiftLimitConditionValue
        assertEquals(shiftId_3, value.currentShiftId)
    }

    private fun expectedStatistic(allowed: Boolean): Statistic = Statistic(
        state.conditionId,
        state.conditionCode,
        values = listOf(
            StatisticValue(ConditionStatisticCode(state.conditionCode, "ALLOWED"), 1, getLevel(allowed))
        )
    )

    private fun expectedLevel(allowed: Boolean): ConditionLevel = ConditionLevel(
        state.conditionId,
        state.conditionCode,
        getLevel(allowed)
    )

    private fun getLevel(allowed: Boolean): BigDecimal = if (allowed) BigDecimal.ONE else BigDecimal.ZERO
}
