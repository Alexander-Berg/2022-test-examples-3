package ru.yandex.market.logistics.yard.controller

import com.nhaarman.mockitokotlin2.eq
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest

class DocumentControllerTest : AbstractSecurityMockedContextualTest() {

    @MockBean
    val ffWorkflowApiClient: FulfillmentWorkflowClientApi? = null

    @BeforeEach
    fun setup() {
        Mockito.`when`(ffWorkflowApiClient!!.getDocuStream(eq("303"))).thenReturn("myDocu".toByteArray())
    }


    @Test
    fun downloadDocu() {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/documents/request-daily-report/303?" +
                    "downloadName=%D0%AF%D0%BD%D0%B4%D0%B5%D0%BA%D1%81_" +
                    "%D0%9C%D0%B0%D1%80%D0%BA%D0%B5%D1%82_" +
                    "%D0%A1%D0%BE%D1%84%D1%8C%D0%B8%D0%BD%D0%BE-1P-2022-04-17.pdf")
        ).andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.content()
                .bytes("myDocu".toByteArray()))
            .andExpect(MockMvcResultMatchers.header()
                .string("Content-Disposition",
                    "attachment;filename=Яндекс_Маркет_Софьино-1P-2022-04-17.pdf"))
    }

}
