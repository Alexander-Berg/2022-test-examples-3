package ru.yandex.market.logistics.calendaring.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.apache.http.entity.ContentType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.service.datetime.geobase.GeobaseProviderApi
import ru.yandex.market.logistics.calendaring.util.FileContentUtils
import ru.yandex.market.logistics.calendaring.util.MockParametersHelper
import ru.yandex.market.logistics.calendaring.util.getFileContent
import ru.yandex.market.logistics.management.entity.type.GateTypeResponse
import java.time.LocalDate
import java.time.LocalTime
import java.util.*


class BookingControllerBookSlotGateIndividualScheduleTest(
    @Autowired var geobaseProviderApi: GeobaseProviderApi,
) : AbstractContextualTest() {


    @BeforeEach
    fun init() {
        setUpMockLmsGetLocationZone(geobaseProviderApi)
    }

    @AfterEach
    fun verifyMocks() {
        verifyNoMoreInteractions(ffwfClientApi!!)
    }


    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/book-slot/gate-individual-schedules/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/book-slot/gate-individual-schedules/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun bookingCreatedSuccessfullyOnOneSlotForOneSupplierOutboundGatesHaveDifferentSchedulesTest() {

        val requestJson: String =
            getFileContent("fixtures/controller/booking/book-slot/gate-individual-schedules/request.json")

        setupLmsGateSchedule(
            warehouseIds = listOf(123L),
            scheduleAGates = setOf(1L),
            scheduleAFrom = LocalTime.of(8, 0),
            scheduleATo = LocalTime.of(12, 0),
            scheduleBGates = setOf(2L),
            scheduleBFrom = LocalTime.of(14, 0),
            scheduleBTo = LocalTime.of(16, 0),
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
            getFileContent("fixtures/controller/booking/book-slot/gate-individual-schedules/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
        verifyBasicFfwfCommunication()
    }



    private fun setupLmsGateSchedule(
        warehouseIds: List<Long> = listOf(1L),
        gateType: GateTypeResponse = GateTypeResponse.INBOUND,
        scheduleAGates: Set<Long> = setOf(1L, 2L),
        scheduleAWorkingDays: Set<LocalDate> = setOf(
            LocalDate.of(2021, 5, 17)),
        scheduleAFrom: LocalTime = LocalTime.of(10, 0),
        scheduleATo: LocalTime = LocalTime.of(22, 0),
        scheduleBGates: Set<Long> = setOf(),
        scheduleBWorkingDays: Set<LocalDate> = setOf(
            LocalDate.of(2021, 5, 17)),
        scheduleBFrom: LocalTime = scheduleAFrom,
        scheduleBTo: LocalTime = scheduleATo,
        active: Boolean = true
    ) {
        for (warehouseId in warehouseIds) {

            Mockito.`when`(
                lmsClient!!.getWarehousesGatesCustomScheduleByPartnerId(
                    eq(warehouseId),
                    ArgumentMatchers.any(LocalDate::class.java),
                    ArgumentMatchers.any(LocalDate::class.java)
                )
            ).thenReturn(
                MockParametersHelper.mockGatesScheduleResponse(
                    MockParametersHelper.mockAvailableGatesResponse(scheduleAGates, EnumSet.of(gateType), scheduleAWorkingDays.map {
                        MockParametersHelper.mockGatesSchedules(
                            it,
                            scheduleAFrom,
                            scheduleATo
                        )
                    }, active).plus(MockParametersHelper.mockAvailableGatesResponse(scheduleBGates, EnumSet.of(gateType), scheduleBWorkingDays.map {
                        MockParametersHelper.mockGatesSchedules(
                            it,
                            scheduleBFrom,
                            scheduleBTo
                        )
                    }, active))
                )
            )
        }
    }

}
