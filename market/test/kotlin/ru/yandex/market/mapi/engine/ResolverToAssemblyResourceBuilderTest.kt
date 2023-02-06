package ru.yandex.market.mapi.engine

import org.junit.jupiter.api.Test


class ResolverToAssemblyResourceBuilderTest: AbstractEngineTest() {

    @Test
    fun testReplacedSectionId() {
        templatorMocker.mockPageResponse("/engine/builderParams/cmsTestPageAssemblerUsingResolverParams.json")
        fapiMocker.mockFapiResponse("/engine/builderParams/resolverResponce.json", "resolvePrime")

        assertScreen(
            "/engine/builderParams/screenResultWithTestParam.json",
            getScreenAny()
        )
    }
}
