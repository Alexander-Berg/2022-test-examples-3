package ru.yandex.market.wms.core.controller.admin

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent

class AdminConsLocationControllerTest : IntegrationTest() {
    @Test
    fun addConsolidationLocation_notValid() {
        mockMvc.perform(post("/admin/cons")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("cons-loc/request/bad-request.json")))
                .andExpect(status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/cons-loc/db/before-2.xml")
    @ExpectedDatabase(value = "/cons-loc/db/after-1.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun addConsolidationLocation_ok() {
        mockMvc.perform(post("/admin/cons")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("cons-loc/request/add-request.json")))
                .andExpect(status().isCreated)
    }

    @Test
    @DatabaseSetup("/cons-loc/db/before-2.xml")
    @ExpectedDatabase(value = "/cons-loc/db/after-2.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun deleteConsolidationLocation() {
        mockMvc.perform(delete("/admin/cons")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("cons-loc/request/delete-request.json")))
                .andExpect(status().isNoContent)
    }

    @Test
    @DatabaseSetup("/cons-loc/db/before-3.xml")
    @ExpectedDatabase(value = "/cons-loc/db/after-3.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun updateConsolidationLocation() {
        mockMvc.perform(put("/admin/cons")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("cons-loc/request/put-request.json")))
                .andExpect(status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup("/cons-loc/db/before.xml")
    fun getConsolidationLocations() {
        mockMvc.perform(get("/admin/cons")
                .accept(APPLICATION_JSON))
                .andExpect(status().isOk)
                .andExpect(content().json(getFileContent("cons-loc/response/get-response.json"), false))
    }
}
