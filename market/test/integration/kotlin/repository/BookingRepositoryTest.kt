package ru.yandex.market.logistics.calendaring.repository

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingStatus
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingType
import ru.yandex.market.logistics.calendaring.client.dto.enums.CalendaringType
import ru.yandex.market.logistics.calendaring.client.dto.enums.SupplierType
import ru.yandex.market.logistics.calendaring.extension.mapper.getBookingTypeSet
import ru.yandex.market.logistics.calendaring.model.domain.ExternalIdentifier
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class BookingRepositoryTest(@Autowired val bookingRepository: BookingRepository) : AbstractContextualTest() {

    val now: Instant = ZonedDateTime.of(2021, 5, 11, 12, 0, 0, 0, ZoneId.of("UTC")).toInstant()!!

    @JpaQueriesCount(1)
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/booking/find-for-warehouse-and-interval/before.xml"])
    fun findForWarehouseAndIntervalTest() {
        val result = bookingRepository.findForWarehouseAndInterval(
            1L,
            LocalDateTime.of(2021, 5, 1, 8, 0, 0).toInstant(ZoneOffset.of("+03:00")),
            LocalDateTime.of(2021, 5, 1, 16, 0, 0).toInstant(ZoneOffset.of("+03:00")),
        )
        softly.assertThat(result).isNotEmpty
        softly.assertThat(result.size).isEqualTo(2)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/booking/get-one-correct/before.xml"])
    fun getOneCorrectTest() {
        val one = bookingRepository.getOne(1L)
        softly.assertThat(one).isNotNull
        softly.assertThat(one.fromTime)
            .isEqualTo(ZonedDateTime.of(2021, 5, 1, 10, 0, 0, 0, ZoneId.of("Europe/Moscow")).toInstant())
        softly.assertThat(one.toTime)
            .isEqualTo(ZonedDateTime.of(2021, 5, 1, 11, 0, 0, 0, ZoneId.of("Europe/Moscow")).toInstant())
        softly.assertThat(one.gateId).isEqualTo(12L)
        softly.assertThat(one.type).isEqualTo(BookingType.MOVEMENT_SUPPLY)
        softly.assertThat(one.warehouseId).isEqualTo(174)
        softly.assertThat(one.id).isEqualTo(1L)
        softly.assertThat(one.externalId).isEqualTo("test-id")
        softly.assertThat(one.source).isEqualTo("TEST")
        softly.assertThat(one.status).isEqualTo(BookingStatus.ACTIVE)
    }

    @Test
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/booking/save-same-time-same-gate/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun bookSlotCannotSaveOnSameTime() {
        val from: ZonedDateTime = ZonedDateTime.of(2021, 5, 1, 10, 0, 0, 0, ZoneId.of("Europe/Moscow"))
        val to: ZonedDateTime = ZonedDateTime.of(2021, 5, 1, 11, 0, 0, 0, ZoneId.of("Europe/Moscow"))

        val externalIdentifier = ExternalIdentifier("1", "FFWF")

        val bookSlot1 =
            bookingRepository.bookSlot(
                174L,
                BookingType.MOVEMENT_SUPPLY,
                1L,
                from,
                to,
                BookingStatus.ACTIVE,
                externalIdentifier,
                "1",
                SupplierType.THIRD_PARTY,
                "101",
                ZoneId.of("Europe/Moscow"),
                CalendaringType.OUTBOUND.getBookingTypeSet().map { it.toString() },
                null,
            )
        val bookSlot2 =
            bookingRepository.bookSlot(
                174L,
                BookingType.MOVEMENT_SUPPLY,
                1L,
                from,
                to,
                BookingStatus.ACTIVE,
                externalIdentifier,
                null,
                SupplierType.THIRD_PARTY,
                "101",
                ZoneId.of("Europe/Moscow"),
                CalendaringType.OUTBOUND.getBookingTypeSet().map { it.toString() },
                null,
            )
        softly.assertThat(bookSlot1).isEqualTo(1L)
        softly.assertThat(bookSlot2).isNull()
    }


    @Test
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/booking/save-same-time-diff-gates/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun bookSlotCanSaveOnSameTimeDifferentGates() {
        val from: ZonedDateTime = ZonedDateTime.of(2021, 5, 1, 10, 0, 0, 0, ZoneId.of("Europe/Moscow"))
        val to: ZonedDateTime = ZonedDateTime.of(2021, 5, 1, 11, 0, 0, 0, ZoneId.of("Europe/Moscow"))

        val externalIdentifier = ExternalIdentifier("1", "FFWF")

        val bookSlot1 =
            bookingRepository.bookSlot(
                174L,
                BookingType.MOVEMENT_SUPPLY,
                1L,
                from,
                to,
                BookingStatus.ACTIVE,
                externalIdentifier,
                "1",
                SupplierType.THIRD_PARTY,
                "101",
                ZoneId.of("Europe/Moscow"),
                CalendaringType.OUTBOUND.getBookingTypeSet().map { it.toString() },
                ZonedDateTime.of(2021, 5, 11, 12, 0, 0, 0, ZoneOffset.UTC).toInstant(),
            )
        val bookSlot2 =
            bookingRepository.bookSlot(
                174L,
                BookingType.MOVEMENT_SUPPLY,
                2L,
                from,
                to,
                BookingStatus.ACTIVE,
                externalIdentifier,
                "2",
                SupplierType.THIRD_PARTY,
                "101",
                ZoneId.of("Europe/Moscow"),
                CalendaringType.OUTBOUND.getBookingTypeSet().map { it.toString() },
                null,
            )
        softly.assertThat(bookSlot1).isEqualTo(1L)
        softly.assertThat(bookSlot2).isEqualTo(2L)
    }

    @Test
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/booking/save-same-time-same-car/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun bookSlotCanSaveOnSameTimeSameCar() {
        val from: ZonedDateTime = ZonedDateTime.of(2021, 5, 1, 10, 0, 0, 0, ZoneId.of("Europe/Moscow"))
        val to: ZonedDateTime = ZonedDateTime.of(2021, 5, 1, 11, 0, 0, 0, ZoneId.of("Europe/Moscow"))

        val externalIdentifier = ExternalIdentifier("1", "FFWF")

        val bookSlot1 =
            bookingRepository.bookSlot(
                174L,
                BookingType.MOVEMENT_WITHDRAW,
                1L,
                from,
                to,
                BookingStatus.ACTIVE,
                externalIdentifier,
                "car1",
                SupplierType.THIRD_PARTY,
                null,
                ZoneId.of("Europe/Moscow"),
                CalendaringType.OUTBOUND.getBookingTypeSet().map { it.toString() },
                null,
            )
        val bookSlot2 =
            bookingRepository.bookSlot(
                174L,
                BookingType.MOVEMENT_WITHDRAW,
                1L,
                from,
                to,
                BookingStatus.ACTIVE,
                externalIdentifier,
                "car1",
                SupplierType.THIRD_PARTY,
                null,
                ZoneId.of("Europe/Moscow"),
                CalendaringType.OUTBOUND.getBookingTypeSet().map { it.toString() },
                null,
            )
        softly.assertThat(bookSlot1).isEqualTo(1L)
        softly.assertThat(bookSlot2).isEqualTo(2L)
    }

    @Test
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/booking/save-same-time-null-car1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun bookSlotCanSaveOnSameTimeNullCar1() {
        val from: ZonedDateTime = ZonedDateTime.of(2021, 5, 1, 10, 0, 0, 0, ZoneId.of("Europe/Moscow"))
        val to: ZonedDateTime = ZonedDateTime.of(2021, 5, 1, 11, 0, 0, 0, ZoneId.of("Europe/Moscow"))

        val externalIdentifier = ExternalIdentifier("1", "FFWF")

        val bookSlot1 =
            bookingRepository.bookSlot(
                174L,
                BookingType.MOVEMENT_WITHDRAW,
                1L,
                from,
                to,
                BookingStatus.ACTIVE,
                externalIdentifier,
                null,
                SupplierType.THIRD_PARTY,
                null,
                ZoneId.of("Europe/Moscow"),
                CalendaringType.OUTBOUND.getBookingTypeSet().map { it.toString() },
                null,
            )
        val bookSlot2 =
            bookingRepository.bookSlot(
                174L,
                BookingType.MOVEMENT_WITHDRAW,
                1L,
                from,
                to,
                BookingStatus.ACTIVE,
                externalIdentifier,
                "car1",
                SupplierType.THIRD_PARTY,
                null,
                ZoneId.of("Europe/Moscow"),
                CalendaringType.OUTBOUND.getBookingTypeSet().map { it.toString() },
                null,
            )
        softly.assertThat(bookSlot1).isEqualTo(1L)
        softly.assertThat(bookSlot2).isNull()
    }

    @Test
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/booking/save-same-time-null-car2/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun bookSlotCanSaveOnSameTimeNullCar2() {
        val from: ZonedDateTime = ZonedDateTime.of(2021, 5, 1, 10, 0, 0, 0, ZoneId.of("Europe/Moscow"))
        val to: ZonedDateTime = ZonedDateTime.of(2021, 5, 1, 11, 0, 0, 0, ZoneId.of("Europe/Moscow"))

        val externalIdentifier = ExternalIdentifier("1", "FFWF")

        val bookSlot1 =
            bookingRepository.bookSlot(
                174L,
                BookingType.MOVEMENT_WITHDRAW,
                1L,
                from,
                to,
                BookingStatus.ACTIVE,
                externalIdentifier,
                "car1",
                SupplierType.THIRD_PARTY,
                null,
                ZoneId.of("Europe/Moscow"),
                CalendaringType.OUTBOUND.getBookingTypeSet().map { it.toString() },
                null,
            )
        val bookSlot2 =
            bookingRepository.bookSlot(
                174L,
                BookingType.MOVEMENT_WITHDRAW,
                1L,
                from,
                to,
                BookingStatus.ACTIVE,
                externalIdentifier,
                null,
                SupplierType.THIRD_PARTY,
                null,
                ZoneId.of("Europe/Moscow"),
                CalendaringType.OUTBOUND.getBookingTypeSet().map { it.toString() },
                null,
            )
        softly.assertThat(bookSlot1).isEqualTo(1L)
        softly.assertThat(bookSlot2).isNull()
    }

    @JpaQueriesCount(1)
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/booking/get-one-correct/before.xml"])
    fun findAllByIdInTest() {
        bookingRepository.findAllByIdIn(listOf(1L))
    }

    @JpaQueriesCount(1)
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/booking/get-one-correct/before.xml"])
    fun findAllByIdInAndStatusTest() {
        bookingRepository.findAllByIdInAndStatus(listOf(1L), BookingStatus.ACTIVE)
    }

    @JpaQueriesCount(1)
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/booking/get-one-correct/before.xml"])
    fun findByExternalIdInAndSourceTest() {
        bookingRepository.findByExternalIdInAndSource(setOf("test-id"), "TEST")
    }

    @JpaQueriesCount(1)
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/booking/get-one-correct/before.xml"])
    fun findByExternalIdInAndSourceAndStatusTest() {
        bookingRepository.findByExternalIdInAndSourceAndStatus(setOf("test-id"), "TEST", BookingStatus.ACTIVE)
    }

    @JpaQueriesCount(1)
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/booking/get-one-correct/before.xml"])
    fun findAllByIdTest() {
        bookingRepository.findByIdIn(listOf(1L))
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/booking/change-expires-at/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/booking/change-expires-at/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateExpiresAt() {

        val now = LocalDateTime.of(2021, 5, 11, 12, 0, 0, 0);

        assertions().assertThat(
            bookingRepository.updateExpiresAt(
                1,
                ZonedDateTime.of(2021, 4, 29, 11, 0, 0, 0, ZoneId.of("UTC")).toInstant(),
                now
            )
        )
            .isEqualTo(1)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/booking/drop-expires-at/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/booking/drop-expires-at/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateExpiresAtNull() {
        val now = LocalDateTime.of(2021, 5, 11, 12, 0, 0, 0);

        assertions().assertThat(bookingRepository.updateExpiresAt(1, null, now)).isEqualTo(1)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/booking/drop-expires-at/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/booking/drop-expires-at/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateExpiresAtMissing() {
        val now = LocalDateTime.of(2021, 5, 11, 12, 0, 0, 0);

        assertions().assertThat(bookingRepository.updateExpiresAt(2, null, now)).isEqualTo(0)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/booking/cancel-expired/before.xml"])
    fun getExpired() {
        assertions()
            .assertThat(bookingRepository.getExpired(now))
            .containsExactly(1L)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/booking/cancel-by-ids/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/booking/cancel-by-ids/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun cancelSlotsBookingByIdTest() {
        val now = LocalDateTime.of(2021, 5, 11, 12, 0, 0, 0);
        bookingRepository.cancelSlotsBookingById(listOf(1), now)
    }
}
