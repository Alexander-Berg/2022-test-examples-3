package ru.yandex.market.logistics.mqm.utils

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import ru.yandex.market.common.util.DateTimeUtils
import java.time.Clock
import java.time.Instant

class DateTimeUtilsTest {

    @Test
    fun roundToNextMoscowMiddayWithDeltaOnlyTimeChanged() {
        roundToNextMoscowMiddayWithDeltaCheck(
            Instant.parse("2020-11-02T08:52:00.00Z"),
            Instant.parse("2020-10-02T08:00:00.00Z"),
            Instant.parse("2020-11-02T09:00:00.00Z")
        )
    }

    @Test
    fun roundToNextMoscowMiddayWithDeltaDateAndTimeChangedBecauseOfTime() {
        roundToNextMoscowMiddayWithDeltaCheck(
            Instant.parse("2020-11-02T11:00:00.00Z"),
            Instant.parse("2020-10-02T08:00:00.00Z"),
            Instant.parse("2020-11-03T09:00:00.00Z")
        )
    }

    @Test
    fun roundToNextMoscowMiddayWithDeltaDateAndTimeChangedBecauseLessThanNow() {
        roundToNextMoscowMiddayWithDeltaCheck(
            Instant.parse("2020-11-02T08:00:00.00Z"),
            Instant.parse("2020-12-02T08:00:00.00Z"),
            Instant.parse("2020-12-02T09:00:00.00Z")
        )
    }

    @Test
    fun roundToNextMoscowMiddayWithDeltaDateAndTimeChangedBecauseLessThanNowAndNowGreaterDelta() {
        roundToNextMoscowMiddayWithDeltaCheck(
            Instant.parse("2020-11-02T08:00:00.00Z"),
            Instant.parse("2020-12-02T08:51:00.00Z"),
            Instant.parse("2020-12-03T09:00:00.00Z")
        )
    }

    @Test
    fun roundToNextMoscowMiddayWithDeltaDateAndTimeChangedBecauseGreaterNowAndNowGreaterDelta() {
        roundToNextMoscowMiddayWithDeltaCheck(
            Instant.parse("2020-12-02T08:55:00.00Z"),
            Instant.parse("2020-12-02T08:51:00.00Z"),
            Instant.parse("2020-12-03T09:00:00.00Z")
        )
    }

    @Test
    fun roundToNextMoscowMiddayWithDeltaDateAndTimeChangedBecauseGreaterNowAndNowGreaterMidday() {
        roundToNextMoscowMiddayWithDeltaCheck(
            Instant.parse("2020-12-02T10:55:00.00Z"),
            Instant.parse("2020-12-02T09:01:00.00Z"),
            Instant.parse("2020-12-03T09:00:00.00Z")
        )
    }

    private fun roundToNextMoscowMiddayWithDeltaCheck(
        given: Instant,
        now: Instant,
        expectedResult: Instant
    ) {
        val actualResult = roundToNextMoscowMiddayWithDelta(given, Clock.fixed(now, DateTimeUtils.MOSCOW_ZONE))
        actualResult shouldBe expectedResult
    }
}
