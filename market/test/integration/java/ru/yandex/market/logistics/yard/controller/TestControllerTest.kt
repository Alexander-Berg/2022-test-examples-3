package ru.yandex.market.logistics.yard.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.util.FileContentUtils

class TestControllerTest : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup("classpath:fixtures/controller/test/create-trip-info/before.xml")
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/test/create-trip-info/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testCreateTripInfo() {
        val content =
            FileContentUtils.getFileContent("classpath:fixtures/controller/test/create-trip-info/request.json")

        mockMvc.perform(
            MockMvcRequestBuilders.post("/test/create-trip-info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content)
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }


    @Test
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/test/create-shop-request-info/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testCreateShopRequestInfo() {

        val content =
            FileContentUtils.getFileContent(
                "classpath:fixtures/controller/test/create-shop-request-info/request.json"
            )
        mockMvc.perform(
            MockMvcRequestBuilders.post("/test/create-shop-request-info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content)
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)

    }
}
