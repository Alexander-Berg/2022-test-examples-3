package ru.yandex.market.logistics.calendaring.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.annotation.ExpectedDatabases
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.HttpServerErrorException
import ru.yandex.market.ff.client.dto.quota.AvailableQuotasForDateDto
import ru.yandex.market.ff.client.dto.quota.AvailableQuotasForServiceDto
import ru.yandex.market.ff.client.dto.quota.GetQuotaResponseDto
import ru.yandex.market.ff.client.dto.quota.TakeQuotaResponseDto
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.service.datetime.geobase.GeobaseProviderApi
import ru.yandex.market.logistics.calendaring.util.FileContentUtils
import ru.yandex.market.logistics.calendaring.util.MockParametersHelper
import ru.yandex.market.logistics.calendaring.util.getFileContent
import ru.yandex.market.logistics.management.entity.type.GateTypeResponse
import ru.yandex.market.logistics.management.entity.type.PartnerType
import java.time.LocalDate
import java.time.LocalTime


class BookingControllerBookSlotTest(
    @Autowired var geobaseProviderApi: GeobaseProviderApi,
) : AbstractContextualTest() {

    @BeforeEach
    fun init() {setUpMockLmsGetLocationZone(geobaseProviderApi)}

    @AfterEach
    fun verifyMocks() {
        verifyNoMoreInteractions(ffwfClientApi!!)
    }

    @Test
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/book-slot/not-in-working-hours/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun badRequestWhenNotInWorkingHoursTest() {

        setupLmsGateSchedule(
            from = LocalTime.of(10, 0),
            to = LocalTime.of(11, 0),
        )

        val requestJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/book-slot/not-in-working-hours/request.json")

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isBadRequest)
            .andReturn()

        val expectedJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/book-slot/not-in-working-hours/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }


    @Test
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/book-slot/no-available-gates/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun badRequestWhenNoActiveGatesTest() {

        setupLmsNotActiveGateSchedule()

        val requestJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/book-slot/no-available-gates/request.json")

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isBadRequest)
            .andReturn()

        val expectedJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/book-slot/no-available-gates/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }

    @Test
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/book-slot/warehouse-not-found/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun badRequestWhenWarehouseNotFoundTest() {

        setupLmsNotActiveGateSchedule()

        val requestJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/book-slot/warehouse-not-found/request.json")

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isBadRequest)
            .andReturn()

        val expectedJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/book-slot/warehouse-not-found/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }

    @Test
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/book-slot/no-possible-quota/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun badRequestWhenNoPossibleQuota() {

        setupLmsNotActiveGateSchedule()

        val requestJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/book-slot/no-possible-quota/request.json")

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isBadRequest)
            .andReturn()

        val expectedJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/book-slot/no-possible-quota/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/booking/slot-duration-settings/slot-duration-settings.xml")
    fun errorIfTryingToBookSlotWithIncorrectDuration() {

        val requestJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/book-slot/too-large-slot/request.json")

        mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isBadRequest)
            .andExpect(content()
                .string("{\"message\":" +
                    "\"Required slot duration 60 minutes is greater than max allowed slot size 30 minutes\"}"))
    }

    @Test
    @DatabaseSetup(
        value = ["classpath:fixtures/controller/booking/slot-duration-settings/slot-duration-settings.xml",
        "classpath:fixtures/controller/booking/book-slot/one-slot-one-supplier-inbound/before.xml"]
    )
    fun tryingToBookSlotWithIncorrectDurationWithSkipValidatedSlotDuration() {

        val requestJson: String = FileContentUtils
            .getFileContent("fixtures/controller/booking/book-slot/skip-validate-slot-duration/request.json")

        mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/book-slot/one-slot-one-supplier-outbound/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/book-slot/one-slot-one-supplier-outbound/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun bookingCreatedSuccessfullyOnOneSlotForOneSupplierOutboundTest() {

        val requestJson: String =
            getFileContent("fixtures/controller/booking/book-slot/one-slot-one-supplier-outbound/request.json")

        setupLmsGateSchedule(
            warehouseIds = listOf(123L),
            gates = setOf(1L),
            gateType = GateTypeResponse.OUTBOUND,
        )

        setUpMockFfwfGetQuota(setOf(LocalDate.of(2021, 5, 15)))
        setUpMockFfwfTakeQuota(LocalDate.of(2021, 5, 15))

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andReturn()

        val expectedJson: String =
            getFileContent("fixtures/controller/booking/book-slot/one-slot-one-supplier-outbound/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
        verifyBasicFfwfCommunication()
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/book-slot/one-slot-one-supplier-inbound/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/book-slot/one-slot-one-supplier-inbound/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun bookingNotCreatedOnOneSlotForOneSupplierInboundTest() {

        val requestJson: String =
            getFileContent("fixtures/controller/booking/book-slot/one-slot-one-supplier-inbound/request.json")

        setupLmsGateSchedule(
            warehouseIds = listOf(123L),
            gates = setOf(1L),
            gateType = GateTypeResponse.INBOUND,
        )

        setUpMockFfwfGetQuota()
        setUpMockFfwfTakeQuota()

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isConflict)
            .andReturn()


        val expectedJson: String =
            getFileContent("fixtures/controller/booking/book-slot/one-slot-one-supplier-inbound/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
        verifyBasicFfwfCommunication(1, 0)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/book-slot/supplier-with-null-id-outbound/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/book-slot/supplier-with-null-id-outbound/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun bookingNotCreatedOnOneSlotForSupplierWithNullIdOutboundTest() {

        val requestJson: String =
            getFileContent("fixtures/controller/booking/book-slot/supplier-with-null-id-outbound/request.json")

        setupLmsGateSchedule(
            warehouseIds = listOf(123L),
            gates = setOf(1L),
            gateType = GateTypeResponse.OUTBOUND,
        )

        setUpMockFfwfGetQuota(setOf(LocalDate.of(2021, 5, 15)))
        setUpMockFfwfTakeQuota(LocalDate.of(2021, 5, 15))

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andReturn()

        val expectedJson: String =
            getFileContent("fixtures/controller/booking/book-slot/supplier-with-null-id-outbound/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
        verifyBasicFfwfCommunication(1, 0)
    }

    @Test
    fun outboundBookingNotCreatedForTomorrowForFFTest() {

        val requestJson: String =
            getFileContent("fixtures/controller/booking/book-slot/outbound-tomorrow/fulfillment/request.json")

        setupLmsGateSchedule(
            warehouseIds = listOf(123L),
            gates = setOf(1L),
            gateType = GateTypeResponse.OUTBOUND,
            workingDays = setOf(
                LocalDate.of(2021, 5, 11),
                LocalDate.of(2021, 5, 12)
            ),
        )

        setUpMockFfwfGetQuota(
            setOf(
                LocalDate.of(2021, 5, 11),
                LocalDate.of(2021, 5, 12)
            )
        )
        setUpMockFfwfTakeQuota(LocalDate.of(2021, 5, 12))


        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andReturn()

        val expectedJson: String =
            getFileContent("fixtures/controller/booking/book-slot/outbound-tomorrow/fulfillment/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

        verifyBasicFfwfCommunication(takeQuotaInvCnt = 0, getQuotaInvCnt = 0)
    }

    @Test
    fun outboundBookingNotCreatedForNextDayOfQuotaFromForFFTest() {

        val requestJson: String =
            getFileContent("fixtures/controller/booking/book-slot/outbound-quota-from/fulfillement/request.json")

        setupLmsGateSchedule(
            warehouseIds = listOf(123L),
            gates = setOf(1L),
            gateType = GateTypeResponse.OUTBOUND,
            workingDays = setOf(
                LocalDate.of(2021, 5, 20),
                LocalDate.of(2021, 5, 21)
            ),
        )

        setUpMockFfwfGetQuota(
            setOf(
                LocalDate.of(2021, 5, 19),
                LocalDate.of(2021, 5, 20)
            )
        )
        setUpMockFfwfTakeQuota(LocalDate.of(2021, 5, 20))


        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andReturn()

        val expectedJson: String =
            getFileContent("fixtures/controller/booking/book-slot/outbound-quota-from/fulfillement/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

        verifyBasicFfwfCommunication(takeQuotaInvCnt = 0, getQuotaInvCnt = 0)
    }

    @Test
    fun outboundBookingCreatedForTodayForSCTest() {

        val requestJson: String =
            getFileContent("fixtures/controller/booking/book-slot/outbound-tomorrow/sorting-center/request.json")

        setupLmsGateSchedule(
            warehouseIds = listOf(123L),
            gates = setOf(1L),
            gateType = GateTypeResponse.OUTBOUND,
            workingDays = setOf(
                LocalDate.of(2021, 5, 11),
                LocalDate.of(2021, 5, 12)
            ),
        )

        setUpMockFfwfGetQuota(
            setOf(
                LocalDate.of(2021, 5, 11),
                LocalDate.of(2021, 5, 12)
            )
        )
        setUpMockFfwfTakeQuota(LocalDate.of(2021, 5, 11))
        setUpMockLmsGetPartner(PartnerType.SORTING_CENTER)

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andReturn()

        val expectedJson: String =
            getFileContent("fixtures/controller/booking/book-slot/outbound-tomorrow/sorting-center/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

        verifyBasicFfwfCommunication(takeQuotaInvCnt = 1, getQuotaInvCnt = 1)
    }

    @Test
    fun outboundBookingNotCreatedForSameDayOfQuotaFromForSCTest() {

        val requestJson: String =
            getFileContent("fixtures/controller/booking/book-slot/outbound-quota-from/sorting-center/request.json")

        setupLmsGateSchedule(
            warehouseIds = listOf(123L),
            gates = setOf(1L),
            gateType = GateTypeResponse.OUTBOUND,
            workingDays = setOf(
                LocalDate.of(2021, 5, 20),
                LocalDate.of(2021, 5, 21)
            ),
        )

        setUpMockFfwfGetQuota(
            setOf(
                LocalDate.of(2021, 5, 19),
                LocalDate.of(2021, 5, 20)
            )
        )
        setUpMockFfwfTakeQuota(LocalDate.of(2021, 5, 20))
        setUpMockLmsGetPartner(PartnerType.SORTING_CENTER)

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andReturn()

        val expectedJson: String =
            getFileContent("fixtures/controller/booking/book-slot/outbound-quota-from/sorting-center/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

        verifyBasicFfwfCommunication(takeQuotaInvCnt = 1, getQuotaInvCnt = 1)
    }

    @Test
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "classpath:fixtures/controller/booking/book-slot/quota-dates-chunks/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT,
        ),
        ExpectedDatabase(
            value = "classpath:fixtures/controller/booking/book-slot/dbqueue.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT,
            connection = "dbqueueDatabaseConnection"
        )
    )
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/book-slot/quota-dates-chunks/before.xml"])
    fun doInChunksWhenManyQuotaPossibleDatesTest() {

        val requestJson: String =
            getFileContent("fixtures/controller/booking/book-slot/quota-dates-chunks/request.json")

        setupLmsGateSchedule(
            warehouseIds = listOf(123L),
            gates = setOf(1L),
            gateType = GateTypeResponse.OUTBOUND,
            workingDays = setOf(
                LocalDate.of(2022, 5, 17),
            ),
        )

        val dates: List<LocalDate> = (0L..50L).map {
                LocalDate.of(2022, 1, 1).minusDays(it) }

        Mockito.`when`(ffwfClientApi!!.getQuota(any()))
            .thenReturn(buildGetQuotaResponseDto(setOf(LocalDate.of(2022, 5, 1)), 0L, 0L))
            .thenReturn(buildGetQuotaResponseDto(setOf(LocalDate.of(2022, 4, 1)), 0L, 0L))
            .thenReturn(buildGetQuotaResponseDto(setOf(LocalDate.of(2022, 3, 1)), 0L, 0L))
            .thenReturn(buildGetQuotaResponseDto(setOf(LocalDate.of(2022, 2, 1)), 0L, 0L))
            .thenReturn(buildGetQuotaResponseDto(dates, 100L, 10L))

        Mockito.`when`(ffwfClientApi!!.takeQuota(any()))
            .thenThrow(HttpServerErrorException(HttpStatus.CONFLICT))
            .thenReturn(TakeQuotaResponseDto(LocalDate.of(2022, 1, 1)))

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andReturn()

        val expectedJson: String =
            getFileContent("fixtures/controller/booking/book-slot/quota-dates-chunks/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

        verifyBasicFfwfCommunication(getQuotaInvCnt = 13, takeQuotaInvCnt = 2)
    }

    private fun buildGetQuotaResponseDto(dates: Collection<LocalDate>, items: Long, pallets: Long): GetQuotaResponseDto {
        return GetQuotaResponseDto.builder()
            .availableQuotasForServices(listOf(
                AvailableQuotasForServiceDto.builder()
                    .availableQuotasForDates(dates.map {
                        AvailableQuotasForDateDto.builder()
                            .date(it)
                            .items(items)
                            .pallets(pallets).build()
                    }
                    ).build()
            )).build()
    }

    private fun setupLmsNotActiveGateSchedule(
        warehouseIds: List<Long> = listOf(1L),
        from: LocalTime = LocalTime.of(10, 0),
        to: LocalTime = LocalTime.of(22, 0),
        gateType: GateTypeResponse = GateTypeResponse.INBOUND,
        workingDays: Set<LocalDate> = setOf(LocalDate.of(2021, 5, 17)),
        gates: Set<Long> = setOf(1L, 2L)
    ) {
        for (warehouseId in warehouseIds) {

            Mockito.`when`(lmsClient!!.getWarehousesGatesCustomScheduleByPartnerId(eq(warehouseId), any(), any())).thenReturn(
                MockParametersHelper.mockGatesScheduleResponse(
                    MockParametersHelper.mockNotAvailableGatesResponse(
                        gates, gateType,
                        workingDays.map {
                            MockParametersHelper.mockGatesSchedules(
                                it,
                                from,
                                to
                            )
                        }
                    ),
                )
            )
        }

    }


}
