package ru.yandex.market.wms.consolidation.modules.consolidation.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.consolidation.modules.consolidation.dao.ConsolidationDao
import ru.yandex.market.wms.consolidation.modules.consolidation.dao.ConsolidationUserDao

class TaskControllerTest : IntegrationTest() {

    @SpyBean
    @Autowired
    private lateinit var consolidationDao: ConsolidationDao

    @SpyBean
    @Autowired
    private lateinit var consolidationUserDao: ConsolidationUserDao

    @Test
    @DatabaseSetup("/tasks/get/before.xml")
    fun getTasks() {
        mockMvc.perform(MockMvcRequestBuilders.get("/tasks/"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(FileContentUtils.getFileContent("tasks/get/response.json"), false)
            )
    }

    @Test
    @DatabaseSetup("/tasks/put/before.xml")
    @ExpectedDatabase(
        value = "/tasks/put/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun assignTasks() {
        mockMvc.perform(
            MockMvcRequestBuilders.put("/tasks/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("tasks/put/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andReturn()
    }

    @Test
    @DatabaseSetup("/tasks/put/before-error.xml")
    @ExpectedDatabase(
        value = "/tasks/put/after-error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun assignTasksWhenUserIsOnDifferentPutWall() {
        mockMvc.perform(
            MockMvcRequestBuilders.put("/tasks/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("tasks/put/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()
    }

    @Test
    @DatabaseSetup("/tasks/before.xml")
    fun assignAndGetTasks() {
        mockMvc.perform(
            MockMvcRequestBuilders.put("/tasks/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("tasks/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()
        mockMvc.perform(MockMvcRequestBuilders.get("/tasks?users=sof-test,sof-test3"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(FileContentUtils.getFileContent("tasks/response.json"), false)
            )
    }

}
