package ru.yandex.market.logistics.calendaring.service.booking

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.client.dto.BookSlotRequest
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingType
import ru.yandex.market.logistics.calendaring.client.dto.enums.SupplierType
import ru.yandex.market.logistics.calendaring.service.datetime.geobase.GeobaseProviderApi
import ru.yandex.market.logistics.management.entity.type.GateTypeResponse
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount
import java.time.LocalDate
import java.time.LocalDateTime

class BookSlotServiceTest(
    @Autowired private val bookSlotsService: BookSlotService,
    @Autowired var geobaseProviderApi: GeobaseProviderApi,
) : AbstractContextualTest() {

    @AfterEach
    fun verifyMocks() {
        verifyNoMoreInteractions(ffwfClientApi!!)
    }

    @Test
    @JpaQueriesCount(17)
    fun happyPath() {

        setUpMockFfwfGetQuota(setOf(LocalDate.of(2021, 5, 15)))
        setUpMockFfwfTakeQuota(LocalDate.of(2021, 5, 15))
        setUpMockLmsGetSchedule(GateTypeResponse.OUTBOUND)
        setUpMockLmsGetLocationZone(geobaseProviderApi)

        bookSlotsService.bookSlot(BookSlotRequest(
            SupplierType.FIRST_PARTY,
            "101",
            1,
            null,
            BookingType.MOVEMENT_WITHDRAW,
            LocalDateTime.of(2021, 5, 17, 10, 0, 0),
            LocalDateTime.of(2021, 5, 17, 11, 0, 0),
            "id123",
            "TEST",
            null,
            10,
            2,
            null
        ))
        val takeQuotaCaptor = verifyBasicFfwfCommunication()
        assertions().assertThat(takeQuotaCaptor.lastValue.possibleDates[0]).isEqualTo(LocalDate.of(2021, 5, 15))
    }

    @Test
    @JpaQueriesCount(17)
    fun happyPathZeroPalletsZeroItems() {

        setUpMockFfwfGetQuota(setOf(LocalDate.of(2021, 5, 15)))
        setUpMockFfwfTakeQuota(LocalDate.of(2021, 5, 15))
        setUpMockLmsGetSchedule(GateTypeResponse.OUTBOUND)
        setUpMockLmsGetLocationZone(geobaseProviderApi)

        bookSlotsService.bookSlot(BookSlotRequest(
            SupplierType.FIRST_PARTY,
            "101",
            1,
            null,
            BookingType.MOVEMENT_WITHDRAW,
            LocalDateTime.of(2021, 5, 17, 10, 0, 0),
            LocalDateTime.of(2021, 5, 17, 11, 0, 0),
            "id123",
            "TEST",
            null,
            0,
            0,
            null
        ))
        val takeQuotaCaptor = verifyBasicFfwfCommunication()
        val quotaDto = takeQuotaCaptor.lastValue
        assertions().assertThat(quotaDto.possibleDates[0]).isEqualTo(LocalDate.of(2021, 5, 15))
        assertions().assertThat(quotaDto.items).isEqualTo(0)
        assertions().assertThat(quotaDto.pallets).isEqualTo(0)
    }

    @Test
    @JpaQueriesCount(17)
    @DatabaseSetup("classpath:fixtures/service/book-slot/connected-inbound/before.xml")
    @ExpectedDatabase("classpath:fixtures/service/book-slot/connected-inbound/after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun happyPathWithConnectedInbound() {

        setUpMockFfwfGetQuota(setOf(LocalDate.of(2021, 5, 15)))
        setUpMockFfwfTakeQuota(LocalDate.of(2021, 5, 15))
        setUpMockLmsGetSchedule(GateTypeResponse.OUTBOUND)
        setUpMockLmsGetLocationZone(geobaseProviderApi)

        bookSlotsService.bookSlot(BookSlotRequest(
            SupplierType.FIRST_PARTY,
            "101",
            1,
            null,
            BookingType.MOVEMENT_WITHDRAW,
            LocalDateTime.of(2021, 5, 17, 10, 0, 0),
            LocalDateTime.of(2021, 5, 17, 11, 0, 0),
            "id123",
            "TEST",
            null,
            10,
            2,
            null
        ))
        val takeQuotaCaptor = verifyBasicFfwfCommunication()
        assertions().assertThat(takeQuotaCaptor.lastValue.possibleDates[0]).isEqualTo(LocalDate.of(2021, 5, 15))
    }

    @Test
    @JpaQueriesCount(15)
    @DatabaseSetup("classpath:fixtures/service/book-slot/connected-outbound/before.xml")
    @ExpectedDatabase("classpath:fixtures/service/book-slot/connected-outbound/after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun happyPathWithConnectedOutbound() {

        setUpMockFfwfGetQuota(setOf(LocalDate.of(2021, 5, 17)))
        setUpMockFfwfTakeQuota(LocalDate.of(2021, 5, 17))
        setUpMockLmsGetSchedule(GateTypeResponse.INBOUND)
        setUpMockLmsGetLocationZone(geobaseProviderApi)

        bookSlotsService.bookSlot(BookSlotRequest(
            SupplierType.FIRST_PARTY,
            "101",
            1,
            null,
            BookingType.MOVEMENT_SUPPLY,
            LocalDateTime.of(2021, 5, 17, 10, 0, 0),
            LocalDateTime.of(2021, 5, 17, 11, 0, 0),
            "id123",
            "TEST",
            null,
            10,
            2,
            null
        ))
        val takeQuotaCaptor = verifyBasicFfwfCommunication()
        assertions().assertThat(takeQuotaCaptor.lastValue.possibleDates[0]).isEqualTo(LocalDate.of(2021, 5, 17))
    }

    @Test
    @JpaQueriesCount(15)
    @ExpectedDatabase("classpath:fixtures/service/book-slot/destination-warehouse-id/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun happyPathWithDestinationWarehouseId() {

        setUpMockFfwfGetQuota(setOf(LocalDate.of(2021, 5, 17)))
        setUpMockFfwfTakeQuota(LocalDate.of(2021, 5, 17))
        setUpMockLmsGetSchedule(GateTypeResponse.INBOUND)
        setUpMockLmsGetLocationZone(geobaseProviderApi)

        bookSlotsService.bookSlot(BookSlotRequest(
            SupplierType.FIRST_PARTY,
            "101",
            1,
            172,
            BookingType.XDOCK_TRANSPORT_SUPPLY,
            LocalDateTime.of(2021, 5, 17, 10, 0, 0),
            LocalDateTime.of(2021, 5, 17, 11, 0, 0),
            "id123",
            "TEST",
            null,
            10,
            2,
            null
        ))
        val takeQuotaCaptor = verifyBasicFfwfCommunication()
        assertions().assertThat(takeQuotaCaptor.lastValue.possibleDates[0]).isEqualTo(LocalDate.of(2021, 5, 17))
    }
}
