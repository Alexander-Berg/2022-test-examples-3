package ru.yandex.direct.jobs.uac.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.adgroup.container.AdGroupsSelectionCriteria
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService
import ru.yandex.direct.core.entity.banner.model.ImageBanner
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository
import ru.yandex.direct.core.entity.banner.repository.filter.BannerFilterFactory
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository
import ru.yandex.direct.core.entity.uac.TRACKING_URL
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AverageBidStrategy
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.DayBudget
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.DayBudgetShowMode
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.jobs.configuration.GrutJobsTest
import ru.yandex.direct.jobs.uac.UpdateAdsJob
import ru.yandex.direct.multitype.entity.LimitOffset
import ru.yandex.grut.objects.proto.Campaign
import ru.yandex.grut.objects.proto.client.Schema
import java.math.BigDecimal

@GrutJobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GrutUpdateAdsJobMobileContentTest {
    @Autowired
    private lateinit var steps: Steps
    @Autowired
    private lateinit var grutSteps: GrutSteps
    @Autowired
    private lateinit var updateAdsJob: UpdateAdsJob
    @Autowired
    private lateinit var grutApiService: GrutApiService
    @Autowired
    private lateinit var adGroupService: AdGroupService
    @Autowired
    private lateinit var keywordRepository: KeywordRepository
    @Autowired
    private lateinit var bannerTypedRepository: BannerTypedRepository

    private val keywords = listOf("keyword1", "keyword2")
    private val minusKeywords = listOf("minus keyword1", "minus keyword2")
    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId
    private var campaignId = -1L

    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!
        grutSteps.createClient(clientInfo)
        updateAdsJob.withShard(clientInfo.shard)

        steps.trustedRedirectSteps().addValidCounters()

        campaignId = grutSteps.createMobileAppCampaign(
            clientInfo,
            createInDirect = true,
            strategy = createAverageBidStrategyWithoutDayBudget(),
            keywords = keywords,
            minusKeywords = minusKeywords,
        )
    }

    @Test
    fun `remove keywords on update`() {
        setUpAssets()
        updateAdsJob.processGrabbedJob(campaignId.toIdString(), clientInfo.uid)

        val createdAdGroups = adGroupService.getAdGroupsBySelectionCriteria(
            AdGroupsSelectionCriteria().withCampaignIds(campaignId),
            LimitOffset.maxLimited(),
            false
        )
        assertThat(createdAdGroups).`as`("Создана одна группа").hasSize(1)
        SoftAssertions.assertSoftly { softly ->
            val adGroup = createdAdGroups[0]
            softly.assertThat(adGroup.minusKeywords).containsExactlyInAnyOrderElementsOf(minusKeywords)
            val actualKeywords = keywordRepository.getKeywordsByAdGroupId(clientInfo.shard, adGroup.id)
            softly.assertThat(actualKeywords.map { it.phrase }).containsExactlyInAnyOrderElementsOf(keywords)
        }

        grutApiService.briefGrutApi.updateBrief(
            Schema.TCampaign.newBuilder().apply {
                meta = Schema.TCampaignMeta.newBuilder().setId(campaignId).build()
                spec = Campaign.TCampaignSpec.newBuilder().apply {
                    campaignBrief = Campaign.TCampaignBrief.newBuilder()
                        .setBriefSynced(false)
                        .build()
                }.build()
            }.build(),
            setPaths = listOf("/spec/campaign_brief/brief_synced"),
            removePaths = listOf("/spec/campaign_brief/keywords", "/spec/campaign_brief/minus_keywords")
        )
        updateAdsJob.processGrabbedJob(campaignId.toIdString(), clientInfo.uid)
        val updatedAdGroups = adGroupService.getAdGroupsBySelectionCriteria(
            AdGroupsSelectionCriteria().withCampaignIds(campaignId),
            LimitOffset.maxLimited(),
            false
        )
        SoftAssertions.assertSoftly { softly ->
            val adGroup = updatedAdGroups[0]
            softly.assertThat(adGroup.minusKeywords).isEmpty()
            val actualKeywords = keywordRepository.getKeywordsByAdGroupId(clientInfo.shard, adGroup.id)
            softly.assertThat(actualKeywords).isEmpty()
        }
    }

    @Test
    fun `video creative with show title and body`() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.DISABLE_VIDEO_CREATIVE, true)
        setUpAssets(withVideo = true)

        updateAdsJob.processGrabbedJob(campaignId.toIdString(), clientInfo.uid)

        val soft = SoftAssertions()
        val banners = bannerTypedRepository.getBannersByCampaignIds(
            clientInfo.shard, listOf(campaignId)).map { it as MobileAppBanner }
        banners.forEach {
            soft.assertThat(it.showTitleAndBody).isEqualTo(true)
        }
        soft.assertAll()
    }

    @Test
    fun `html5 creative and update tracking url`() {
        setUpAssets(withHtml5 = true)

        updateAdsJob.processGrabbedJob(campaignId.toIdString(), clientInfo.uid)
        val grutCampaign = grutApiService.briefGrutApi.getBrief(campaignId)!!
        val newTrackingUrl = "https://app.appsflyer.com/com.cocoplay.fashion"

        val soft = SoftAssertions()
        var banners = bannerTypedRepository.getSafely(clientInfo.shard,
            BannerFilterFactory.bannerCampaignIdFilter(listOf(campaignId)), ImageBanner::class.java)
        soft.assertThat(banners).hasSize(1)
        soft.assertThat(banners[0].href).isEqualTo(TRACKING_URL)
        grutApiService.briefGrutApi.updateBrief(
            Schema.TCampaign.newBuilder().apply {
                meta = Schema.TCampaignMeta.newBuilder().setId(campaignId).build()
                spec = Campaign.TCampaignSpec.newBuilder().apply {
                    campaignBrief = Campaign.TCampaignBrief.newBuilder()
                        .setBriefSynced(false)
                        .setTargetHref(
                            Campaign.TCampaignBrief.TTargetHref.newBuilder().apply {
                                href = grutCampaign.spec.campaignBrief.targetHref.href
                                trackingUrl = newTrackingUrl
                            }.build()
                        )
                        .build()
                }.build()
            }.build(),
            setPaths = listOf("/spec/campaign_brief/brief_synced", "/spec/campaign_brief/target_href/tracking_url")
        )
        updateAdsJob.processGrabbedJob(campaignId.toIdString(), clientInfo.uid)
        banners = bannerTypedRepository.getSafely(clientInfo.shard,
            BannerFilterFactory.bannerCampaignIdFilter(listOf(campaignId)), ImageBanner::class.java)
        soft.assertThat(banners[0].href).isEqualTo(newTrackingUrl)
        soft.assertAll()
    }

    private fun setUpAssets(withVideo: Boolean = false, withHtml5: Boolean = false) {
        val assetIds = mutableListOf<String>()
        assetIds.add(grutSteps.createTitleAsset(clientId, "Title1"))
        assetIds.add(grutSteps.createTextAsset(clientId, "Text1"))
        if (withVideo) {
            assetIds.add(grutSteps.createDefaultVideoAsset(clientId))
        }
        if (withHtml5) {
            assetIds.add(grutSteps.createDefaultHtml5Asset(clientId))
            steps.creativeSteps().addDefaultHtml5Creative(clientInfo, 271980L)
        }
        grutSteps.setCustomAssetIdsToCampaign(campaignId, assetIds)
    }

    private fun createAverageBidStrategyWithoutDayBudget(): AverageBidStrategy {
        return AverageBidStrategy()
            .withAverageBid(BigDecimal(15))
            .withMaxWeekSum(BigDecimal(2795))
            .withDayBudget(
                DayBudget()
                    .withDailyChangeCount(0L)
                    .withDayBudget(BigDecimal.ZERO)
                    .withShowMode(DayBudgetShowMode.DEFAULT))
    }
}
