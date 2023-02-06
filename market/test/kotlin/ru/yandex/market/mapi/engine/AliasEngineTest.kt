package ru.yandex.market.mapi.engine

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.core.MapiHeaders
import ru.yandex.market.mapi.core.util.mockFlags

/**
 * @author Ilya Kislitsyn / ilyakis@ / 26.04.2022
 */
class AliasEngineTest : AbstractEngineTest() {

    @Test
    fun testAliases() {
        templatorMocker.mockPageResponse("/engine/alias/screenWithAliasGeneral.json")

        fapiMocker.mockFapiResponse("/engine/resolverTestData2.json", "resolvePrimeWithoutAlias")
        fapiMocker.mockFapiResponse("/engine/resolverTestData2.json", "resolvePrimeWithAliasNone")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json", "resolvePrime")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json", "resolvePrime2Calls")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json", "resolvePrimeDuplicate")

        assertScreen(
            "/engine/alias/screenWithAliasGeneralResponse.json",
            getScreenAny()
        )

        fapiMocker.verifyCall(1, "resolvePrimeWithoutAlias")
        fapiMocker.verifyCall(1, "resolvePrimeWithAliasNone")
        fapiMocker.verifyCall(0, "resolvePrime")
        fapiMocker.verifyCall(1, "resolvePrime2Calls")
        fapiMocker.verifyCall(1, "resolvePrimeDuplicate")

        fapiMocker.verifyNoMoreInteractions()
    }

    @Test
    fun testAliasesRaw() {
        mockFlags(MapiHeaders.FLAG_INT_KEEP_RESOURCE, MapiHeaders.FLAG_INT_KEEP_RAW_RESPONSE)

        templatorMocker.mockPageResponse("/engine/alias/screenWithAliasSimple.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json", "resolvePrime")

        assertScreen("/engine/alias/screenWithAliasSimpleRaw.json", getScreenAny())
    }

    @Test
    fun testAliasesFail() {
        templatorMocker.mockPageResponse("/engine/alias/screenWithAliasSimple.json")
        fapiMocker.mockFapiErrorResponse()

        assertScreen("/engine/alias/screenWithAliasSimpleFail.json", getScreenAny())
    }

    @Test
    fun testAliasesTimeout() {
        templatorMocker.mockPageResponse("/engine/alias/screenWithAliasSimple.json")
        fapiMocker.mockFapiTimeoutFuture()

        assertScreen("/engine/alias/screenWithAliasSimpleTimeout.json", getScreenAny())
    }
}