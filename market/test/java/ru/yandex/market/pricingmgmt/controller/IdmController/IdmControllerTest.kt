package ru.yandex.market.pricingmgmt.controller.IdmController

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest
import ru.yandex.market.pricingmgmt.config.tvm.DummyTvmClient
import ru.yandex.passport.tvmauth.TvmClient

@AutoConfigureMockMvc
class IdmControllerTest : AbstractFunctionalTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var tvmClient: TvmClient

    private val TVM_TICKET_HEADER = "X-Ya-Service-Ticket"
    private val TVM_IDM_PROD = 2001600

    @Test
    @DbUnitDataSet(before = ["IdmControllerTest.roles-in-db.csv"])
    fun testGetInfo_validInput_returnsValidJson() {

        // IDM Controller принимает запросы только от продового IDM
        (tvmClient as DummyTvmClient).mockSourceTvmId(TVM_IDM_PROD)

        performTest(DummyTvmClient.VALID_TVM_TICKET)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.warning").isEmpty())
            .andExpect(jsonPath("$.error").isEmpty())
            .andExpect(jsonPath("$.fatal").isEmpty())
            .andExpect(jsonPath("$.roles.slug").value("group"))
            .andExpect(jsonPath("$.roles.name").value("Группа"))
            .andExpect(
                jsonPath("$.roles.values.ROLE_PRICING_MGMT_ACCESS")
                    .value("Доступ к интерфейсу управления ценами")
            )
            .andExpect(
                jsonPath("$.roles.values.ROLE_ADMIN_FARMA")
                    .value("Админ FARMA")
            )
    }

    @Test
    fun testGetInfo_expiredTvmTicket_Forbidden() {

        // IDM Controller принимает запросы только от продового IDM
        (tvmClient as DummyTvmClient).mockSourceTvmId(TVM_IDM_PROD)

        performTest(DummyTvmClient.EXPIRED_TVM_TICKET)
            .andExpect(status().isForbidden)
    }

    @Test
    fun testGetInfo_tvmTicketNotFromIdm_Forbidden() {

        // IDM Controller принимает запросы только от продового IDM
        (tvmClient as DummyTvmClient).mockSourceTvmId(121212)

        performTest(DummyTvmClient.VALID_TVM_TICKET)
            .andExpect(status().isForbidden)
    }

    private fun performTest(withTvmTicket: String): ResultActions {
        return mockMvc.perform(
            MockMvcRequestBuilders.get("/idm/info/")
                .header(TVM_TICKET_HEADER, withTvmTicket)
        )
    }
}


