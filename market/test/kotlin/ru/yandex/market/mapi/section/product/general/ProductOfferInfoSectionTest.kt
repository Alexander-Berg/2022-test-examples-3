package ru.yandex.market.mapi.section.product.general

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveProductOffersResponse
import ru.yandex.market.mapi.client.fapi.response.ResolveSkuInfoResponse
import ru.yandex.market.mapi.client.fapi.response.product.ResolveSupplierInfoByIdResponse
import ru.yandex.market.mapi.section.AbstractSectionTest

/**
 * @author Dasha Rednikina / dashared@ / 30.06.2022
 */
class ProductOfferInfoSectionTest : AbstractSectionTest() {

    private val assembler = ProductOfferInfoAssembler()

    private val resolversMap = mapOf(
        ResolveSkuInfoResponse.RESOLVER to "/section/product/resolveSkuInfo.json",
        ResolveProductOffersResponse.RESOLVER to "/section/product/resolveProductOffers.json",
        ResolveSupplierInfoByIdResponse.RESOLVER to "/section/product/resolveSupplierInfoById.json"
    )

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            fileMap = resolversMap,
            expected = "/section/product/general/offerInfoAssembly.json",
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolversMap,
            "/section/product/general/offerInfoSection.json"
        )
    }

    private fun buildWidget(): ProductOfferInfoSection {
        return ProductOfferInfoSection().apply {
            addDefParams()
        }
    }
}
