package ru.yandex.market.mapi.engine

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.assembler.AbstractFapiPrefetchHandler
import ru.yandex.market.mapi.client.fapi.dto.FapiResponseCollections
import ru.yandex.market.mapi.core.MapiHeaders
import ru.yandex.market.mapi.core.model.MapiErrorHandler
import ru.yandex.market.mapi.core.model.screen.ResourceResolver
import ru.yandex.market.mapi.core.model.screen.ScreenResponse
import ru.yandex.market.mapi.core.model.section.EngineTestMultiAssembler
import ru.yandex.market.mapi.core.util.daoJsonTree
import ru.yandex.market.mapi.core.util.mockFlags
import kotlin.reflect.KClass

/**
 * @author Ilya Kislitsyn / ilyakis@ / 08.07.2022
 */
class PrefetchEngineTest : AbstractEngineTest() {
    companion object {
        private const val RESOLVER_ANOTHER = "anotherResolver"
        private const val RESOLVER_ANOTHER_KEY = "anotherResolverV1"
    }

    @BeforeEach
    fun init() {
        // raw response is required in these tests
        mockFlags(MapiHeaders.FLAG_INT_KEEP_RESOURCE)
    }

    @Test
    fun testPrefetchOk() {
        templatorMocker.mockPageResponse("/engine/prefetch/prefetchScreen.json")

        fapiMocker.mockFapiResponse("/engine/prefetch/resolverWithAlias.json", EngineTestMultiAssembler.RES_FIRST)
        fapiMocker.mockFapiResponse("/engine/multi/resolverMultiSecond.json", EngineTestMultiAssembler.RES_SECOND)
        fapiMocker.mockFapiResponse("/engine/prefetch/resolverAnother.json", RESOLVER_ANOTHER)

        assertScreen("/engine/prefetch/prefetchScreenResult.json", getScreenWithPrefetch())

        fapiMocker.verifyCall(1, EngineTestMultiAssembler.RES_FIRST)
        fapiMocker.verifyCall(1, EngineTestMultiAssembler.RES_SECOND)
        fapiMocker.verifyCall(1, RESOLVER_ANOTHER)
    }

    @Test
    fun testPrefetchFailFirst() {
        templatorMocker.mockPageResponse("/engine/prefetch/prefetchScreen.json")

        fapiMocker.mockFapiErrorResponse(EngineTestMultiAssembler.RES_FIRST)
        fapiMocker.mockFapiResponse("/engine/multi/resolverMultiSecond.json", EngineTestMultiAssembler.RES_SECOND)
        fapiMocker.mockFapiResponse("/engine/prefetch/resolverAnother.json", RESOLVER_ANOTHER)

        assertScreen("/engine/prefetch/prefetchScreenFailFirst.json", getScreenWithPrefetch())
    }

    @Test
    fun testPrefetchFailSecond() {
        templatorMocker.mockPageResponse("/engine/prefetch/prefetchScreen.json")

        fapiMocker.mockFapiResponse("/engine/prefetch/resolverWithAlias.json", EngineTestMultiAssembler.RES_FIRST)
        fapiMocker.mockFapiResponse("/engine/multi/resolverMultiSecond.json", EngineTestMultiAssembler.RES_SECOND)
        fapiMocker.mockFapiResponse("/engine/prefetch/resolverError.json", RESOLVER_ANOTHER)

        assertScreen("/engine/prefetch/prefetchScreenFailSecond.json", getScreenWithPrefetch())
    }

    @Test
    fun testPrefetchFailSecondRaw() {
        mockFlags(MapiHeaders.FLAG_INT_KEEP_RESOURCE, MapiHeaders.FLAG_INT_KEEP_RAW_RESPONSE)

        templatorMocker.mockPageResponse("/engine/prefetch/prefetchScreen.json")

        fapiMocker.mockFapiResponse("/engine/prefetch/resolverWithAlias.json", EngineTestMultiAssembler.RES_FIRST)
        fapiMocker.mockFapiResponse("/engine/multi/resolverMultiSecond.json", EngineTestMultiAssembler.RES_SECOND)
        fapiMocker.mockFapiResponse("/engine/prefetch/resolverError.json", RESOLVER_ANOTHER)

        assertScreen("/engine/prefetch/prefetchScreenFailSecondRaw.json", getScreenWithPrefetch())
    }

    private fun getScreenWithPrefetch(): ScreenResponse {
        return getScreen {
            cmsPageType = "any"

            val resolverParams = mutableMapOf<String, Any?>(
                "fixedParam" to "fixedValue"
            )
            customResolverParams = resolverParams

            // one with alias, another is not
            prefetchResolvers = listOf(
                ResourceResolver().apply {
                    alias = "simpleAlias"
                    resolver = EngineTestMultiAssembler.RES_FIRST
                    params = daoJsonTree(
                        "skk" to 123123
                    )
                },
                ResourceResolver().apply {
                    resolver = RESOLVER_ANOTHER
                    params = daoJsonTree()
                }
            )

            prefetchResolverHandler = TestPrefetchHandler(resolverParams)
        }
    }

    class TestPrefetchHandler(val params: MutableMap<String, Any?>) : AbstractFapiPrefetchHandler() {
        override val typeMap: Map<String, KClass<*>>
            get() = mapOf(
                EngineTestMultiAssembler.RES_FIRST_WITH_VERSION to EngineTestMultiAssembler.TestResponseFirst::class,
                RESOLVER_ANOTHER_KEY to ResponseAnother::class,
            )

        override fun handle(responseMap: Map<String, Any>, errorHandler: MapiErrorHandler) {
            params["staticParam"] = "staticValue"

            responseMap.getResponse<EngineTestMultiAssembler.TestResponseFirst>()?.let {
                params["sku"] = it.sku
            }

            responseMap.getResponse<ResponseAnother>()?.let {
                params["vendorId"] = it.collections?.vendorId
            }
        }
    }

    class ResponseAnother : FapiResponseCollections<Data2>()

    class Data2 {
        lateinit var vendorId: String
    }
}