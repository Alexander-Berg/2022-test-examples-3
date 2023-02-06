package ru.yandex.market.logistics.calendaring.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.annotation.ExpectedDatabases
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.apache.http.entity.ContentType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.UnknownHttpStatusCodeException
import ru.yandex.market.ff.client.dto.quota.TakeQuotaResponseDto
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.service.datetime.geobase.GeobaseProviderApi
import ru.yandex.market.logistics.calendaring.util.getFileContent
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointGateResponse
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointGatesScheduleResponse
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDateTimeResponse
import ru.yandex.market.logistics.management.entity.type.GateTypeResponse
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

class BookingControllerTest(
    @Autowired var geobaseProviderApi: GeobaseProviderApi,
) : AbstractContextualTest() {

    @BeforeEach
    fun init() {
        setUpMockLmsGetSchedule(GateTypeResponse.INBOUND)

        setUpMockLmsGetLocationZone(geobaseProviderApi)
        setUpMockFfwfGetQuota()
        setUpMockFfwfTakeQuota()
    }

    @AfterEach
    fun verifyMocks() {
        verifyNoMoreInteractions(ffwfClientApi!!)
    }

    @Test
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "classpath:fixtures/controller/booking/book-slot/create-successfully/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT,
        ),
        ExpectedDatabase(
            value = "classpath:fixtures/controller/booking/book-slot/dbqueue.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT,
            connection = "dbqueueDatabaseConnection"
        )
    )
    fun bookingCreatedSuccessfullyTest() {

        val requestJson: String =
            getFileContent("fixtures/controller/booking/book-slot/create-successfully/request.json")

        setUpMockFfwfTakeQuotaReturnAfterFourRetryableExceptions()
        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andReturn()

        val expectedJson: String =
            getFileContent("fixtures/controller/booking/book-slot/create-successfully/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

        verifyBasicFfwfCommunication(takeQuotaInvCnt = 5)
    }

    @Test
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "classpath:fixtures/controller/booking/book-slot/not-take-quota-type/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT
        ),
        ExpectedDatabase(
            value = "classpath:fixtures/controller/booking/book-slot/dbqueue.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT,
            connection = "dbqueueDatabaseConnection"
        ),
    )
    fun bookingCreatedButQuotaNotTakenForNotTakeQuotaTypeTest() {

        val requestJson: String =
            getFileContent("fixtures/controller/booking/book-slot/not-take-quota-type/request.json")

        setUpMockFfwfTakeQuotaReturnAfterFourRetryableExceptions()
        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andReturn()

        val expectedJson: String =
            getFileContent("fixtures/controller/booking/book-slot/not-take-quota-type/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

        verify(ffwfClientApi!!, times(0)).getQuota(any())
    }

    @Test
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/book-slot/create-conflict-due-quota-exhausted/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun bookingFailDueToQuotaExhaustedWhileGetQuota() {

        val requestJson: String = getFileContent(
            "fixtures/controller/booking/book-slot/create-conflict-due-quota-exhausted/request.json")

        setUpMockFfwfGetQuota(items = 0, pallets = 0)
        mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isConflict)

        verifyBasicFfwfCommunication(1, 0)
    }

    @Test
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/book-slot/create-conflict-due-take-quota-conflict/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun bookingFailDueToQuotaExhaustedWhileTakeQuota() {

        val requestJson: String = getFileContent(
            "fixtures/controller/booking/book-slot/create-conflict-due-take-quota-conflict/request.json")

        setUpMockFfwfTakeQuotaHttpStatusCodeException(409)
        mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isConflict)

        verifyBasicFfwfCommunication()
    }

    @Test
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/book-slot/create-fail-due-take-quota-failure/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun bookingFailDueNotRetryableFfwfApiCallErrorTest() {

        val requestJson: String = getFileContent(
            "fixtures/controller/booking/book-slot/create-fail-due-take-quota-failure/request.json")

        setUpMockFfwfTakeQuotaReturnAfterHttpStatusCodeException(400)
        mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isBadRequest)

        verifyBasicFfwfCommunication()
    }

    @Test
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/book-slot/create-fail-due-take-quota-failure/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun bookingFailDueNotRetryableFfwfApiCallErrorWithUnknownHttpCodeTest() {

        val requestJson: String = getFileContent(
            "fixtures/controller/booking/book-slot/create-fail-due-take-quota-failure/request.json")

        setUpMockFfwfTakeQuotaReturnAfterHttpStatusCodeException(600)
        mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isInternalServerError)

        verifyBasicFfwfCommunication()
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/book-slot/second-booking-for-ext-id/dataset.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/book-slot/second-booking-for-ext-id/dataset.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun whenExistsExternalIdThenExistingBookingTest() {

        val requestJson: String =
            getFileContent("fixtures/controller/booking/book-slot/second-booking-for-ext-id/request.json")

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andReturn()

        val expectedJson: String =
            getFileContent("fixtures/controller/booking/book-slot/second-booking-for-ext-id/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/book-slot/some-slot-booked/before.xml"])
    @ExpectedDatabases(
        ExpectedDatabase(
            "classpath:fixtures/controller/booking/book-slot/some-slot-booked/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT
        ),
        ExpectedDatabase(
            value = "classpath:fixtures/controller/booking/book-slot/some-slot-booked/dbqueue.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT,
            connection = "dbqueueDatabaseConnection"
        )
    )
    fun whenSomeSlotBookedThenBookingCreatedTest() {

        val requestJson: String =
            getFileContent("fixtures/controller/booking/book-slot/some-slot-booked/request.json")

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isOk)
            .andReturn()

        val expectedJson: String =
            getFileContent("fixtures/controller/booking/book-slot/some-slot-booked/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
        verifyBasicFfwfCommunication()
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/book-slot/slot-booked/before.xml"])
    fun whenSlotBookedThenBookingNotCreatedTest() {

        val requestJson: String =
            getFileContent("fixtures/controller/booking/book-slot/slot-booked/request.json")

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isConflict)
            .andReturn()

        val expectedJson: String =
            getFileContent("fixtures/controller/booking/book-slot/slot-booked/response.json")
        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

        verifyBasicFfwfCommunication(1, 0)
    }

    @Test
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "classpath:fixtures/controller/booking/book-slot/save-meta-info/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT,
        ),
        ExpectedDatabase(
            value = "classpath:fixtures/controller/booking/book-slot/dbqueue.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT,
            connection = "dbqueueDatabaseConnection"
        )
    )
    fun bookingMetaInfoSavedSuccessfullyTest() {

        val requestJson: String =
            getFileContent("fixtures/controller/booking/book-slot/save-meta-info/request.json")

        mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isOk)
            .andReturn()

        verifyBasicFfwfCommunication()
    }

    @Test
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "classpath:fixtures/controller/booking/book-slot/round-up/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT,
        ),
        ExpectedDatabase(
            value = "classpath:fixtures/controller/booking/book-slot/dbqueue.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT,
            connection = "dbqueueDatabaseConnection"
        )
    )
    fun bookingCreatedWithRoundUpSuccessfullyTest() {

        val requestJson: String =
            getFileContent("fixtures/controller/booking/book-slot/round-up/request.json")

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andReturn()

        val expectedJson: String =
            getFileContent("fixtures/controller/booking/book-slot/round-up/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

        verifyBasicFfwfCommunication()
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/cancel-booking/cancel-successfully/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/cancel-booking/cancel-successfully/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun bookingCancelledSuccessfullyTest() {
        mockMvc!!.perform(
            MockMvcRequestBuilders.delete("/booking")
                .param("bookingIds", "1", "2")
        ).andExpect(status().isOk)
        val captor = argumentCaptor<List<Long>>()
        verify(ffwfClientApi!!, times(1)).deactivateBooking(captor.capture())
        assertions().assertThat(captor.lastValue).containsExactlyInAnyOrder(1, 2)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/cancel-booking/already-cancelled/dataset.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/cancel-booking/already-cancelled/dataset.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun whenBookingCancelledThenDoNothing() {
        mockMvc!!.perform(
            MockMvcRequestBuilders.delete("/booking")
                .param("bookingIds", "1", "3")
        ).andExpect(status().isOk)
        val captor = argumentCaptor<List<Long>>()
        verify(ffwfClientApi!!, times(1)).deactivateBooking(captor.capture())
        assertions().assertThat(captor.lastValue).containsExactlyInAnyOrder(1, 3)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/link-external-ids/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/link-external-ids/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun linkBookingsByExternalId() {

        val requestJson: String =
            getFileContent("fixtures/controller/booking/link-external-ids/request.json")

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/1/link-external-ids")
                .content(requestJson)
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        ).andExpect(status().isOk)
            .andDo(MockMvcResultHandlers.print())
            .andReturn()

        val expectedJson: String =
            getFileContent("fixtures/controller/booking/link-external-ids/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/get-booked-slots/before.xml"])
    fun getBookedSlotsSuccessfullyTest() {

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.get("/booking/get-booked-slots/101/2021-05-17T10:30:00/2021-05-17T18:00:00")
                .param("warehouseIds", "123")
        ).andExpect(status().isOk)
            .andDo(MockMvcResultHandlers.print())
            .andReturn()

        val expectedJson: String =
            getFileContent("fixtures/controller/booking/get-booked-slots/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

    }

    private fun setUpMockFfwfTakeQuotaReturnAfterFourRetryableExceptions() {
        Mockito.`when`(ffwfClientApi!!.takeQuota(any()))
            .thenThrow(UnknownHttpStatusCodeException(499, "nginx", null, null, null))
            .thenThrow(HttpServerErrorException(HttpStatus.BAD_GATEWAY))
            .thenThrow(HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE))
            .thenThrow(HttpServerErrorException(HttpStatus.GATEWAY_TIMEOUT))
            .thenReturn(TakeQuotaResponseDto(LocalDate.of(2021, 5, 17)))
    }


    private fun getGateScheduleLmsResponse(): LogisticsPointGatesScheduleResponse? {
        return LogisticsPointGatesScheduleResponse.newBuilder()
            .schedule(listOf(getScheduleDateTimeResponse()))
            .gates(
                HashSet(
                    listOf(
                        getInboundGateResponse(1, "1"),
                        getInboundGateResponse(2, "1a"),
                        getInboundGateResponse(3, "2"),
                        getOutboundGateResponse(4, "4out"),
                        getOutboundGateResponse(5, "5out")
                    )
                )
            )
            .logisticsPointId(1L)
            .build()
    }

    private fun getScheduleDateTimeResponse(): ScheduleDateTimeResponse? {
        return ScheduleDateTimeResponse.newBuilder()
            .date(DATE)
            .from(WORKING_FROM)
            .to(WORKING_TO)
            .build()
    }


    private fun getInboundGateResponse(id: Long, gateNumber: String): LogisticsPointGateResponse? {
        return LogisticsPointGateResponse.newBuilder()
            .id(id)
            .gateNumber(gateNumber)
            .types(EnumSet.of(GateTypeResponse.INBOUND))
            .enabled(true)
            .build()
    }

    private fun getOutboundGateResponse(id: Long, gateNumber: String): LogisticsPointGateResponse? {
        return LogisticsPointGateResponse.newBuilder()
            .id(id)
            .gateNumber(gateNumber)
            .types(EnumSet.of(GateTypeResponse.OUTBOUND))
            .enabled(true)
            .build()
    }

    companion object {
        private val DATE = LocalDate.of(2021, 5, 17)
        private val WORKING_FROM = LocalTime.of(9, 0)
        private val WORKING_TO = LocalTime.of(20, 0)
    }
}
