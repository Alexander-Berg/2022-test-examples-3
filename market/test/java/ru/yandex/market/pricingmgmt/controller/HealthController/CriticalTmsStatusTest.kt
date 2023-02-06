package ru.yandex.market.pricingmgmt.controller.HealthController

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.service.TimeService
import ru.yandex.market.pricingmgmt.util.DateTimeTestingUtil

class CriticalTmsStatusTest : ControllerTest() {

    companion object {
        private const val HANDLE_PATH = "/health/critical-tms-jobs-status"

        private const val OK_RESPONSE = "0;Critical TMS jobs are operating normally"

        private const val DCO_EXECUTOR = "dcoPriceExecutor"
        private const val SEL_EXECUTOR = "priceSelectionExecutor"

        private val mockDateTime = DateTimeTestingUtil.createOffsetDateTime(2020, 4, 20, 4, 0, 0)
    }

    @MockBean
    private lateinit var timeService: TimeService

    @BeforeEach
    fun mockTime() {
        `when`(timeService.getNowOffsetDateTime()).thenReturn(mockDateTime)
    }

    @Test
    fun testEmptyOK() {
        mockMvc.perform(MockMvcRequestBuilders.get(HANDLE_PATH)).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().string(OK_RESPONSE)
            )
    }

    @Test
    @DbUnitDataSet(before = ["CriticalTmsStatusTest.ok.before.csv"])
    fun testOk() {
        mockMvc.perform(MockMvcRequestBuilders.get(HANDLE_PATH)).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().string(OK_RESPONSE))
    }

    private fun getCriticalMessage(name: String) = "2;$name last ran 2100 seconds ago"

    @Test
    @DbUnitDataSet(before = ["CriticalTmsStatusTest.crit-dco.before.csv"])
    fun testCriticalDco() {
        mockMvc.perform(MockMvcRequestBuilders.get(HANDLE_PATH)).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().string(getCriticalMessage(DCO_EXECUTOR)))
    }

    @Test
    @DbUnitDataSet(before = ["CriticalTmsStatusTest.crit-sel.before.csv"])
    fun testCriticalSelection() {
        mockMvc.perform(MockMvcRequestBuilders.get(HANDLE_PATH)).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().string(getCriticalMessage(SEL_EXECUTOR)))
    }

    private fun getWarnMessage(name: String) = "1;$name last ran 1500 seconds ago"

    @Test
    @DbUnitDataSet(before = ["CriticalTmsStatusTest.warn-dco.before.csv"])
    fun testWarningDco() {
        mockMvc.perform(MockMvcRequestBuilders.get(HANDLE_PATH)).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().string(getWarnMessage(DCO_EXECUTOR)))
    }

    @Test
    @DbUnitDataSet(before = ["CriticalTmsStatusTest.warn-sel.before.csv"])
    fun testWarningSel() {
        mockMvc.perform(MockMvcRequestBuilders.get(HANDLE_PATH)).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().string(getWarnMessage(SEL_EXECUTOR)))
    }
}
