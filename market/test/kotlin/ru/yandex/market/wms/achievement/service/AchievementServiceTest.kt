package ru.yandex.market.wms.achievement.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import ru.yandex.market.wms.achievement.dao.AchievementDao
import ru.yandex.market.wms.achievement.logging.BusinessLogService
import ru.yandex.market.wms.achievement.model.achievement.AchievementType
import ru.yandex.market.wms.achievement.model.achievement.GrantedAchievement
import ru.yandex.market.wms.achievement.model.condition.ConditionLevel
import ru.yandex.market.wms.achievement.model.condition.ConditionState
import ru.yandex.market.wms.achievement.model.condition.impl.TestCondition
import ru.yandex.market.wms.achievement.model.entity.AchievementEntity
import ru.yandex.market.wms.achievement.model.entity.AchievementStateEntity
import ru.yandex.market.wms.achievement.model.entity.UserEntity
import ru.yandex.market.wms.achievement.service.storage.StorageService
import ru.yandex.market.wms.achievement.utils.mapToSet
import ru.yandex.market.wms.achievement.utils.toBigDecimal
import java.math.BigDecimal
import java.time.Instant

internal class AchievementServiceTest {
    private val achievementDao: AchievementDao = mock()
    private val conditionService: ConditionService = mock()
    private val eventService: EventService = mock()
    private val notificationService: NotificationService = mock()
    private val storageService: StorageService = mock()
    private val businessLogService: BusinessLogService = mock()

    private val achievementService = AchievementService(
        achievementDao, conditionService, storageService, businessLogService
    )

    private val user = UserEntity(1, "vasilenko-mni", "sof")
    private val condition = TestCondition()
    private val conditionState = ConditionState(1, condition.code, user, condition.config(), condition.value(), Instant.parse("2022-04-25T10:40:00Z"), 1, 1)

    private val achievementEntity = AchievementEntity(
        id = 1,
        title = "test",
        description = "test description",
        comment = "test comment",
        type = AchievementType.OPEN,
        maxLevel = BigDecimal(2),
        startedAt = Instant.parse("2022-04-26T05:00:00Z"),
        addDate = Instant.parse("2022-04-25T10:40:00Z"),
        addWho = "testUser",
        whsCode = "sof",
    )

    private val achievementState = AchievementStateEntity(
        user.id!!, achievementEntity.id!!, BigDecimal(1.3), Instant.now()
    )

    private val conditionLevel = ConditionLevel(conditionState.conditionId, condition.code, BigDecimal(0.8))
    private val conditionLevel2 = ConditionLevel(conditionState.conditionId, condition.code, BigDecimal(1))

    @Test
    fun checkStateUpdatedNotLevel() {
        //given
        val knownStates = setOf(conditionState).mapToSet { it.conditionId }
        whenever(achievementDao.getAllByConditions(knownStates))
            .thenReturn(listOf(achievementEntity))
        whenever(achievementDao.getStatesByUserAndAchievements(user.id!!, setOf(achievementEntity.id!!), true))
            .thenReturn(listOf(achievementState))
        whenever(conditionService.getConditionStates(achievementEntity.id!!, user)).thenReturn(
            emptyList()
        )
        whenever(conditionService.getConditionLevels(any()))
            .thenReturn(setOf(conditionLevel))
        whenever(achievementDao.updateState(any()))
            .thenAnswer {
                val state: AchievementStateEntity = it.arguments[0] as AchievementStateEntity
                assertEquals(conditionLevel.level + achievementState.level.toInt().toBigDecimal(), state.level)
                return@thenAnswer null
            }
        //when
        val givenAchievements = achievementService.checkAchievements(user, knownStates)
        assertTrue(givenAchievements.isEmpty())
        //then
        verify(achievementDao).updateState(any())
    }

    @Test
    fun checkNewLevel() {
        //given
        val knownStates = setOf(conditionState).mapToSet { it.conditionId }
        whenever(achievementDao.getAllByConditions(knownStates))
            .thenReturn(listOf(achievementEntity))
        whenever(achievementDao.getStatesByUserAndAchievements(user.id!!, setOf(achievementEntity.id!!), true))
            .thenReturn(listOf(achievementState))
        whenever(conditionService.getConditionStates(achievementEntity.id!!, user)).thenReturn(
            emptyList()
        )
        whenever(conditionService.getConditionLevels(any()))
            .thenReturn(setOf(conditionLevel2))
        whenever(achievementDao.updateState(any()))
            .thenAnswer {
                val state: AchievementStateEntity = it.arguments[0] as AchievementStateEntity
                assertEquals(conditionLevel2.level + achievementState.level.toInt().toBigDecimal(), state.level)
                return@thenAnswer null
            }
        //when
        val givenAchievements = achievementService.checkAchievements(user, knownStates)
        //then
        verify(achievementDao).updateState(any())
        assertEquals(givenAchievements, listOf(
            GrantedAchievement(
                newLevel = 2,
                oldLevel = 1,
                achievement = achievementEntity
            )
        ))
    }

    @Test
    fun checkAlreadyMaxLevelAchievement() {
        //given
        val nonRepeatableEntity = AchievementEntity(
            id = 1,
            title = "test",
            description = "test description",
            comment = "test comment",
            type = AchievementType.OPEN,
            maxLevel = BigDecimal(1),
            startedAt = Instant.parse("2022-04-26T05:00:00Z"),
            addDate = Instant.parse("2022-04-25T10:40:00Z"),
            addWho = "testUser",
            whsCode = "sof"
        )
        val knownStates = setOf(conditionState).mapToSet { it.conditionId }
        whenever(achievementDao.getAllByConditions(knownStates))
            .thenReturn(listOf(nonRepeatableEntity))
        whenever(achievementDao.getStatesByUserAndAchievements(user.id!!, setOf(nonRepeatableEntity.id!!)))
            .thenReturn(listOf(achievementState))
        whenever(conditionService.getConditionStates(nonRepeatableEntity.id!!, user)).thenReturn(
            emptyList()
        )
        whenever(conditionService.getConditionLevels(any()))
            .thenReturn(setOf(conditionLevel))

        //when
        val givenAchievements = achievementService.checkAchievements(user, knownStates)
        assertTrue(givenAchievements.isEmpty())
        //then
        verifyNoInteractions(notificationService)
        verifyNoInteractions(eventService)
    }
}
