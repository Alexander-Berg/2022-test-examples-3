package ru.yandex.market.logistics.yard.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.util.FileContentUtils

class PassControllerTest : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup("classpath:fixtures/configurator/controller/pass/before.xml")
    @ExpectedDatabase(
        value = "classpath:fixtures/configurator/controller/pass/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testIssue() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/pass/service/1/pass-issue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("classpath:fixtures/configurator/controller/pass/issue-request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    /**
    Кейс когда валидный пропуск есть. Должен вернуть только он.
     */
    @Test
    @DatabaseSetup("classpath:fixtures/configurator/controller/pass/search/1/before.xml")
    fun testSearchPassExist() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/pass/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"warehouseId\":1, \"shopId\":123, \"requestId\":111, \"date\":\"2022-01-26\"}")
        )
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent("classpath:fixtures/configurator/controller/pass/search/1/response.json")
                )
            )
    }

    /**
    Кейс когда валидного пропуска нет, но дата заявки не просрочена.
    В таком случае поиск должен вернуть все пропуска в день заявки.
     */
    @Test
    @DatabaseSetup("classpath:fixtures/configurator/controller/pass/search/2/before.xml")
    fun testSearchPassDoesntExistButDateIsValid() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/pass/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"warehouseId\":1, \"shopId\":123, \"requestId\":112, \"date\":\"2020-01-01\"}")
        )
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent("classpath:fixtures/configurator/controller/pass/search/2/response.json")
                )
            )
    }


    /**
    Кейс когда валидного пропуска нет, и дата заявки просрочена.
    В таком случае поиск должен вернуть все пропуска с таким shopId за 2х недельный период с сегодняшнего дня.
     */
    @Test
    @DatabaseSetup("classpath:fixtures/configurator/controller/pass/search/3/before.xml")
    fun testSearchPassDoesntExistAndInvalid() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/pass/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"warehouseId\":1, \"shopId\":123, \"requestId\":112, \"date\":\"2019-12-31\"}")
        )
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent("classpath:fixtures/configurator/controller/pass/search/3/response.json")
                )
            )
    }

    @Test
    @DatabaseSetup("classpath:fixtures/configurator/controller/pass/search/before.xml")
    fun testGetByUuid() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/pass/5c37b723-e65c-4e74-a4bf-959eebf7a701")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent("classpath:fixtures/configurator/controller/pass/search/response-by-uuid.json")
                )
            )
    }

    @Test
    @DatabaseSetup("classpath:fixtures/configurator/controller/pass/link-pass-and-request/before.xml")
    @ExpectedDatabase(
        value = "classpath:fixtures/configurator/controller/pass/link-pass-and-request/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testLinkPassAndRequest() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/pass/link-pass-and-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"passId\": 1,\"requestId\": 6517068}")
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup("classpath:fixtures/configurator/controller/pass/search/1/before.xml")
    fun testSearchPassAllExist() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/pass/search-all")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"requestsIds\":[111,222]}")
        )
            .andExpect(MockMvcResultMatchers.content().string("{\"222\":null,\"111\":{\"id\":1,\"name\":\"Test Testov\",\"externalId\":null,\"yardClientId\":1,\"createdAt\":\"2020-01-01T20:00:00\",\"licencePlate\":\"A1212AA\",\"truck\":false,\"timeToArrive\":\"2020-01-01T21:00:00\",\"deadline\":\"2020-01-02T21:00:00\",\"passNumber\":null,\"comment\":null}}"))
    }

}
