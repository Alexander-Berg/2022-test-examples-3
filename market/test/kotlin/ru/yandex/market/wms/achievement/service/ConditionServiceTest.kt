package ru.yandex.market.wms.achievement.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.wms.achievement.dao.ConditionDao
import ru.yandex.market.wms.achievement.model.condition.ConditionState
import ru.yandex.market.wms.achievement.model.condition.impl.TestCondition
import ru.yandex.market.wms.achievement.model.entity.ConditionEntity
import ru.yandex.market.wms.achievement.model.entity.ConditionStateEntity
import ru.yandex.market.wms.achievement.model.entity.UserEntity
import ru.yandex.market.wms.achievement.model.metric.MetricEvent
import ru.yandex.market.wms.achievement.model.metric.PickingItemMetric
import java.time.Instant

internal class ConditionServiceTest {
    private val conditionDao: ConditionDao = mock()

    private val condition =
        TestCondition()
    private val user =
        UserEntity(1, "vasilenko-mni", "sof")
    private val conditionEntity =
        ConditionEntity(1, condition.code, condition.config(), Instant.now(), "ADDED", Instant.now(), "EDITED", 1)
    private val conditionStateEntity =
        ConditionStateEntity(conditionEntity.id!!, user.id!!, condition.value(), Instant.now(), 1)
    private val conditionState =
        ConditionState(conditionEntity.id!!, condition.code, user, conditionEntity.config, conditionStateEntity.value, Instant.now(), 1, 1)

    private val conditionService = ConditionService(conditionDao, listOf(condition))

    @Test
    fun getChangedStates() {
        //given
        whenever(conditionDao.getByCodeAndAchievements(listOf(1), condition.code, condition.configClass))
            .thenReturn(listOf(conditionEntity))
        whenever(
            conditionDao.getStatesForUserIdAndConditions(user.id!!, setOf(conditionEntity.id!!), condition.valueClass)
        )
            .thenReturn(listOf(conditionStateEntity))
        whenever(conditionDao.updateState(any())).thenReturn(1)
        //when
        val changedStates = conditionService.updateAndReturnChangedStates(MetricEvent(PickingItemMetric(1), user, Instant.now()), listOf(1))
        //then
        assertEquals(1, changedStates.size)
        changedStates.first().also {
            assertEquals(conditionState.conditionId, it.conditionId)
            assertEquals(conditionState.config, it.config)
            assertNotEquals(conditionState.value, it.value)
        }
        verify(conditionDao).updateState(any())
    }

    @Test
    fun getConditionLevels() {
        //given
        whenever(conditionDao.getByAchievement(any(), any())).thenReturn(listOf(conditionEntity))
        //when
        val conditionStates = conditionService.getConditionStates(1, user)
        val conditionLevels = conditionService.getConditionLevels(conditionStates)
        //then
        assertEquals(1, conditionLevels.size)
        assertEquals(condition.getLevel(conditionState), conditionLevels.first())
        verify(conditionDao).getByAchievement(any(), any())
    }
}
