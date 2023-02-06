package ru.yandex.market.mapi.client.antirobot

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import ru.yandex.market.common.retrofit.CommonRetrofitHttpExecutionException
import ru.yandex.market.mapi.client.AbstractClientTest
import ru.yandex.market.request.trace.Module
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class ValidationServiceClientTest : AbstractClientTest() {
    private val client = ValidationServiceClient(mockedRetrofit(Module.ANTIROBOT_VALIDATION))

    @Test
    fun generateNonce() {
        mockClientResponse("/client/antirobot/generateNonceResponse.json")

        assertResponse(
            client.generateNonce(GenerateNonceBody("fake_nonce", "fake_app_id")),
            "/client/antirobot/generateNonceResponseParsed.json"
        )
    }

    @Test
    fun authenticateAndroid() {
        mockClientResponse("/client/antirobot/authenticateResponse.json")

        assertResponse(
            client.authenticateAndroid(AuthenticateAndroidBody("fake_attestation", "fake_uuid")),
            "/client/antirobot/authenticateResponseParsed.json"
        )
    }

    @Test
    fun invalidAuthenticateAndroid() {
        mockClientResponse("/client/antirobot/invalidAuthenticateResponse.json", status = HttpStatus.BAD_REQUEST)

        val exception = assertFailsWith(
            ExecutionException::class,
            "",
            block = {
                client.authenticateAndroid(AuthenticateAndroidBody("fake_attestation", "fake_uuid")).get()
            }
        )
        assertEquals(CommonRetrofitHttpExecutionException::class, exception.cause!!::class)
        assertEquals("HTTP 400, {\n" +
            "    \"error\": \"invalid attestation: failed to parse JWT: certificate verification failure....\"\n" +
            "} @ POST http://someurl-antirobot_validation/android/authenticate",  exception.cause!!.message)
    }

    @Test
    fun authenticateIos() {
        mockClientResponse("/client/antirobot/authenticateResponse.json")

        assertResponse(
            client.authenticateIos(AuthenticateIosBody("fake_attestation", "fake_uuid", "fake_bundle_id")),
            "/client/antirobot/authenticateResponseParsed.json"
        )
    }
}