package ru.yandex.market.mapi.section.product.medical

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveSkuInfoResponse
import ru.yandex.market.mapi.section.AbstractSectionTest

/**
 * @author Ilya Kislitsyn / ilyakis@ / 11.04.2022
 */
class ProductInstructionSectionTest : AbstractSectionTest() {
    private val assembler = ProductInstructionAssembler()

    @Test
    fun testAssemblyBasic() {
        assembler.testAssembly(
            fileMap = mapOf(
                ResolveSkuInfoResponse.RESOLVER to "/section/product/resolveSkuInfo.json"
            ),
            expected = "/section/product/medical/instructionAssembly.json",
            config = buildConfig()
        )
    }

    @Test
    fun testAssemblyMedical() {
        assembler.testAssembly(
            fileMap = mapOf(
                ResolveSkuInfoResponse.RESOLVER to "/section/product/medical/resolveSkuInfoMedical.json"
            ),
            expected = "/section/product/medical/instructionAssemblyMedical.json",
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
                ResolveSkuInfoResponse.RESOLVER to "/section/product/medical/resolveSkuInfoMedical.json"
            ),
            "/section/product/medical/instructionWidgetMedical.json",
            config = buildConfig()
        )
    }

    private fun buildWidget(): ProductInstructionSection {
        return ProductInstructionSection().apply {
            addDefParams()
        }
    }

    private fun buildConfig(): Any {
        return ProductInstructionAssembler.Config().apply {
            instructionsPointsKeys = listOf("Противопоказания", "Способ применения и дозы")
        }
    }
}
