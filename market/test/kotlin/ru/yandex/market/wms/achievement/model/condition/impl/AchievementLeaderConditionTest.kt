package ru.yandex.market.wms.achievement.model.condition.impl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import ru.yandex.market.wms.achievement.logging.BusinessLogService
import ru.yandex.market.wms.achievement.model.Statistic
import ru.yandex.market.wms.achievement.model.StatisticValue
import ru.yandex.market.wms.achievement.model.condition.ConditionLevel
import ru.yandex.market.wms.achievement.model.condition.ConditionState
import ru.yandex.market.wms.achievement.model.condition.ConditionStatisticCode
import ru.yandex.market.wms.achievement.model.entity.UserEntity
import ru.yandex.market.wms.achievement.model.metric.AchievementGrantedMetric
import ru.yandex.market.wms.achievement.model.metric.AchievementTakenAwayMetric
import ru.yandex.market.wms.achievement.model.metric.MetricEvent
import ru.yandex.market.wms.achievement.service.AchievementService
import java.math.BigDecimal
import java.time.Instant

internal class AchievementLeaderConditionTest {
    private val condition = AchievementLeaderCondition()
    private val user = UserEntity(1, "test", "sof")
    private val leaderUser = UserEntity(2, "leader-test", "sof")
    private val config = AchievementLeaderConfig(leaderUser.id!!)
    private val value = AchievementLeaderValue(currentCount = 1, leaderUser.id!!, leaderCount = 2)
    private val state = ConditionState(1, condition.code, user, config, value, Instant.now(), 1, 1)

    private val achievementService: AchievementService = mock()
    private val businessLogService: BusinessLogService = mock()

    init {
        ReflectionTestUtils.setField(condition, "achievementService", achievementService)
        ReflectionTestUtils.setField(condition, "businessLogService", businessLogService)
    }

    @Test
    fun statistic() {
        assertEquals(expectedStatistic(), condition.getStatistic(state))
    }

    @Test
    fun initialValue() {
        whenever(achievementService.getAchievementsCount(leaderUser.id!!)).thenReturn(2)
        assertEquals(AchievementLeaderValue(0, config.leaderUserId, 2), condition.getInitialValue(config))
    }

    @Test
    fun getStateAfterAchievementLevelChanged() {
        val newState1 = condition.getStateAfterAchievementLevelChanged(state, 1)
        assertEquals(state.copy(config = AchievementLeaderConfig(user.id!!)), newState1)
    }

    @Test
    fun level() {
        assertEquals(expectedLevel(BigDecimal.ZERO), condition.getLevel(state))
        val newState = condition.onMetric(state, achievementGrantedEvent(user.id!!))
        assertEquals(expectedLevel(BigDecimal.ONE), condition.getLevel(newState))
    }

    @Test
    fun onAchievementGrantedMetric() {
        val achievementGrantedToUser = achievementGrantedEvent(user.id!!)
        val newState1 = condition.onMetric(state, achievementGrantedToUser)
        assertEquals(
            state.copy(value = value.copy(currentCount = value.currentCount + achievementGrantedToUser.metric.grantedLevel)),
            newState1
        )

        val achievementGrantedToLeader = achievementGrantedEvent(leaderUser.id!!)
        val newState2 = condition.onMetric(state, achievementGrantedToLeader)
        assertEquals(
            state.copy(value = value.copy(leaderCount = value.leaderCount + achievementGrantedToLeader.metric.grantedLevel)),
            newState2
        )
    }

    @Test
    fun onAchievementTakenAwayMetric() {
        val achievementTakenAwayFromUser = achievementTakenAwayEvent(user.id!!)
        val newState1 = condition.onMetric(state, achievementTakenAwayFromUser)
        assertEquals(
            state.copy(value = value.copy(currentCount = value.currentCount - achievementTakenAwayFromUser.metric.takenLevel)),
            newState1
        )

        val achievementTakenAwayFromLeader = achievementTakenAwayEvent(leaderUser.id!!)
        val newState2 = condition.onMetric(state, achievementTakenAwayFromLeader)
        assertEquals(
            state.copy(value = value.copy(leaderCount = value.leaderCount - achievementTakenAwayFromLeader.metric.takenLevel)),
            newState2
        )
    }

    private fun expectedStatistic(): Statistic = Statistic(
        state.conditionId,
        state.conditionCode,
        values = listOf(
            StatisticValue(
                ConditionStatisticCode(state.conditionCode, "ACHIEVEMENT_COUNT"),
                value.leaderCount,
                value.currentCount
            )
        )
    )

    private fun expectedLevel(level: BigDecimal): ConditionLevel = ConditionLevel(
        state.conditionId,
        condition.code,
        level
    )

    private fun achievementGrantedEvent(userId: Long) =
        MetricEvent(AchievementGrantedMetric(2, userId, 123), user, Instant.now())

    private fun achievementTakenAwayEvent(userId: Long) =
        MetricEvent(AchievementTakenAwayMetric(1, userId, 123), user, Instant.now())
}
