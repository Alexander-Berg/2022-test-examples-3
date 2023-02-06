package ru.yandex.market.wms.consolidation.modules.preconsolidation.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent
import ru.yandex.market.wms.transportation.client.TransportationClient

class PreConsolidationControllerTest : IntegrationTest() {
    @MockBean
    @Autowired
    private lateinit var transportationClient: TransportationClient

    @Test
    @DatabaseSetup("/reassign-station/controller/before.xml")
    fun getAssignableStationForWave01() {
        mockMvc.perform(MockMvcRequestBuilders.get("/precons/assignable-stations-to-wave/01"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(
                getFileContent("reassign-station/controller/response-wave01-stations.json"), false))
    }

    @Test
    @DatabaseSetup("/reassign-station/controller/before.xml")
    fun getAssignableStationForWave02() {
        mockMvc.perform(MockMvcRequestBuilders.get("/precons/assignable-stations-to-wave/02"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(
                getFileContent("reassign-station/controller/response-wave02-stations.json"), false))
    }

    @Test
    @DatabaseSetup("/reassign-station/controller/before.xml")
    fun getAssignableStationForNotexistedWave() {
        mockMvc.perform(MockMvcRequestBuilders.get("/precons/assignable-stations-to-wave/00"))
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
    }

    @Test
    @DatabaseSetup("/reassign-station/controller/before.xml")
    fun getAssignableStationForWrongWaveType() {
        mockMvc.perform(MockMvcRequestBuilders.get("/precons/assignable-stations-to-wave/03"))
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
    }

    @Test
    @DatabaseSetup("/reassign-station/controller/before.xml")
    fun getAssignableStationForWrongWaveState() {
        mockMvc.perform(MockMvcRequestBuilders.get("/precons/assignable-stations-to-wave/04"))
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
    }

    @Test
    @DatabaseSetup("/reassign-station/controller/before.xml")
    fun assignStationToWave() {
        mockMvc.perform(MockMvcRequestBuilders.put("/precons/assign-station-to-wave")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("reassign-station/controller/request-assign-stations.json")))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(
                getFileContent("reassign-station/controller/response-assign-stations.json"), false))
    }

    @Test
    @DatabaseSetup("/reassign-station/controller/before.xml")
    @ExpectedDatabase("/reassign-station/controller/after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun assignStationToWaveForce() {
        mockMvc.perform(MockMvcRequestBuilders.put("/precons/assign-station-to-wave")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("reassign-station/controller/request-assign-stations-force.json")))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(
                getFileContent("reassign-station/controller/response-assign-stations-force.json"), false))
    }
}
