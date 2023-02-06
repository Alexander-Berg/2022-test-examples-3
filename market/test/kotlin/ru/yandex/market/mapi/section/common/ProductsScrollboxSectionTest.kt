package ru.yandex.market.mapi.section.common

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.enums.FapiPromoType
import ru.yandex.market.mapi.client.fapi.model.FapiDiscount
import ru.yandex.market.mapi.client.fapi.response.ResolveCompetitiveCardResponse
import ru.yandex.market.mapi.client.fapi.response.product.ResolveAlsoViewedResponse
import ru.yandex.market.mapi.client.fapi.response.product.ResolveComplementaryProductGroupsResponse
import ru.yandex.market.mapi.client.fapi.response.product.ResolveDJUniversalProductsResponse
import ru.yandex.market.mapi.client.fapi.response.product.ResolvePrimeResponse
import ru.yandex.market.mapi.client.fapi.response.product.ResolveProductsByHistoryResponse
import ru.yandex.market.mapi.client.fapi.util.OfferUtilsTest.Companion.testOffer
import ru.yandex.market.mapi.client.fapi.util.OfferUtilsTest.Companion.testPrice
import ru.yandex.market.mapi.client.fapi.util.OfferUtilsTest.Companion.testPromo
import ru.yandex.market.mapi.core.util.mockFlags
import ru.yandex.market.mapi.core.util.mockRearrs
import ru.yandex.market.mapi.model.section.ShowMoreLink
import ru.yandex.market.mapi.model.section.StartEndTimer
import ru.yandex.market.mapi.section.AbstractSectionTest
import ru.yandex.market.mapi.section.common.product.ProductScrollboxAssembler
import ru.yandex.market.mapi.section.common.product.ProductSnippet
import ru.yandex.market.mapi.section.common.product.ProductsScrollboxSection
import ru.yandex.market.mapi.section.common.product.buildDiscountBadge
import java.math.BigDecimal
import kotlin.test.assertEquals

/**
 * report tests - for all logic
 * dj test - to check djResult in widget + analytics
 * history - only minimum assembly test
 * @author Ilya Kislitsyn / ilyakis@ / 21.01.2022
 */
class ProductsScrollboxSectionTest : AbstractSectionTest() {
    private val assembler = ProductScrollboxAssembler()

    private val primeResolver = ResolvePrimeResponse.RESOLVER
    private val djResolver = ResolveDJUniversalProductsResponse.RESOLVER
    private val historyResolver = ResolveProductsByHistoryResponse.RESOLVER
    private val complementaryResolver = ResolveComplementaryProductGroupsResponse.RESOLVER
    private val alsoViewedResolver = ResolveAlsoViewedResponse.RESOLVER
    private val competitiveCardResolver = ResolveCompetitiveCardResponse.RESOLVER

    @Test
    fun testReportScrollboxAssemblySimple() {
        assembler.testAssembly(
            fileMap = mapOf(primeResolver to "/section/common/product/testSimpleData.json"),
            expected = "/section/common/product/testSimpleResult.json",
            config = getSimpleConfig()
        )
    }

    @Test
    fun testReportScrollboxAssemblyExpress() {
        assembler.testAssembly(
            fileMap = mapOf(primeResolver to "/section/common/product/testExpressData.json"),
            expected = "/section/common/product/testExpressResult.json",
            config = getSimpleConfig()
        )
    }

    @Test
    fun testReportWidgetInteractions() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverResponseMap = mapOf(primeResolver to "/section/common/product/testSimpleData.json"),
            expected = "/section/common/product/testSimpleSectionResult.json"
        )
    }

    @Test
    fun testReportSectionMinCount() {
        testSectionResult(
            buildWidget().apply { minCountToShow = 2 },
            assembler,
            buildAnyResolver(),
            resolverResponseMap = mapOf(primeResolver to "/section/common/product/testSimpleData.json"),
            expected = "/section/common/product/testSimpleSectionResultMinCount.json"
        )
    }

    @Test
    fun testReportSnippetInteractions() {
        testContentResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverResponseMap = mapOf(primeResolver to "/section/common/product/testSimpleData.json"),
            expected = "/section/common/product/testSimpleContentResult.json"
        )
    }

    @Test
    fun testReportSnippetInteractionsWithMore() {
        testSectionResult(
            buildWidget().apply {
                showMore = ShowMoreLink().apply {
                    url = "some url"
                    text = "some text"
                }
                timer = StartEndTimer()
            },
            assembler,
            buildAnyResolver(),
            resolverResponseMap = mapOf(primeResolver to "/section/common/product/testSimpleData.json"),
            expected = "/section/common/product/testSimpleSectionResultWithMore.json"
        )
    }

    @Test
    fun testReportScrollboxAssemblyMultiplePictures() {
        assembler.testAssembly(
            fileMap = mapOf(primeResolver to "/section/common/product/testSimpleData.json"),
            expected = "/section/common/product/testTwoPhotoResult.json",
            config = ProductScrollboxAssembler.Config(maxPictureCount = 2)
        )
    }

    @Test
    fun testReportScrollboxAssemblyMultipleItems() {
        assembler.testAssembly(
            fileMap = mapOf(primeResolver to "/section/common/product/testMultipleData.json"),
            expected = "/section/common/product/testMultipleResult.json",
            config = getSimpleConfig()
        )
    }

    @Test
    fun testReportScrollboxAssemblyForVisualSnippets() {
        mockRearrs("visual_carousel_rearr")
        assembler.testAssembly(
            fileMap = mapOf(primeResolver to "/section/common/product/testMultiplyVisualData.json"),
            expected = "/section/common/product/testMultiplyVisualResult.json",
            config = getSimpleConfig()
        )
    }

    @Test
    fun testReportBundleSnippetInteractions() {
        testContentResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverResponseMap = mapOf(primeResolver to "/section/common/product/testMultipleData.json"),
            expected = "/section/common/product/testMultipleBundleSnippet.json",
            filter = { snippet ->
                (snippet as? ProductSnippet)?.productSnippetParams?.offerId == "bundle_offer_id"
            }
        )
    }

    @Test
    fun testFapiInvalidResponse() {
        assembler.testAssemblyErrors(
            mapOf(primeResolver to "/section/common/product/testIncorrectNoOffer.json"),
            listOf("No offer data for result = oops-no-offer")
        )
        assembler.testAssemblyErrors(
            mapOf(primeResolver to "/section/common/product/testIncorrectCorrupted.json"),
            listOf("Unexpected empty collections in response")
        )
    }

    @Test
    fun testDjScrollboxAssembly() {
        assembler.testAssembly(
            fileMap = mapOf(djResolver to "/section/common/product/djFapiResponse.json"),
            expected = "/section/common/product/djAssembled.json",
            config = getSimpleConfig()
        )
    }

    @Test
    fun testCompetitiveOfferAssembly() {
        testContentResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverResponseMap = mapOf(competitiveCardResolver to "/section/common/product/competitiveOfferResponse.json"),
            expected = "/section/common/product/testCompetitiveOfferResult.json"
        )
    }

    @Test
    fun testDjWidgetInteractions() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildDjResolver(),
            resolverResponseMap = mapOf(djResolver to "/section/common/product/djFapiResponse.json"),
            expected = "/section/common/product/djSectionResult.json"
        )
    }

    @Test
    fun testDjSnippetInteractions() {
        testContentResult(
            buildWidget(),
            assembler,
            buildDjResolver(),
            resolverResponseMap = mapOf(djResolver to "/section/common/product/djFapiResponse.json"),
            expected = "/section/common/product/djContentResult.json"
        )
    }

    @Test
    fun testHistoryScrollboxAssembly() {
        assembler.testAssembly(
            fileMap = mapOf(historyResolver to "/section/common/product/historyFapiResponse.json"),
            expected = "/section/common/product/historyAssembled.json",
            config = getSimpleConfig()
        )
    }

    @Test
    fun testAlsoViewedScrollboxAssembly() {
        assembler.testAssembly(
            fileMap = mapOf(alsoViewedResolver to "/section/common/product/alsoViewed.json"),
            expected = "/section/common/product/alsoViewedAssembled.json",
            config = getSimpleConfig()
        )
    }

    @Test
    fun testScrollboxPreorderAction() {
        testContentResult(
            buildWidget(),
            assembler,
            buildDjResolver(),
            resolverResponseMap = mapOf(djResolver to "/section/common/product/testPreorderData.json"),
            expected = "/section/common/product/testPreorderResult.json"
        )
    }

    @Test
    fun testComplementaryScrollboxAssembly() {
        assembler.testAssembly(
            fileMap = mapOf(complementaryResolver to "/section/common/product/complementaryProductGroups.json"),
            expected = "/section/common/product/complementaryAssembled.json",
            config = getSimpleConfig()
        )
    }

    private fun getSimpleConfig(): ProductScrollboxAssembler.Config {
        return ProductScrollboxAssembler.Config(maxPictureCount = 1)
    }

    private fun buildWidget(): ProductsScrollboxSection {
        return ProductsScrollboxSection().apply {
            addDefParams()
            minCountToShow = 0
            showMore = null
        }
    }

    @Test
    fun testDiscountBadge() {
        assertEquals(null, testOffer().buildDiscountBadge(null))
        assertEquals(
            null,
            testOffer(
                discount = FapiDiscount(
                    oldPrice = testPrice(),
                    oldDiscountPrice = testPrice()
                )
            ).buildDiscountBadge(null)
        )

        // without promo
        val percent = BigDecimal.valueOf(12.3)
        assertEquals(
            ProductSnippet.DiscountBadge(
                description = "–12.3%",
                isPersonalDiscount = false
            ),
            testOffer(
                discount = FapiDiscount(
                    oldPrice = testPrice(),
                    oldDiscountPrice = testPrice(),
                    percent = percent
                )
            ).buildDiscountBadge(null)
        )

        // some promo - non-personal
        assertEquals(
            ProductSnippet.DiscountBadge(
                description = "–12.3%",
                isPersonalDiscount = false
            ),
            testOffer(
                discount = FapiDiscount(oldPrice = testPrice(), oldDiscountPrice = testPrice(), percent = percent),
            ).buildDiscountBadge(
                promos = listOf(
                    testPromo(type = FapiPromoType.CASHBACK)
                )
            )
        )

        // personal only on personal promo
        assertEquals(
            ProductSnippet.DiscountBadge(
                description = "–12.3%",
                isPersonalDiscount = true
            ),
            testOffer(
                discount = FapiDiscount(oldPrice = testPrice(), oldDiscountPrice = testPrice(), percent = percent),
            ).buildDiscountBadge(
                promos = listOf(
                    testPromo(
                        type = FapiPromoType.DIRECT_DISCOUNT,
                        isPersonal = true,
                    )
                )
            )
        )
    }

    @Test
    fun testReportScrollboxAssemblyTokenBadges() {
        mockRearrs("market_resale_goods_exp=1")
        mockFlags("hypeGoodsOnKt")

        assembler.testAssembly(
            fileMap = mapOf(primeResolver to "/section/common/product/testTokenBadgesData.json"),
            expected = "/section/common/product/testTokenBadgesResult.json",
            config = getSimpleConfig()
        )
    }

    private fun buildDjResolver() = buildResolver(
        "resolveDJUniversalProducts", mapOf(
            "djPlace" to "somePlace",
            "page" to "11111",
            "other" to "invalid",
            "rawParams" to mapOf(
                "raw1" to "value1",
                "raw2" to "value2",
            )
        )
    )
}
