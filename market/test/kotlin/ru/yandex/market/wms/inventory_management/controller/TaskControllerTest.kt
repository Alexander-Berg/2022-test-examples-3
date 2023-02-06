package ru.yandex.market.wms.inventory_management.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.kotlin.reset
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.core.base.dto.AdminLocsDto
import ru.yandex.market.wms.core.base.response.GetAdminLocsResponse
import ru.yandex.market.wms.core.client.CoreClient
import ru.yandex.market.wms.inventory_management.config.InventoryManagementIntegrationTest

class TaskControllerTest : InventoryManagementIntegrationTest() {

    @Autowired
    @MockBean
    private lateinit var coreClient: CoreClient

    @Autowired
    private lateinit var mapper: ObjectMapper

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @BeforeEach
    fun clean() {
        reset(coreClient)
        jdbc.execute("alter sequence inventory_management.SEQ_TASK_GROUP restart with 1;")
    }

    @Test
    @DatabaseSetup("/controller/create-tasks/before.xml")
    @ExpectedDatabase(value = "/controller/create-tasks/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun createTasks() {
        val requestBuilder = MockMvcRequestBuilders.post("/tasks/create")
            .contentType(MediaType.APPLICATION_JSON)
            .content(FileContentUtils.getFileContent("controller/create-tasks/request.json"))

        val content: List<AdminLocsDto> = mapper.readValue(
            FileContentUtils.getFileContent("controller/create-tasks/get-admin-locs.json"),
            object : TypeReference<List<AdminLocsDto>>(){})

        Mockito.doReturn(GetAdminLocsResponse(20, 0, 6, content))
            .`when`(coreClient).selectLocs(
                sort = anyString(),
                order = anyString(),
                filter = anyString(),
                limit = anyString(),
                offset = anyString()
            )

        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/create-tasks/before.xml")
    @ExpectedDatabase(value = "/controller/create-tasks/after-by-locs.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun createTasksByLocs() {
        val requestBuilder = MockMvcRequestBuilders.post("/tasks/create")
            .contentType(MediaType.APPLICATION_JSON)
            .content(FileContentUtils.getFileContent("controller/create-tasks/by-locs-request.json"))

        val content: List<AdminLocsDto> = mapper.readValue(
            FileContentUtils.getFileContent("controller/create-tasks/get-admin-locs.json"),
            object : TypeReference<List<AdminLocsDto>>(){})

        Mockito.doReturn(GetAdminLocsResponse(20, 0, 6, content))
            .`when`(coreClient).selectLocs(
                sort = anyString(),
                order = anyString(),
                filter = anyString(),
                limit = anyString(),
                offset = anyString()
            )

        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun createTasksEmptyRequest() {
        val requestBuilder = MockMvcRequestBuilders.post("/tasks/create")
            .contentType(MediaType.APPLICATION_JSON)
            .content(FileContentUtils.getFileContent("controller/create-tasks/empty-request.json"))

        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/create-tasks/empty-req-response.json"
                    ),
                    true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/cancel-tasks/before.xml")
    @ExpectedDatabase(value = "/controller/cancel-tasks/2/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun cancelTasksOK() {
        val requestBuilder = MockMvcRequestBuilders.put("/tasks/cancel")
            .contentType(MediaType.APPLICATION_JSON)
            .content(FileContentUtils.getFileContent("controller/cancel-tasks/2/request.json"))

        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/cancel-tasks/2/response.json"
                    ),
                    true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/cancel-tasks/before.xml")
    @ExpectedDatabase(value = "/controller/cancel-tasks/1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun cancelTasksCompleteWithErrors() {
        val requestBuilder = MockMvcRequestBuilders.put("/tasks/cancel")
            .contentType(MediaType.APPLICATION_JSON)
            .content(FileContentUtils.getFileContent("controller/cancel-tasks/1/request.json"))

        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/cancel-tasks/1/response.json"
                    ),
                    true
                )
            )
    }

    @Test
    fun cancelTasksEmptyRequest() {
        val requestBuilder = MockMvcRequestBuilders.put("/tasks/cancel")
            .contentType(MediaType.APPLICATION_JSON)
            .content(FileContentUtils.getFileContent("controller/cancel-tasks/3/empty-request.json"))

        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/cancel-tasks/3/response.json"
                    ),
                    true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/get-tasks/before.xml")
    fun getTasksWithDateFilterWithWarehouseTimeZone() {
        val requestBuilder = MockMvcRequestBuilders.get("/tasks")
            .param("filter", "inventDate=='2022-07-07 03:00:00'")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/get-tasks/date-filter-response.json"
                    ),
                    true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/get-tasks/before.xml")
    fun getTasksWithComplexFilterAndSorting() {
        val requestBuilder = MockMvcRequestBuilders.get("/tasks")
            .param("filter", "groupId==2;externalTaskId=in=(110,120)")
            .param("sort", "loc")
            .param("order", "DESC")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/get-tasks/complex-filter-response.json"
                    ),
                    true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/get-tasks/before.xml")
    fun getTasksHasDiscrepancies() {
        val requestBuilder = MockMvcRequestBuilders.get("/tasks")
            .param("filter", "hasDiscrepancies==true")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/get-tasks/has-discr-response.json"
                    ),
                    true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/recreate-tasks/before.xml")
    @ExpectedDatabase(value = "/controller/recreate-tasks/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun recreateTasks() {
        val requestBuilder = MockMvcRequestBuilders.post("/tasks/recreate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(FileContentUtils.getFileContent("controller/recreate-tasks/request.json"))

        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/recreate-tasks/response.json"
                    ),
                    false
                )
            )
    }
}
