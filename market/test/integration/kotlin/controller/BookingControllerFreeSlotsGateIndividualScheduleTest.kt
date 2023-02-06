package ru.yandex.market.logistics.calendaring.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingType
import ru.yandex.market.logistics.calendaring.client.dto.enums.SupplierType
import ru.yandex.market.logistics.calendaring.service.datetime.geobase.GeobaseProviderApi
import ru.yandex.market.logistics.calendaring.util.FileContentUtils
import ru.yandex.market.logistics.calendaring.util.MockParametersHelper
import ru.yandex.market.logistics.management.entity.type.GateTypeResponse
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.*


class BookingControllerFreeSlotsGateIndividualScheduleTest(
    @Autowired var geobaseProviderApi: GeobaseProviderApi,
) :
    AbstractContextualTest() {

    @BeforeEach
    fun init() {
        setUpMockLmsGetLocationZone(geobaseProviderApi)
        setUpMockFfwfGetQuota()

    }

    @AfterEach
    fun verifyMocks() {
        verifyNoMoreInteractions(ffwfClientApi!!)
    }


    @Test
    @DatabaseSetup("classpath:fixtures/controller/booking/free-slots/gate-individual-schedules/before.xml")
    fun gatesHaveDifferentSchedules() {
        setupLmsCustomGateSchedule(
            scheduleAGates = setOf(1L),
            scheduleAWorkingDays = setOf(LocalDate.of(2021, 5, 17)),
            scheduleAFrom = LocalTime.of(8, 0),
            scheduleATo = LocalTime.of(12, 0),

            scheduleBGates = setOf(2L),
            scheduleBWorkingDays = setOf(LocalDate.of(2021, 5, 17)),
            scheduleBFrom = LocalTime.of(14, 0),
            scheduleBTo = LocalTime.of(18, 0),
        )

        val params: MultiValueMap<String, String> = LinkedMultiValueMap()
        params.add("warehouseIds", "1")
        params.add("bookingType", BookingType.SUPPLY.toString())
        params.add("slotDurationMinutes", "60")
        params.add("calendaringStep", "30")
        params.add("from", "2021-05-17T07:00")
        params.add("to", "2021-05-17T22:00")
        params.add("supplierType", SupplierType.THIRD_PARTY.toString())
        params.add("supplierId", "101")
        params.add("takenItems", "50")
        params.add("takenPallets", "2")


        val mvcResult: MvcResult = performGetSlots(params)

        val jsonResponseNoFile = getJsonResponseNoFile("/gate-individual-schedules/response.json")
        JSONAssert.assertEquals(jsonResponseNoFile, mvcResult.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
        verifyBasicFfwfCommunication(1, 0)
    }


    private fun setupLmsCustomGateSchedule(
        warehouseIds: List<Long> = listOf(1L),
        gateType: GateTypeResponse = GateTypeResponse.INBOUND,
        scheduleAGates: Set<Long> = setOf(1L, 2L),
        scheduleAWorkingDays: Set<LocalDate> = setOf(
            LocalDate.of(2021, 5, 17),
            LocalDate.of(2021, 5, 18)),
        scheduleAFrom: LocalTime = LocalTime.of(10, 0),
        scheduleATo: LocalTime = LocalTime.of(22, 0),
        scheduleBGates: Set<Long> = setOf(),
        scheduleBWorkingDays: Set<LocalDate> = setOf(
            LocalDate.of(2021, 5, 17),
            LocalDate.of(2021, 5, 18)),
        scheduleBFrom: LocalTime = LocalTime.of(12, 0),
        scheduleBTo: LocalTime = LocalTime.of(16, 0)
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
                    }).plus(MockParametersHelper.mockAvailableGatesResponse(scheduleBGates, EnumSet.of(gateType), scheduleBWorkingDays.map {
                        MockParametersHelper.mockGatesSchedules(
                            it,
                            scheduleBFrom,
                            scheduleBTo
                        )
                    }))
                )
            )
        }
    }

    private fun performGetSlots(params: MultiValueMap<String, String>): MvcResult {
        return mockMvc!!.perform(
            MockMvcRequestBuilders.get("/booking/free-slots")
                .contentType(MediaType.APPLICATION_JSON)
                .params(params)
        ).andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
    }

    private fun getJsonResponseNoFile(name: String): String {
        return FileContentUtils.getFileContent("fixtures/controller/booking/free-slots/$name")
    }

}
