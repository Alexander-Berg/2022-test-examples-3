package ru.yandex.market.logistics.yard.configurator

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import ru.yandex.common.util.application.EnvironmentType
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.util.FileContentUtils

class GraphConfigurationControllerTest: AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup("classpath:fixtures/configurator/controller/create-graph/before.xml")
    @ExpectedDatabase("classpath:fixtures/configurator/controller/create-graph/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun successfulConfigure() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/graph/create")
            .contentType(MediaType.APPLICATION_JSON)
            .content(FileContentUtils.getFileContent("classpath:fixtures/configurator/controller/create-graph/request.json")))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup("classpath:fixtures/configurator/controller/create-graph/before.xml")
    @ExpectedDatabase("classpath:fixtures/configurator/controller/create-graph/after-without-params.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun successfulConfigureWithRestrictionsActionsPriorityFunctionsWithoutParams() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/graph/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent(
                    "classpath:fixtures/configurator/controller/create-graph/request-without-params.json")))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup("classpath:fixtures/configurator/controller/create-graph/before.xml")
    @ExpectedDatabase("classpath:fixtures/configurator/controller/create-graph/after-fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun failWhenHasSameNameCapacities() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/graph/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent(
                    "classpath:fixtures/configurator/controller/create-graph/request-with-same-name-capacities.json")))
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(content().string("{\"message\":\"There are 2 or more capacities with same name\"}"))
    }

    @Test
    @DatabaseSetup("classpath:fixtures/configurator/controller/create-graph/before.xml")
    @ExpectedDatabase("classpath:fixtures/configurator/controller/create-graph/after-fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun failWhenIncorrectInitialState() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/graph/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent(
                    "classpath:fixtures/configurator/controller/create-graph/request-with-incorrect-initial-state.json")))
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(content().string("{\"message\":\"Initial state is incorrect\"}"))
    }

    @Test
    @DatabaseSetup("classpath:fixtures/configurator/controller/get-by-serviceId/before.xml")
    fun testGetService() {
        val expected = FileContentUtils.getFileContent(
            "classpath:fixtures/configurator/controller/get-by-serviceId/response.json"
        )
        mockMvc.perform(
            MockMvcRequestBuilders.get("/graph/service/123")
                .contentType(MediaType.APPLICATION_JSON)
                )
            .andExpect(content().json(expected))
    }

    @Test
    @DatabaseSetup("classpath:fixtures/configurator/controller/get-graph-template/before.xml")
    fun testGetGraphTemplate() {
        val expected = FileContentUtils.getFileContent(
            "classpath:fixtures/configurator/controller/get-graph-template/response.json"
        )
        mockMvc.perform(
            MockMvcRequestBuilders.get("/graph/template/1")
        )
            .andExpect(content().json(expected))
    }

    @Test
    @DatabaseSetup("classpath:fixtures/configurator/controller/save-graph-template/before.xml")
    @ExpectedDatabase("classpath:fixtures/configurator/controller/save-graph-template/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun testSaveGraphTemplate() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/graph/template")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("classpath:fixtures/configurator/controller/save-graph-template/request.json")))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup("classpath:fixtures/configurator/controller/delete-graph/before.xml")
    @ExpectedDatabase(
        "classpath:fixtures/configurator/controller/delete-graph/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testDeleteService() {
        val environment = System.getProperty(EnvironmentType.YANDEX_ENVIRONMENT_TYPE_PROPERTY)
        try {
            System.setProperty(EnvironmentType.YANDEX_ENVIRONMENT_TYPE_PROPERTY, EnvironmentType.TESTING.value)
            mockMvc.perform(
                MockMvcRequestBuilders.delete("/graph/service/123")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
        } finally {
            if (environment == null) {
                System.clearProperty(EnvironmentType.YANDEX_ENVIRONMENT_TYPE_PROPERTY)
            } else {
                System.setProperty(EnvironmentType.YANDEX_ENVIRONMENT_TYPE_PROPERTY, environment)
            }
        }
    }

    @Test
    @DatabaseSetup("classpath:fixtures/configurator/controller/copy/before.xml")
    fun testCopy() {
        val expected = FileContentUtils.getFileContent(
            "classpath:fixtures/configurator/controller/copy/response.json"
        )
        mockMvc.perform(
            MockMvcRequestBuilders.get("/graph/copy/123/456")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/graph/service/456")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(content().json(expected))
    }

}
