package ru.yandex.market.logistics.calendaring.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.service.datetime.geobase.GeobaseProviderApi
import ru.yandex.market.logistics.calendaring.util.FileContentUtils
import ru.yandex.market.logistics.calendaring.util.MockParametersHelper
import ru.yandex.market.logistics.calendaring.util.getFileContent
import ru.yandex.market.logistics.management.entity.type.GateTypeResponse
import java.time.LocalDate
import java.time.LocalTime

class BookingControllerBookSlotsTest(
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
        value = "classpath:fixtures/controller/booking/book-slots/not-in-working-hours/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun badRequestWhenNotInWorkingHoursTest() {

        setupLmsGateSchedule(
            from = LocalTime.of(10, 0),
            to = LocalTime.of(11, 0),
        )
        setUpMockFfwfGetQuota(setOf(LocalDate.of(2021, 5, 17)))

        val requestJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/book-slots/not-in-working-hours/request.json")

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/batch")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()

        val expectedJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/book-slots/not-in-working-hours/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

//        verify(ffwfClientApi!!).getQuota(any())
        verifyBasicFfwfCommunication(getQuotaInvCnt = 1, takeQuotaInvCnt = 0)
    }


    @Test
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/book-slots/no-available-gates/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun badRequestWhenNoActiveGatesTest() {

        setupLmsGateSchedule(
            warehouseIds = listOf(2L),
            from = LocalTime.of(10, 0),
            to = LocalTime.of(11, 0),
        )
        setUpMockFfwfGetQuota(setOf(LocalDate.of(2021, 5, 17)))

        setupLmsNotActiveGateSchedule()

        val requestJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/book-slots/no-available-gates/request.json")

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/batch")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()

        val expectedJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/book-slots/no-available-gates/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

//        verify(ffwfClientApi!!).getQuota(any())
        verifyBasicFfwfCommunication(getQuotaInvCnt = 1, takeQuotaInvCnt = 0)
    }

    @Test
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/book-slots/warehouse-not-found/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun badRequestWhenWarehouseNotFoundTest() {

        setupLmsGateSchedule(
            warehouseIds = listOf(2),
            from = LocalTime.of(10, 0),
            to = LocalTime.of(11, 0),
        )
        setUpMockFfwfGetQuota(setOf(LocalDate.of(2021, 5, 17)))

        setupLmsNotActiveGateSchedule()

        val requestJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/book-slots/warehouse-not-found/request.json")

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/batch")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()

        val expectedJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/book-slots/warehouse-not-found/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

//        verify(ffwfClientApi!!).getQuota(any())
        verifyBasicFfwfCommunication(getQuotaInvCnt = 1, takeQuotaInvCnt = 0)
    }

    @Test
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/book-slots/no-possible-quota/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun badRequestWhenNoPossibleQuota() {
        setupLmsNotActiveGateSchedule()

        val requestJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/book-slots/no-possible-quota/request.json")

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/batch")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()

        val expectedJson: String =
            FileContentUtils.getFileContent("fixtures/controller/booking/book-slots/no-possible-quota/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/book-slots/one-slot-one-supplier-outbound/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/book-slots/one-slot-one-supplier-outbound/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun bookingCreatedSuccessfullyOnOneSlotForOneSupplierOutboundTest() {

        val requestJson: String =
            getFileContent("fixtures/controller/booking/book-slots/one-slot-one-supplier-outbound/request.json")

        setupLmsGateSchedule(
            warehouseIds = listOf(123L),
            gates = setOf(1L),
            gateType = GateTypeResponse.OUTBOUND,
        )

        setUpMockFfwfGetQuota(setOf(LocalDate.of(2021, 5, 15)))
        setUpMockFfwfTakeQuota(LocalDate.of(2021, 5, 15))

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/batch")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andReturn()

        val expectedJson: String =
            getFileContent("fixtures/controller/booking/book-slots/one-slot-one-supplier-outbound/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

        verifyBasicFfwfCommunication(getQuotaInvCnt = 2, takeQuotaInvCnt = 2)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/book-slots/slot-busy/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/book-slots/slot-busy/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun slotIsBusy() {

        val requestJson: String =
            getFileContent("fixtures/controller/booking/book-slots/slot-busy/request.json")

        setupLmsGateSchedule(
            warehouseIds = listOf(123L),
            gates = setOf(1L),
            gateType = GateTypeResponse.OUTBOUND,
        )

        setUpMockFfwfGetQuota(setOf(LocalDate.of(2021, 5, 15)))
        setUpMockFfwfTakeQuota(LocalDate.of(2021, 5, 15))

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/batch")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andReturn()

        val expectedJson: String =
            getFileContent("fixtures/controller/booking/book-slots/slot-busy/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

        verifyBasicFfwfCommunication(getQuotaInvCnt = 2, takeQuotaInvCnt = 0)
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
