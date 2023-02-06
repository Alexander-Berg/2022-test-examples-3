package ru.yandex.market.mapi.section.product.general

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveSkuInfoResponse
import ru.yandex.market.mapi.core.MapiConstants
import ru.yandex.market.mapi.core.util.mockQueryParams
import ru.yandex.market.mapi.section.AbstractSectionTest

/**
 * @author Ilya Kislitsyn / ilyakis@ / 11.04.2022
 */
class ProductRedirectInfoSectionTest : AbstractSectionTest() {
    private val assembler = ProductRedirectInfoAssembler()

    val resolverMap = mapOf(
        ResolveSkuInfoResponse.RESOLVER to "/section/product/resolveSkuInfo.json"
    )

    @Test
    fun testAssembly() {
        mockQueryParams(
            mapOf(
                MapiConstants.TEXT to "что-то ищется"
            )
        )

        assembler.testAssembly(
            fileMap = resolverMap,
            expected = "/section/product/general/redirectAssembly.json",
            config = buildConfig()
        )
    }

    @Test
    fun testNotAssemblyWithoutText() {
        assembler.testAssembly(
            fileMap = resolverMap,
            expected = "/section/product/general/redirectAssemblyHidden.json",
            config = buildConfig()
        )
    }

    @Test
    fun testSectionWithText() {
        mockQueryParams(
            mapOf(
                MapiConstants.TEXT to "что-то ищется"
            )
        )

        testSectionResult(
            buildSection(),
            assembler,
            buildAnyResolver(),
            resolverMap,
            "/section/product/general/redirectSection.json"
        )
    }

    @Test
    fun testSectionWithoutText() {
        testSectionResult(
            buildSection(),
            assembler,
            buildAnyResolver(),
            resolverMap,
            "/section/product/general/redirectSectionWithoutText.json",
            config = buildConfig()
        )
    }

    private fun buildSection(): ProductRedirectInfoSection {
        return ProductRedirectInfoSection().apply {
            addDefParams()
        }
    }

    private fun buildConfig(): ProductRedirectInfoSection.AssembledData {
        return ProductRedirectInfoSection.AssembledData().apply {
            text = "Другой текст про поиск"
            linkText = "другая ссылка"
            link = "http://somelink?"
        }
    }
}
