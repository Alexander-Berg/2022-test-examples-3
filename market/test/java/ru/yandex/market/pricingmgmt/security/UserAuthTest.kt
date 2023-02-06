package ru.yandex.market.pricingmgmt.security

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest
import ru.yandex.market.pricingmgmt.util.configurers.BlackBoxConfigurer
import javax.servlet.http.Cookie

@AutoConfigureMockMvc
class UserAuthTest : AbstractFunctionalTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var blackBoxConfigurer: BlackBoxConfigurer

    companion object {
        private val TEST_LOGIN = "testLogin"

        private val ENDPOINT_DEPARTMENTS = "/api/v1/departments/"
        private val ENDPOINT_AUTH_CONTEXT = "/api/v1/security/get-context"
    }

    @Test
    @DbUnitDataSet(before = ["UserAuthTest.role-in-db.csv"])
    fun testAuth_allConditionsMatched_Successful() {
        mockBlackBoxResponse(TEST_LOGIN, "OK")

        performTest(ENDPOINT_DEPARTMENTS).andExpect(status().isOk())
    }

    @Test
    @DbUnitDataSet(before = ["UserAuthTest.role-in-db.csv"])
    fun testAuth_unknownLogin_Forbidden() {
        mockBlackBoxResponse("fake_login", "OK")

        performTest(ENDPOINT_DEPARTMENTS).andExpect(status().isForbidden())
    }

    @Test
    @DbUnitDataSet(before = ["UserAuthTest.role-in-db.csv"])
    fun testAuth_blackBoxReturnedError_Forbidden() {
        mockBlackBoxResponse(TEST_LOGIN, "horrible error")

        performTest(ENDPOINT_DEPARTMENTS).andExpect(status().isForbidden())
    }

    @Test
    @DbUnitDataSet(before = ["UserAuthTest.role-not-mapped.csv"])
    fun testAuth_roleNotMapped_Forbidden() {
        mockBlackBoxResponse(TEST_LOGIN, "OK")

        performTest(ENDPOINT_DEPARTMENTS).andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Нет доступа к интерфейсу управления ценами"))
            .andExpect(jsonPath("$.rolesMissing.length()").value(1))
            .andExpect(jsonPath("$.rolesMissing[0].name").value("ROLE_PRICING_MGMT_ACCESS"))
            .andExpect(jsonPath("$.rolesMissing[0].description").value("Доступ к интерфейсу управления ценами"))
    }

    @Test
    @DbUnitDataSet(before = ["UserAuthTest.no-role-user-mapping.csv"])
    fun testAuth_noRoleUserMapping_Forbidden() {
        mockBlackBoxResponse(TEST_LOGIN, "OK")

        performTest(ENDPOINT_DEPARTMENTS).andExpect(status().isForbidden())
    }

    @Test
    @DbUnitDataSet(before = ["UserAuthTest.user-has-no-roles.csv"])
    fun getAuthContext_userHasNoRoles_Successful() {
        mockBlackBoxResponse(TEST_LOGIN, "OK")

        performTest(ENDPOINT_AUTH_CONTEXT)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user_id").value(1))
            .andExpect(jsonPath("$.login").value("testLogin"))
            .andExpect(jsonPath("$.roles.length()").value(0))
    }

    private fun performTest(endpoint: String): ResultActions {
        return mockMvc.perform(
            get(endpoint)
                .cookie(Cookie("Session_id", "123"))
        )
    }

    private fun mockBlackBoxResponse(login: String, error: String) {
        blackBoxConfigurer.mockBlackBoxResponse(
            """<?xml version="1.0" encoding="UTF-8"?>
                    <doc>
                      <age>207</age>
                      <expires_in>7775793</expires_in>
                      <ttl>5</ttl>
                      <error>${error}</error>
                      <status id="0">VALID</status>
                      <uid hosted="0">123456789</uid>
                      <login>${login}</login>
                      <karma confirmed="0">0</karma>
                      <karma_status>0</karma_status>
                      <auth>
                        <password_verification_age>207</password_verification_age>
                      </auth>
                      <connection_id>s:7878787878:_DDDDERF3454SDDD:8b</connection_id>
                    </doc>
                """
        )
    }
}
