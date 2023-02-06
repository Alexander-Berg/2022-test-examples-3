package ru.yandex.market.logistics.mqm.service

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.service.yt.dto.YtScScSchedule
import java.time.Clock
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Поле intake_schedule может быть: "1 раз в 2 недели по нечетным неделям", "Ежедневно", "Еженедельно",
 * "Еженедельно ", "По будням", "по будням"
 * Поле intake_day: "1", "1,2,3,4,5", "1,2,4,5,6", и тд, "6", "7", "everyday" (встречается совместно с intake_schedule="Ежедневно")
 * Проверяем разные комбинации.
 * Если комбинация нераспознана, метод возвращает null
 */
class TransitScheduleCalculationServiceImplTest {
    private lateinit var transitScheduleCalculationService: TransitScheduleCalculationServiceImpl

    @BeforeEach
    fun init() {
        transitScheduleCalculationService = TransitScheduleCalculationServiceImpl(
            Clock.fixed(DEFAULT_TIME.toInstant(), DateTimeUtils.MOSCOW_ZONE))
    }

    /**
     * 08:00 + 10h = 18:00 и 08:00 + 10.5h = 18:30")
     */
    @DisplayName("Расписания того же дня, что старт")
    @Test
    fun shouldCalculateExpectedTimeAtTheSameDay() {
        assertSoftly {
            transitScheduleCalculationService.calculateTransitExpectedTime(
                YtScScSchedule(1L, 2L, Duration.ofHours(10), "ежедневно", "1,2,3,4,5,6,7", LocalTime.NOON, LocalTime.NOON)
            ) shouldBe ZonedDateTime.parse("2021-09-26T18:00:00+03").toInstant()
            transitScheduleCalculationService.calculateTransitExpectedTime(
                YtScScSchedule(1L, 2L, Duration.ofMinutes((10.5 * 60).toLong()), "Ежедневно", "1,2,3,4,5,6,7", LocalTime.NOON, LocalTime.NOON)
            ) shouldBe ZonedDateTime.parse("2021-09-26T18:30:00+03").toInstant()
        }
    }

    /**
     *  08:00 + next day delivery started from 9:00 + 10h = 19:00
     */
    @DisplayName("Расписания отправки в последующие дни")
    @Test
    fun shouldCalculateExpectedTimeAtNextDays() {
        assertSoftly {
            transitScheduleCalculationService.calculateTransitExpectedTime(
                YtScScSchedule(1L, 2L, Duration.ofHours(10), "по будням", "1,2,3,4,5", LocalTime.NOON, LocalTime.NOON)
            ) shouldBe ZonedDateTime.parse("2021-09-27T19:00:00+03").toInstant()
            transitScheduleCalculationService.calculateTransitExpectedTime(
                YtScScSchedule(1L, 2L, Duration.ofHours(10), "еженедельно ", "2,3", LocalTime.NOON, LocalTime.NOON)
            ) shouldBe ZonedDateTime.parse("2021-09-28T19:00:00+03").toInstant()
            transitScheduleCalculationService.calculateTransitExpectedTime(
                YtScScSchedule(1L, 2L, Duration.ofHours(10), "1 раз в 2 недели по нечетным неделям ", "6", LocalTime.NOON, LocalTime.NOON)
            ) shouldBe ZonedDateTime.parse("2021-10-02T19:00:00+03").toInstant()
        }
    }

    @DisplayName("Нераспознанная комбинация")
    @Test
    fun shouldReturnNullOnUnparsed() {
        assertSoftly {
            transitScheduleCalculationService.calculateTransitExpectedTime(
                YtScScSchedule(1L, 2L, Duration.ofHours(10), "что-то непонятное", "6", LocalTime.NOON, LocalTime.NOON)
            ) shouldBe null
        }
    }

    companion object {
        val DEFAULT_TIME: ZonedDateTime = ZonedDateTime.parse("2021-09-26T08:00:00.00+03") // it's Sunday
    }
}
