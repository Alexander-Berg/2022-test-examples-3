package ru.yandex.direct.antifraud.client

import okhttp3.mockwebserver.MockResponse
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import ru.yandex.direct.antifraud.client.model.Action
import ru.yandex.direct.tvm.TvmIntegration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AntifraudClientTest {
    @JvmField
    @RegisterExtension
    var mockedAntifraud = MockedAntifraud()

    private lateinit var antifraudClient: AntifraudClient

    val url = "https://passport.yandex.ru//auth/user-validate?track_id=...&origin=..."

    @BeforeAll
    fun init() {
        val tvmIntegration = Mockito.mock(TvmIntegration::class.java)
        Mockito.`when`(tvmIntegration.getTicket(ArgumentMatchers.any())).thenReturn("TVM_TICKET")
        Mockito.`when`(tvmIntegration.isEnabled).thenReturn(true)
        antifraudClient = mockedAntifraud.createClient(tvmIntegration)

        val requestBody =
            "{" +
                "\"channel\":\"challenge\"," +
                "\"uid\":11300000574130," +
                "\"login_id\":\"s:1552589986921:hqy mBQ:4f\"," +
                "\"retpath\":\"https://direct.yandex.ru/\"," +
                "\"force_challenge\":true" +
                "}"

        val responseBody =
            "{" +
                "\"action\":\"ALLOW\"," +
                "\"reason\":\"\"," +
                "\"tags\":[{\"url\":\"$url\"}]," +
                "\"tx_id\":\"challenge/null:gen-d6ad1a8e-6f7b-499d-ac1b-efe0b6811324\"" +
                "}"

        val response =
            MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(responseBody)

        mockedAntifraud.add(
            "POST:/score?consumer=direct_phone_verification:$requestBody",
            response
        )
    }

    @Test
    internal fun successPostAndNoExceptionReturned() {
        Assertions.assertThatCode { getVerdict() }.doesNotThrowAnyException()
    }

    @Test
    internal fun successPostAndGetResult() {
        val verdict = getVerdict()
        Assertions.assertThat(verdict.status).isEqualTo(Action.ALLOW)
        Assertions.assertThat(verdict.challenge).isNotNull
        Assertions.assertThat(verdict.challenge).isEqualTo(url)
    }

    private fun getVerdict() =
        antifraudClient.getVerdict(
            uid = 11300000574130,
            loginId = "s:1552589986921:hqy mBQ:4f",
            retpath = "https://direct.yandex.ru/",
            forceChallenge = true
        )
}
