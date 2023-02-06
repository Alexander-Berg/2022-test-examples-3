package ru.yandex.market.wms.core.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils

class SupportControllerTest : IntegrationTest() {

    @Test
    @DatabaseSetup("/controller/support/write-off/before.xml")
    @ExpectedDatabase("/controller/support/write-off/after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun writeOffSomeSerialNumbers() {
        mockMvc.perform(MockMvcRequestBuilders.post("/support/write-off")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/support/write-off/request.json")))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }
}
