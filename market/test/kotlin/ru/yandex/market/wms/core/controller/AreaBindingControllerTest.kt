package ru.yandex.market.wms.core.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent

class AreaBindingControllerTest : IntegrationTest() {

    @Test
    @DatabaseSetup("/controller/area-binding/bind/db/before.xml")
    @ExpectedDatabase(value = "/controller/area-binding/bind/db/before.xml", assertionMode = NON_STRICT)
    fun bind_buildingDoesNotExist() {
        mockMvc.perform(post("/area-binding/bind")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/area-binding/bind/request/bind-buildingDoesNotExist.json")))
                .andExpect(status().isUnprocessableEntity)
    }

    @Test
    @DatabaseSetup("/controller/area-binding/bind/db/before.xml")
    @ExpectedDatabase(value = "/controller/area-binding/bind/db/before.xml", assertionMode = NON_STRICT)
    fun bind_areaDoesNotExist() {
        mockMvc.perform(post("/area-binding/bind")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/area-binding/bind/request/bind-areaDoesNotExist.json")))
                .andExpect(status().isUnprocessableEntity)
    }

    @Test
    @DatabaseSetup("/controller/area-binding/bind/db/before.xml")
    @ExpectedDatabase(value = "/controller/area-binding/bind/db/after.xml", assertionMode = NON_STRICT)
    fun bind() {
        mockMvc.perform(post("/area-binding/bind")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/area-binding/bind/request/bind.json")))
                .andExpect(status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup("/controller/area-binding/get-areas/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/area-binding/get-areas/immutable-state.xml", assertionMode = NON_STRICT)
    fun listAreas() {
        mockMvc.perform(get("/area-binding/areas")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(
                getFileContent("controller/area-binding/get-areas/response.json")))
    }

    @Test
    @DatabaseSetup("/controller/area-binding/get-buildings/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/area-binding/get-buildings/immutable-state.xml", assertionMode = NON_STRICT)
    fun listBuildings() {
        mockMvc.perform(get("/area-binding/buildings")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(
                getFileContent("controller/area-binding/get-buildings/response.json")))
    }
}
