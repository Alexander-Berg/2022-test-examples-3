package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.banner.model.DynamicBanner
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner
import ru.yandex.direct.core.entity.banner.model.PerformanceBannerMain
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator
import ru.yandex.direct.core.testing.data.TestFeeds.defaultFeed
import ru.yandex.direct.core.testing.data.TestGroups
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.FeedInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.Tables
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.ess.common.utils.TablesEnum
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResource
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorTestConfiguration
import ru.yandex.adv.direct.banner.resources.FeedInfo as FeedInfoMessage

const val MARKET_BUSINESS_ID = 333L
const val MARKET_SHOP_ID = 444L
const val MARKET_FEED_ID = 555L

@ContextConfiguration(classes = [EssLogicProcessorTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class BannerFeedInfoLoaderTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var loader: BannerFeedInfoLoader

    @Autowired
    private lateinit var bsOrderIdCalculator: BsOrderIdCalculator

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    private lateinit var clientInfo: ClientInfo

    @BeforeEach
    fun beforeEach() {
        clientInfo = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun exportAbsentBanner() {
        val loadedResources = loader.loadResources(
            1, listOf(
                BsExportBannerResourcesObject.Builder()
                    .setBid(Long.MAX_VALUE)
                    .setResourceType(BannerResourceType.BANNER_FEED_INFO)
                    .build()
            )
        )
        assertThat(loadedResources.resources).isEmpty()
    }

    @Test
    fun exportTextBannerWithoutFeedOnGroup() {
        val bannerInfo = steps.bannerSteps().createActiveTextBanner()

        val loadedResources = loader.loadResources(
            bannerInfo.shard, listOf(
                BsExportBannerResourcesObject.Builder()
                    .setBid(bannerInfo.bannerId)
                    .setResourceType(BannerResourceType.BANNER_FEED_INFO)
                    .build()
            )
        )
        assertThat(loadedResources.resources)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                BannerResource.Builder<FeedInfoMessage>()
                    .setBid(bannerInfo.bannerId)
                    .setPid(bannerInfo.adGroupId)
                    .setCid(bannerInfo.campaignId)
                    .setBsBannerId(bannerInfo.banner.bsBannerId)
                    .setOrderId(bsOrderIdCalculator.calculateOrderId(bannerInfo.campaignId))
                    .setResource(null)
                    .build()
            )
    }

    @Test
    fun exportTextBannerWithFeedByBannerId() {
        val bannerInfo1 = steps.bannerSteps().createActiveTextBanner()
        // добавляем второй баннер в группу, но не ожидаем, что он выгрузится
        steps.bannerSteps().createActiveTextBanner(bannerInfo1.adGroupInfo)
        val feedInfo = createFeedWithMarketIds(bannerInfo1.clientInfo)
        createTestAdGroupsTextRecord(bannerInfo1.adGroupInfo, feedInfo.feedId)

        val loadedResources = loader.loadResources(
            bannerInfo1.shard, listOf(
                BsExportBannerResourcesObject.Builder()
                    .setBid(bannerInfo1.bannerId)
                    .setResourceType(BannerResourceType.BANNER_FEED_INFO)
                    .build()
            )
        )

        val expectedFeedInfoMessage = createFeedInfoMessage(feedInfo.feedId)
        assertThat(loadedResources.resources)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                BannerResource.Builder<FeedInfoMessage>()
                    .setBid(bannerInfo1.bannerId)
                    .setPid(bannerInfo1.adGroupId)
                    .setCid(bannerInfo1.campaignId)
                    .setBsBannerId(bannerInfo1.banner.bsBannerId)
                    .setOrderId(bsOrderIdCalculator.calculateOrderId(bannerInfo1.campaignId))
                    .setResource(expectedFeedInfoMessage)
                    .build()
            )
    }

    @Test
    fun exportPerformanceBannerWithFeedByBannerId() {
        val feedInfo = createFeedWithMarketIds(clientInfo)
        val campaignInfo = steps.campaignSteps().createActivePerformanceCampaign(clientInfo)
        val adGroupInfo = steps.adGroupSteps().createActivePerformanceAdGroup(campaignInfo, feedInfo.feedId)

        val bannerInfo1 = steps.performanceBannerSteps().createPerformanceBanner(adGroupInfo)
        // добавляем второй баннер в группу, но не ожидаем, что он выгрузится
        steps.performanceBannerSteps().createPerformanceBanner(bannerInfo1.adGroupInfo)

        val loadedResources = loader.loadResources(
            bannerInfo1.shard, listOf(
                BsExportBannerResourcesObject.Builder()
                    .setBid(bannerInfo1.bannerId)
                    .setResourceType(BannerResourceType.BANNER_FEED_INFO)
                    .build()
            )
        )

        val expectedFeedInfoMessage = createFeedInfoMessage(feedInfo.feedId)
        assertThat(loadedResources.resources)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                BannerResource.Builder<FeedInfoMessage>()
                    .setBid(bannerInfo1.bannerId)
                    .setPid(bannerInfo1.adGroupId)
                    .setCid(bannerInfo1.campaignId)
                    .setBsBannerId((bannerInfo1.getBanner() as PerformanceBanner).bsBannerId)
                    .setOrderId(campaignInfo.campaign.orderId)
                    .setResource(expectedFeedInfoMessage)
                    .build()
            )
    }

    @Test
    fun exportPerformanceBannerMainWithFeedByBannerId() {
        val feedInfo = createFeedWithMarketIds(clientInfo)
        val campaignInfo = steps.campaignSteps().createActivePerformanceCampaign(clientInfo)
        val adGroupInfo = steps.adGroupSteps().createActivePerformanceAdGroup(campaignInfo, feedInfo.feedId)
        val bannerInfo = steps.performanceMainBannerSteps().createPerformanceMainBanner(adGroupInfo)

        val loadedResources = loader.loadResources(
            bannerInfo.shard, listOf(
                BsExportBannerResourcesObject.Builder()
                    .setBid(bannerInfo.bannerId)
                    .setResourceType(BannerResourceType.BANNER_FEED_INFO)
                    .build()
            )
        )

        val expectedFeedInfoMessage = createFeedInfoMessage(feedInfo.feedId)
        assertThat(loadedResources.resources)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                BannerResource.Builder<FeedInfoMessage>()
                    .setBid(bannerInfo.bannerId)
                    .setPid(bannerInfo.adGroupId)
                    .setCid(bannerInfo.campaignId)
                    .setBsBannerId((bannerInfo.getBanner() as PerformanceBannerMain).bsBannerId)
                    .setOrderId(campaignInfo.campaign.orderId)
                    .setResource(expectedFeedInfoMessage)
                    .build()
            )
    }

    @Test
    fun exportDynamicBannerWithFeedByBannerId() {
        val campaignInfo = steps.campaignSteps().createActiveDynamicCampaign(clientInfo)
        val feedInfo = createFeedWithMarketIds(clientInfo)
        val dynamicFeedAdGroup = steps.adGroupSteps().createAdGroup(
            TestGroups.activeDynamicFeedAdGroup(campaignInfo.campaignId, feedInfo.feedId), campaignInfo
        )

        val bannerInfo1 = steps.dynamicBannerSteps().createDynamicBanner(dynamicFeedAdGroup)
        // добавляем второй баннер в группу, но не ожидаем, что он выгрузится
        steps.dynamicBannerSteps().createDynamicBanner(dynamicFeedAdGroup)

        val loadedResources = loader.loadResources(
            bannerInfo1.shard, listOf(
                BsExportBannerResourcesObject.Builder()
                    .setBid(bannerInfo1.bannerId)
                    .setResourceType(BannerResourceType.BANNER_FEED_INFO)
                    .build()
            )
        )

        val expectedFeedInfoMessage = createFeedInfoMessage(feedInfo.feedId)
        assertThat(loadedResources.resources)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                BannerResource.Builder<FeedInfoMessage>()
                    .setBid(bannerInfo1.bannerId)
                    .setPid(bannerInfo1.adGroupId)
                    .setCid(bannerInfo1.campaignId)
                    .setBsBannerId((bannerInfo1.getBanner() as DynamicBanner).bsBannerId)
                    .setOrderId(campaignInfo.campaign.orderId)
                    .setResource(expectedFeedInfoMessage)
                    .build()
            )
    }

    @Test
    fun exportDynamicBannerWithFeed_InTextAdGroup() {
        val textAdGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo)
        val feedInfo = createFeedWithMarketIds(clientInfo)
        createTestAdGroupsTextRecord(textAdGroupInfo, feedInfo.feedId)

        val bannerInfo = steps.dynamicBannerSteps().createDynamicBanner(textAdGroupInfo)

        val loadedResources = loader.loadResources(
            bannerInfo.shard, listOf(
                BsExportBannerResourcesObject.Builder()
                    .setBid(bannerInfo.bannerId)
                    .setResourceType(BannerResourceType.BANNER_FEED_INFO)
                    .build()
            )
        )

        val expectedFeedInfoMessage = createFeedInfoMessage(feedInfo.feedId)
        assertThat(loadedResources.resources)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                BannerResource.Builder<FeedInfoMessage>()
                    .setBid(bannerInfo.bannerId)
                    .setPid(bannerInfo.adGroupId)
                    .setCid(bannerInfo.campaignId)
                    .setBsBannerId((bannerInfo.getBanner() as DynamicBanner).bsBannerId)
                    .setOrderId(textAdGroupInfo.campaignInfo.campaign.orderId)
                    .setResource(expectedFeedInfoMessage)
                    .build()
            )
    }

    @Test
    fun exportTextBannerWithFeedByBannerId_FeedWithoutMarketIds() {
        val bannerInfo1 = steps.bannerSteps().createActiveTextBanner()

        // фид без market* идентификаторов, не считается фидом который нужно выгружать -- считаем что на группе нет фида
        val feedInfo = steps.feedSteps().createDefaultFeed(bannerInfo1.clientInfo)
        createTestAdGroupsTextRecord(bannerInfo1.adGroupInfo, feedInfo.feedId)

        val loadedResources = loader.loadResources(
            bannerInfo1.shard, listOf(
                BsExportBannerResourcesObject.Builder()
                    .setBid(bannerInfo1.bannerId)
                    .setResourceType(BannerResourceType.BANNER_FEED_INFO)
                    .build()
            )
        )
        assertThat(loadedResources.resources)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                BannerResource.Builder<FeedInfoMessage>()
                    .setBid(bannerInfo1.bannerId)
                    .setPid(bannerInfo1.adGroupId)
                    .setCid(bannerInfo1.campaignId)
                    .setBsBannerId(bannerInfo1.banner.bsBannerId)
                    .setOrderId(bsOrderIdCalculator.calculateOrderId(bannerInfo1.campaignId))
                    .setResource(null)
                    .build()
            )
    }

    @Test
    fun exportTextBannerWithFeedByGroupId() {
        val bannerInfo1 = steps.bannerSteps().createActiveTextBanner()
        // добавляем второй баннер в группу, и ожидаем, что он выгрузится тоже
        val bannerInfo2 = steps.bannerSteps().createActiveTextBanner(bannerInfo1.adGroupInfo)

        val feedInfo = createFeedWithMarketIds(bannerInfo1.clientInfo)
        createTestAdGroupsTextRecord(bannerInfo1.adGroupInfo, feedInfo.feedId)

        val loadedResources = loader.loadResources(
            bannerInfo1.shard, listOf(
                BsExportBannerResourcesObject.Builder()
                    .setResourceType(BannerResourceType.BANNER_FEED_INFO)
                    .setAdditionalTable(TablesEnum.ADGROUPS_TEXT)
                    .setAdditionalId(bannerInfo1.adGroupId)
                    .build()
            )
        )

        val expectedFeedInfoMessage = createFeedInfoMessage(feedInfo.feedId)
        assertThat(loadedResources.resources)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                BannerResource.Builder<FeedInfoMessage>()
                    .setBid(bannerInfo1.bannerId)
                    .setPid(bannerInfo1.adGroupId)
                    .setCid(bannerInfo1.campaignId)
                    .setBsBannerId(bannerInfo1.banner.bsBannerId)
                    .setOrderId(bsOrderIdCalculator.calculateOrderId(bannerInfo1.campaignId))
                    .setResource(expectedFeedInfoMessage)
                    .build(),
                BannerResource.Builder<FeedInfoMessage>()
                    .setBid(bannerInfo2.bannerId)
                    .setPid(bannerInfo2.adGroupId)
                    .setCid(bannerInfo2.campaignId)
                    .setBsBannerId(bannerInfo2.banner.bsBannerId)
                    .setOrderId(bsOrderIdCalculator.calculateOrderId(bannerInfo2.campaignId))
                    .setResource(expectedFeedInfoMessage)
                    .build()
            )
    }

    @Test
    fun exportPerformanceBannerWithFeedByGroupId() {
        val feedInfo = createFeedWithMarketIds(clientInfo)

        val campaignInfo = steps.campaignSteps().createActivePerformanceCampaign(clientInfo)
        val adGroupInfo = steps.adGroupSteps().createActivePerformanceAdGroup(campaignInfo, feedInfo.feedId)

        val bannerInfo1 = steps.performanceBannerSteps().createPerformanceBanner(adGroupInfo)
        // добавляем второй баннер в группу, и ожидаем, что он выгрузится тоже
        val bannerInfo2 = steps.performanceBannerSteps().createPerformanceBanner(adGroupInfo)

        val loadedResources = loader.loadResources(
            bannerInfo1.shard, listOf(
                BsExportBannerResourcesObject.Builder()
                    .setResourceType(BannerResourceType.BANNER_FEED_INFO)
                    .setAdditionalTable(TablesEnum.ADGROUPS_PERFORMANCE)
                    .setAdditionalId(bannerInfo1.adGroupId)
                    .build()
            )
        )

        val expectedFeedInfoMessage = createFeedInfoMessage(feedInfo.feedId)
        assertThat(loadedResources.resources)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                BannerResource.Builder<FeedInfoMessage>()
                    .setBid(bannerInfo1.bannerId)
                    .setPid(bannerInfo1.adGroupId)
                    .setCid(bannerInfo1.campaignId)
                    .setBsBannerId((bannerInfo1.getBanner() as PerformanceBanner).bsBannerId)
                    .setOrderId(campaignInfo.campaign.orderId)
                    .setResource(expectedFeedInfoMessage)
                    .build(),
                BannerResource.Builder<FeedInfoMessage>()
                    .setBid(bannerInfo2.bannerId)
                    .setPid(bannerInfo2.adGroupId)
                    .setCid(bannerInfo2.campaignId)
                    .setBsBannerId((bannerInfo2.getBanner() as PerformanceBanner).bsBannerId)
                    .setOrderId(campaignInfo.campaign.orderId)
                    .setResource(expectedFeedInfoMessage)
                    .build()
            )
    }

    @Test
    fun exportDynamicBannerWithFeedByGroupId() {
        val campaignInfo = steps.campaignSteps().createActiveDynamicCampaign(clientInfo)
        val feedInfo = createFeedWithMarketIds(clientInfo)
        val dynamicFeedAdGroup = steps.adGroupSteps().createAdGroup(
            TestGroups.activeDynamicFeedAdGroup(campaignInfo.campaignId, feedInfo.feedId), campaignInfo
        )

        val bannerInfo1 = steps.dynamicBannerSteps().createDynamicBanner(dynamicFeedAdGroup)
        // добавляем второй баннер в группу, и ожидаем, что он выгрузится тоже
        val bannerInfo2 = steps.dynamicBannerSteps().createDynamicBanner(dynamicFeedAdGroup)

        val loadedResources = loader.loadResources(
            bannerInfo1.shard, listOf(
                BsExportBannerResourcesObject.Builder()
                    .setResourceType(BannerResourceType.BANNER_FEED_INFO)
                    .setAdditionalTable(TablesEnum.ADGROUPS_DYNAMIC)
                    .setAdditionalId(bannerInfo1.adGroupId)
                    .build()
            )
        )

        val expectedFeedInfoMessage = createFeedInfoMessage(feedInfo.feedId)
        assertThat(loadedResources.resources)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                BannerResource.Builder<FeedInfoMessage>()
                    .setBid(bannerInfo1.bannerId)
                    .setPid(bannerInfo1.adGroupId)
                    .setCid(bannerInfo1.campaignId)
                    .setBsBannerId((bannerInfo1.getBanner() as DynamicBanner).bsBannerId)
                    .setOrderId(campaignInfo.campaign.orderId)
                    .setResource(expectedFeedInfoMessage)
                    .build(),
                BannerResource.Builder<FeedInfoMessage>()
                    .setBid(bannerInfo2.bannerId)
                    .setPid(bannerInfo2.adGroupId)
                    .setCid(bannerInfo2.campaignId)
                    .setBsBannerId((bannerInfo2.getBanner() as DynamicBanner).bsBannerId)
                    .setOrderId(campaignInfo.campaign.orderId)
                    .setResource(expectedFeedInfoMessage)
                    .build()
            )
    }

    @Test
    fun exportTextBannerWithFeedByFeedId() {
        val bannerInfo1 = steps.bannerSteps().createActiveTextBanner()
        // добавляем второй баннер в группу, и ожидаем, что он выгрузится тоже
        val bannerInfo2 = steps.bannerSteps().createActiveTextBanner(bannerInfo1.adGroupInfo)

        val feedInfo = createFeedWithMarketIds(bannerInfo1.clientInfo)
        createTestAdGroupsTextRecord(bannerInfo1.adGroupInfo, feedInfo.feedId)

        val loadedResources = loader.loadResources(
            bannerInfo1.shard, listOf(
                BsExportBannerResourcesObject.Builder()
                    .setResourceType(BannerResourceType.BANNER_FEED_INFO)
                    .setAdditionalTable(TablesEnum.FEEDS)
                    .setAdditionalId(feedInfo.feedId)
                    .build()
            )
        )

        val expectedFeedInfoMessage = createFeedInfoMessage(feedInfo.feedId)
        assertThat(loadedResources.resources)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                BannerResource.Builder<FeedInfoMessage>()
                    .setBid(bannerInfo1.bannerId)
                    .setPid(bannerInfo1.adGroupId)
                    .setCid(bannerInfo1.campaignId)
                    .setBsBannerId(bannerInfo1.banner.bsBannerId)
                    .setOrderId(bsOrderIdCalculator.calculateOrderId(bannerInfo1.campaignId))
                    .setResource(expectedFeedInfoMessage)
                    .build(),
                BannerResource.Builder<FeedInfoMessage>()
                    .setBid(bannerInfo2.bannerId)
                    .setPid(bannerInfo2.adGroupId)
                    .setCid(bannerInfo2.campaignId)
                    .setBsBannerId(bannerInfo2.banner.bsBannerId)
                    .setOrderId(bsOrderIdCalculator.calculateOrderId(bannerInfo2.campaignId))
                    .setResource(expectedFeedInfoMessage)
                    .build()
            )
    }

    @Test
    fun exportBannersWithFeedByFeedId() {
        val feedInfo = createFeedWithMarketIds(clientInfo)

        val textCampaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo)
        val textBannerInfo = steps.bannerSteps().createActiveTextBanner(textCampaignInfo)
        createTestAdGroupsTextRecord(textBannerInfo.adGroupInfo, feedInfo.feedId)

        val dynamicCampaignInfo = steps.campaignSteps().createActiveDynamicCampaign(clientInfo)
        val dynamicFeedAdGroup = steps.adGroupSteps().createAdGroup(
            TestGroups.activeDynamicFeedAdGroup(dynamicCampaignInfo.campaignId, feedInfo.feedId), dynamicCampaignInfo
        )
        val dynamicBannerInfo = steps.dynamicBannerSteps().createDynamicBanner(dynamicFeedAdGroup)

        val performanceCampaignInfo = steps.campaignSteps().createActivePerformanceCampaign(clientInfo)
        val adGroupInfo = steps.adGroupSteps().createActivePerformanceAdGroup(performanceCampaignInfo, feedInfo.feedId)
        val performanceBannerInfo = steps.performanceBannerSteps().createPerformanceBanner(adGroupInfo)

        val loadedResources = loader.loadResources(
            clientInfo.shard, listOf(
                BsExportBannerResourcesObject.Builder()
                    .setResourceType(BannerResourceType.BANNER_FEED_INFO)
                    .setAdditionalTable(TablesEnum.FEEDS)
                    .setAdditionalId(feedInfo.feedId)
                    .build()
            )
        )

        val expectedTextFeedInfoMessage = createFeedInfoMessage(feedInfo.feedId)
        val expectedFeedInfoMessage = createFeedInfoMessage(feedInfo.feedId)
        assertThat(loadedResources.resources)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                BannerResource.Builder<FeedInfoMessage>()
                    .setBid(textBannerInfo.bannerId)
                    .setPid(textBannerInfo.adGroupId)
                    .setCid(textBannerInfo.campaignId)
                    .setBsBannerId(textBannerInfo.banner.bsBannerId)
                    .setOrderId(textCampaignInfo.campaign.orderId)
                    .setResource(expectedTextFeedInfoMessage)
                    .build(),
                BannerResource.Builder<FeedInfoMessage>()
                    .setBid(dynamicBannerInfo.bannerId)
                    .setPid(dynamicBannerInfo.adGroupId)
                    .setCid(dynamicBannerInfo.campaignId)
                    .setBsBannerId((dynamicBannerInfo.getBanner() as DynamicBanner).bsBannerId)
                    .setOrderId(dynamicCampaignInfo.campaign.orderId)
                    .setResource(expectedFeedInfoMessage)
                    .build(),
                BannerResource.Builder<FeedInfoMessage>()
                    .setBid(performanceBannerInfo.bannerId)
                    .setPid(performanceBannerInfo.adGroupId)
                    .setCid(performanceBannerInfo.campaignId)
                    .setBsBannerId((performanceBannerInfo.getBanner() as PerformanceBanner).bsBannerId)
                    .setOrderId(performanceCampaignInfo.campaign.orderId)
                    .setResource(expectedFeedInfoMessage)
                    .build()
            )
    }

    private fun createFeedWithMarketIds(clientInfo: ClientInfo): FeedInfo {
        val feed = defaultFeed(clientInfo.clientId)
        feed.marketBusinessId = MARKET_BUSINESS_ID
        feed.marketShopId = MARKET_SHOP_ID
        feed.marketFeedId = MARKET_FEED_ID

        val feedInfo = FeedInfo()
            .withClientInfo(clientInfo)
            .withFeed(feed)

        steps.feedSteps().createFeed(feedInfo)
        return feedInfo
    }

    private fun createFeedInfoMessage(feedId: Long) = FeedInfoMessage.newBuilder()
        .setMarketBusinessId(MARKET_BUSINESS_ID)
        .setMarketShopId(MARKET_SHOP_ID)
        .setMarketFeedId(MARKET_FEED_ID)
        .setDirectFeedId(feedId)
        .build()

    private fun createTestAdGroupsTextRecord(adGroup: AdGroupInfo, feedId: Long) {
        dslContextProvider.ppc(adGroup.shard)
            .insertInto(Tables.ADGROUPS_TEXT)
            .set(Tables.ADGROUPS_TEXT.PID, adGroup.adGroupId)
            .set(Tables.ADGROUPS_TEXT.FEED_ID, feedId)
            .onDuplicateKeyUpdate()
            .set(Tables.ADGROUPS_TEXT.FEED_ID, feedId)
            .execute()
    }
}
