package ru.yandex.market.logistics.calendaring.client

import org.apache.commons.io.IOUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.ResponseCreator
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import ru.yandex.market.logistics.calendaring.client.config.CalendaringServiceClientTestConfig
import ru.yandex.market.logistics.calendaring.client.dto.BookConsolidatedSlotRequest
import ru.yandex.market.logistics.calendaring.client.dto.BookSlotRequest
import ru.yandex.market.logistics.calendaring.client.dto.BookingIdAndSizeDTO
import ru.yandex.market.logistics.calendaring.client.dto.BookingListResponseV2
import ru.yandex.market.logistics.calendaring.client.dto.DecreaseConsolidatedSlotRequest
import ru.yandex.market.logistics.calendaring.client.dto.DecreaseSlotRequest
import ru.yandex.market.logistics.calendaring.client.dto.DestinationWithTakenLimits
import ru.yandex.market.logistics.calendaring.client.dto.GetAvailableLimitRequest
import ru.yandex.market.logistics.calendaring.client.dto.GetFreeSlotsRequest
import ru.yandex.market.logistics.calendaring.client.dto.GetFreeSlotsWithDestinationsRequest
import ru.yandex.market.logistics.calendaring.client.dto.LinkBookingsByExternalIdRequest
import ru.yandex.market.logistics.calendaring.client.dto.UpdateExpiresAtRequest
import ru.yandex.market.logistics.calendaring.client.dto.UpdateExternalIdRequest
import ru.yandex.market.logistics.calendaring.client.dto.UpdateSlotRequest
import ru.yandex.market.logistics.calendaring.client.dto.WarehouseFreeSlotsResponse
import ru.yandex.market.logistics.calendaring.client.dto.WarehouseNotFreeSlotsResponse
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingStatus
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingType
import ru.yandex.market.logistics.calendaring.client.dto.enums.SupplierType
import ru.yandex.market.logistics.calendaring.client.dto.exceptions.BookSlotException
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime


@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [CalendaringServiceClientTestConfig::class])
class CalendaringServiceClientRequestsTest(
    @Value("\${calendaring-service.api.host}")
    private val host: String,
    @Autowired
    private val calendaringServiceClient: CalendaringServiceClientApi,
    @Autowired
    private val mockRestServiceServer: MockRestServiceServer
) {

    @Test
    fun postBookingSuccessfully() {

        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                IOUtils.toString(
                    ClassLoader.getSystemResourceAsStream("fixtures/book-slot-response.json"),
                    StandardCharsets.UTF_8
                )
            )

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking"))
            .andRespond(response)

        val bookSlotRequest = BookSlotRequest(
            SupplierType.THIRD_PARTY,
            "101",
            1L,
            null,
            BookingType.SUPPLY,
            LocalDateTime.of(2021, 5, 17, 10, 0, 0),
            LocalDateTime.of(2021, 5, 17, 11, 0, 0),
            "id",
            "source",
            null,
            100,
            1
        )
        calendaringServiceClient.bookSlot(bookSlotRequest)
    }

    @Test
    fun postBookingBatchSuccessfully() {

        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                IOUtils.toString(
                    ClassLoader.getSystemResourceAsStream("fixtures/book-slots-response.json"),
                    StandardCharsets.UTF_8
                )
            )

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking/batch"))
            .andRespond(response)

        val bookSlotRequest1 = BookSlotRequest(
            SupplierType.THIRD_PARTY,
            "101",
            1L,
            null,
            BookingType.SUPPLY,
            LocalDateTime.of(2021, 5, 17, 10, 0, 0),
            LocalDateTime.of(2021, 5, 17, 11, 0, 0),
            "id",
            "source",
            null,
            100,
            1
        )
        val bookSlotRequest2 = BookSlotRequest(
            SupplierType.THIRD_PARTY,
            "101",
            1L,
            null,
            BookingType.SUPPLY,
            LocalDateTime.of(2021, 5, 17, 11, 0, 0),
            LocalDateTime.of(2021, 5, 17, 12, 0, 0),
            "id",
            "source",
            null,
            100,
            1
        )
        calendaringServiceClient.bookSlots(listOf(bookSlotRequest1, bookSlotRequest2))
    }


    @Test
    fun postConsolidatedBookingsSuccessfully() {

        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                IOUtils.toString(
                    ClassLoader.getSystemResourceAsStream("fixtures/book-consolidated-slot-response.json"),
                    StandardCharsets.UTF_8
                )
            )

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking/consolidated"))
            .andRespond(response)

        val bookSlotRequest = BookConsolidatedSlotRequest(
            listOf(
                BookSlotRequest(
                    SupplierType.THIRD_PARTY,
                    "101",
                    1L,
                    null,
                    BookingType.SUPPLY,
                    LocalDateTime.of(2021, 5, 17, 10, 0, 0),
                    LocalDateTime.of(2021, 5, 17, 11, 0, 0),
                    "id",
                    "source",
                    null,
                    100,
                    1
                )
            )
        )
        calendaringServiceClient.bookConsolidatedSlot(bookSlotRequest)
    }

    @Test
    fun postBookingAlreadyBookedTest() {

        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.CONFLICT)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                IOUtils.toString(
                    ClassLoader.getSystemResourceAsStream("fixtures/already-booked-slot-response.json"),
                    StandardCharsets.UTF_8
                )
            )

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking"))
            .andRespond(response)

        val bookSlotRequest = BookSlotRequest(
            SupplierType.THIRD_PARTY,
            "101",
            1L,
            null,
            BookingType.SUPPLY,
            LocalDateTime.of(2021, 5, 17, 10, 0, 0),
            LocalDateTime.of(2021, 5, 17, 11, 0, 0, 0),
            "id",
            "source",
            null,
            100,
            1
        )
        Assertions.assertThrows(BookSlotException::class.java) {
            calendaringServiceClient.bookSlot(
                bookSlotRequest
            )
        }
    }

    @Test
    fun putBookingSuccessfully() {

        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                IOUtils.toString(
                    ClassLoader.getSystemResourceAsStream("fixtures/update-booking-response.json"),
                    StandardCharsets.UTF_8
                )
            )

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking"))
            .andRespond(response)

        val updateSlotRequest = UpdateSlotRequest(
            bookingId = 1L,
            from = LocalDateTime.of(2021, 5, 17, 10, 0, 0),
            to = LocalDateTime.of(2021, 5, 17, 11, 0, 0),
            takenItems = 100,
            takenPallets = 1
        )
        calendaringServiceClient.updateSlot(updateSlotRequest)

    }

    @Test
    fun decreaseSlotSuccessfully() {

        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                IOUtils.toString(
                    ClassLoader.getSystemResourceAsStream("fixtures/update-booking-response.json"),
                    StandardCharsets.UTF_8
                )
            )

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking/decrease-slot"))
            .andRespond(response)

        val decreaseSlotRequest = DecreaseSlotRequest(
            bookingId = 1L,
            from = LocalDateTime.of(2021, 5, 17, 10, 0, 0),
            to = LocalDateTime.of(2021, 5, 17, 11, 0, 0),
            takenItems = 100,
            takenPallets = 1
        )
        calendaringServiceClient.decreaseSlot(decreaseSlotRequest)

    }

    @Test
    fun decreaseConsolidatedSlotSuccessfully() {

        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                IOUtils.toString(
                    ClassLoader.getSystemResourceAsStream("fixtures/update-consolidated-booking-response.json"),
                    StandardCharsets.UTF_8
                )
            )

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking/decrease-consolidated-slot"))
            .andRespond(response)

        val decreaseSlotRequest = DecreaseConsolidatedSlotRequest(
            bookings = listOf(BookingIdAndSizeDTO(1L, 0L, 0L)),
            from = LocalDateTime.of(2021, 5, 17, 10, 0, 0),
            to = LocalDateTime.of(2021, 5, 17, 11, 0, 0),
        )
        calendaringServiceClient.decreaseConsolidatedSlot(decreaseSlotRequest)

    }

    @Test
    @Throws(Exception::class)
    fun getFreeSlotsReturnsSuccessfullyTest() {

        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                IOUtils.toString(
                    ClassLoader.getSystemResourceAsStream("fixtures/get-free-slots-response.json"),
                    StandardCharsets.UTF_8
                )
            )

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking/free-slots?takenItems=10&supplierId=%D0%9A101&bookingType=SUPPLY&ignoredBookings=1&from=2021-05-01T10:00&to=2021-05-01T11:00&takenPallets=1&supplierType=THIRD_PARTY&warehouseIds=1&quotaFrom=2021-01-01&slotDurationMinutes=60"))
            .andRespond(response)

        val getFreeSlotsRequest = GetFreeSlotsRequest(
            setOf(1L),
            null,
            BookingType.SUPPLY,
            60,
            null,
            LocalDateTime.of(2021, 5, 1, 10, 0, 0),
            LocalDateTime.of(2021, 5, 1, 11, 0, 0),
            SupplierType.THIRD_PARTY,
            "К101",
            10,
            1,
            LocalDate.of(2021, 1, 1),
            listOf(1L)
        )

        val freeSlotsResponse: List<WarehouseFreeSlotsResponse> =
            calendaringServiceClient.getFreeSlots(getFreeSlotsRequest).warehousesSlots
        assertThat(freeSlotsResponse[0].warehouseId).isEqualTo(1)
    }

    @Test
    @Throws(Exception::class)
    fun validateSlotsReturnsSuccessfullyTest() {

        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                IOUtils.toString(
                    ClassLoader.getSystemResourceAsStream("fixtures/get-free-slots-response.json"),
                    StandardCharsets.UTF_8
                )
            )

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking/slots-without-quota-check?takenItems=10&supplierId=%D0%9A101&bookingType=SUPPLY&ignoredBookings=1&from=2021-05-01T10:00&to=2021-05-01T11:00&takenPallets=1&supplierType=THIRD_PARTY&warehouseIds=1&quotaFrom=2021-01-01&slotDurationMinutes=60"))
            .andRespond(response)

        val getFreeSlotsRequest = GetFreeSlotsRequest(
            setOf(1L),
            null,
            BookingType.SUPPLY,
            60,
            null,
            LocalDateTime.of(2021, 5, 1, 10, 0, 0),
            LocalDateTime.of(2021, 5, 1, 11, 0, 0),
            SupplierType.THIRD_PARTY,
            "К101",
            10,
            1,
            LocalDate.of(2021, 1, 1),
            listOf(1L)
        )

        val freeSlotsResponse: List<WarehouseFreeSlotsResponse> =
            calendaringServiceClient.getSlotsWithoutQuotaCheck(getFreeSlotsRequest).warehousesSlots
        assertThat(freeSlotsResponse[0].warehouseId).isEqualTo(1)
    }

    @Test
    @Throws(Exception::class)
    fun getNotFreeSlotsReasonsReturnsSuccessfullyTest() {

        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                IOUtils.toString(
                    ClassLoader.getSystemResourceAsStream("fixtures/get-not-free-slots-response.json"),
                    StandardCharsets.UTF_8
                )
            )

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking/not-free-slots-reasons?takenItems=10&supplierId=%D0%9A101&bookingType=SUPPLY&ignoredBookings=1&from=2021-05-01T10:00&to=2021-05-01T11:00&takenPallets=1&supplierType=THIRD_PARTY&warehouseIds=1&quotaFrom=2021-01-01&slotDurationMinutes=60"))
            .andRespond(response)

        val getFreeSlotsRequest = GetFreeSlotsRequest(
            setOf(1L),
            null,
            BookingType.SUPPLY,
            60,
            null,
            LocalDateTime.of(2021, 5, 1, 10, 0, 0),
            LocalDateTime.of(2021, 5, 1, 11, 0, 0),
            SupplierType.THIRD_PARTY,
            "К101",
            10,
            1,
            LocalDate.of(2021, 1, 1),
            listOf(1L)
        )

        val freeSlotsResponse: List<WarehouseNotFreeSlotsResponse> =
            calendaringServiceClient.getNotFreeSlotsReasons(getFreeSlotsRequest).warehousesSlots
        assertThat(freeSlotsResponse[0].warehouseId).isEqualTo(1)
    }

    @Test
    fun getSlotReturnsSuccessfullyTest() {

        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                IOUtils.toString(
                    ClassLoader.getSystemResourceAsStream("fixtures/get-slot-response.json"),
                    StandardCharsets.UTF_8
                )
            )
        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking/1"))
            .andRespond(response)

        val slot = calendaringServiceClient.getSlot(1L)!!
        assertThat(slot.id).isEqualTo(1)
        assertThat(slot.from).isEqualTo(ZonedDateTime.of(2021, 5, 17, 10, 0, 0, 0, ZoneId.of("Europe/Moscow")))
        assertThat(slot.to).isEqualTo(ZonedDateTime.of(2021, 5, 17, 11, 0, 0, 0, ZoneId.of("Europe/Moscow")))
        assertThat(slot.gateId).isEqualTo(1)
        assertThat(slot.status).isEqualTo(BookingStatus.ACTIVE)
        assertThat(slot.createdAt).isEqualTo(LocalDateTime.of(2021, 5, 1, 10, 0, 0))
    }

    @Test
    fun getSlotNotFoundTest() {

        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.NOT_FOUND)

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking/1"))
            .andRespond(response)

        val slot = calendaringServiceClient.getSlot(1L)
        assertThat(slot).isNull()
    }

    @Test
    fun getLimitByBookingIdTest() {

        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                IOUtils.toString(
                    ClassLoader.getSystemResourceAsStream("fixtures/get-booking-limit.json"),
                    StandardCharsets.UTF_8
                )
            )
        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/limit/booking/1"))
            .andRespond(response)

        val slot = calendaringServiceClient.getLimitByBookingId(1L)!!
        assertThat(slot.quotaDate).isEqualTo(LocalDate.of(2021, 5, 1))

    }

    @Test
    fun getSlotWithoutTimeZoneReturnsSuccessfullyTest() {

        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                IOUtils.toString(
                    ClassLoader.getSystemResourceAsStream("fixtures/get-slot-response.json"),
                    StandardCharsets.UTF_8
                )
            )
        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking/1"))
            .andRespond(response)

        val slot = calendaringServiceClient.getSlot(1L)!!
        assertThat(slot.id).isEqualTo(1)
        assertThat(slot.from).isEqualTo(ZonedDateTime.of(2021, 5, 17, 10, 0, 0, 0, ZoneId.of("Europe/Moscow")))
        assertThat(slot.to).isEqualTo(ZonedDateTime.of(2021, 5, 17, 11, 0, 0, 0, ZoneId.of("Europe/Moscow")))
        assertThat(slot.gateId).isEqualTo(1)
        assertThat(slot.status).isEqualTo(BookingStatus.ACTIVE)
    }


    @Test
    fun getSlotWithoutTimeZoneNotFoundTest() {

        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.NOT_FOUND)

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking/1"))
            .andRespond(response)

        val slot = calendaringServiceClient.getSlot(1L)
        assertThat(slot).isNull()
    }

    @Test
    fun cancelSlotsTest() {
        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.OK)

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking?bookingIds=1,2,3,4,5,6"))
            .andRespond(response)

        calendaringServiceClient.cancelSlots(setOf(1, 2, 3, 4, 5, 6))
    }

    @Test
    fun cancelSingleSlotTest() {
        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.OK)

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking?bookingIds=1"))
            .andRespond(response)

        calendaringServiceClient.cancelSlot(1)
    }

    @Test
    fun getFreeSlotsWithDestinations() {

        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                IOUtils.toString(
                    ClassLoader.getSystemResourceAsStream("fixtures/get-free-slots-response.json"),
                    StandardCharsets.UTF_8
                )
            )

        val expectedRequestBody = IOUtils.toString(
            ClassLoader.getSystemResourceAsStream(
                "fixtures/get-slots-with-destinations-body.json"
            ),
            "UTF-8"
        )

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking/free-slots-with-destinations"))
            .andExpect(MockRestRequestMatchers.content().json(expectedRequestBody))
            .andRespond(response)

        calendaringServiceClient.getFreeSlotsWithDestinations(GetFreeSlotsWithDestinationsRequest(
            setOf(172L),
            null,
            mapOf(172L to setOf(DestinationWithTakenLimits(171, 10, 5),
                DestinationWithTakenLimits(147, 1000, 50)
            )),
            BookingType.SUPPLY,
            60,
            null,
            LocalDateTime.of(2021, 5, 17, 7, 0),
            LocalDateTime.of(2021, 5, 18, 22, 0),
            SupplierType.THIRD_PARTY,
            "101",
            1005,
            55,
            null,
            listOf(1),
            null
        ))

    }

    @Test
    fun getTimezoneByWarehouseIdTest() {
        val id: Long = 300
        val expected = ZoneId.of("Asia/Yekaterinburg")

        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"tzname\":\"$expected\"}")

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/get-zone/by-warehouse/$id"))
            .andRespond(response)
        assertThat(calendaringServiceClient.getTimezoneByWarehouseId(id)).isEqualTo(expected)
    }

    @Test
    fun findSlotById() {
        val response: ResponseCreator = MockRestResponseCreators
            .withStatus(HttpStatus.OK)
            .body("{\"bookedSlotInfo\":[]}")
            .contentType(MediaType.APPLICATION_JSON)

        val requestId = "1"

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking/booked-slot?id=$requestId"))
            .andRespond(response)

        calendaringServiceClient.findSlotById(requestId)
    }

    @Test
    fun updateExternalId() {

        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking/external-id"))
            .andRespond(response)

        val updateExternalIdRequest = UpdateExternalIdRequest(
            bookingId = 1,
            oldExternalId = "old-1",
            newExternalId = "new-1",
            source = "TESTФ"
        )
        calendaringServiceClient.updateExternalId(updateExternalIdRequest)
    }

    @Test
    fun updateExpiresAt() {

        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
        val expectedRequestBody = IOUtils.toString(
            ClassLoader.getSystemResourceAsStream(
                "fixtures/change-expires-at.json"
            ),
            "UTF-8"
        );

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking/expires-at"))
            .andExpect(MockRestRequestMatchers
                .content()
                .json(expectedRequestBody)
            )
            .andRespond(response)

        val updateExternalIdRequest = UpdateExpiresAtRequest(
            bookingId = 1,
            expiresAt = ZonedDateTime.of(2021, 4, 29, 11, 0, 0, 0, ZoneId.of("UTC"))
        )
        calendaringServiceClient.updateExpiresAt(updateExternalIdRequest)
    }

    @Test
    fun dropExpiresAt() {
        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
        val expectedRequestBody = IOUtils.toString(
            ClassLoader.getSystemResourceAsStream(
                "fixtures/drop-expires-at.json"
            ),
            "UTF-8"
        );

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking/expires-at"))
            .andExpect(MockRestRequestMatchers
                .content()
                .json(expectedRequestBody)
            )
            .andRespond(response)

        val updateExternalIdRequest = UpdateExpiresAtRequest(
            bookingId = 1,
            expiresAt = null
        )
        calendaringServiceClient.updateExpiresAt(updateExternalIdRequest)
    }

    @Test
    fun getSlotByExternalIdentifier() {

        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                IOUtils.toString(
                    ClassLoader.getSystemResourceAsStream("fixtures/get-slot-list-response.json"),
                    StandardCharsets.UTF_8
                )
            )

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking/find-by-external-ids"))
            .andRespond(response)

        calendaringServiceClient.getSlotByExternalIdentifiers(setOf("ID1"), "TEST", null)

    }

    @Test
    fun getSlotByExternalIdentifierV2() {

        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                IOUtils.toString(
                    ClassLoader.getSystemResourceAsStream("fixtures/get-slot-list-response.json"),
                    StandardCharsets.UTF_8
                )
            )

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking/find-by-external-ids"))
            .andRespond(response)

        val bookingListResponse: BookingListResponseV2? =
            calendaringServiceClient.getSlotByExternalIdentifiersV2(setOf("ID1"), "TEST", null)

        assertThat(bookingListResponse!!.bookings[0].from).isEqualTo(
            ZonedDateTime.of(
                2021, 5, 17, 10, 0, 0, 0, ZoneId.of("+0300")
            )
        )

        assertThat(bookingListResponse!!.bookings[0].to).isEqualTo(
            ZonedDateTime.of(
                2021, 5, 17, 11, 0, 0, 0, ZoneId.of("+0300")
            )
        )
    }

    @Test
    fun getSlotByExternalIdentifierWithStatus() {

        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                IOUtils.toString(
                    ClassLoader.getSystemResourceAsStream("fixtures/get-slot-list-response.json"),
                    StandardCharsets.UTF_8
                )
            )

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking/find-by-external-ids"))
            .andRespond(response)

        calendaringServiceClient.getSlotByExternalIdentifiers(setOf("ID1"), "TEST", BookingStatus.ACTIVE)
    }

    @Test
    fun getBookingsByIds() {

        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                IOUtils.toString(
                    ClassLoader.getSystemResourceAsStream("fixtures/get-slot-list-response.json"),
                    StandardCharsets.UTF_8
                )
            )

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking/find-by-ids"))
            .andRespond(response)

        calendaringServiceClient.getBookingsByIds(setOf(1, 2, 3), BookingStatus.ACTIVE)
    }

    @Test
    fun getBookingsByIdsV2() {

        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                IOUtils.toString(
                    ClassLoader.getSystemResourceAsStream("fixtures/get-slot-list-response.json"),
                    StandardCharsets.UTF_8
                )
            )

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking/find-by-ids"))
            .andRespond(response)

        calendaringServiceClient.getBookingsByIdsV2(setOf(1, 2, 3), BookingStatus.ACTIVE)
    }

    @Test
    fun getAvailableLimitTest() {

        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                IOUtils.toString(
                    ClassLoader.getSystemResourceAsStream("fixtures/get-available-quota-response.json"),
                    StandardCharsets.UTF_8
                )
            )

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/limit/available/1/WITHDRAW/FIRST_PARTY/2021-01-01,2021-01-02"))
            .andRespond(response)

        val request = GetAvailableLimitRequest(
            warehouseId = 1,
            bookingType = BookingType.WITHDRAW,
            supplierType = SupplierType.FIRST_PARTY,
            dates = setOf(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 2))
        )

        calendaringServiceClient.getAvailableLimit(request)
    }

    @Test
    fun linkBookingsByExternalIdTest() {

        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                IOUtils.toString(
                    ClassLoader.getSystemResourceAsStream("fixtures/link-bookings-by-external-Id-response.json"),
                    StandardCharsets.UTF_8
                )
            )

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking/1/link-external-ids"))
            .andRespond(response)

        val request = LinkBookingsByExternalIdRequest(
            source = "TEST",
            externalIds = listOf("id1", "id2")
        )

        calendaringServiceClient.linkBookingsByExternalId(1, request)
    }

    @Test
    fun getBookedSlots() {

        val response: ResponseCreator = MockRestResponseCreators.withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                IOUtils.toString(
                    ClassLoader.getSystemResourceAsStream("fixtures/get-booked-slot-response.json"),
                    StandardCharsets.UTF_8
                )
            )

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking/get-booked-slots/2/2021-11-29T10:00/2021-11-29T18:00?warehouseIds=1"))
            .andRespond(response)

        calendaringServiceClient.getBookedSlots(
            setOf(1L), "2",
            LocalDateTime.of(2021, 11, 29, 10, 0),
            LocalDateTime.of(2021, 11, 29, 18, 0)
        )
    }

}
