package ru.yandex.market.mapi.section.product.general

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveSkuInfoResponse
import ru.yandex.market.mapi.section.AbstractSectionTest

/**
 * @author Ilya Kislitsyn / ilyakis@ / 11.04.2022
 */
class ProductWarningsSectionTest : AbstractSectionTest() {
    private val assembler = ProductWarningsAssembler()

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            fileMap = mapOf(
                ResolveSkuInfoResponse.RESOLVER to "/section/product/resolveSkuInfo.json"
            ),
            expected = "/section/product/general/warningsAssembly.json",
            config = buildConfig()
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            mapOf(
                ResolveSkuInfoResponse.RESOLVER to "/section/product/resolveSkuInfo.json"
            ),
            "/section/product/general/warningsWidget.json"
        )
    }

    private fun buildWidget(): ProductWarningsSection {
        return ProductWarningsSection().apply {
            addDefParams()
        }
    }

    private fun buildConfig(): ProductWarningsAssembler.Config {
        return ProductWarningsAssembler.Config().apply {
            warningsToShow = listOf("test_ok")
            warningsToHide = listOf("appearance")
        }
    }
}
