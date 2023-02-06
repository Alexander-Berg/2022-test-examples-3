@file:Suppress("UNCHECKED_CAST")

package ru.yandex.market.integration.npd.api.v1.controller

import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.common.retrofit.ExecuteCall
import ru.yandex.market.common.retrofit.RetryStrategy
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.integration.npd.AbstractFunctionalTest
import ru.yandex.mj.generated.client.fns_integration_client.model.GetPermissionsRequest
import ru.yandex.mj.generated.client.fns_integration_client.model.GetPermissionsResponse
import ru.yandex.mj.generated.client.fns_integration_client.model.GetPermissionsResponseResponse
import ru.yandex.mj.generated.client.fns_integration_client.model.Permissions
import java.util.concurrent.CompletableFuture

class PartnerControllerTest : AbstractFunctionalTest() {

    @Test
    fun testPartnerNotFound() {
        //language=json
        val expectedJson = "" +
            "{\n" +
            "  \"errors\": [\n" +
            "    {\n" +
            "      \"message\": \"Partner 222 was not found\",\n" +
            "      \"details\": {\n" +
            "        \"partnerId\": 222\n" +
            "      },\n" +
            "      \"code\": \"NOT_FOUND\"\n" +
            "    }\n" +
            "  ]\n" +
            "}"

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/partners/222/permissions")
                .contentType("application/json")
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(MockMvcResultMatchers.content().json(expectedJson))
    }

    @Test
    @DbUnitDataSet(before = ["PartnerControllerTest.wrongApplication.before.csv"])
    fun testWrongApplicationStatus() {
        //language=json
        val expectedJson = "" +
            "{\n" +
            "  \"errors\": [\n" +
            "    {\n" +
            "      \"message\": \"Partner 222 has wrong application status\",\n" +
            "      \"details\": {\n" +
            "        \"partnerId\": 222,\n" +
            "        \"status\": \"PENDING\"\n" +
            "      },\n" +
            "      \"code\": \"BAD_REQUEST\"\n" +
            "    }\n" +
            "  ]\n" +
            "}"

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/partners/222/permissions")
                .contentType("application/json")
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.content().json(expectedJson))
    }

    @Test
    @DbUnitDataSet(before = ["PartnerControllerTest.getPermissions.before.csv"])
    fun testGetPermissions() {
        //language=json
        val expectedJson = "" +
            "{\n" +
            "  \"permissions\": [\n" +
            "    \"TAXPAYER_UPDATE\",\n" +
            "    \"INCOME_LIST\"\n" +
            "  ]\n" +
            "}"

        val getPermissionsCallMock = Mockito.mock(ExecuteCall::class.java) as ExecuteCall<GetPermissionsResponse, RetryStrategy>
        Mockito.`when`(getPermissionsCallMock.schedule())
            .thenReturn(CompletableFuture.completedFuture(
                GetPermissionsResponse().response(
                    GetPermissionsResponseResponse().permissions(listOf(
                        Permissions.TAXPAYER_UPDATE,
                        Permissions.INCOME_LIST
                    ))
                )
            ))
        Mockito.`when`(client.getPermissions(ArgumentMatchers.any(GetPermissionsRequest::class.java)))
            .thenReturn(getPermissionsCallMock)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/partners/222/permissions")
                .contentType("application/json")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json(expectedJson))
    }
}
