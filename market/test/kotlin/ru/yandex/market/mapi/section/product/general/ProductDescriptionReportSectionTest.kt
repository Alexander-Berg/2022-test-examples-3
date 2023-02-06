package ru.yandex.market.mapi.section.product.general

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveSkuInfoResponse
import ru.yandex.market.mapi.core.util.mockUuid
import ru.yandex.market.mapi.section.AbstractSectionTest

/**
 * @author Ilya Kislitsyn / ilyakis@ / 11.04.2022
 */
class ProductDescriptionReportSectionTest : AbstractSectionTest() {
    private val assembler = ProductDescriptionReportAssembler()

    @Test
    fun testAssembly() {
        mockUuid("some-uuid")

        assembler.testAssembly(
            fileMap = mapOf(
                ResolveSkuInfoResponse.RESOLVER to "/section/product/resolveSkuInfo.json"
            ),
            expected = "/section/product/general/descriptionReportAssembly.json",
        )
    }

    @Test
    fun testSection() {
        mockUuid("some-uuid")

        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            mapOf(
                ResolveSkuInfoResponse.RESOLVER to "/section/product/resolveSkuInfo.json"
            ),
            "/section/product/general/descriptionReportWidget.json"
        )
    }


    @Test
    fun testSectionNoUuid() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            mapOf(
                ResolveSkuInfoResponse.RESOLVER to "/section/product/resolveSkuInfo.json"
            ),
            "/section/product/general/descriptionReportWidgetNoUuid.json"
        )
    }

    private fun buildWidget(): ProductDescriptionReportSection {
        return ProductDescriptionReportSection().apply {
            addDefParams()
        }
    }
}
