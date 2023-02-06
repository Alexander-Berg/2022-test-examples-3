package ru.yandex.market.wms.achievement.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anySet
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.wms.achievement.logging.BusinessLogService
import ru.yandex.market.wms.achievement.model.Metric
import ru.yandex.market.wms.achievement.model.Settings
import ru.yandex.market.wms.achievement.model.condition.ConditionState
import ru.yandex.market.wms.achievement.model.condition.impl.TestCondition
import ru.yandex.market.wms.achievement.model.entity.UserEntity
import ru.yandex.market.wms.achievement.model.metric.MetricEvent
import ru.yandex.market.wms.achievement.model.metric.MetricEventDto
import ru.yandex.market.wms.achievement.model.metric.MetricType
import ru.yandex.market.wms.achievement.model.metric.PickingItemMetric
import ru.yandex.market.wms.achievement.utils.mapToSet
import ru.yandex.market.wms.shared.libs.its.settings.provider.MockSettingsProvider
import ru.yandex.market.wms.shared.libs.its.settings.provider.SettingsProvider
import java.time.Instant

internal class MetricServiceTest {
    private val conditionService: ConditionService = mock()
    private val achievementService: AchievementService = mock()
    private val grantAchievementService: GrantAchievementService = mock()
    private val userService: UserService = mock()
    private val businessLogService: BusinessLogService = mock()
    private val settings = Settings(
        metric = Metric(
            enabled = true,
            supportedTypes = setOf(MetricType.PICKING_ITEM),
            users = emptySet()
        )
    )

    private val settingsEnableForOne = Settings(
        metric = Metric(
            enabled = true,
            supportedTypes = setOf(MetricType.PICKING_ITEM),
            users = setOf("second_test_user")
        )
    )

    private val condition = TestCondition()

    private fun setupMetricService(settings: Settings): MetricService {
        val settingsProvider: SettingsProvider<Settings> = MockSettingsProvider(settings)

        return MetricService(
            conditionService,
            userService,
            achievementService,
            grantAchievementService,
            businessLogService,
            settingsProvider
        )
    }

    @BeforeEach
    fun init() {
        Mockito.reset(conditionService, achievementService)
    }

    @Test
    fun receiveMetric() {
        //given
        val username = "test_user"
        val whs = "sof"
        val metric = PickingItemMetric(1, "some_area")
        val date = Instant.now()
        val user = UserEntity(1, username, whs)
        val event = MetricEvent(metric, user, date)
        val conditionState =
            ConditionState(1, condition.code, user, condition.config(), condition.value(), Instant.now(), 1, 1)
        whenever(achievementService.getActiveAchievementsIds(eq("sof"))).thenReturn(listOf(1L))
        whenever(conditionService.updateAndReturnChangedStates(event, listOf(1L))).thenReturn(setOf(conditionState))
        whenever(userService.getOrCreateUser(username, whs)).thenReturn(user)
        //when
        setupMetricService(settings).onMetric(event.toDto())
        //then
        verify(conditionService).updateAndReturnChangedStates(event, listOf(1L))
        verify(achievementService).checkAchievements(user, setOf(conditionState).mapToSet { it.conditionId })
    }

    @Test
    fun receiveMetricWhenEnableForOne() {
        //given
        val usernames = listOf("test_user", "second_test_user")
        val whs = "sof"
        val metric = PickingItemMetric(1, "some_area")
        val date = Instant.now()
        val users = usernames.map { UserEntity(1, it, whs) }
        val events = users.map { MetricEvent(metric, it, date) }
        val conditionStates =
            users.map { ConditionState(1, condition.code, it, condition.config(), condition.value(), Instant.now(), 1, 1) }

        whenever(achievementService.getActiveAchievementsIds(eq("sof"))).thenReturn(listOf(1L))
        whenever(
            conditionService.updateAndReturnChangedStates(
                events[0],
                listOf(1)
            )
        ).thenReturn(setOf(conditionStates[0]))
        whenever(
            conditionService.updateAndReturnChangedStates(
                events[1],
                listOf(1)
            )
        ).thenReturn(setOf(conditionStates[1]))
        whenever(
            userService.getOrCreateUser(
                events[1].user.username,
                events[1].user.whsCode
            )
        ).thenReturn(events[1].user)
        //when
        events.forEach { setupMetricService(settingsEnableForOne).onMetric(it.toDto()) }
        //then
        verify(conditionService, times(1)).updateAndReturnChangedStates(any(), anyList())
        verify(achievementService, times(1)).checkAchievements(any(), anySet())
    }

    @Test
    fun receiveMetricNoActiveAchievements() {
        //given
        val username = "test_user"
        val whs = "sof"
        val metric = PickingItemMetric(1, "some_area")
        val date = Instant.now()
        val user = UserEntity(1, username, whs)
        val event = MetricEvent(metric, user, date)

        whenever(achievementService.getActiveAchievementsIds(eq("sof"))).thenReturn(emptyList())
        whenever(userService.getOrCreateUser(username, whs)).thenReturn(user)
        //when
        setupMetricService(settings).onMetric(event.toDto())
        //then
        verify(conditionService, never()).updateAndReturnChangedStates(any(), any())
        verify(conditionService, never()).updateAndReturnChangedStates(any(), any())
        verify(achievementService, never()).checkAchievements(any(), any())
    }

    private fun MetricEvent<*>.toDto() =
        MetricEventDto(this.metric, this.user.username, this.user.whsCode, this.user.shiftId, this.date)
}
