package ru.yandex.market.mapi.section.product.ugc

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveProductReviewsResponse
import ru.yandex.market.mapi.client.fapi.response.ResolveSkuInfoResponse
import ru.yandex.market.mapi.client.fapi.response.ResolveVideosByProductIdResponse
import ru.yandex.market.mapi.section.AbstractSectionTest

/**
 * @author Ilya Kislitsyn / ilyakis@ / 11.04.2022
 */
class ProductReviewsPhotoGallerySectionTest : AbstractSectionTest() {
    private val assembler = ProductReviewsPhotoGalleryAssembler()
    val resolverMap = mapOf(
        ResolveSkuInfoResponse.RESOLVER to "/section/product/resolveSkuInfo.json",
        ResolveProductReviewsResponse.RESOLVER to "/section/product/ugc/resolveProductReviews.json",
        ResolveVideosByProductIdResponse.RESOLVER to "/section/product/ugc/resolveVideosByProductId.json",
    )

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            fileMap = resolverMap,
            expected = "/section/product/ugc/reviewsPhotoGalleryAssembly.json",
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverMap,
            "/section/product/ugc/reviewsPhotoGallerySection.json"
        )
    }

    @Test
    fun testContentInteractions() {
        testContentResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverResponseMap = resolverMap,
            expected = "/section/product/ugc/reviewsPhotoGalleryResult.json"
        )
    }

    private fun buildWidget(): ProductReviewsPhotoGallerySection {
        return ProductReviewsPhotoGallerySection().apply {
            addDefParams()
        }
    }
}
