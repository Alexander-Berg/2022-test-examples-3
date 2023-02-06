package ru.yandex.market.mapi.controller

import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.yandex.market.mapi.core.AbstractSpringTest
import ru.yandex.market.mapi.core.contract.ScreenProcessor
import ru.yandex.market.mapi.core.model.response.MapiScreenRequestBody
import ru.yandex.market.mapi.core.model.screen.ScreenRequest
import ru.yandex.market.mapi.core.util.JsonHelper
import ru.yandex.market.mapi.core.util.asResource
import ru.yandex.market.mapi.core.util.assertJson
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Minimum spring config to run controller tests.
 * @author Ilya Kislitsyn / ilyakis@ / 31.03.2022
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    classes = [ControllerTestsConfig::class]
)
abstract class AbstractApiControllerTest : AbstractSpringTest() {
    @Autowired
    lateinit var screenProcessor: ScreenProcessor

    /**
     * In most cases response object does not matters.
     * So just use same response to check logic of all interactions.
     */
    protected fun checkApiCallNewFormat(screen: String, call: () -> String) {
        val mockResponseFile = "/common/simpleScreenResponseNewFormat.json"
        val mockResponseDtoFile = "/common/simpleScreenResponseNewFormatDto.json"

        whenever(screenProcessor.getScreen(argThat { request ->
            request?.cmsPageType == screen
        }, anyOrNull())).thenReturn(
            JsonHelper.parse(mockResponseFile.asResource())
        )

        assertJson(call.invoke(), mockResponseDtoFile, "Response")
    }

    protected fun mockAnyProcessorResult() {
        whenever(screenProcessor.getScreen(any(), anyOrNull())).thenReturn(
            JsonHelper.parse("/common/simpleScreenResponse.json".asResource())
        )
    }

    protected fun verifySingleCall(
        screenName: String? = null,
        verifier: (ScreenRequest) -> Unit = { }
    ) {
        verifyCall(times = 1, screenName) { _, request ->
            verifier.invoke(request)
        }
    }

    protected fun verifyCall(
        times: Int = 1,
        screenName: String? = null,
        verifier: (Int, ScreenRequest) -> Unit = { _, _ -> }
    ) {
        val requestCaptor = argumentCaptor<ScreenRequest>()
        verify(screenProcessor, times(times)).getScreen(requestCaptor.capture(), anyOrNull())

        assertEquals(screenName, requestCaptor.firstValue.cmsPageType)
        requestCaptor.allValues.forEachIndexed(verifier)
    }

    protected fun <T : Any> simpleRequest(request: T): MapiScreenRequestBody<T> {
        return MapiScreenRequestBody<T>().also { body ->
            body.request = request
        }
    }

    protected fun assertResolverParams(expected: MutableMap<String, Any?>, request: ScreenRequest) {
        val actual = request.customResolverParams
        assertNotNull(actual)

        assertEquals(expected.size, actual.size)
        assertTrue { expected.keys.containsAll(actual.keys) }
        expected.keys.forEach { key -> assertEquals(expected[key], actual[key]) }
    }
}