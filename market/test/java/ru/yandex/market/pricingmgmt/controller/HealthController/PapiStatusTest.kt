package ru.yandex.market.pricingmgmt.controller.HealthController

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest

class PapiStatusTest : ControllerTest() {

    companion object {
        private const val HANDLE_PATH = "/health/papi-proxy-status"

        private const val OK_RESPONSE = "0;PAPI Proxy is operating normally"
        private const val WARN_RESPONSE = "1;Prices have not been uploaded to PAPI for 360 seconds"
        private const val CRITICAL_RESPONSE = "2;Prices have not been uploaded to PAPI for 1200 seconds"
    }

    @Test
    fun testEmptyOK() {
        mockMvc.perform(get(HANDLE_PATH)).andExpect(status().isOk).andExpect(content().string(OK_RESPONSE))
    }

    @Test
    @DbUnitDataSet(before = ["PapiStatusTest.ok.before.csv"])
    fun testOk() {
        mockMvc.perform(get(HANDLE_PATH)).andExpect(status().isOk).andExpect(content().string(OK_RESPONSE))
    }

    @Test
    @DbUnitDataSet(before = ["PapiStatusTest.crit.before.csv"])
    fun testCrit() {
        mockMvc.perform(get(HANDLE_PATH)).andExpect(status().isOk).andExpect(content().string(CRITICAL_RESPONSE))
    }

    @Test
    @DbUnitDataSet(before = ["PapiStatusTest.warn.before.csv"])
    fun testWarn() {
        mockMvc.perform(get(HANDLE_PATH)).andExpect(status().isOk).andExpect(content().string(WARN_RESPONSE))
    }
}
