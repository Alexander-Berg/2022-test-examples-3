package ru.yandex.market.logistics.mqm.service.processor.qualityrule.flow.group

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.common.util.DateTimeUtils.MOSCOW_ZONE
import ru.yandex.market.logistics.mqm.entity.PlanFactGroup
import ru.yandex.market.logistics.mqm.entity.QualityRule
import ru.yandex.market.logistics.mqm.entity.additionaldata.PlanFactGroupAdditionalData
import ru.yandex.market.logistics.mqm.entity.aggregationentity.AggregationEntity
import ru.yandex.market.logistics.mqm.service.logging.LogService

@ExtendWith(MockitoExtension::class)
class ScheduleNextDayFlowTest {

    @Mock
    private lateinit var logService: LogService

    @AfterEach
    fun tearDown() {
        verifyNoMoreInteractions(logService)
    }

    @DisplayName("Проверить, что flow применяется, если групп обрабатывается до ожидаемого ScheduleTime")
    @Test
    fun isApplicable() {
        mockFlow().isApplicable(TEST_RULE, mockGroup()) shouldBe true
    }

    @DisplayName("Проверить, что flow не применяется, если групп пуста")
    @Test
    fun notApplicableIfNoDate() {
        val group = mockGroup(data = null)
        mockFlow().isApplicable(TEST_RULE, group) shouldBe false
    }

    @DisplayName("Проверить, что flow не применяется, группу обрабатывается после ожидаемого ScheduleTime")
    @Test
    fun notApplicableIfCalledAfterScheduleTime() {
        val testNow = EXPECTED_SCHEDULE_TIME.plusSeconds(1)
        mockFlow(testNow).isApplicable(TEST_RULE, mockGroup()) shouldBe false
    }

    @DisplayName("Проверка времени, на которое назначается следующая обработка")
    @Test
    fun apply() {
        val testGroup = mockGroup()
        mockFlow().apply(TEST_RULE, testGroup) shouldBe Optional.of(EXPECTED_SCHEDULE_TIME)
        verify(logService).logRescheduleProcessing(testGroup, EXPECTED_SCHEDULE_TIME)
    }

    @DisplayName("Проверить, что flow не возвращает время, если у группы нет даты")
    @Test
    fun applyReturnEmptyIfNoDate() {
        val testGroup = mockGroup(data = null)
        mockFlow().apply(TEST_RULE, testGroup) shouldBe Optional.empty()
        verify(logService).logRescheduleProcessing(testGroup, null)
    }

    private fun mockFlow(
        now: Instant = EXPECTED_SCHEDULE_TIME.minusSeconds(1),
    ) = ScheduleNextDayFlow(Clock.fixed(now, MOSCOW_ZONE), SCHEDULE_TIME, logService)

    private fun mockGroup(
        data: LocalDate? = GROUP_DAY,
    ) = PlanFactGroup().apply {
        setData(PlanFactGroupAdditionalData(aggregationEntity = AggregationEntity(date = data)))
    }

    companion object {
        private val SCHEDULE_TIME: LocalTime = LocalTime.of(12, 0)
        private val GROUP_DAY = LocalDate.of(2021, 10, 21)
        private val EXPECTED_SCHEDULE_TIME = LocalDateTime.of(
            GROUP_DAY.plusDays(1),
            SCHEDULE_TIME
        ).atZone(MOSCOW_ZONE).toInstant()
        private val TEST_RULE = QualityRule()
    }
}
