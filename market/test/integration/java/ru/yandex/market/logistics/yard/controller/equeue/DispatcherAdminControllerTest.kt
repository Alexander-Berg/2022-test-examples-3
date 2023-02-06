package ru.yandex.market.logistics.yard.controller.equeue

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.service.UserService

class DispatcherAdminControllerTest : AbstractSecurityMockedContextualTest() {

    @MockBean
    val userService: UserService? = null

    @BeforeEach
    fun setup() {
        Mockito.`when`(userService!!.getPrincipalLogin()).thenReturn("test_login")
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/admin/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/dispatcher/admin/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun logout() {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/window/1/logout"))
            .andExpect(MockMvcResultMatchers.content().json("{\"result\": \"Logout successful\"}"))
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/admin/before.xml"])
    fun logoutError() {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/window/2/logout"))
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/admin/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/dispatcher/admin/after_force.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun logoutForce() {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/window/2/logout?force=true"))
            .andExpect(MockMvcResultMatchers.content().json("{\"result\": \"Logout successful\"}"))
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

}
