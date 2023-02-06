package ru.yandex.market.wms.pickbylight.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType

import ru.yandex.market.wms.pickbylight.configuration.PickByLightIntegrationTest
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import ru.yandex.market.wms.pickbylight.model.StationOperation
import ru.yandex.market.wms.pickbylight.model.StationOperation.InMode
import ru.yandex.market.wms.pickbylight.model.StationOperation.OutMode

class StationOperationControllerTest : PickByLightIntegrationTest() {

    @Autowired
    private lateinit var mapper: ObjectMapper

    @Test
    @DatabaseSetup("/db/controller/st-op/get-all/db.xml")
    @ExpectedDatabase("/db/controller/st-op/get-all/db.xml", assertionMode = NON_STRICT_UNORDERED)
    fun getAll() {
        val expected = listOf(
            StationOperation("S01", true, InMode.DEFAULT, true, OutMode.DEFAULT),
            StationOperation("S02", true, InMode.SCAN_CELL, false, OutMode.DEFAULT)
        )
        mockMvc.perform(get("/station-operations"))
            .andExpect(status().isOk)
            .andExpect(content().json(mapper.writeValueAsString(expected), true))
    }

    @Test
    @DatabaseSetup("/db/controller/st-op/update/before.xml")
    @ExpectedDatabase("/db/controller/st-op/update/after.xml", assertionMode = NON_STRICT_UNORDERED)
    fun update() {
        val update = StationOperation("S02", true, InMode.PUSH_BUTTON, true, OutMode.DEFAULT)
        val request = put("/station-operations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(update))
        mockMvc.perform(request).andExpect(status().isOk)
    }
}
