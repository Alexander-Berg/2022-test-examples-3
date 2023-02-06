package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader

import org.assertj.core.api.Assertions
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject
import ru.yandex.direct.core.entity.banner.model.BannerToSendShowTitleAndBodyFlagForBsExport
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResourcesStat
import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate.NO
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType.BANNER_SHOW_TITLE_AND_BODY
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResource.Builder

internal class BannerShowTitleAndBodyLoaderTest {
    private lateinit var newBannerTypedRepository: BannerTypedRepository
    private lateinit var bannerShowTitleAndBodyLoader: BannerShowTitleAndBodyLoader
    private lateinit var bsOrderIdCalculator: BsOrderIdCalculator

    @BeforeEach
    fun setUp() {
        newBannerTypedRepository = mock(BannerTypedRepository::class.java)
        bsOrderIdCalculator = mock(BsOrderIdCalculator::class.java)
        val context = BannerResourcesLoaderContext(newBannerTypedRepository, BannerResourcesHelper(bsOrderIdCalculator))
        bannerShowTitleAndBodyLoader = BannerShowTitleAndBodyLoader(context)
    }

    @Test
    fun deletedBannerCreativeTest() {
        mockBannerServices()
        val `object` = BsExportBannerResourcesObject.Builder()
            .setBid(1L)
            .setResourceType(BANNER_SHOW_TITLE_AND_BODY)
            .setDeleted(true)
            .build()
        val resourceFromDb = bannerWithCommonFields
        doReturn(listOf(resourceFromDb))
            .`when`(newBannerTypedRepository)
            .getSafely(anyInt(), anyCollection(), eq(BannerToSendShowTitleAndBodyFlagForBsExport::class.java))
        val res = bannerShowTitleAndBodyLoader.loadResources(SHARD, listOf(`object`))
        val expectedStat = BannerResourcesStat().setSent(1).setCandidates(1)
        val expectedBannerResource = resourceWithCommonFields
            .setResource(null)
            .build()
        Assertions.assertThat(res.resources).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(expectedBannerResource)
        Assertions.assertThat(res.stat).isEqualToComparingFieldByFieldRecursively(expectedStat)
    }

    @Test
    fun deletedBannerCreativeAndBannerTest() {
        val `object` = BsExportBannerResourcesObject.Builder()
            .setBid(2L)
            .setResourceType(BANNER_SHOW_TITLE_AND_BODY)
            .setDeleted(true)
            .build()
        doReturn(listOf<BannerToSendShowTitleAndBodyFlagForBsExport>())
            .`when`(newBannerTypedRepository)
            .getSafely(anyInt(), anyCollection(), eq(BannerToSendShowTitleAndBodyFlagForBsExport::class.java))
        val expectedStat = BannerResourcesStat().setSent(0).setCandidates(1)
        val res = bannerShowTitleAndBodyLoader.loadResources(SHARD, listOf(`object`))
        Assertions.assertThat(res.resources).isEmpty()
        Assertions.assertThat(res.stat).isEqualToComparingFieldByFieldRecursively(expectedStat)
    }

    @Test
    fun trueValueTest() {
        mockBannerServices()
        val `object` = BsExportBannerResourcesObject.Builder()
            .setBid(2L)
            .setResourceType(BANNER_SHOW_TITLE_AND_BODY)
            .build()
        val resourceFromDb = bannerWithCommonFields
            .withCreativeId(123L)
            .withCreativeStatusModerate(NO)
            .withShowTitleAndBody(true)
        doReturn(listOf(resourceFromDb))
            .`when`(newBannerTypedRepository)
            .getSafely(anyInt(), anyCollection(), eq(BannerToSendShowTitleAndBodyFlagForBsExport::class.java))
        val res = bannerShowTitleAndBodyLoader.loadResources(SHARD, listOf(`object`))
        val expectedStat = BannerResourcesStat().setSent(1).setCandidates(1)
        val expectedResource = resourceWithCommonFields
            .setResource(true)
            .build()
        Assertions.assertThat(res.resources).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(expectedResource)
        Assertions.assertThat(res.stat).isEqualToComparingFieldByFieldRecursively(expectedStat)
    }

    @Test
    fun falseValueTest() {
        mockBannerServices()
        val `object` = BsExportBannerResourcesObject.Builder()
            .setBid(2L)
            .setResourceType(BANNER_SHOW_TITLE_AND_BODY)
            .build()
        val resourceFromDb = bannerWithCommonFields
            .withCreativeId(123L)
            .withCreativeStatusModerate(NO)
            .withShowTitleAndBody(false)
        doReturn(listOf(resourceFromDb))
            .`when`(newBannerTypedRepository)
            .getSafely(anyInt(), anyCollection(), eq(BannerToSendShowTitleAndBodyFlagForBsExport::class.java))
        val res = bannerShowTitleAndBodyLoader.loadResources(SHARD, listOf(`object`))
        val expectedStat = BannerResourcesStat().setSent(1).setCandidates(1)
        val expectedResource = resourceWithCommonFields
            .setResource(false)
            .build()
        Assertions.assertThat(res.resources).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(expectedResource)
        Assertions.assertThat(res.stat).isEqualToComparingFieldByFieldRecursively(expectedStat)
    }

    @Test
    fun notDeletedObjectButAbsentInDbTest() {
        val `object` = BsExportBannerResourcesObject.Builder()
            .setBid(2L)
            .setResourceType(BANNER_SHOW_TITLE_AND_BODY)
            .build()
        doReturn(listOf<BannerToSendShowTitleAndBodyFlagForBsExport>())
            .`when`(newBannerTypedRepository)
            .getSafely(anyInt(), anyCollection(), eq(BannerToSendShowTitleAndBodyFlagForBsExport::class.java))
        val expectedStat = BannerResourcesStat().setSent(0).setCandidates(1)
        val res = bannerShowTitleAndBodyLoader.loadResources(SHARD, listOf(`object`))
        Assertions.assertThat(res.resources).isEmpty()
        Assertions.assertThat(res.stat).isEqualToComparingFieldByFieldRecursively(expectedStat)
    }

    private val resourceWithCommonFields: Builder<Boolean?>
        private get() = Builder<Boolean?>()
            .setBid(2L)
            .setPid(3L)
            .setCid(5L)
            .setBsBannerId(40L)
            .setOrderId(30L)
    private val bannerWithCommonFields: BannerToSendShowTitleAndBodyFlagForBsExport
        private get() = TextBanner()
            .withId(2L)
            .withAdGroupId(3L)
            .withCampaignId(5L)
            .withBsBannerId(40L)

    private fun mockBannerServices() {
        `when`(bsOrderIdCalculator.calculateOrderIdIfNotExist(anyInt(), anyCollection())).thenReturn(mapOf(5L to 30L))
    }

    companion object {
        private const val SHARD = 1
    }
}
