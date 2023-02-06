package ru.yandex.market.mapi.core

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.mapi.core.util.JsonHelper
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * @author Ilya Kislitsyn / ilyakis@ / 31.03.2022
 */
@ExtendWith(SpringExtension::class)
@AutoConfigureMockMvc
@ActiveProfiles("junit")
abstract class AbstractSpringTest {
    companion object {
        // simple declaration of result codes - used everywhere
        val OK = MockMvcResultMatchers.status().isOk
        val BAD_4XX = MockMvcResultMatchers.status().is4xxClientError
        val BAD_5XX = MockMvcResultMatchers.status().is5xxServerError

        // MediaType.APPLICATION_JSON_UTF8 - deprecated, but required to return content in correct charset
        val MEDIA_JSON_UTF8 = MediaType("application", "json", StandardCharsets.UTF_8)
    }

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @BeforeEach
    fun resetMocks() {
        MockContext.resetMocks()
    }

    fun getResource(path: String): String {
        return javaClass.getResource(path)?.readText(Charsets.UTF_8)
            ?: throw IllegalArgumentException("No resource by given path: $path")
    }

    fun mvcCall(
        requestBuilder: MockHttpServletRequestBuilder,
        expected: ResultMatcher = OK,
        expectedType: MediaType? = MEDIA_JSON_UTF8,
        alsoExpect: ResultMatcher = ResultMatcher.matchAll(),
        body: Any? = null,
        contentType: MediaType = MEDIA_JSON_UTF8
    ): String {
        // Jetty config is not called, so request context is not cleaned in tests.
        // So clean manually
        MapiContext.clean()

        val request = requestBuilder
            // to make operations safely ignore test timings
            .header(MapiHeaders.HEADER_MAPI_TIMESTAMP, Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli())
        requestBuilder.contentType(MediaType.APPLICATION_JSON)

        if (body != null) {
            request.contentType(contentType)
                .content(JsonHelper.toString(body))
        }

        return try {
            mockMvc.perform(request)
                .andDo(MockMvcResultHandlers.print())
                .andExpect(expected)
                .andExpect(expectedType?.let { MockMvcResultMatchers.content().contentType(it) }
                    ?: ResultMatcher.matchAll())
                .andExpect(alsoExpect)
                .andReturn().response.contentAsString
        } catch (e: Exception) {
            throw RuntimeException("Failed mvcMock call", e)
        }
    }
}
