package ru.yandex.market.mapi.section.common

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveProfitIndexEntryResponse
import ru.yandex.market.mapi.section.AbstractSectionTest
import ru.yandex.market.mapi.section.common.hotlink.HotlinkProfitabilityIndexAssembler
import ru.yandex.market.mapi.section.common.hotlink.HotlinkSnippetAssembler
import ru.yandex.market.mapi.section.common.hotlink.HotlinksSection

/**
 * @author Ilya Kislitsyn / ilyakis@ / 11.03.2022
 */
class HotlinksSectionTest : AbstractSectionTest() {
    private val assembler = HotlinkSnippetAssembler()
    private val assemblerProfit = HotlinkProfitabilityIndexAssembler()

    val resolver = ResolveProfitIndexEntryResponse.RESOLVER

    @Test
    fun testAssembly() {
        // should fail - any resolver assembly is not expected in this assembler
        assembler.testAssembly(
            fileMap = mapOf(resolver to "/section/common/hotlink/resolveProfitIndexEntry.json"),
            expected = "/section/common/hotlink/assembled.json",
        )
    }

    @Test
    fun testAssemblyProfit() {
        // should fail - resolver assembly is not expected in this assembler
        assemblerProfit.testAssembly(
            fileMap = mapOf(resolver to "/section/common/hotlink/resolveProfitIndexEntry.json"),
            expected = "/section/common/hotlink/assembledProfit.json",
            config = buildProfitConfig()
        )
    }

    @Test
    fun testAssemblyProfitNoConfig() {
        // should fail - resolver assembly is not expected in this assembler
        assemblerProfit.testAssembly(
            fileMap = mapOf(resolver to "/section/common/hotlink/resolveProfitIndexEntry.json"),
            expected = "/section/common/hotlink/assembledProfitError.json",
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            buildWidget(),
            assemblerProfit,
            buildAnyResolver(),
            resolverResponseMap = mapOf(resolver to "/section/common/hotlink/resolveProfitIndexEntry.json"),
            expected = "/section/common/hotlink/sectionResult.json",
            config = buildProfitConfig()
        )
    }

    @Test
    fun testSnippetProfitInteractions() {
        testContentResult(
            buildWidget(),
            assemblerProfit,
            buildAnyResolver(),
            resolverResponseMap = mapOf(resolver to "/section/common/hotlink/resolveProfitIndexEntry.json"),
            expected = "/section/common/hotlink/contentResultProfit.json",
            config = buildProfitConfig()
        )
    }

    @Test
    fun testStaticSnippetInteractions() {
        testStaticContentResult(
            buildWidget(),
            assembler,
            staticContentFile = "/section/common/hotlink/staticContent.json",
            expected = "/section/common/hotlink/staticContentResult.json"
        )
    }

    private fun buildWidget(): HotlinksSection {
        return HotlinksSection().apply {
            addDefParams()
        }
    }

    private fun buildProfitConfig() = HotlinkProfitabilityIndexAssembler.Config(
        hotlinkType = "profit",
        deeplink = "deeplinkUrl",
        title = "Some title",
    )
}