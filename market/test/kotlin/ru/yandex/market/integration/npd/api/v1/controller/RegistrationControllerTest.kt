@file:Suppress("UNCHECKED_CAST")

package ru.yandex.market.integration.npd.api.v1.controller

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.common.retrofit.ExecuteCall
import ru.yandex.market.common.retrofit.RetryStrategy
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.integration.npd.AbstractFunctionalTest
import ru.yandex.market.integration.npd.service.FnsErrorCode
import ru.yandex.mj.generated.client.fns_integration_client.model.BindByPhoneRequest
import ru.yandex.mj.generated.client.fns_integration_client.model.BindResponse
import ru.yandex.mj.generated.client.fns_integration_client.model.BindResponseResponse
import ru.yandex.mj.generated.client.fns_integration_client.model.BindStatusRequest
import ru.yandex.mj.generated.client.fns_integration_client.model.BindStatusResponse
import ru.yandex.mj.generated.client.fns_integration_client.model.BindStatusResponseResponse
import ru.yandex.mj.generated.client.fns_integration_client.model.GetPermissionsRequest
import ru.yandex.mj.generated.client.fns_integration_client.model.GetPermissionsResponse
import ru.yandex.mj.generated.client.fns_integration_client.model.GetPermissionsResponseResponse
import ru.yandex.mj.generated.client.fns_integration_client.model.Permissions
import java.util.concurrent.CompletableFuture

class RegistrationControllerTest : AbstractFunctionalTest() {

    @BeforeEach
    fun before() {
        Mockito.reset(client)
    }

    @Test
    @DbUnitDataSet(after = ["RegistrationApi.createRequest.after.csv"])
    fun testCreateRequest() {
        val expectedFnsRequestId = "123456789"

        //language=json
        val expectedJson = "" +
            "{\n" +
            "  \"partnerId\": 222,\n" +
            "  \"phone\": \"79991122333\",\n" +
            "  \"status\": \"PENDING\",\n" +
            "  \"fnsRequestId\":  \"123456789\"\n" +
            "}"

        val bindByPhoneCallMock = Mockito.mock(ExecuteCall::class.java) as ExecuteCall<BindResponse, RetryStrategy>
        Mockito.`when`(bindByPhoneCallMock.schedule())
            .thenReturn(CompletableFuture.completedFuture(
                BindResponse().response(BindResponseResponse().requestId(expectedFnsRequestId))
            ))
        Mockito.`when`(client.bindByPhone(ArgumentMatchers.any(BindByPhoneRequest::class.java)))
            .thenReturn(bindByPhoneCallMock)

        val bindStatusCallMock = Mockito.mock(ExecuteCall::class.java) as ExecuteCall<BindStatusResponse, RetryStrategy>
        Mockito.`when`(bindStatusCallMock.schedule())
            .thenReturn(CompletableFuture.completedFuture(
                BindStatusResponse().response(
                    BindStatusResponseResponse()
                        .resultCode(BindStatusResponseResponse.ResultCodeEnum.IN_PROGRESS)
                        .inn("123456789")
                )
            ))
        Mockito.`when`(client.bindStatus(ArgumentMatchers.any(BindStatusRequest::class.java)))
            .thenReturn(bindStatusCallMock)

        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/v1/partners/222/application")
                .param("uid", "111")
                .contentType("application/json")
                .content("{ \"phone\": \"79991122333\" }")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expectedJson))
    }

    @Test
    @DbUnitDataSet(after = ["RegistrationApi.createRequest.error.csv"])
    fun testCreateRequestErrorFnsRequest() {
        //language=json
        val expectedJson = "" +
            "{\n" +
            "  \"partnerId\": 222,\n" +
            "  \"phone\": \"79991122333\",\n" +
            "  \"status\": \"NEW\"\n" +
            "}"

        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/v1/partners/222/application")
                .param("uid", "111")
                .contentType("application/json")
                .content("{ \"phone\": \"79991122333\" }")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expectedJson))
    }

    @Test
    @DbUnitDataSet(
        before = ["RegistrationApi.createRequest.existed.csv"],
        after = ["RegistrationApi.createRequest.existed.csv"]
    )
    fun testCreateRequestWithExisted() {

        //language=json
        val expectedJson = "" +
            "{\n" +
            "  \"partnerId\": 222,\n" +
            "  \"phone\": \"79991122333\",\n" +
            "  \"status\": \"PENDING\",\n" +
            "  \"fnsRequestId\":  \"123456789\"\n" +
            "}"
        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/v1/partners/222/application")
                .param("uid", "111")
                .contentType("application/json")
                .content("{ \"phone\": \"79991122333\" }")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expectedJson))
    }

    @Test
    @DbUnitDataSet(
        before = ["RegistrationApi.createRequest.existed.csv"]
    )
    fun testCreateRequestWithExistedAndDifferentPhone() {

        //language=json
        val expectedJson = "" +
            "{\n" +
            "  \"partnerId\": 222,\n" +
            "  \"phone\": \"79990001122\",\n" +
            "  \"status\": \"NEW\",\n" +
            "  \"fnsRequestId\":  null\n" +
            "}"
        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/v1/partners/222/application")
                .param("uid", "111")
                .contentType("application/json")
                .content("{ \"phone\": \"79990001122\" }")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expectedJson))
    }

    @Test
    @DbUnitDataSet(before = ["RegistrationApi.createRequest.after.csv"])
    fun testGetApplication() {
        //language=json
        val expectedJson = "" +
            "{\n" +
            "  \"partnerId\": 222,\n" +
            "  \"phone\": \"79991122333\",\n" +
            "  \"status\": \"PENDING\",\n" +
            "  \"inn\": \"123456789\",\n" +
            "  \"fnsRequestId\": \"123456789\"\n" +
            "}"

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/partners/222/application")
                .contentType("application/json")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expectedJson))
    }

    @Test
    @DbUnitDataSet(before = ["RegistrationApi.getFullApplication.after.csv"])
    fun testFullGetApplication() {
        //language=json
        val expectedJson = "" +
            "{\n" +
            "  \"partnerId\": 222,\n" +
            "  \"phone\": \"7999112233\",\n" +
            "  \"status\": \"PENDING\",\n" +
            "  \"inn\": \"123456789\",\n" +
            "  \"fnsRequestId\": \"123456789\",\n" +
            "  \"createdDate\":\"2022-05-16T21:00:00Z\"," +
            "  \"updatedDate\":\"2022-05-16T21:00:00Z\"" +
            "}"

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/partners/222/application")
                .contentType("application/json")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expectedJson))
    }

    @Test
    fun testGetApplicationNotFound() {
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
            MockMvcRequestBuilders.get("/api/v1/partners/222/application")
                .contentType("application/json")
        )
            .andExpect(status().isNotFound)
            .andExpect(content().json(expectedJson))
    }

    @Test
    @DbUnitDataSet(
        before = ["RegistrationApi.actualizeApplication.before.csv"],
        after = ["RegistrationApi.actualizeApplication.after.csv"]
    )
    fun testActualizeApplication() {
        val expected = BindStatusResponse()
        val response = BindStatusResponseResponse()
        response.resultCode = BindStatusResponseResponse.ResultCodeEnum.COMPLETED
        response.inn = "123456789"
        expected.response = response
        val callMock: ExecuteCall<BindStatusResponse, RetryStrategy> = Mockito.mock(ExecuteCall::class.java)
            as ExecuteCall<BindStatusResponse, RetryStrategy>
        Mockito.`when`(callMock.schedule()).thenReturn(CompletableFuture.completedFuture(expected))
        Mockito.`when`(client.bindStatus(ArgumentMatchers.any(BindStatusRequest::class.java))).thenReturn(callMock)

        //language=json
        val expectedJson = "" +
            "{\n" +
            "  \"partnerId\": 222,\n" +
            "  \"inn\": \"123456789\",\n" +
            "  \"phone\": \"7999112233\",\n" +
            "  \"status\": \"DONE\"\n" +
            "}"

        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/v1/partners/222/application/actualize")
                .param("uid", "111")
                .contentType("application/json")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expectedJson))
    }

    @Test
    @DbUnitDataSet(
        before = ["RegistrationApi.createRequest.error.csv"],
        after = ["RegistrationApi.createRequest.after.csv"]
    )
    fun testActualizeApplicationIfWasErrorWhileCreation() {
        val expectedFnsRequestId = "123456789"
        val bindByPhoneCallMock = Mockito.mock(ExecuteCall::class.java) as ExecuteCall<BindResponse, RetryStrategy>
        Mockito.`when`(bindByPhoneCallMock.schedule())
            .thenReturn(CompletableFuture.completedFuture(
                BindResponse().response(BindResponseResponse().requestId(expectedFnsRequestId))
            ))
        Mockito.`when`(client.bindByPhone(ArgumentMatchers.any(BindByPhoneRequest::class.java)))
            .thenReturn(bindByPhoneCallMock)

        val bindStatusCallMock = Mockito.mock(ExecuteCall::class.java) as ExecuteCall<BindStatusResponse, RetryStrategy>
        Mockito.`when`(bindStatusCallMock.schedule())
            .thenReturn(CompletableFuture.completedFuture(
                BindStatusResponse().response(
                    BindStatusResponseResponse()
                        .resultCode(BindStatusResponseResponse.ResultCodeEnum.IN_PROGRESS)
                        .inn("123456789")
                )
            ))
        Mockito.`when`(client.bindStatus(ArgumentMatchers.any(BindStatusRequest::class.java)))
            .thenReturn(bindStatusCallMock)

        //language=json
        val expectedJson = "" +
            "{\n" +
            "  \"partnerId\": 222,\n" +
            "  \"inn\": \"123456789\",\n" +
            "  \"phone\": \"79991122333\",\n" +
            "  \"status\": \"PENDING\"\n" +
            "}"

        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/v1/partners/222/application/actualize")
                .param("uid", "111")
                .contentType("application/json")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expectedJson))
    }

    @Test
    @DbUnitDataSet(
        before = ["RegistrationApi.actualizeDoneApplication.before.csv"],
        after = ["RegistrationApi.actualizeDoneApplication.after.csv"]
    )
    fun testActualizeApplicationInDoneNoPermissions() {
        val getPermissionsCallMock = Mockito.mock(ExecuteCall::class.java)
            as ExecuteCall<GetPermissionsResponse, RetryStrategy>
        Mockito.`when`(getPermissionsCallMock.schedule())
            .thenReturn(CompletableFuture.completedFuture(
                GetPermissionsResponse().errorCode(FnsErrorCode.PLATFORM_ERROR_TAXPAYER_UNBOUND)
            ))
        Mockito.`when`(client.getPermissions(ArgumentMatchers.any(GetPermissionsRequest::class.java)))
            .thenReturn(getPermissionsCallMock)


        //language=json
        val expectedJson = "" +
            "{\n" +
            "  \"partnerId\": 222,\n" +
            "  \"inn\": \"1234567\",\n" +
            "  \"phone\": \"7999112233\",\n" +
            "  \"status\": \"ERROR\"\n" +
            "}"

        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/v1/partners/222/application/actualize")
                .param("uid", "111")
                .contentType("application/json")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expectedJson))
    }

    @Test
    @DbUnitDataSet(
        before = ["RegistrationApi.actualizeDoneApplication.before.csv"],
        after = ["RegistrationApi.actualizeDoneApplication.before.csv"]
    )
    fun testActualizeApplicationInDone() {
        val getPermissionsCallMock = Mockito.mock(ExecuteCall::class.java)
            as ExecuteCall<GetPermissionsResponse, RetryStrategy>
        Mockito.`when`(getPermissionsCallMock.schedule())
            .thenReturn(CompletableFuture.completedFuture(
                GetPermissionsResponse().response(
                    GetPermissionsResponseResponse().permissions(listOf(Permissions.INCOME_LIST))
                )
            ))
        Mockito.`when`(client.getPermissions(ArgumentMatchers.any(GetPermissionsRequest::class.java)))
            .thenReturn(getPermissionsCallMock)


        //language=json
        val expectedJson = "" +
            "{\n" +
            "  \"partnerId\": 222,\n" +
            "  \"inn\": \"1234567\",\n" +
            "  \"phone\": \"7999112233\",\n" +
            "  \"status\": \"DONE\"\n" +
            "}"

        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/v1/partners/222/application/actualize")
                .param("uid", "111")
                .contentType("application/json")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expectedJson))
    }

    @Test
    @DbUnitDataSet(
        before = ["RegistrationApi.actualizeDoneApplication.before.csv"],
        after = ["RegistrationApi.actualizeDoneApplicationTaxPayerNotFound.after.csv"]
    )
    fun testActualizeApplicationInDoneTaxPayerNotFound() {
        val getPermissionsCallMock = Mockito.mock(ExecuteCall::class.java)
            as ExecuteCall<GetPermissionsResponse, RetryStrategy>
        Mockito.`when`(getPermissionsCallMock.schedule())
            .thenReturn(CompletableFuture.completedFuture(
                GetPermissionsResponse().errorCode(FnsErrorCode.PLATFORM_ERROR_TAXPAYER_UNREGISTERED)
            ))
        Mockito.`when`(client.getPermissions(ArgumentMatchers.any(GetPermissionsRequest::class.java)))
            .thenReturn(getPermissionsCallMock)


        //language=json
        val expectedJson = "" +
            "{\n" +
            "  \"partnerId\": 222,\n" +
            "  \"inn\": \"1234567\",\n" +
            "  \"phone\": \"7999112233\",\n" +
            "  \"status\": \"NOT_FOUND\"\n" +
            "}"

        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/v1/partners/222/application/actualize")
                .param("uid", "111")
                .contentType("application/json")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expectedJson))
    }

    @Test
    fun testNotFoundFns() {
        //language=json
        val expectedJson = "" +
            "{\n" +
            "  \"partnerId\": 222,\n" +
            "  \"phone\": \"79991122333\",\n" +
            "  \"status\": \"NOT_FOUND\",\n" +
            "  \"fnsRequestId\":  null\n" +
            "}"

        val bindByPhoneCallMock = Mockito.mock(ExecuteCall::class.java) as ExecuteCall<BindResponse, RetryStrategy>
        Mockito.`when`(bindByPhoneCallMock.schedule())
            .thenReturn(CompletableFuture.completedFuture(
                BindResponse().errorCode(FnsErrorCode.PLATFORM_ERROR_TAXPAYER_UNREGISTERED))
            )
        Mockito.`when`(client.bindByPhone(ArgumentMatchers.any(BindByPhoneRequest::class.java)))
            .thenReturn(bindByPhoneCallMock)

        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/v1/partners/222/application")
                .param("uid", "111")
                .contentType("application/json")
                .content("{ \"phone\": \"79991122333\" }")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expectedJson))
    }

    companion object {
        @JvmStatic
        fun phones() = listOf(
            Arguments.of("79991122333"),
            Arguments.of("+79991122333"),
            Arguments.of("+7 9991122333"),
            Arguments.of("+7 (999)1122333"),
            Arguments.of("+7 (999)-112-23-33")
        )
    }

    @ParameterizedTest()
    @MethodSource("phones")
    fun testFormatPhoneNumber(phone: String) {
        val expectedFnsRequestId = "123456789"

        //language=json
        val expectedJson = "" +
            "{\n" +
            "  \"partnerId\": 222,\n" +
            "  \"phone\": \"79991122333\",\n" +
            "  \"status\": \"PENDING\",\n" +
            "  \"fnsRequestId\":  \"123456789\"\n" +
            "}"

        val bindByPhoneCallMock = Mockito.mock(ExecuteCall::class.java) as ExecuteCall<BindResponse, RetryStrategy>
        Mockito.`when`(bindByPhoneCallMock.schedule())
            .thenReturn(CompletableFuture.completedFuture(
                BindResponse().response(BindResponseResponse().requestId(expectedFnsRequestId))
            ))
        Mockito.`when`(client.bindByPhone(ArgumentMatchers.any(BindByPhoneRequest::class.java)))
            .thenReturn(bindByPhoneCallMock)

        val bindStatusCallMock = Mockito.mock(ExecuteCall::class.java) as ExecuteCall<BindStatusResponse, RetryStrategy>
        Mockito.`when`(bindStatusCallMock.schedule())
            .thenReturn(CompletableFuture.completedFuture(
                BindStatusResponse().response(
                    BindStatusResponseResponse()
                        .resultCode(BindStatusResponseResponse.ResultCodeEnum.IN_PROGRESS)
                        .inn("123456789")
                )
            ))
        Mockito.`when`(client.bindStatus(ArgumentMatchers.any(BindStatusRequest::class.java)))
            .thenReturn(bindStatusCallMock)

        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/v1/partners/222/application")
                .param("uid", "111")
                .contentType("application/json")
                .content("{ \"phone\": \"$phone\" }")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expectedJson))
    }

    @Test
    fun testAlreadyBound() {
        val xml = "" +
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
            "\t<soap:Body>\n" +
            "\t\t<GetMessageResponse xmlns=\"urn://x-artefacts-gnivc-ru/inplat/servin/OpenApiAsyncMessageConsumerService/types/1.0\">\n" +
            "\t\t\t<ProcessingStatus>COMPLETED</ProcessingStatus>\n" +
            "\t\t\t<Message>\n" +
            "\t\t\t\t<SmzPlatformError xmlns=\"urn://x-artefacts-gnivc-ru/ais3/SMZ/SmzPartnersIntegrationService/types/1.0\" xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ns2=\"urn://x-artefacts-gnivc-ru/ais3/SMZ/SmzPartnersValidationService/types/1.0\">\n" +
            "\t\t\t\t\t<Code>TAXPAYER_ALREADY_BOUND</Code>\n" +
            "\t\t\t\t\t<Message>Налогоплательщик с ИНН 782800030897 уже привязан к партнеру</Message>\n" +
            "\t\t\t\t\t<Args>\n" +
            "\t\t\t\t\t\t<Key>INN</Key>\n" +
            "\t\t\t\t\t\t<Value>782800030897</Value>\n" +
            "\t\t\t\t\t</Args>\n" +
            "\t\t\t\t</SmzPlatformError>\n" +
            "\t\t\t</Message>\n" +
            "\t\t</GetMessageResponse>\n" +
            "\t</soap:Body>\n" +
            "</soap:Envelope>"

        //language=json
        val expectedJson = "" +
            "{\n" +
            "  \"partnerId\": 222,\n" +
            "  \"phone\": \"79991122333\",\n" +
            "  \"status\": \"DONE\",\n" +
            "  \"fnsRequestId\": null\n" +
            "}"

        val bindByPhoneCallMock = Mockito.mock(ExecuteCall::class.java) as ExecuteCall<BindResponse, RetryStrategy>
        Mockito.`when`(bindByPhoneCallMock.schedule())
            .thenReturn(CompletableFuture.completedFuture(
                BindResponse()
                    .errorCode(FnsErrorCode.PLATFORM_ERROR_TAXPAYER_ALREADY_BOUND)
                    .errorMessage("Ошибка платформы код:TAXPAYER_ALREADY_BOUND, сообщение:Налогоплательщик с ИНН 782800030897 уже привязан к партнеру")
                    .errorXml(xml)
            ))
        Mockito.`when`(client.bindByPhone(ArgumentMatchers.any(BindByPhoneRequest::class.java)))
            .thenReturn(bindByPhoneCallMock)

        val bindStatusCallMock = Mockito.mock(ExecuteCall::class.java) as ExecuteCall<BindStatusResponse, RetryStrategy>
        Mockito.`when`(bindStatusCallMock.schedule())
            .thenReturn(CompletableFuture.completedFuture(
                BindStatusResponse().response(
                    BindStatusResponseResponse()
                        .resultCode(BindStatusResponseResponse.ResultCodeEnum.IN_PROGRESS)
                        .inn("123456789")
                )
            ))
        Mockito.`when`(client.bindStatus(ArgumentMatchers.any(BindStatusRequest::class.java)))
            .thenReturn(bindStatusCallMock)

        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/v1/partners/222/application")
                .param("uid", "111")
                .contentType("application/json")
                .content("{ \"phone\": \"79991122333\" }")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expectedJson))
    }
}
