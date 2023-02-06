package ru.yandex.market.mapi.section.product.general

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveProductOffersResponse
import ru.yandex.market.mapi.client.fapi.response.ResolveSkuInfoResponse
import ru.yandex.market.mapi.core.MapiHeaders
import ru.yandex.market.mapi.core.util.mockMapiContext
import ru.yandex.market.mapi.section.AbstractSectionTest
import ru.yandex.market.mapi.section.product.content.ProductGalleryAssembler
import ru.yandex.market.mapi.section.product.content.ProductGalleryAssembler.Companion.THREE_DIMENSION_RESOLVER
import ru.yandex.market.mapi.section.product.content.ProductGallerySection

/**
 * @author Ilya Kislitsyn / ilyakis@ / 11.04.2022
 */
class ProductGallerySectionTest : AbstractSectionTest() {
    private val assembler = ProductGalleryAssembler(randomStringGenerator = { " " })

    private val resolversMap = mapOf(
        ResolveSkuInfoResponse.RESOLVER to "/section/product/resolveSkuInfo.json",
        ResolveProductOffersResponse.RESOLVER to "/section/product/resolveProductOffers.json",
        THREE_DIMENSION_RESOLVER to "/section/product/content/resolveThreeDimensionalModelData.json"
    )

    private fun initContext(platform: String, osVersion: String? = null) {
        mockMapiContext { context ->
            context.appPlatform = platform
            context.osVersionRaw = osVersion
        }
    }

    @Test
    fun testAssemblyForAndroidAllowVideos() {
        initContext(platform = MapiHeaders.PLATFORM_ANDROID, osVersion = "30")
        assembler.testAssembly(
            fileMap = resolversMap,
            expected = "/section/product/content/galleryAssembly.json",
            config = buildConfig()
        )
    }

    @Test
    fun testAssemblyForAndroidSkipVideos() {
        initContext(platform = MapiHeaders.PLATFORM_ANDROID, osVersion = "23")
        assembler.testAssembly(
            fileMap = resolversMap,
            expected = "/section/product/content/galleryAssemblySkipVideos.json",
            config = buildConfig()
        )
    }

    @Test
    fun testAssemblyForIos() {
        initContext(platform = MapiHeaders.PLATFORM_IOS)
        assembler.testAssembly(
            fileMap = resolversMap,
            expected = "/section/product/content/galleryAssembly.json",
            config = buildConfig()
        )
    }

    @Test
    fun testContentResultForIos() {
        initContext(platform = MapiHeaders.PLATFORM_IOS)
        testContentResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverResponseMap = resolversMap,
            expected = "/section/product/content/galleryAssemblyResult.json"
        )
    }

    @Test
    fun testContentResultForAndroidAllowVideos() {
        initContext(platform = MapiHeaders.PLATFORM_ANDROID, osVersion = "28")
        testContentResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverResponseMap = resolversMap,
            expected = "/section/product/content/galleryAssemblyResult.json"
        )
    }

    @Test
    fun testContentResultForAndroidSkipVideos() {
        initContext(platform = MapiHeaders.PLATFORM_ANDROID, osVersion = "23")
        testContentResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverResponseMap = resolversMap,
            expected = "/section/product/content/galleryAssemblyResultSkipVideos.json"
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolversMap,
            "/section/product/content/gallerySection.json",
            config = buildConfig()
        )
    }

    private fun buildWidget(): ProductGallerySection {
        return ProductGallerySection().apply {
            addDefParams()
        }
    }

    private fun buildConfig(): ProductGalleryAssembler.Config {
        return ProductGalleryAssembler.Config().apply {
            height = 123
            shouldMultiply = true
            horizontalInsets = 42
            aspectRatio = 10.0
        }
    }
}
