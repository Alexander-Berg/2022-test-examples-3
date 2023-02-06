package ru.yandex.market.logistics.calendaring.service.booking

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.client.dto.enums.CalendaringType
import ru.yandex.market.logistics.calendaring.util.DateTimeInterval
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount
import java.time.ZoneId
import java.time.ZonedDateTime

class BookingServiceTest(@Autowired private val bookingService: BookingService) : AbstractContextualTest() {


    @Test
    @JpaQueriesCount(1)
    fun getBookedSlotsForWarehouseJpaQueryCountTest() {

        val dateTimeInterval = DateTimeInterval(
            ZonedDateTime.of(2021, 5, 17, 10, 0, 0, 0, ZoneId.of("Europe/Moscow")),
            ZonedDateTime.of(2021, 5, 17, 11, 0, 0, 0, ZoneId.of("Europe/Moscow"))
        )

        bookingService.getBookedSlotsForWarehouse(1, dateTimeInterval)
    }


    @Test
    @JpaQueriesCount(1)
    fun getBookedSlotsForWarehouseWithMetaJpaQueryCountTest() {

        val dateTimeInterval = DateTimeInterval(
            ZonedDateTime.of(2021, 5, 17, 10, 0, 0, 0, ZoneId.of("Europe/Moscow")),
            ZonedDateTime.of(2021, 5, 17, 11, 0, 0, 0, ZoneId.of("Europe/Moscow"))
        )

        bookingService.getBookedSlotsForWarehouseWithMeta(1, dateTimeInterval, CalendaringType.INBOUND)
    }

}
