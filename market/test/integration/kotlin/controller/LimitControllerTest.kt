package ru.yandex.market.logistics.calendaring.controller

import com.nhaarman.mockitokotlin2.any
import org.apache.http.entity.ContentType
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.ff.client.dto.quota.AvailableQuotasForDateDto
import ru.yandex.market.ff.client.dto.quota.AvailableQuotasForServiceDto
import ru.yandex.market.ff.client.dto.quota.GetQuotaResponseDto
import ru.yandex.market.ff.client.dto.quota.TakeQuotaResponseDto
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.util.getFileContent
import java.time.LocalDate

class LimitControllerTest: AbstractContextualTest()  {

    @Test
    fun getBookingLimitSuccessfullyTest(){

        val quotaDate = LocalDate.of(2021, 5, 1)
        `when`(ffwfClientApi!!.findByBookingId(any())).thenReturn(TakeQuotaResponseDto(quotaDate))

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.get("/limit/booking/1")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andReturn()

        val expectedJson: String =
            getFileContent("fixtures/controller/limit/booking/get-limit/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)

    }


    @Test
    fun getAvailableLimitSuccessfully() {

        val getQuotaResponseDto = GetQuotaResponseDto(
            listOf(
                AvailableQuotasForServiceDto(
                    1L, listOf(
                        AvailableQuotasForDateDto(
                            LocalDate.of(2021, 1, 1),
                            100, 10
                        ),
                        AvailableQuotasForDateDto(
                            LocalDate.of(2021, 1, 2),
                            200, 20
                        )
                    )
                )
            )
        )

        `when`(ffwfClientApi!!.getQuota(any()))
            .thenReturn(getQuotaResponseDto)

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.get("/limit/available/1/WITHDRAW/FIRST_PARTY/2021-01-01,2021-01-02")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andDo(MockMvcResultHandlers.print())
            .andReturn()

        val expectedJson: String =
            getFileContent("fixtures/controller/limit/get-available/get-successfully/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }

}
