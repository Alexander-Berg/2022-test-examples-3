package ru.yandex.direct.core.entity.banner.service

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields
import ru.yandex.direct.core.entity.banner.model.InternalBanner
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestNewInternalBanners
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.NewInternalBannerInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.result.MassResult
import ru.yandex.direct.test.utils.TestUtils
import ru.yandex.direct.testing.matchers.result.MassResultMatcher

@CoreTest
@RunWith(JUnitParamsRunner::class)
class InternalBannerSuspendResumeServiceTest {

    companion object {
        private const val RESUMED = true
        private const val SUSPENDED = false

        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var bannerSuspendResumeService: BannerSuspendResumeService

    @Autowired
    private lateinit var bannerRepository: BannerTypedRepository
    private lateinit var adGroupInfo: AdGroupInfo
    private lateinit var clientId: ClientId
    private var clientUid: Long = 0


    fun testData_ForResumeSuspendInternalBanners_CheckStoppedByUrlMonitoring() = listOf(
            listOf(SUSPENDED, false, false),
            listOf(SUSPENDED, true, false),
            listOf(RESUMED, false, false),
            listOf(RESUMED, true, false),
    )

    @Before
    fun setUp() {
        adGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup()
        clientId = adGroupInfo.clientId
        clientUid = adGroupInfo.clientInfo.uid
    }


    @Test
    @Parameters(method = "testData_ForResumeSuspendInternalBanners_CheckStoppedByUrlMonitoring")
    @TestCaseName("resumeSuspendInternalBanners for isEnableBanner={0}, isStoppedByUrlMonitoring={1},"
            + " expectedIsStoppedByUrlMonitoring={2}")
    fun resumeSuspendInternalBanners_CheckStoppedByUrlMonitoring(isEnableBanner: Boolean,
                                                                 isStoppedByUrlMonitoring: Boolean,
                                                                 expectedIsStoppedByUrlMonitoring: Boolean) {
        val bannerInfo = createInternalBanner(!isEnableBanner, isStoppedByUrlMonitoring)

        val result = applyResume(bannerInfo.bannerId, isEnableBanner)
        TestUtils.assumeThat(result, MassResultMatcher.isSuccessful())

        val actual = getBanner(bannerInfo.shard, bannerInfo.bannerId)
        assertThat(actual.statusShow)
                .`as`("statusShow")
                .isEqualTo(isEnableBanner)
        assertThat(actual.isStoppedByUrlMonitoring)
                .`as`("isStoppedByUrlMonitoring")
                .isEqualTo(expectedIsStoppedByUrlMonitoring)
    }


    private fun createInternalBanner(statusShow: Boolean, isStoppedByUrlMonitoring: Boolean): NewInternalBannerInfo {
        val banner = TestNewInternalBanners.fullInternalBanner(adGroupInfo.campaignId, adGroupInfo.adGroupId)
                .withStatusShow(statusShow)
                .withIsStoppedByUrlMonitoring(isStoppedByUrlMonitoring)
        return steps.internalBannerSteps().createInternalBanner(NewInternalBannerInfo()
                .withAdGroupInfo(adGroupInfo)
                .withCampaignInfo(adGroupInfo.campaignInfo)
                .withBanner(banner))
    }

    /**
     * Обновляет у баннера `statusShow` на `resume` через вызов [BannerSuspendResumeService]
     */
    private fun applyResume(bannerId: Long, resume: Boolean): MassResult<Long> {
        val changes = listOf(
                ModelChanges(bannerId, BannerWithSystemFields::class.java).process(resume,
                        BannerWithSystemFields.STATUS_SHOW))
        return bannerSuspendResumeService.suspendResumeBanners(clientId, clientUid, changes, resume)
    }

    private fun getBanner(shard: Int, bannerId: Long): InternalBanner {
        return bannerRepository.getStrictlyFullyFilled(shard, listOf(bannerId), InternalBanner::class.java)[0]
    }

}
