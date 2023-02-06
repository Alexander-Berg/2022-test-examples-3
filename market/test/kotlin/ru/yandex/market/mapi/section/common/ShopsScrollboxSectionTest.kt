package ru.yandex.market.mapi.section.common

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.model.FapiPictureCms
import ru.yandex.market.mapi.section.AbstractSectionTest
import ru.yandex.market.mapi.section.common.shops.*

class ShopsScrollboxSectionTest : AbstractSectionTest() {
    private val shopsScrollboxAssembler = HyperlocalShopSnippetAssembler()
    private val resolver = "resolveHyperlocalShopInfoV1"

    @Test
    fun testAssembly() {
        shopsScrollboxAssembler.testAssembly(
            fileMap = mapOf(resolver to "/section/common/shops/fapiResponse.json"),
            expected = "/section/common/shops/assembled.json",
            config = ConfigBuilder().big().build()
        )
    }

    @Test
    fun testAssemblySmall() {
        shopsScrollboxAssembler.testAssembly(
            fileMap = mapOf(resolver to "/section/common/shops/fapiResponse.json"),
            expected = "/section/common/shops/assembledSmall.json",
            config = ConfigBuilder().small().build()
        )
    }

    @Test
    fun testAssemblyDefaultFirstPos() {
        shopsScrollboxAssembler.testAssembly(
            fileMap = mapOf(resolver to "/section/common/shops/fapiResponse.json"),
            expected = "/section/common/shops/assembledSmallWithDefaultFirstPos.json",
            config = ConfigBuilder().small().defaultSnippet(0).build()
        )
    }



    @Test
    fun testAssemblyDefaultSecondPos() {
        shopsScrollboxAssembler.testAssembly(
            fileMap = mapOf(resolver to "/section/common/shops/fapiResponse.json"),
            expected = "/section/common/shops/assembledSmallWithDefaultFirstPos.json",
            config = ConfigBuilder().small().defaultSnippet(1).build()
        )
    }

    @Test
    fun testAssemblyDefaultWithBig() {
        shopsScrollboxAssembler.testAssembly(
            fileMap = mapOf(resolver to "/section/common/shops/fapiResponse.json"),
            expected = "/section/common/shops/assembledBig.json",
            config = ConfigBuilder().big().defaultSnippet(0).build()
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            buildWidget(),
            shopsScrollboxAssembler,
            buildResolver("resolveHyperlocalShopInfo", emptyMap()),
            resolverResponseMap = mapOf(resolver to "/section/common/stories/fapiResponse.json"),
            expected = "/section/common/shops/sectionResult.json"
        )
    }

    @Test
    fun testContentResult() {
        testContentResult(
            buildWidget(),
            shopsScrollboxAssembler,
            buildResolver("resolveHyperlocalShopInfo", emptyMap()),
            resolverResponseMap = mapOf(resolver to "/section/common/shops/fapiResponse.json"),
            expected = "/section/common/shops/contentResult.json",
            config = ConfigBuilder().small().defaultSnippet(1).build()
        )
    }

    private fun buildWidget(): ShopsScrollboxSection {
        return ShopsScrollboxSection().apply {
            id = "Test shop section id"
            title = "Test shop section title"
            type = ShopsScrollboxSection::class.simpleName
        }
    }

    private class ConfigBuilder {
        private val config = HyperlocalShopSnippetAssembler.Config()

        fun big(): ConfigBuilder {
            config.snippetSize = HyperlocalShopSnippetAssembler.Config.SnippetSize.BIG
            return  this
        }

        fun small(): ConfigBuilder {
            config.snippetSize = HyperlocalShopSnippetAssembler.Config.SnippetSize.SMALL
            return this
        }

        fun defaultSnippet(position: Int): ConfigBuilder {
            config.defaultSnippets = listOf(
                ShopSectionDefaultSnippet(
                    title = "default title",
                    deeplink = "yamarket://",
                    subtitle = "default subtitle",
                    shopColor = null,
                    separatorSide = ShopSnippetSeparatorSide.BOTH,
                    subtitleColor = "#222222",
                    isVisible = true,
                    position = position,
                    icon = FapiPictureCms("https://yandex.ru"),
                    type = ShopDefaultSnippetSnippetType.MARKET
                )
            )
            return this
        }

        fun build(): HyperlocalShopSnippetAssembler.Config {
            return config
        }
    }

}
