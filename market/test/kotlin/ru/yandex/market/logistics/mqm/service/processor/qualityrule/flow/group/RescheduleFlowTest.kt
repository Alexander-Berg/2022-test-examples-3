package ru.yandex.market.logistics.mqm.service.processor.qualityrule.flow.group;

import com.nhaarman.mockitokotlin2.whenever
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.flow.BaseRescheduleFlow
import java.time.Clock
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Optional
import java.util.stream.Stream

@ExtendWith(MockitoExtension::class)
class BaseRescheduleFlowTest {

    @Mock
    lateinit var clock: Clock

    @ParameterizedTest
    @MethodSource("intProvider")
    @DisplayName("Проверяет, что время переносится до ближайшей 15ти минутки (00,15,30,45).")
    fun validReschedulingTime(time: LocalDateTime) {
        val rescheduleFlow = mockRescheduleFlow()
        val quarter = (0..3).random()
        val minutes = (1..14).random()
        val expectTime = time.plusMinutes((15 * (quarter + 1)).toLong())
        val testTime = time.plusMinutes((15 * quarter).toLong()).plusMinutes(minutes.toLong())
        whenever(clock.instant()).thenReturn(testTime.toInstant(ZoneOffset.UTC))

        rescheduleFlow.calculateNextDateTime() shouldBe Optional.of(expectTime.toInstant(ZoneOffset.UTC))
    }

    @Test
    @DisplayName("Проверяет угловой случай до полуночи.")
    fun validEdgeCase() {
        val rescheduleFlow = mockRescheduleFlow()
        val currentTime = LocalDateTime.of(2021, 10, 15, 23, 59, 1)
            .toInstant(ZoneOffset.UTC)
        val expectedTime = LocalDateTime.of(2021, 10, 16, 0, 0)
            .toInstant(ZoneOffset.UTC)
        whenever(clock.instant()).thenReturn(currentTime)
        rescheduleFlow.calculateNextDateTime() shouldBe Optional.of(expectedTime)
    }

    @Test
    @DisplayName("Проверяет угловой случай до для экспрeсса.")
    fun validEdgeCaseExpress() {
        val rescheduleFlow = mockRescheduleFlow(RescheduleFlow.ENDLESS_RESCHEDULE_EXPRESS_TIMES)
        val currentTime = LocalDateTime.of(2021, 10, 15, 23, 59, 1)
            .toInstant(ZoneOffset.UTC)
        val expectedTime = LocalDateTime.of(2021, 10, 16, 0, 0)
            .toInstant(ZoneOffset.UTC)
        whenever(clock.instant()).thenReturn(currentTime)
        rescheduleFlow.calculateNextDateTime() shouldBe Optional.of(expectedTime)
    }

    @Test
    @DisplayName("Проверяет, что время переносится до ближайших, кратных 5ти минут.")
    fun validReschedulingTimeExpress() {
        val rescheduleFlow = mockRescheduleFlow(RescheduleFlow.ENDLESS_RESCHEDULE_EXPRESS_TIMES)
        val currentTime = LocalDateTime.of(2021, 10, 15, 15, 22, 1)
            .toInstant(ZoneOffset.UTC)
        val expectedTime = LocalDateTime.of(2021, 10, 15, 15, 25)
            .toInstant(ZoneOffset.UTC)
        whenever(clock.instant()).thenReturn(currentTime)
        rescheduleFlow.calculateNextDateTime() shouldBe Optional.of(expectedTime)
    }

    private fun mockRescheduleFlow(
        rescheduleTimes: List<LocalTime> = RescheduleFlow.ENDLESS_RESCHEDULE_TIMES
    ): FlowStub {
        return FlowStub(clock, rescheduleTimes)
    }

    companion object {
        @JvmStatic
        fun intProvider(): Stream<LocalDateTime> =
            Stream.iterate(0) { n -> n + 1 }
                .limit(23)
                .map { LocalDateTime.of(2021, 10, 15, it, 0) }
    }
}

class FlowStub(
    clock: Clock,
    times: List<LocalTime>
): BaseRescheduleFlow(
    clock,
    times
) {
}
