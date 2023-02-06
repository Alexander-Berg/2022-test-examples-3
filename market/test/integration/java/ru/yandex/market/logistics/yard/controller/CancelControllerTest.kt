package ru.yandex.market.logistics.yard.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.util.FileContentUtils

class CancelControllerTest : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup("classpath:fixtures/controller/cancel/before.xml")
    fun getAllAvailableForSearchServices() {
        val expected = FileContentUtils.getFileContent(
            "classpath:fixtures/controller/cancel/response.json"
        )
        mockMvc.perform(
            MockMvcRequestBuilders.get("/cancel/reasons")
        )
            .andExpect(MockMvcResultMatchers.content().json(expected))
    }

}
