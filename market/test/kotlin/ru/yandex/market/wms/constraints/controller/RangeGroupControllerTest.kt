package ru.yandex.market.wms.constraints.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent
import ru.yandex.market.wms.constraints.config.ConstraintsIntegrationTest

class RangeGroupControllerTest: ConstraintsIntegrationTest() {
    @Test
    @DatabaseSetup("/controller/range-group/get/data.xml")
    fun `getRangeGroups with non-existent zone`() {
        testGetRangeGroups("empty", listOf("abc"))
    }

    @Test
    @DatabaseSetup("/controller/range-group/get/data.xml")
    fun `getRangeGroups by full list of zones`() {
        testGetRangeGroups("all", listOf("ZONE 1", "ZONE 2", "ZONE 3"))
    }

    @Test
    @DatabaseSetup("/controller/range-group/get/data.xml")
    fun `getRangeGroups by list of zones`() {
        testGetRangeGroups("specific-zones", listOf("ZONE 1", "ZONE 2"))
    }

    private fun testGetRangeGroups(testCase: String, putawayZones: List<String>) {
        val request = get("/range-group")

        if (putawayZones.isNotEmpty()) {
            request.param("putawayZones", *putawayZones.toTypedArray())
        }

        mockMvc.perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(
                content()
                    .json(getFileContent("controller/range-group/get/$testCase/response.json"), true)
            )
    }
}
