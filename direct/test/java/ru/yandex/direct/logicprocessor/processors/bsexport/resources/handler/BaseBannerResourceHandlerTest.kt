package ru.yandex.direct.logicprocessor.processors.bsexport.resources.handler

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import ru.yandex.adv.direct.banner.resources.BannerResources
import ru.yandex.direct.bstransport.yt.repository.resources.BaseBannerResourcesYtRepository
import ru.yandex.direct.bstransport.yt.utils.CaesarIterIdGenerator
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcProperty
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.common.log.service.LogBsExportEssService
import ru.yandex.direct.core.entity.banner.type.image.BannerImageRepository
import ru.yandex.direct.core.entity.banner.type.image.container.ImageBannerBsData
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResource
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResourcesStat
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.IBannerResourceLoader
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.LoaderResult

internal class BaseBannerResourceHandlerTest {
    private lateinit var bannerResourceLoader: IBannerResourceLoader<TestResource>
    private lateinit var resourcesYtRepository: BaseBannerResourcesYtRepository
    private lateinit var testBannerResourceHandler: TestBannerResourceHandler
    private lateinit var caesarIterIdGenerator: CaesarIterIdGenerator
    private lateinit var bannerImageRepository: BannerImageRepository
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    companion object {
        private const val SHARD = 5
        private const val BID = 1L
        private const val BANNER_ID = 303L
        private const val IMAGE_ID = 1001L
        private const val IMAGE_BANNER_ID = 505L
    }

    @BeforeEach
    fun init() {
        bannerResourceLoader = Mockito.mock(IBannerResourceLoader::class.java) as IBannerResourceLoader<TestResource>
        resourcesYtRepository = Mockito.mock(BaseBannerResourcesYtRepository::class.java)
        caesarIterIdGenerator = Mockito.mock(CaesarIterIdGenerator::class.java)
        bannerImageRepository = Mockito.mock(BannerImageRepository::class.java)
        ppcPropertiesSupport = mock()
    }

    @Test
    fun handleTest() {
        setUpProperties()
        val expectedBannerResourceYtRecords = getExpectedBannerResourceYtRecords()

        testHandle(expectedBannerResourceYtRecords)
    }

    @Test
    fun handle_WithoutImageCopyBanner_ExportOfImageCopyBannerResourcesEnabledTest() {
        setUpProperties(true)
        val expectedBannerResourceYtRecords = getExpectedBannerResourceYtRecords()

        testHandle(expectedBannerResourceYtRecords)
    }

    @Test
    fun handle_WithImageCopyBanner_ExportForImageCopyBannerResourcesEnabledTest() {
        setUpProperties(true)
        val expectedBannerResourceYtRecords = getExpectedBannerResourceYtRecords(hasImageBannerResource = true)
        val bidToBannerImageBsData = mapOf(
            BID to ImageBannerBsData(BID, IMAGE_ID, 5L, IMAGE_BANNER_ID, false)
        )

        testHandle(expectedBannerResourceYtRecords, bidToBannerImageBsData = bidToBannerImageBsData)
    }

    @Test
    fun handle_WithImageCopyBanner_ExportForImageCopyBannerResourcesDisabledTest() {
        setUpProperties(false)
        val expectedBannerResourceYtRecords = getExpectedBannerResourceYtRecords()
        val bidToBannerImageBsData = mapOf(
            BID to ImageBannerBsData(BID, IMAGE_ID, 5L, IMAGE_BANNER_ID, false)
        )

        testHandle(expectedBannerResourceYtRecords, bidToBannerImageBsData = bidToBannerImageBsData)
    }

    @Test
    fun handle_WithImageCopyBanner_ExportForImageCopyBannerResourcesDisabled_EnabledForSpecificBidsTest() {
        setUpProperties(false, setOf(BID))
        val expectedBannerResourceYtRecords = getExpectedBannerResourceYtRecords(hasImageBannerResource = true)
        val bidToBannerImageBsData = mapOf(
            BID to ImageBannerBsData(BID, IMAGE_ID, 5L, IMAGE_BANNER_ID, false)
        )

        testHandle(expectedBannerResourceYtRecords, bidToBannerImageBsData = bidToBannerImageBsData)
    }

    private fun setUpProperties(
        exportForImageCopyBannerEnabled: Boolean = false,
        imageCopyBannerBidsToExport: Set<Long> = emptySet()
    ) {
        val exportAssetsOnImageCopyBannersEnabledProp = mock<PpcProperty<Boolean>>()
        whenever(exportAssetsOnImageCopyBannersEnabledProp.getOrDefault(any()))
            .doReturn(exportForImageCopyBannerEnabled)
        whenever(ppcPropertiesSupport.get(eq(PpcPropertyNames.BS_EXPORT_ASSETS_FOR_IMAGE_COPY_BANNER_ENABLED), any()))
            .doReturn(exportAssetsOnImageCopyBannersEnabledProp)

        val imageCopyBannerBidsToExportAssetsProp = mock<PpcProperty<Set<Long>>>()
        whenever(imageCopyBannerBidsToExportAssetsProp.getOrDefault(any()))
            .doReturn(imageCopyBannerBidsToExport)
        whenever(ppcPropertiesSupport.get(eq(PpcPropertyNames.BS_EXPORT_ASSETS_FOR_IMAGE_COPY_BANNER_BY_BIDS), any()))
            .doReturn(imageCopyBannerBidsToExportAssetsProp)

        val clock = Clock.fixed(Instant.ofEpochSecond(1586345827L), ZoneOffset.UTC) // 2020-04-08
        testBannerResourceHandler = TestBannerResourceHandler(
            bannerResourceLoader, resourcesYtRepository,
            caesarIterIdGenerator, bannerImageRepository, ppcPropertiesSupport, clock
        )
    }

    private fun getExpectedBannerResourceYtRecords(
        hasBannerResource: Boolean = true,
        hasImageBannerResource: Boolean = false
    ): List<BannerResources> {
        val bannerResource = BannerResources.newBuilder()
            .setBannerId(BANNER_ID)
            .setOrderId(101L)
            .setExportId(BID)
            .setAdgroupId(2L)
            .setIterId(123L)
            .setUpdateTime(1586345827L)
            .build()
        val imageBannerResource = BannerResources.newBuilder()
            .setBannerId(IMAGE_BANNER_ID)
            .setOrderId(101L)
            .setExportId(IMAGE_ID)
            .setAdgroupId(2L)
            .setIterId(123L)
            .setUpdateTime(1586345827L)
            .build()

        return if (hasBannerResource) {
            if (hasImageBannerResource) listOf(bannerResource, imageBannerResource) else listOf(bannerResource)
        } else {
            if (hasImageBannerResource) listOf(imageBannerResource) else listOf()
        }
    }

    private fun testHandle(
        expectedBannerResourceYtRecords: List<BannerResources>,
        objectDeleted: Boolean = false,
        bidToBannerImageBsData: Map<Long, ImageBannerBsData> = emptyMap()
    ) {
        val resourceObject = BsExportBannerResourcesObject.Builder()
            .setBid(BID)
            .setResourceType(null)
            .setDeleted(objectDeleted)
            .build()
        val objects = listOf(resourceObject)
        val bannerResource = BannerResource.Builder<TestResource>()
            .setBid(BID)
            .setPid(2L)
            .setCid(3L)
            .setBsBannerId(BANNER_ID)
            .setOrderId(101L)
            .setResource(TestResource())
            .build()
        Mockito.`when`(bannerResourceLoader.loadResources(ArgumentMatchers.anyInt(), ArgumentMatchers.anyCollection()))
            .thenReturn(LoaderResult(java.util.List.of(bannerResource), BannerResourcesStat()))
        Mockito.`when`(caesarIterIdGenerator.generateCaesarIterId()).thenReturn(123L)
        Mockito.`when`(bannerImageRepository.getBannerImageIdsFromBids(SHARD, setOf(BID)))
            .thenReturn(bidToBannerImageBsData)

        testBannerResourceHandler.handle(SHARD, objects)
        val argument = ArgumentCaptor.forClass(List::class.java)
        Mockito.verify(resourcesYtRepository)
            .modify(argument.capture() as List<BannerResources>?)

        val allValues = argument.allValues
        Assertions.assertThat(allValues).containsExactlyInAnyOrder(expectedBannerResourceYtRecords)
    }

    @Test
    fun handle_ResourceNotReadyInDbTest() {
        setUpProperties()
        val resourceObject = BsExportBannerResourcesObject.Builder()
            .setBid(BID)
            .setResourceType(null)
            .build()
        val objects = listOf(resourceObject)
        Mockito.`when`(bannerResourceLoader.loadResources(ArgumentMatchers.anyInt(), ArgumentMatchers.anyCollection()))
            .thenReturn(LoaderResult(listOf(), BannerResourcesStat()))

        testBannerResourceHandler.handle(SHARD, objects)
        Mockito.verify(resourcesYtRepository, Mockito.never())
            .modify(ArgumentMatchers.any())
    }

    private class TestBannerResourceHandler constructor(
        bannerResourcesLoader: IBannerResourceLoader<TestResource>,
        bannerResourcesYtRepository: BaseBannerResourcesYtRepository,
        caesarIterIdGenerator: CaesarIterIdGenerator,
        bannerImageRepository: BannerImageRepository,
        ppcPropertiesSupport: PpcPropertiesSupport,
        clock: Clock
    ) : BaseBannerResourceHandler<TestResource>(
        bannerResourcesLoader,
        bannerResourcesYtRepository,
        Mockito.mock(LogBsExportEssService::class.java),
        caesarIterIdGenerator,
        bannerImageRepository,
        ppcPropertiesSupport,
        clock
    ) {
        override fun mapResourceToProto(): (TestResource, BannerResources.Builder) -> Unit =
            { _, _ -> }


        override fun bannerResourceType(): BannerResourceType {
            return BannerResourceType.BANNER_LOGO
        }
    }

    private class TestResource
}
