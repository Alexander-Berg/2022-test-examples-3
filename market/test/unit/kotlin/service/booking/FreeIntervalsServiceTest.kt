package ru.yandex.market.logistics.calendaring.service.booking

import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.calendaring.base.SoftAssertionSupport
import ru.yandex.market.logistics.calendaring.util.DateTimeInterval
import java.time.ZoneId
import java.time.ZonedDateTime

class FreeIntervalsServiceTest: SoftAssertionSupport() {

    private val freeIntervalsService: FreeIntervalsService = FreeIntervalsServiceImpl()

    @Test
    fun nonWorkingHoursWhenWorkingHoursInsidePeriod() {

        val period = DateTimeInterval(
            ZonedDateTime.of(2021, 1, 1, 10, 0, 0, 0, ZoneId.of("UTC")),
            ZonedDateTime.of(2021, 1, 3, 18, 0, 0, 0, ZoneId.of("UTC")),
        )

        val workingDays: List<DateTimeInterval> =
        listOf(

            DateTimeInterval(
                ZonedDateTime.of(2021, 1, 1, 11, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2021, 1, 2, 9, 0, 0, 0, ZoneId.of("UTC")),
            ),
            DateTimeInterval(
                ZonedDateTime.of(2021, 1, 2, 11, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2021, 1, 3, 9, 0, 0, 0, ZoneId.of("UTC")),
            )
        )

        val nonWorkingHours = freeIntervalsService.getNonWorkingHours(period, workingDays)
        softly.assertThat(nonWorkingHours[0].from.dayOfMonth).isEqualTo(1)
        softly.assertThat(nonWorkingHours[0].from.hour).isEqualTo(10)
        softly.assertThat(nonWorkingHours[0].to.hour).isEqualTo(11)

        softly.assertThat(nonWorkingHours[1].from.dayOfMonth).isEqualTo(2)
        softly.assertThat(nonWorkingHours[1].from.hour).isEqualTo(9)
        softly.assertThat(nonWorkingHours[1].to.hour).isEqualTo(11)

        softly.assertThat(nonWorkingHours[2].from.dayOfMonth).isEqualTo(3)
        softly.assertThat(nonWorkingHours[2].from.hour).isEqualTo(9)
        softly.assertThat(nonWorkingHours[2].to.hour).isEqualTo(18)
    }

    @Test
    fun nonWorkingHoursWhenWorkingHoursOutsidePeriod() {

        val period = DateTimeInterval(
            ZonedDateTime.of(2021, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC")),
            ZonedDateTime.of(2021, 1, 3, 8, 0, 0, 0, ZoneId.of("UTC")),
        )

        val workingDays: List<DateTimeInterval> =
            listOf(

                DateTimeInterval(
                    ZonedDateTime.of(2021, 1, 1, 11, 0, 0, 0, ZoneId.of("UTC")),
                    ZonedDateTime.of(2021, 1, 2, 9, 0, 0, 0, ZoneId.of("UTC")),
                ),
                DateTimeInterval(
                    ZonedDateTime.of(2021, 1, 2, 11, 0, 0, 0, ZoneId.of("UTC")),
                    ZonedDateTime.of(2021, 1, 3, 9, 0, 0, 0, ZoneId.of("UTC")),
                )
            )

        val nonWorkingHours = freeIntervalsService.getNonWorkingHours(period, workingDays)

        println(nonWorkingHours)
        softly.assertThat(nonWorkingHours[0].from.dayOfMonth).isEqualTo(2)
        softly.assertThat(nonWorkingHours[0].from.hour).isEqualTo(9)
        softly.assertThat(nonWorkingHours[0].to.hour).isEqualTo(11)

    }



}
