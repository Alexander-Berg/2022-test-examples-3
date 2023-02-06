package ru.yandex.market.logistics.calendaring.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.util.getFileContent

/**
 * Функциональный тест для IDM контроллера
 */
internal class IdmControllerTest : AbstractContextualTest() {
    @Test
    @DatabaseSetup(value = [
        "classpath:fixtures/security/idm-roles.xml",
        "classpath:fixtures/repository/user-repository/before.xml"
    ])
    @ExpectedDatabase(value = "classpath:fixtures/repository/user-repository/before.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    @Throws(java.lang.Exception::class)
    fun shouldReturnAllExistedRoles() {
        mockMvc!!.perform(
            MockMvcRequestBuilders.get("/idm/info/")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json(getFileContent("fixtures/controller/idm/info-result.json")))
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/user-repository/before.xml"])
    @ExpectedDatabase(value = "classpath:fixtures/repository/user-repository/before.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    @Throws(java.lang.Exception::class)
    fun shouldReturnListOfUsersWithRoles() {
        mockMvc!!.perform(MockMvcRequestBuilders.get("/idm/get-all-roles/")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json(getFileContent("fixtures/controller/idm/roles-result.json")))
    }

    @Test
    @DatabaseSetup(value = [
        "classpath:fixtures/security/idm-roles.xml",
        "classpath:fixtures/repository/user-repository/before.xml",
    ])
    @ExpectedDatabase(value = "classpath:fixtures/repository/user-repository/after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @Throws(java.lang.Exception::class)
    fun shouldAddNewRole() {
        mockMvc!!.perform(
            MockMvcRequestBuilders.post("/idm/add-role/")
                .content("login=" + "TEST_LOGIN_2" + "&role={\"role\":\"request_calendaring_147\"}")
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json(getFileContent("fixtures/controller/idm/ok-result.json")))
    }

    @Test
    @DatabaseSetup(value = [
        "classpath:fixtures/security/idm-roles.xml",
        "classpath:fixtures/repository/user-repository/before-delete.xml"
    ])
    @ExpectedDatabase(value = "classpath:fixtures/repository/user-repository/after-delete.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @Throws(java.lang.Exception::class)
    fun shouldRemoveRole() {
        mockMvc!!.perform(
            MockMvcRequestBuilders.post("/idm/remove-role/")
                .content("login=" + "TEST_LOGIN_2" + "&role={\"role\":\"request_calendaring_147\"}")
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json(getFileContent("fixtures/controller/idm/ok-result.json")))
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun shouldReturnErrorWhenExceptionHappen() {
        mockMvc!!.perform(
            MockMvcRequestBuilders.post("/idm/add-role/")
                .content("login=" + "TEST_LOGIN_2" + "&role={bad_json}")
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json(getFileContent("fixtures/controller/idm/error-result.json")))
    }

    @Test
    @DatabaseSetup("classpath:fixtures/security/idm-roles.xml")
    @Throws(java.lang.Exception::class)
    fun shouldReturnOkWhenTryingDeleteNonExistedRole() {
        mockMvc!!.perform(
            MockMvcRequestBuilders.post("/idm/remove-role/")
                .content("login=" + "TEST_LOGIN_2" + "&role={\"role\":\"request_calendaring_147\"}")
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json(getFileContent("fixtures/controller/idm/ok-result.json")))
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun shouldReturnErrorWhenTryingAddWrongRole() {
        mockMvc!!.perform(
            MockMvcRequestBuilders.post("/idm/remove-role/")
                .content("login=" + "TEST_LOGIN_2" + "&role={\"role\":\"bad_role\"}")
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json(getFileContent("fixtures/controller/idm/error-result.json")))
    }
}
