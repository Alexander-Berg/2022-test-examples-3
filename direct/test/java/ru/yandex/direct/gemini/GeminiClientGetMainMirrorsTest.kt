package ru.yandex.direct.gemini

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.apache.commons.io.IOUtils
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.notNullValue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.test.utils.TestUtils.assumeThat
import java.net.URLEncoder.encode
import java.nio.charset.StandardCharsets

@RunWith(Parameterized::class)
class GeminiClientGetMainMirrorsTest : GeminiClientTestBase() {

    companion object {
        const val BASIC_PARAMS =
            "normal_kv_report=yes&sp_meta_search=multi_proxy&meta_search=first_found&saas_no_text_split=yes&hr=json"
        const val PATH = "/gemini"

        @Parameterized.Parameters(name = "{1}")
        @JvmStatic
        fun getParameters() = listOf(
            arrayOf(
                "gemini/valid_server_answer_example_1.json",
                listOf("https://onelovebox-shop.ru"),
                mapOf("https://onelovebox-shop.ru" to "https://www.onelovebox-shop.ru/")
            ),
            arrayOf(
                "gemini/valid_server_answer_example_2.json",
                listOf("https://onelovebox-shop.ru", "https://yandex.ru", "incorrect_url"),
                mapOf("https://onelovebox-shop.ru" to "https://www.onelovebox-shop.ru/",
                    "https://yandex.ru" to "https://yandex.ru/")
            )
        )
    }

    @get:Rule
    val springMethodRule = SpringMethodRule()

    @Parameterized.Parameter
    lateinit var answerExamplePath: String

    @Parameterized.Parameter(value = 1)
    lateinit var urls: List<String>

    @Parameterized.Parameter(value = 2)
    lateinit var expectedResult: Map<String, String>

    @Test
    fun getMainMirror_success() {
        val result = geminiClient.getMainMirrors(urls)
        assertThat(result).isEqualTo(expectedResult)
    }

    override fun dispatcher(): Dispatcher {
        return object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                softAssertions.assertThat(request.method).isEqualTo("GET")
                val decodedUrlParams = urls.asSequence()
                    .map { encode(it, StandardCharsets.UTF_8.name()) }
                    .map { "text=$it" }
                    .joinToString("&")
                val expectedPath = "$PATH?$BASIC_PARAMS&gemini_user=$USER&$decodedUrlParams&gemini_type=mirror"
                softAssertions.assertThat(request.path).isEqualTo(expectedPath)
                return MockResponse().setBody(getAnswerExample())
            }
        }
    }

    private fun getAnswerExample(): String {
        val classloader = Thread.currentThread().contextClassLoader
        classloader.getResourceAsStream(answerExamplePath).use {
            assumeThat(it, notNullValue())
            return IOUtils.toString(it, StandardCharsets.UTF_8)
        }
    }
}
