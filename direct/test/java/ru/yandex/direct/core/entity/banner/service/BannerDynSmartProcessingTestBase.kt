package ru.yandex.direct.core.entity.banner.service

import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.adgroup.model.DynSmartAdGroup
import ru.yandex.direct.core.entity.adgroup.model.StatusBLGenerated
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner
import ru.yandex.direct.core.entity.banner.model.PerformanceBannerMain
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestNewDynamicBanners
import ru.yandex.direct.core.testing.data.TestNewPerformanceBanners
import ru.yandex.direct.core.testing.data.TestNewPerformanceMainBanners
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.NewBannerInfo
import ru.yandex.direct.core.testing.repository.TestAdGroupRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.feature.FeatureName

@CoreTest
@RunWith(Parameterized::class)
abstract class BannerDynSmartProcessingTestBase(
    @Suppress("UNUSED") private val description: String,
    private val createAdGroup: (steps: Steps) -> AdGroupInfo,
    protected val createBanner: (steps: Steps, adGroupInfo: AdGroupInfo) -> NewBannerInfo,
    protected val prepareBanner: (steps: Steps, adGroupInfo: AdGroupInfo) -> BannerWithAdGroupId
) {
    @get:Rule
    val springMethodRule = SpringMethodRule()

    companion object {
        private fun createTextAdGroup(steps: Steps): AdGroupInfo {
            val feedInfo = steps.feedSteps().createDefaultFeed()
            val adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(feedInfo.clientInfo, feedInfo.feedId, null)
            val offerRetargeting = steps.offerRetargetingSteps().defaultOfferRetargetingForGroup(adGroupInfo)
            steps.offerRetargetingSteps().addOfferRetargetingToAdGroup(offerRetargeting, adGroupInfo)
            return adGroupInfo
        }
        private fun createDynamicFeedAdGroup(steps: Steps): AdGroupInfo {
            val clientInfo = steps.clientSteps().createDefaultClient()
            val adGroupInfo = steps.adGroupSteps().createDraftDynamicFeedAdGroup(clientInfo)
            steps.dynamicTextAdTargetsSteps().createDefaultDynamicFeedAdTarget(adGroupInfo)
            return adGroupInfo
        }
        private fun createDynamicTextAdGroup(steps: Steps): AdGroupInfo {
            val clientInfo = steps.clientSteps().createDefaultClient()
            val adGroupInfo = steps.adGroupSteps().createDraftDynamicTextAdGroup(clientInfo)
            steps.dynamicTextAdTargetsSteps().createDefaultDynamicTextAdTarget(adGroupInfo)
            return adGroupInfo
        }
        private fun createPerformanceAdGroup(steps: Steps): AdGroupInfo {
            val adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup()
            steps.performanceFilterSteps().addPerformanceFilter(adGroupInfo)
            return adGroupInfo
        }

        private fun createDynamicBanner(steps: Steps, adGroupInfo: AdGroupInfo) =
            steps.dynamicBannerSteps().createDynamicBanner(adGroupInfo)
        private fun createPerformanceBanner(steps: Steps, adGroupInfo: AdGroupInfo) =
            steps.performanceBannerSteps().createPerformanceBanner(adGroupInfo)
        private fun createPerformanceMainBanner(steps: Steps, adGroupInfo: AdGroupInfo): NewBannerInfo {
            steps.featureSteps().addClientFeature(adGroupInfo.clientId, FeatureName.SMART_NO_CREATIVES, true)
            return steps.performanceMainBannerSteps().createPerformanceMainBanner(adGroupInfo)
        }

        @Suppress("UNUSED_PARAMETER")
        private fun prepareDynamicBanner(steps: Steps, adGroupInfo: AdGroupInfo) = TestNewDynamicBanners.clientDynamicBanner()
        private fun preparePerformanceBanner(steps: Steps, adGroupInfo: AdGroupInfo): PerformanceBanner {
            val creativeInfo = steps.creativeSteps().addDefaultPerformanceCreative(adGroupInfo.clientInfo)
            return TestNewPerformanceBanners.clientPerformanceBanner(creativeInfo.creativeId)
        }
        private fun preparePerformanceMainBanner(steps: Steps, adGroupInfo: AdGroupInfo): PerformanceBannerMain {
            steps.featureSteps().addClientFeature(adGroupInfo.clientId, FeatureName.SMART_NO_CREATIVES, true)
            return TestNewPerformanceMainBanners.clientPerformanceMainBanner()
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters() = arrayOf(
            arrayOf("dynamic in text", 
                this::createTextAdGroup, this::createDynamicBanner, this::prepareDynamicBanner),
            arrayOf("performance in text",
                this::createTextAdGroup, this::createPerformanceBanner, this::preparePerformanceBanner),
            arrayOf("performance_main in text",
                this::createTextAdGroup, this::createPerformanceMainBanner, this::preparePerformanceMainBanner),
            arrayOf("dynamic in dynamic_feed",
                this::createDynamicFeedAdGroup, this::createDynamicBanner, this::prepareDynamicBanner),
            arrayOf("dynamic in dynamic_text",
                this::createDynamicTextAdGroup, this::createDynamicBanner, this::prepareDynamicBanner),
            arrayOf("performance in performance",
                this::createPerformanceAdGroup, this::createPerformanceBanner, this::preparePerformanceBanner),
            arrayOf("performance_main in performance",
                this::createPerformanceAdGroup, this::createPerformanceMainBanner, this::preparePerformanceMainBanner),
        )
    }

    @Autowired
    protected lateinit var steps: Steps
    @Autowired
    protected lateinit var testAdGroupRepository: TestAdGroupRepository
    @Autowired
    protected lateinit var adGroupRepository: AdGroupRepository

    protected lateinit var adGroupInfo: AdGroupInfo

    @Before
    fun createAdGroup() {
        adGroupInfo = createAdGroup(steps)
    }

    protected fun updateAdGroup(statusModerate: StatusModerate, statusBLGenerated: StatusBLGenerated) {
        testAdGroupRepository.updateStatusModerate(adGroupInfo.shard, listOf(adGroupInfo.adGroupId), statusModerate)
        testAdGroupRepository.updateStatusBlGenerated(adGroupInfo.shard, listOf(adGroupInfo.adGroupId), statusBLGenerated)
    }

    protected fun getAdGroup(): DynSmartAdGroup {
        return adGroupRepository.getAdGroups(adGroupInfo.shard, listOf(adGroupInfo.adGroupId))[0] as DynSmartAdGroup
    }
}
