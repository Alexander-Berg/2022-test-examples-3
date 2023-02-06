package ru.yandex.market.logistics.calendaring.client

import org.apache.commons.io.IOUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import ru.yandex.market.logistics.calendaring.client.config.CalendaringServiceClientTestConfig
import ru.yandex.market.logistics.calendaring.client.dto.BookedSlot
import ru.yandex.market.logistics.calendaring.client.dto.PutBookedSlotRequest
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingStatus
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingType
import ru.yandex.market.logistics.calendaring.client.dto.enums.SupplierType
import java.nio.charset.StandardCharsets
import java.time.ZoneId
import java.time.ZonedDateTime


@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [CalendaringServiceClientTestConfig::class])
class CalendaringServiceClientExternallyTest(
    @Value("\${calendaring-service.api.host}")
    private val host: String,
    @Autowired
    private val calendaringServiceClient: CalendaringServiceClientApi,
    @Autowired
    private val mockRestServiceServer: MockRestServiceServer
) {

    @Test
    fun putBookingSuccessfully() {

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking/externally"))
            .andExpect(content().json(IOUtils.toString(
                ClassLoader.getSystemResourceAsStream("fixtures/put-booking-externally-request.json"),
                StandardCharsets.UTF_8
            )))
            .andRespond(withStatus(OK))


        val bookedSlot = PutBookedSlotRequest(
            BookedSlot(
                SupplierType.FIRST_PARTY,
                123,
                1,
                BookingType.SUPPLY,
                ZonedDateTime.of(2021, 5, 17, 10, 0, 0, 0, ZoneId.of("Asia/Yekaterinburg")),
                ZonedDateTime.of(2021, 5, 17, 11, 0, 0, 0, ZoneId.of("Asia/Yekaterinburg")),
                "OOO ABC"
            ),
            "id123",
            "TEST",
            BookingStatus.ACTIVE
        )
        calendaringServiceClient.putExternallyBookedSlot(bookedSlot)
    }

    @Test
    fun putBookingWithoutSupplierIdSuccessfully() {

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/booking/externally"))
            .andExpect(content().json(IOUtils.toString(
                ClassLoader.getSystemResourceAsStream("fixtures/put-booking-externally-request.json"),
                StandardCharsets.UTF_8
            )))
            .andRespond(withStatus(OK))


        val bookedSlot = PutBookedSlotRequest(
            BookedSlot(
                SupplierType.FIRST_PARTY,
                123,
                1,
                BookingType.SUPPLY,
                ZonedDateTime.of(2021, 5, 17, 10, 0, 0, 0, ZoneId.of("Asia/Yekaterinburg")),
                ZonedDateTime.of(2021, 5, 17, 11, 0, 0, 0, ZoneId.of("Asia/Yekaterinburg")),
            ),
            "id123",
            "TEST",
            BookingStatus.ACTIVE
        )
        calendaringServiceClient.putExternallyBookedSlot(bookedSlot)
    }
}
