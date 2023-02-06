package ru.yandex.market.logistics.yard.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.service.UserService
import ru.yandex.market.logistics.yard.util.FileContentUtils

class ServiceControllerTest : AbstractSecurityMockedContextualTest() {

    @MockBean
    val userService: UserService? = null

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/service/full_info_before.xml"])
    fun getAllServicesWithCapacityUnits() {
        Mockito.`when`(userService!!.getPrincipalLogin()).thenReturn("test_login_1")

        val expected = FileContentUtils.getFileContent(
            "classpath:fixtures/controller/service-controller/response.json"
        )

        mockMvc.perform(
            MockMvcRequestBuilders.get("/services")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.content().json(expected))
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/service/full_info_before.xml"])
    fun getServiceByIdWithCapacityUnits() {
        Mockito.`when`(userService!!.getPrincipalLogin()).thenReturn("test_login_1")

        val expected = FileContentUtils.getFileContent(
            "classpath:fixtures/controller/service-controller/response_with_single_service.json"
        )

        mockMvc.perform(
            MockMvcRequestBuilders.get("/services/2")
        )
            .andExpect(MockMvcResultMatchers.content().json(expected))
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/service/full_info_before.xml"])
    fun getServicesWithUrls() {
        val expected = FileContentUtils.getFileContent(
            "classpath:fixtures/controller/service-controller/response_with_single_service_and_url.json"
        )

        mockMvc.perform(
            MockMvcRequestBuilders.get("/servicesWithUrls/1,2")
        )
            .andExpect(MockMvcResultMatchers.content().json(expected))
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/service/full_info_before_with_login.xml"])
    fun getServiceByIdWithCapacityUnitsForLoggedIn() {
        Mockito.`when`(userService!!.getPrincipalLogin()).thenReturn("test_login")

        val expected = FileContentUtils.getFileContent(
            "classpath:fixtures/controller/service-controller/response_with_login.json"
        )

        mockMvc.perform(
            MockMvcRequestBuilders.get("/services")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.content().json(expected))
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/service/full_info_before_with_login.xml"])
    fun getServiceByIdWithCapacityUnitsWithoutLogin() {
        Mockito.`when`(userService!!.getPrincipalLogin()).thenReturn("test_login_1")

        val expected = FileContentUtils.getFileContent(
            "classpath:fixtures/controller/service-controller/response_without_login.json"
        )

        mockMvc.perform(
            MockMvcRequestBuilders.get("/services")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.content().json(expected))
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/service/update_params_before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/service/update_params_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateServicesParams() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/services/params")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent(
                    "classpath:fixtures/repository/service/update_params_request.json")))
            .andExpect(MockMvcResultMatchers.status().isOk)
    }
}
