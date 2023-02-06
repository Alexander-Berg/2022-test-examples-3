package ru.yandex.market.wms.dimensionmanagement.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.dimensionmanagement.configuration.DimensionManagementIntegrationTest

class UtilsControllerTest: DimensionManagementIntegrationTest() {

    @Test
    @DatabaseSetup("/controller/utils-controller/validate-dimensions/before.xml")
    fun validateDimensionsOk() {
        val result = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/utils/validate-dimensions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/utils-controller/validate-dimensions/1/" +
                        "request.json")
        ))

        result.andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/utils-controller/validate-dimensions/1/response.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/utils-controller/validate-dimensions/before.xml")
    fun validateDimensionsFail() {
        val result = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/utils/validate-dimensions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/utils-controller/validate-dimensions/2/" +
                        "request.json")
                ))

        result.andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/utils-controller/validate-dimensions/2/response.json"
                    )
                )
            )
    }
}
