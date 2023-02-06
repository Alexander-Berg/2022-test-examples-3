package ru.yandex.market.logistics.calendaring.service.validation


import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.model.exception.BadRequestException
import ru.yandex.market.logistics.calendaring.service.datetime.DateTimeService
import ru.yandex.market.logistics.calendaring.service.system.SystemPropertyService
import ru.yandex.market.logistics.calendaring.service.system.keys.SystemPropertyIntegerKey
import ru.yandex.market.logistics.calendaring.util.DateTimeInterval
import java.time.ZoneId
import java.time.ZonedDateTime

internal class SlotValidationServiceImplTest(@Autowired private val periodValidationService: PeriodValidationService,
                                             @Autowired private val dateTimeService: DateTimeService,
                                             @Autowired private val systemPropertyService: SystemPropertyService) : AbstractContextualTest() {
    val now = dateTimeService.localDateTimeNow()

    @Test
    fun validateSlotDuration() {
        val slotDuration = -1
        val exception = assertThrows<BadRequestException> { periodValidationService.validateSlotDuration(slotDuration) }
        assertEquals("Required slot duration $slotDuration minutes should be positive", exception.message)
    }

    /** now = 2021-05-11T12:00 in UTC
     from = 2021-05-11T16:00 +05 = 2021-05-11T11:00 in UTC
     to =  2021-05-11T18:00 +05 = 2021-05-11T13:00 in UTC
    */
    @Test
    fun validateRequestIntervalFromInPast() {
        val from = ZonedDateTime.of(2021,5,11,16,0,0,0, ZoneId.of("Asia/Yekaterinburg"))
        val to = ZonedDateTime.of(2021,5,11,18,0,0,0, ZoneId.of("Asia/Yekaterinburg"))
        val period = DateTimeInterval(from, to)
        assertDoesNotThrow { periodValidationService.validateRequestInterval(period) }
    }

    @Test
    fun validateRequestIntervalToInPast2() {
        val period = DateTimeInterval.of(now.plusDays(-2), now.plusDays(-1), ZoneId.of("Asia/Yekaterinburg"))

        val exception: Throwable = assertThrows<BadRequestException> { periodValidationService.validateRequestInterval(period) }
        assertEquals("${period.to} shouldn't be in the past", exception.message)
    }

    @Test
    fun validateRequestIntervalFromAfterTo() {
        val period = DateTimeInterval.of(now, now.plusDays(-1), ZoneId.of("Asia/Yekaterinburg"))
        val exception: Throwable = assertThrows<BadRequestException> { periodValidationService.validateRequestInterval(period) }
        assertEquals("${period.from} " +
            "shouldn't be after or equals ${period.to}", exception.message)
    }

    @Test
    fun validateRequestIntervalFromOverLimit() {
        val bookingLimitDays = systemPropertyService.getProperty(SystemPropertyIntegerKey.SLOT_BOOKING_LIMIT_DAYS).toLong()
        val period = DateTimeInterval.of(now, now.plusDays(bookingLimitDays + 1), ZoneId.of("Asia/Yekaterinburg"))

        val exception: Throwable = assertThrows<BadRequestException> { periodValidationService.validateRequestInterval(period) }
        assertEquals("request interval shouldn't exceed $bookingLimitDays days", exception.message)
    }

}
