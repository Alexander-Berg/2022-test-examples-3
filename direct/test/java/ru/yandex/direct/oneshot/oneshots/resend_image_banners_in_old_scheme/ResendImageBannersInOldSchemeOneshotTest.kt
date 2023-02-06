package ru.yandex.direct.oneshot.oneshots.resend_image_banners_in_old_scheme


import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.core.entity.banner.model.BannerImageOpts
import ru.yandex.direct.core.entity.banner.model.old.OldStatusBannerImageModerate
import ru.yandex.direct.core.entity.bs.resync.queue.service.BsResyncService
import ru.yandex.direct.core.testing.data.TestBanners
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.TextBannerInfo
import ru.yandex.direct.core.testing.repository.TestBannerImageRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.Tables.BANNER_IMAGES
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.oneshot.configuration.OneshotTest
import ru.yandex.direct.oneshot.oneshots.resend_image_banners_in_old_scheme.repository.ResendImageBannersInOldSchemeRepository
import ru.yandex.direct.oneshot.oneshots.uc.UacConverterYtRepository
import ru.yandex.direct.test.utils.randomPositiveLong
import ru.yandex.direct.ytwrapper.client.YtProvider
import ru.yandex.direct.ytwrapper.model.YtCluster
import ru.yandex.direct.ytwrapper.model.YtOperator

@OneshotTest
@RunWith(JUnitParamsRunner::class)
class ResendImageBannersInOldSchemeOneshotTest {
    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    lateinit var steps: Steps

    @Autowired
    lateinit var dslContextProvider: DslContextProvider

    @Autowired
    lateinit var shardHelper: ShardHelper

    @Autowired
    lateinit var resendImageBannersInOldSchemeRepository: ResendImageBannersInOldSchemeRepository

    @Autowired
    lateinit var bsResyncService: BsResyncService

    @Autowired
    lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Autowired
    lateinit var testBannerImageRepository: TestBannerImageRepository

    private var uacConverterYtRepository = mock<UacConverterYtRepository>()
    private var ytProvider = mock<YtProvider>()
    private var ytOperator = mock<YtOperator>()
    lateinit var resendImageBannersInOldSchemeOneshot: ResendImageBannersInOldSchemeOneshot
    lateinit var clientInfo: ClientInfo

    @Before
    fun init() {
        whenever(ytProvider.getOperator(any())).thenReturn(ytOperator)
        whenever(ytOperator.exists(any())).thenReturn(true)
        MockitoAnnotations.openMocks(this)

        clientInfo = steps.clientSteps().createDefaultClient()
        resendImageBannersInOldSchemeOneshot = ResendImageBannersInOldSchemeOneshot(
            ytProvider,
            uacConverterYtRepository,
            shardHelper,
            resendImageBannersInOldSchemeRepository,
            bsResyncService,
            ppcPropertiesSupport,
        )
    }

    fun testOptsData() = listOf(
        listOf(
            "Есть single_ad_to_bs и bannerId > 0",
            setOf(BannerImageOpts.SINGLE_AD_TO_BS), randomPositiveLong(), 0L, true
        ),
        listOf(
            "Нет single_ad_to_bs и bannerId > 0",
            null, 5L, 5L, false
        ),
        listOf(
            "Пустой opts и bannerId > 0",
            emptySet<BannerImageOpts>(), 7L, 7L, false
        ),
        listOf(
            "Есть single_ad_to_bs и bannerId = 0",
            setOf(BannerImageOpts.SINGLE_AD_TO_BS), randomPositiveLong(), 0L, true
        ),
    )

    @Test
    @Parameters(method = "testOptsData")
    @TestCaseName("{0}")
    fun testSingleAdToBsFlagAndSendToBs(
        description: String,
        bannerImageOpts: Set<BannerImageOpts>?,
        bsImageBannerId: Long,
        expectBsImageBannerId: Long,
        expectInBsQueue: Boolean
    ) {
        val bannerInfo = createImageBanner(bsImageBannerId)
        val bannerId = bannerInfo.bannerId

        if (bannerImageOpts != null) {
            testBannerImageRepository.updateImageOpts(bannerInfo.shard, bannerId, bannerImageOpts)
        }

        mockReadTableByRowRange(listOf(bannerInfo.campaignId))

        val input = Param(YtCluster.HAHN, "path", 1)
        resendImageBannersInOldSchemeOneshot.execute(input, null)

        val bsBannerIdAndOptsByBid = getOptsAndBsBannerIdByBannerId(bannerInfo.shard, listOf(bannerId))
        val bsResyncQueue = bsResyncService.getBsResyncItemsByCampaignIds(listOf(bannerInfo.campaignId))
            .firstOrNull()

        val expectOpts = if (expectInBsQueue) "" else bannerImageOpts?.joinToString()

        SoftAssertions().apply {
            assertThat(bsBannerIdAndOptsByBid)
                .`as`("Проверка картиночного баннера")
                .hasSize(1)
                .containsEntry(bannerId, Pair(expectBsImageBannerId, expectOpts))
            if (expectInBsQueue) {
                assertThat(bsResyncQueue?.bannerId)
                    .`as`("Проверка очереди отправки в БК")
                    .isNotNull
                    .isEqualTo(bannerId)
            } else {
                assertThat(bsResyncQueue)
                    .`as`("Проверка очереди отправки в БК")
                    .isNull()
            }
        }.assertAll()
    }

    private fun createImageBanner(bsImageBannerId: Long): TextBannerInfo {
        val banner = TestBanners.activeTextBanner()
            .withBsBannerId(randomPositiveLong())

        val bannerInfo = steps.bannerSteps().createBanner(banner)
        steps.bannerSteps().createBannerImage(
            bannerInfo,
            steps.bannerSteps().createBannerImageFormat(clientInfo),
            TestBanners.defaultBannerImage(bannerInfo.bannerId, RandomStringUtils.randomAlphanumeric(16))
                .withStatusModerate(OldStatusBannerImageModerate.READY)
                .withBsBannerId(bsImageBannerId)
                .withName("name")
        )
        return bannerInfo
    }

    private fun mockReadTableByRowRange(camapignIds: List<Long>) {
        Mockito.doAnswer {
            camapignIds
        }.`when`(uacConverterYtRepository)
            .getCampaignIdsFromYtTable(
                eq(YtCluster.HAHN),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong()
            )
    }

    private fun getOptsAndBsBannerIdByBannerId(
        shard: Int,
        bannerIds: List<Long>,
    ): Map<Long, Pair<Long, String?>> = dslContextProvider.ppc(shard)
        .select(BANNER_IMAGES.BID, BANNER_IMAGES.BANNER_ID, BANNER_IMAGES.OPTS)
        .from(BANNER_IMAGES)
        .where(BANNER_IMAGES.BID.`in`(bannerIds))
        .fetchMap(BANNER_IMAGES.BID)
        { (_, bannerId, opts) ->
            Pair(bannerId, opts)
        }
}
