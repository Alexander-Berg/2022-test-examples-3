package ru.yandex.market.wms.dimensionmanagement.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.dimensionmanagement.configuration.DimensionManagementIntegrationTest

class MeasureStationsControllerTest : DimensionManagementIntegrationTest() {

    @Test
    @DatabaseSetup("/controller/measure-stations/get-locations-to-stations/immutable.xml")
    @ExpectedDatabase(
        value = "/controller/measure-stations/get-locations-to-stations/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getStationInfoWithContainersHappyPath() {
        val result = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/stations/locations")
        )
        result.andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/measure-stations/get-locations-to-stations/response.json"
                    )
                )
            )
    }
}
