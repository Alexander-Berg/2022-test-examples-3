package ru.yandex.market.wms.consolidation.modules.admin

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent

internal class AdminControllerTest: IntegrationTest() {

    @Test
    @DatabaseSetup("/admin-controller/before/before-1.xml")
    fun getPartialWaveConfigs() {
        mockMvc.perform(get("/admin/configs"))
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("admin-controller/response-1.json"), false))
    }

    @Test
    @DatabaseSetup("/admin-controller/before/before-1.xml")
    @ExpectedDatabase("/admin-controller/after/after-1.xml", assertionMode = NON_STRICT_UNORDERED)
    fun savePartialWaveConfigs() {
        mockMvc.perform(put("/admin/configs")
            .contentType(APPLICATION_JSON)
            .content(getFileContent("admin-controller/request-1.json")))
            .andExpect(status().is2xxSuccessful)
    }

    @Test
    fun savePartialWaveConfigs_badRequest() {
        mockMvc.perform(put("/admin/configs")
            .contentType(APPLICATION_JSON)
            .content(getFileContent("admin-controller/request-2.json")))
            .andExpect(status().isBadRequest)
    }
}
