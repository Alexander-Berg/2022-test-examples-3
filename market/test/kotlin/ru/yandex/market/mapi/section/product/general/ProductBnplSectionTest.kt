package ru.yandex.market.mapi.section.product.general

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveBnplPlanResponse
import ru.yandex.market.mapi.client.fapi.response.ResolveProductOffersResponse
import ru.yandex.market.mapi.client.fapi.response.ResolveSkuInfoResponse
import ru.yandex.market.mapi.core.util.date.MapiDateBuilder.parseDateTimeIsoWithTZ
import ru.yandex.market.mapi.section.AbstractSectionTest
import ru.yandex.market.mapi.section.bnpl.ProductBnplAssembler
import ru.yandex.market.mapi.section.bnpl.ProductBnplSection
import kotlin.test.assertEquals

class ProductBnplSectionTest : AbstractSectionTest() {
    private val assembler = ProductBnplAssembler()

    private val resolversMap = mapOf(
        ResolveSkuInfoResponse.RESOLVER to "/section/product/resolveSkuInfo.json",
        ResolveProductOffersResponse.RESOLVER to "/section/product/resolveProductOffers.json",
        ResolveBnplPlanResponse.RESOLVER to "/section/product/resolveBnplPlan.json",
    )

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            fileMap = resolversMap,
            expected = "/section/product/content/bnplAssembly.json",
        )
    }

    @Test
    fun testContentResult() {
        testContentResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverResponseMap = resolversMap,
            expected = "/section/product/content/bnplAssemblyResult.json"
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolversMap,
            "/section/product/content/bnplSection.json",
        )
    }

    @Test
    fun testBnplPaymentDate() {
        assertEquals(
            "6 июл",
            assembler.toBnplPaymentDate(parseDateTimeIsoWithTZ("2022-07-06T13:00:17Z"))
        )

        assertEquals(
            "6 авг",
            assembler.toBnplPaymentDate(parseDateTimeIsoWithTZ("2022-08-06T14:00:17Z"))
        )

        assertEquals(
            "6 сент",
            assembler.toBnplPaymentDate(parseDateTimeIsoWithTZ("2022-09-06T15:00:17Z"))
        )
    }

    private fun buildWidget(): ProductBnplSection {
        return ProductBnplSection().apply {
            addDefParams()
        }
    }
}
