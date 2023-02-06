package ru.yandex.market.tpl.internal.controller.partner.surcharge

import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.tpl.core.domain.surcharge.entity.SurchargeType
import ru.yandex.market.tpl.core.domain.surcharge.service.SurchargeTypeQueryService
import ru.yandex.market.tpl.core.mvc.PartnerCompanyHandler
import ru.yandex.market.tpl.internal.BaseShallowTest
import ru.yandex.market.tpl.internal.WebLayerTest

@WebLayerTest(PartnerSurchargeTypeController::class)
internal class PartnerSurchargeTypeControllerTest : BaseShallowTest() {

    @MockBean
    private lateinit var surchargeTypeQueryService: SurchargeTypeQueryService

    @Test
    fun `getAll - success`() {
        `when`(surchargeTypeQueryService.findAll()).thenReturn(
            listOf(getType("1"), getType("2"))
        )

        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/internal/partner/surcharge-types")
                    .header(PartnerCompanyHandler.COMPANY_HEADER, -1L)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk)
            .andExpect(content().json(getFileContent("partner/surcharge/response-getAll-success.json")))
    }

    @Test
    fun `getAll - empty`() {
        `when`(surchargeTypeQueryService.findAll()).thenReturn(listOf())

        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/internal/partner/surcharge-types")
                    .header(PartnerCompanyHandler.COMPANY_HEADER, -1L)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk)
            .andExpect(content().json(getFileContent("partner/surcharge/response-getAll-empty.json")))
    }

    private fun getType(id: String) = SurchargeType(
        id = id,
        code = "$id-code",
        name = "$id-name",
        type = SurchargeType.Type.PENALTY,
        description = "$id-desc",
        userShiftIsRequired = false,
        deleted = false,
    )

}
