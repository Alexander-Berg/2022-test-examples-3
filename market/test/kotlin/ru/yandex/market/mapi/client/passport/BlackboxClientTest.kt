package ru.yandex.market.mapi.client.passport

import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import ru.yandex.market.common.retrofit.ahc.CustomAsyncHttpClientCall
import ru.yandex.market.mapi.client.AbstractClientTest
import ru.yandex.market.mapi.core.MapiContext
import ru.yandex.market.mapi.core.model.MapiError
import ru.yandex.market.request.trace.Module
import java.util.concurrent.TimeoutException
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * @author Ilya Kislitsyn / ilyakis@ / 27.01.2022
 */
class BlackboxClientTest : AbstractClientTest() {
    private val client = BlackboxClient(mockedRetrofit(Module.BLACKBOX))
    private val clientWithRetry = BlackboxClient(mockedRetrofitWithRetry(Module.BLACKBOX))

    @Test
    fun testBlackboxCall() {
        mockClientResponse("/client/passport/oauthResponse.json")

        assertResponse(
            client.checkOauth("sometoken", "12321"),
            "/client/passport/oauthResponseParsed.json"
        )

        verifyClientCall(times = 1) { _, request ->
            request.assertRequest(HttpMethod.POST, "/blackbox")
            request.assertQuery(
                "method" to "oauth",
                "format" to "json",
                "get_user_ticket" to "yes",
                "oauth_token" to "sometoken",
                "userip" to "12321",
                "aliases" to "13"
            )
            request.assertHeaders(
                CustomAsyncHttpClientCall.REQ_TIMEOUT_MS_HEADER to "1000"
            )
        }
    }

    @Test
    fun testBlackboxFailedCall() {
        mockClientResponse("/client/passport/oauthResponseWithError.json")

        val info = client.checkOauth("sometoken", "12321").get()

        assertNull(info)
        assertEquals(1, MapiContext.get().getErrorList().size)
        assertEquals(
            MapiError.BLACKBOX_OAUTH_INVALID.toDto("Invalid oauth token: BB error text"),
            MapiContext.get().getErrorList().first()
        )
    }

    @Test
    fun testBlackboxNoRetryCall() {
        mockClientResponse("/client/passport/oauthResponseWithError.json", status = HttpStatus.NOT_FOUND)

        val info = clientWithRetry.checkOauth("sometoken", "12321").get()

        assertNull(info)
        assertEquals(1, MapiContext.get().getErrorList().size)
        assertEquals(
            MapiError.BLACKBOX_ERROR.toDto(
                "HTTP 404, {\n" +
                    "    \"error\": \"BB error text\",\n" +
                    "    \"status\": {\n" +
                    "        \"id\": 4\n" +
                    "    }\n" +
                    "}\n" +
                    " @ POST http://someurl-blackbox/blackbox?method=oauth&format=json&get_user_ticket=yes&oauth_token=sometoken&userip=12321&aliases=13"
            ),
            MapiContext.get().getErrorList().first()
        )

        verifyClientCall(times = 1)
    }

    @Test
    fun testBlackboxWithRetryCall() {
        mockClientResponse("/client/passport/oauthResponseWithError.json", status = HttpStatus.BAD_GATEWAY)

        val info = clientWithRetry.checkOauth("sometoken", "12321").get()

        assertNull(info)
        assertEquals(1, MapiContext.get().getErrorList().size)
        assertEquals(
            MapiError.BLACKBOX_ERROR.toDto(
                "HTTP 502, {\n" +
                    "    \"error\": \"BB error text\",\n" +
                    "    \"status\": {\n" +
                    "        \"id\": 4\n" +
                    "    }\n" +
                    "}\n" +
                    " @ POST http://someurl-blackbox/blackbox?method=oauth&format=json&get_user_ticket=yes&oauth_token=sometoken&userip=12321&aliases=13"
            ),
            MapiContext.get().getErrorList().first()
        )

        verifyClientCall(times = 2)
    }

    @Test
    fun testBlackboxWithNoRetryOnExceptionCall() {
        mockClientResponse("/client/passport/oauthResponseWithError.json",
            withException = { RuntimeException("Help me, i'm a failure") })

        val info = clientWithRetry.checkOauth("sometoken", "12321").get()

        assertNull(info)
        assertEquals(1, MapiContext.get().getErrorList().size)
        assertEquals(
            MapiError.BLACKBOX_ERROR.toDto("java.lang.RuntimeException: Help me, i'm a failure @ POST http://someurl-blackbox/blackbox?method=oauth&format=json&get_user_ticket=yes&oauth_token=sometoken&userip=12321&aliases=13"),
            MapiContext.get().getErrorList().first()
        )

        verifyClientCall(times = 1)
    }

    @Test
    fun testBlackboxWithRetryOnExceptionCall() {
        mockClientResponse("/client/passport/oauthResponseWithError.json",
            withException = { TimeoutException("Retry me if possible") })

        val info = clientWithRetry.checkOauth("sometoken", "12321").get()

        assertNull(info)
        assertEquals(1, MapiContext.get().getErrorList().size)
        assertEquals(
            MapiError.BLACKBOX_ERROR.toDto("java.util.concurrent.TimeoutException: Retry me if possible @ POST http://someurl-blackbox/blackbox?method=oauth&format=json&get_user_ticket=yes&oauth_token=sometoken&userip=12321&aliases=13"),
            MapiContext.get().getErrorList().first()
        )

        verifyClientCall(times = 2)
    }
}
