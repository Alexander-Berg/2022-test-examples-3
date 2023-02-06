package ru.yandex.market.logistics.yard.controller.idm

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.util.FileContentUtils

class IdmV2Controller : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup("classpath:idm/v2/info/before.xml")
    fun testInfo() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/v2/idm/info/")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent("classpath:idm/v2/info/response.json")
                )
            )
    }

    @Test
    @DatabaseSetup("classpath:idm/v2/get-all-roles/before.xml")
    fun testGetAllRoles() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/v2/idm/get-all-roles/")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent("classpath:idm/v2/get-all-roles/response.json")
                )
            )
    }

    @Test
    @DatabaseSetup("classpath:idm/v2/add-role/before.xml")
    @ExpectedDatabase(
        value = "classpath:idm/v2/add-role/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testAddRole() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/v2/idm/add-role/")
                .content(
                    "login=" + "TEST_LOGIN_2" +
                        "&role={\"role\":\"admin\",\"service\":\"172\", \"capacity\":\"1130\" }"
                )
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andExpect(
                MockMvcResultMatchers.content().json("{\"code\":0}")
            )
    }

    @Test
    @DatabaseSetup("classpath:idm/v2/add-role/before.xml")
    @ExpectedDatabase(
        value = "classpath:idm/v2/add-role/all-services-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testAddRoleAllServices() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/v2/idm/add-role/")
                .content("login=" + "TEST_LOGIN_2" + "&role={\"role\":\"admin\", \"service\":\"All\"}")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andExpect(
                MockMvcResultMatchers.content().json("{\"code\":0}")
            )
    }

    @Test
    @DatabaseSetup("classpath:idm/v2/add-role/before.xml")
    @ExpectedDatabase(
        value = "classpath:idm/v2/add-role/all-capacities-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testAddRoleAllCapacities() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/v2/idm/add-role/")
                .content("login=" + "TEST_LOGIN_2" + "&role={\"role\":\"admin\",\"service\":\"172\",\"capacity\":\"All\" }")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andExpect(
                MockMvcResultMatchers.content().json("{\"code\":0}")
            )
    }

    @Test
    @DatabaseSetup(value = ["classpath:idm/v2/remove-role/before.xml"])
    @ExpectedDatabase(
        value = "classpath:idm/v2/remove-role/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun shouldRemoveRole() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/v2/idm/remove-role/")
                .content("login=" + "TEST_LOGIN_2" + "&role={\"role\":\"admin\"}")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andExpect(
                MockMvcResultMatchers.content().json("{\"code\":0}")
            )
    }

    @Test
    @DatabaseSetup(value = ["classpath:idm/v2/remove-role/before-with-capacity.xml"])
    @ExpectedDatabase(
        value = "classpath:idm/v2/remove-role/after-with-capacity.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testRemoveCapacityRole() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/v2/idm/remove-role/")
                .content("login=" + "TEST_LOGIN_2" + "&role={\"role\":\"admin\",\"service\":\"100\"}")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andExpect(
                MockMvcResultMatchers.content().json("{\"code\":0}")
            )
    }
}
