package ru.yandex.market.wms.inventorization.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.inventorization.service.FewItemsInventoryService

class FewItemsInventoryTaskControllerTest : IntegrationTest() {

    @MockBean
    @Autowired
    private lateinit var fewItemsInventoryService: FewItemsInventoryService

    @Test
    fun `CreateFewItemsInventoryTasks success`() {
        val request = MockMvcRequestBuilders.post("/few-items-tasks").contentType(MediaType.APPLICATION_JSON)
            .content(FileContentUtils.getFileContent("controller/few-items-tasks/request.json"))

        mockMvc.perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(FileContentUtils.getFileContent("controller/few-items-tasks/response.json"), true)
            )
            .andReturn()
    }
}
