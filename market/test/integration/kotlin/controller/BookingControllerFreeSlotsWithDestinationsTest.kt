package ru.yandex.market.logistics.calendaring.controller

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
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.ff.client.dto.quota.AvailableQuotaForServiceAndDestinationDto
import ru.yandex.market.ff.client.dto.quota.AvailableQuotasForDateDto
import ru.yandex.market.ff.client.dto.quota.GetQuotaWithDestinationsResponseDto
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.service.datetime.geobase.GeobaseProviderApi
import ru.yandex.market.logistics.calendaring.util.getFileContent
import ru.yandex.market.logistics.management.entity.type.GateTypeResponse
import java.time.LocalDate
import java.time.LocalTime

class BookingControllerFreeSlotsWithDestinationsTest(
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
    fun getFreeSlotsWithDestinationServiceId() {

        val requestJson: String =
            getFileContent("fixtures/controller/booking/free-slots-with-destinations/1/request.json")

        setupLmsGateSchedule(
            warehouseIds = listOf(172),
            from = LocalTime.of(10, 0),
            to = LocalTime.of(12, 0),
            gateType = GateTypeResponse.INBOUND,
            gates = setOf(1L, 2L),
        )

        val response = GetQuotaWithDestinationsResponseDto(listOf(
            AvailableQuotaForServiceAndDestinationDto(172, 171, listOf(
                AvailableQuotasForDateDto(LocalDate.of(2021, 5, 17), 15, 7),
                AvailableQuotasForDateDto(LocalDate.of(2021, 5, 18), 8, 7),
            )),
            AvailableQuotaForServiceAndDestinationDto(172, 147, listOf(
                AvailableQuotasForDateDto(LocalDate.of(2021, 5, 17), 1002, 51),
                AvailableQuotasForDateDto(LocalDate.of(2021, 5, 18), 1003, 52),
            ))
        ))
        Mockito.`when`(ffwfClientApi!!.getQuotaWithDestinations(ArgumentMatchers.argThat { param -> param.destinationWarehouseIds.equals(setOf(147L, 171L)) }))
            .thenReturn(response)

        val result = performGetSlots(requestJson)
        Mockito.verify(ffwfClientApi!!).getQuotaWithDestinations(ArgumentMatchers.argThat { param -> param.destinationWarehouseIds.equals(setOf(147L, 171L)) })

        val expectedJson: String =
            getFileContent("fixtures/controller/booking/free-slots-with-destinations/1/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }

    private fun performGetSlots(requestJson: String): MvcResult {
        return mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/free-slots-with-destinations")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
    }
}
