package ru.yandex.market.wms.achievement.model.condition.impl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.yandex.market.wms.achievement.model.Statistic
import ru.yandex.market.wms.achievement.model.condition.ConditionLevel
import ru.yandex.market.wms.achievement.model.condition.ConditionState
import ru.yandex.market.wms.achievement.model.entity.UserEntity
import ru.yandex.market.wms.achievement.model.metric.AchievementGrantedMetric
import ru.yandex.market.wms.achievement.model.metric.AchievementTakenAwayMetric
import ru.yandex.market.wms.achievement.model.metric.MetricEvent
import java.math.BigDecimal
import java.time.Instant

internal class RollingOneConditionTest {
    private val condition = RollingOneCondition()
    private val user = UserEntity(1, "test", "sof")
    private val holderUser = UserEntity(2, "leader-test", "sof")
    private val achievementId: Long = 123
    private val config = RollingOneConfig(achievementId, holderUserId = holderUser.id!!)
    private val value = RollingOneValue(HolderStatus.HAVE_NOT)
    private val state = ConditionState(1, condition.code, user, config, value, Instant.now(), 1, 1)

    @Test
    fun statistic() {
        assertEquals(expectedStatistic(), condition.getStatistic(state))
    }

    @Test
    fun initialValue() {
        assertEquals(RollingOneValue(), condition.getInitialValue(config))
    }

    @Test
    fun level() {
        HolderStatus.values().forEach { status ->
            assertEquals(expectedLevel(status), condition.getLevel(getStateOnStatus(status)))
        }
    }

    @Test
    fun onAchievementGrantedMetric() {
        val grantedEvent1 = MetricEvent(AchievementGrantedMetric(1, user.id!!, achievementId), user, Instant.now())
        val newState1 = condition.onMetric(state, grantedEvent1)
        assertEquals(state, newState1)

        val grantedEvent2 = MetricEvent(AchievementGrantedMetric(1, user.id!!, achievementId), user, Instant.now())
        val newState2 = condition.onMetric(getStateOnStatus(HolderStatus.HOLD), grantedEvent2)
        assertEquals(getStateOnStatus(HolderStatus.LOST), newState2)
    }

    @Test
    fun getStateAfterAchievementLevelChanged() {
        val newState1 = condition.getStateAfterAchievementLevelChanged(getStateOnStatus(HolderStatus.HOLD), -1)
        assertEquals(getStateOnStatus(HolderStatus.HAVE_NOT), newState1)

        val newState2 = condition.getStateAfterAchievementLevelChanged(getStateOnStatus(HolderStatus.HOLD), 1)
        assertEquals(getStateOnStatus(HolderStatus.HOLD, config.copy(holderUserId = user.id!!)), newState2)
    }

    private fun expectedStatistic(): Statistic = Statistic(
        state.conditionId,
        state.conditionCode,
        values = listOf()
    )

    private fun expectedLevel(status: HolderStatus): ConditionLevel = ConditionLevel(
        state.conditionId,
        condition.code,
        when (status) {
            HolderStatus.HAVE_NOT -> BigDecimal.ONE
            HolderStatus.HOLD -> BigDecimal.ZERO
            HolderStatus.LOST -> BigDecimal.valueOf(-1)
        }
    )

    private fun getStateOnStatus(status: HolderStatus, newConfig: RollingOneConfig = config): ConditionState =
        state.copy(value = value.copy(status), config = newConfig)

    private fun achievementGrantedEvent(userId: Long) =
        MetricEvent(AchievementGrantedMetric(1, userId, achievementId), user, Instant.now())

    private fun achievementTakenAwayEvent(userId: Long) =
        MetricEvent(AchievementTakenAwayMetric(1, userId, achievementId), user, Instant.now())
}
