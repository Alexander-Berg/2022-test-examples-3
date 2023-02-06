package ru.yandex.market.mapi.controller.validation

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import ru.yandex.market.common.retrofit.CommonRetrofitHttpExecutionException
import ru.yandex.market.mapi.AbstractMapiTest
import ru.yandex.market.mapi.client.antirobot.AuthenticateResponse
import ru.yandex.market.mapi.client.antirobot.NonceResponse
import ru.yandex.market.mapi.client.antirobot.ValidationServiceClient
import ru.yandex.market.mapi.core.MapiHeaders
import ru.yandex.market.mapi.core.util.JsonHelper
import ru.yandex.market.mapi.core.util.assertJson
import java.util.concurrent.CompletableFuture

internal class MobileValidationControllerTest : AbstractMapiTest() {

    @Autowired
    private lateinit var validationServiceClient: ValidationServiceClient

    @Test
    fun generateNonce() {
        mockGenerateNonceResponse("/client/antirobot/generateNonceResponse.json")

        val response = mvcCall(
            MockMvcRequestBuilders.post("/api/validation/generate_nonce")
                .header(MapiHeaders.HEADER_UUID, "fake_uuid")
                .header(MobileValidationController.HEADER_APP_ID, "fake_app_id")
        )

        assertJson(response, "/client/antirobot/generateNonceResponseParsed.json", "Response")
    }

    @Test
    fun generateNonceMissingUuid() {
        mockGenerateNonceResponse("/client/antirobot/generateNonceResponse.json")

        mvcCall(
            MockMvcRequestBuilders.post("/api/validation/generate_nonce")
                .header(MobileValidationController.HEADER_APP_ID, "fake_app_id"),
            expected = BAD_4XX,
            expectedType = null
        )
    }

    @Test
    fun generateNonceMissingAppId() {
        mockGenerateNonceResponse("/client/antirobot/generateNonceResponse.json")

        val response = mvcCall(
            MockMvcRequestBuilders.post("/api/validation/generate_nonce")
                .header(MapiHeaders.HEADER_UUID, "fake_uuid")
        )

        assertJson(response, "/client/antirobot/generateNonceResponseParsed.json", "Response")
    }

    @Test
    fun checkAndroidToken() {
        mockAuthenticateAndroidResponse("/client/antirobot/authenticateResponse.json")

        val body = CheckTokenBody("fake_attestation")

        val response = mvcCall(
            MockMvcRequestBuilders.post("/api/validation/check_android_token")
                .contentType(MEDIA_JSON_UTF8)
                .content(JsonHelper.toString(body))
                .header(MapiHeaders.HEADER_UUID, "fake_uuid")
        )

        assertJson(response, "/client/antirobot/authenticateResponseParsed.json", "Response")
    }

    @Test
    fun checkAndroidTokenBadRequest() {
        whenever(validationServiceClient.authenticateAndroid(any())).then {
            CompletableFuture.failedFuture<CommonRetrofitHttpExecutionException>(
                CommonRetrofitHttpExecutionException("{\"error\": \"error\"}", 400, null)
            )
        }

        val body = CheckTokenBody("fake_attestation")

        mvcCall(
            MockMvcRequestBuilders.post("/api/validation/check_android_token")
                .contentType(MEDIA_JSON_UTF8)
                .content(JsonHelper.toString(body))
                .header(MapiHeaders.HEADER_UUID, "fake_uuid"),
            expected = BAD_4XX,
            expectedType = null
        )
    }

    @Test
    fun checkIosToken() {
        mockAuthenticateAndroidResponse("/client/antirobot/authenticateResponse.json")

        val body = CheckTokenBody("fake_attestation")

        val response = mvcCall(
            MockMvcRequestBuilders.post("/api/validation/check_ios_token")
                .contentType(MEDIA_JSON_UTF8)
                .content(JsonHelper.toString(body))
                .header(MapiHeaders.HEADER_UUID, "fake_uuid")
        )

        assertJson(response, "/client/antirobot/authenticateResponseParsed.json", "Response")
    }

    private fun mockGenerateNonceResponse(file: String) {
        whenever(validationServiceClient.generateNonce(any())).then {
            // generate new response on each call
            CompletableFuture.completedFuture(JsonHelper.parse<NonceResponse>(getResource(file)))
        }
    }

    private fun mockAuthenticateAndroidResponse(file: String) {
        whenever(validationServiceClient.authenticateAndroid(any())).then {
            // generate new response on each call
            CompletableFuture.completedFuture(JsonHelper.parse<AuthenticateResponse>(getResource(file)))
        }

        whenever(validationServiceClient.authenticateIos(any())).then {
            // generate new response on each call
            CompletableFuture.completedFuture(JsonHelper.parse<AuthenticateResponse>(getResource(file)))
        }
    }
}