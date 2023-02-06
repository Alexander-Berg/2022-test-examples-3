package ru.yandex.direct.jobs.uac.service

import com.nhaarman.mockitokotlin2.anyVararg
import com.nhaarman.mockitokotlin2.doReturn
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.bannerstorage.client.DummyBannerStorageClient
import ru.yandex.direct.bannerstorage.client.model.Template
import ru.yandex.direct.core.entity.adgroup.container.AdGroupsSelectionCriteria
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate
import ru.yandex.direct.core.entity.banner.model.DynamicBanner
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner
import ru.yandex.direct.core.entity.banner.model.PerformanceBannerMain
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository
import ru.yandex.direct.core.entity.campaign.model.CampaignSource
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.getCreativeGroup
import ru.yandex.direct.core.entity.uac.model.AdvType
import ru.yandex.direct.core.entity.uac.model.DirectCampaignStatus
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAccountRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectAdGroupRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectAdRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAccount
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectCampaign
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.repository.TestCampaignRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AverageCpaStrategy
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.jobs.configuration.JobsTest
import ru.yandex.direct.jobs.uac.UpdateAdsJob
import ru.yandex.direct.multitype.entity.LimitOffset
import ru.yandex.direct.test.utils.randomPositiveInt
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@JobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class YdbUpdateAdsJobTest {
    companion object {
        private val KEYWORDS = listOf("keyword1", "keyword2")
        private val VALID_MODERATION_STATUSES = listOf(BannerStatusModerate.YES, BannerStatusModerate.READY,
            BannerStatusModerate.SENDING, BannerStatusModerate.SENT)
        private const val CREATIVES_COUNT = 5
    }

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var updateAdsJob: UpdateAdsJob

    @Autowired
    private lateinit var adGroupService: AdGroupService

    @Autowired
    private lateinit var uacYdbAccountRepository: UacYdbAccountRepository

    @Autowired
    lateinit var uacYdbAdGroupRepository: UacYdbDirectAdGroupRepository

    @Autowired
    lateinit var uacYdbAdRepository: UacYdbDirectAdRepository

    @Autowired
    private lateinit var uacYdbCampaignRepository: UacYdbCampaignRepository

    @Autowired
    private lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    private lateinit var uacYdbDirectCampaignRepository: UacYdbDirectCampaignRepository

    @Autowired
    private lateinit var bannerTypedRepository: BannerTypedRepository

    @Autowired
    private lateinit var keywordRepository: KeywordRepository

    @Autowired
    private lateinit var testCampaignRepository: TestCampaignRepository

    @Autowired
    private lateinit var dummyBannerStorageClient: DummyBannerStorageClient

    private lateinit var clientInfo: ClientInfo
    private lateinit var userInfo: UserInfo
    private lateinit var uacCampaignId: String
    private lateinit var titleCampaignContent1: UacYdbCampaignContent
    private lateinit var titleCampaignContent2: UacYdbCampaignContent
    private lateinit var textCampaignContent1: UacYdbCampaignContent
    private lateinit var textCampaignContent2: UacYdbCampaignContent
    private var accountId = ""


    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        userInfo = clientInfo.chiefUserInfo!!

        val campaign = TestCampaigns.activeTextCampaign(null, null)
            .withStrategy(
                AverageCpaStrategy().withMaxWeekSum(1000.toBigDecimal()).withAverageCpa(100.toBigDecimal()));

        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        uacCampaignId = campaignInfo.campaignId.toIdString()

        val account = UacYdbAccount(uid = userInfo.uid, directClientId = userInfo.clientId.asLong())
        accountId = account.id
        uacYdbAccountRepository.saveAccount(account)

        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.ECOM_UC_NEW_BACKEND_ENABLED, false)
    }

    private fun createCampaignAndContents(draft: Boolean = false, briefSynced: Boolean = false) {
        val uacCampaign = createYdbCampaign(
            id = uacCampaignId,
            advType = AdvType.TEXT,
            keywords = KEYWORDS,
            accountId = accountId,
            briefSynced = briefSynced,
            startedAt = if (draft) null else LocalDateTime.now(),
        )

        uacYdbCampaignRepository.addCampaign(uacCampaign)
        val directCampaign = UacYdbDirectCampaign(
            id = uacCampaign.id,
            directCampaignId = uacCampaignId.toIdLong(),
            status = DirectCampaignStatus.CREATED,
            syncedAt = LocalDateTime.now(),
            rejectReasons = null,
        )
        uacYdbDirectCampaignRepository.saveDirectCampaign(directCampaign)
        titleCampaignContent1 = createDefaultTitleContent(uacCampaign.id, "title1")
        titleCampaignContent2 = createDefaultTitleContent(uacCampaign.id, "title2")
        textCampaignContent1 = createDefaultTextContent(uacCampaign.id, "text1")
        textCampaignContent2 = createDefaultTextContent(uacCampaign.id, "text2")
    }

    private fun createDefaultTitleContent(uacCampaignId: String, title: String): UacYdbCampaignContent {
        val titleCampaignContent = createCampaignContent(
            campaignId = uacCampaignId,
            type = MediaType.TITLE,
            text = title,
        )
        uacYdbCampaignContentRepository.addCampaignContents(listOf(titleCampaignContent))
        return titleCampaignContent
    }

    private fun createDefaultTextContent(uacCampaignId: String, text: String): UacYdbCampaignContent {
        val textCampaignContent = createCampaignContent(
            campaignId = uacCampaignId,
            type = MediaType.TEXT,
            text = text,
        )
        uacYdbCampaignContentRepository.addCampaignContents(listOf(textCampaignContent))
        return textCampaignContent
    }

    @Test
    fun test() {
        createCampaignAndContents()
        updateAdsJob.withShard(clientInfo.shard)
        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        val soft = SoftAssertions()
        checkCommon(soft)
        soft.assertAll()
    }

    @Test
    fun test_draftCampaign() {
        createCampaignAndContents(draft = true)
        updateAdsJob.withShard(clientInfo.shard)

        assertThrows<IllegalStateException> {
            updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)
        }

        val adGroups = adGroupService.getAdGroupsBySelectionCriteria(AdGroupsSelectionCriteria().withCampaignIds(
            setOf(uacCampaignId.toIdLong())), LimitOffset.maxLimited(), false)
        val banners = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(uacCampaignId.toIdLong()))
            .map { it as TextBanner }

        SoftAssertions.assertSoftly {
            it.assertThat(adGroups).isEmpty()
            it.assertThat(banners).isEmpty()
        }
    }

    @Test
    fun test_syncedCampaign() {
        createCampaignAndContents(briefSynced = true)
        updateAdsJob.withShard(clientInfo.shard)

        assertThrows<IllegalStateException> {
            updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)
        }

        val adGroups = adGroupService.getAdGroupsBySelectionCriteria(AdGroupsSelectionCriteria().withCampaignIds(
            setOf(uacCampaignId.toIdLong())), LimitOffset.maxLimited(), false)
        val banners = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(uacCampaignId.toIdLong()))
            .map { it as TextBanner }

        SoftAssertions.assertSoftly {
            it.assertThat(adGroups).isEmpty()
            it.assertThat(banners).isEmpty()
        }
    }

    @Test
    fun testEcom() {
        doReturn(Template(0, "", emptyList(), emptyList()))
            .`when`(dummyBannerStorageClient).getTemplate(anyInt(), anyVararg())

        val creativeIds = List(CREATIVES_COUNT) { randomPositiveInt() }
        doReturn(getCreativeGroup(creativeIds))
            .`when`(dummyBannerStorageClient).createSmartCreativeGroup(any())

        val directCampaign = TestCampaigns.activeTextCampaign(clientInfo.clientId, clientInfo.uid)
            .withOrderId(0L)
            .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB))
            .withStrategy(TestCampaigns.averageCpaStrategy())

        val campaignInfo = steps.campaignSteps().createCampaign(directCampaign, clientInfo)
        val feedId = steps.feedSteps().createDefaultFeed(clientInfo).feedId

        val uacCampaign = createYdbCampaign(
            advType = AdvType.TEXT,
            startedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
            id = campaignInfo.campaignId.toIdString(),
            appId = null,
            isEcom = true,
            feedId = feedId,
            accountId = accountId,
            keywords = KEYWORDS,
        )
        uacYdbCampaignRepository.addCampaign(uacCampaign)

        uacCampaignId = uacCampaign.id

        // Create subcampaigns for ecom
        val smartCampaign = TestCampaigns.activePerformanceCampaign(clientInfo.clientId, clientInfo.uid)
            .withMasterCid(uacCampaignId.toIdLong())
            .withStrategy(TestCampaigns.averageCpaStrategy())
        val dynamicCampaign = TestCampaigns.activeDynamicCampaign(clientInfo.clientId, clientInfo.uid)
            .withMasterCid(uacCampaignId.toIdLong())
            .withStrategy(TestCampaigns.averageCpaStrategy())
        val smartInfo = steps.campaignSteps().createCampaign(smartCampaign, clientInfo)
        val dynamicInfo = steps.campaignSteps().createCampaign(dynamicCampaign, clientInfo)

        testCampaignRepository.setSource(clientInfo.shard, smartInfo.campaignId, CampaignSource.UAC)
        testCampaignRepository.setSource(clientInfo.shard, dynamicInfo.campaignId, CampaignSource.UAC)

        val ydbDirectCampaign = UacYdbDirectCampaign(
            id = uacCampaign.id,
            directCampaignId = campaignInfo.campaignId,
            status = DirectCampaignStatus.CREATED,
            syncedAt = LocalDateTime.now(),
            rejectReasons = null,
        )
        uacYdbDirectCampaignRepository.saveDirectCampaign(ydbDirectCampaign)

        titleCampaignContent1 = createDefaultTitleContent(uacCampaign.id, "title1")
        textCampaignContent1 = createDefaultTextContent(uacCampaign.id, "text1")
        titleCampaignContent2 = createDefaultTitleContent(uacCampaign.id, "title2")
        textCampaignContent2 = createDefaultTextContent(uacCampaign.id, "text2")

        updateAdsJob.withShard(clientInfo.shard)
        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        SoftAssertions.assertSoftly { soft ->
            checkCommon(soft)
            checkDynamicBanners(soft, dynamicInfo.campaignId)
            checkSmartBanners(soft, smartInfo.campaignId, creativeIds)
        }
    }

    @Test
    fun testEcomOnNewBackend() {
        val clientId = clientInfo.clientId!!
        steps.featureSteps().enableClientFeature(clientId, FeatureName.ECOM_UC_NEW_BACKEND_ENABLED)
        steps.featureSteps().enableClientFeature(clientId, FeatureName.ENABLED_DYNAMIC_FEED_AD_TARGET_IN_TEXT_AD_GROUP)
        steps.featureSteps().enableClientFeature(clientId, FeatureName.CREATIVE_FREE_ECOM_UC)
        steps.featureSteps().enableClientFeature(clientId, FeatureName.SMART_NO_CREATIVES)

        doReturn(Template(0, "", emptyList(), emptyList()))
            .`when`(dummyBannerStorageClient).getTemplate(anyInt(), anyVararg())

        val creativeIds = List(CREATIVES_COUNT) { randomPositiveInt() }
        doReturn(getCreativeGroup(creativeIds))
            .`when`(dummyBannerStorageClient).createSmartCreativeGroup(any())

        val directCampaign = TestCampaigns.activeTextCampaign(clientInfo.clientId, clientInfo.uid)
            .withOrderId(0L)
            .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB))
            .withStrategy(TestCampaigns.averageCpaStrategy())

        val campaignInfo = steps.campaignSteps().createCampaign(directCampaign, clientInfo)
        val feedId = steps.feedSteps().createDefaultFeed(clientInfo).feedId

        val uacCampaign = createYdbCampaign(
            advType = AdvType.TEXT,
            startedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
            id = campaignInfo.campaignId.toIdString(),
            appId = null,
            isEcom = true,
            feedId = feedId,
            accountId = accountId,
            keywords = KEYWORDS,
        )
        uacYdbCampaignRepository.addCampaign(uacCampaign)

        uacCampaignId = uacCampaign.id

        val ydbDirectCampaign = UacYdbDirectCampaign(
            id = uacCampaign.id,
            directCampaignId = campaignInfo.campaignId,
            status = DirectCampaignStatus.CREATED,
            syncedAt = LocalDateTime.now(),
            rejectReasons = null,
        )
        uacYdbDirectCampaignRepository.saveDirectCampaign(ydbDirectCampaign)

        titleCampaignContent1 = createDefaultTitleContent(uacCampaign.id, "title1")
        textCampaignContent1 = createDefaultTextContent(uacCampaign.id, "text1")
        titleCampaignContent2 = createDefaultTitleContent(uacCampaign.id, "title2")
        textCampaignContent2 = createDefaultTextContent(uacCampaign.id, "text2")

        updateAdsJob.withShard(clientInfo.shard)
        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        SoftAssertions.assertSoftly { soft ->
            checkCommon(soft, 7) // 4 ТГО, 2 ДО, 1 смарт
            checkDynamicBannersOnNewBackend(soft, campaignInfo.campaignId)
            checkSmartBannersOnNewBackend(soft, campaignInfo.campaignId)
        }
    }

    private fun checkCommon(soft: SoftAssertions) {
        checkCommon(soft, 4)
    }

    private fun checkCommon(soft: SoftAssertions, expectedYdbBannersCount: Int) {
        val banners = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(uacCampaignId.toIdLong()))
            .filterIsInstance<TextBanner>()
        soft.assertThat(banners).hasSize(4)
        val gotTitleAntTextList = banners.map { TitleAndText(it.title, it.body) }
        soft.assertThat(gotTitleAntTextList).containsExactlyInAnyOrder(
            TitleAndText(titleCampaignContent1.text!!, textCampaignContent1.text!!),
            TitleAndText(titleCampaignContent1.text!!, textCampaignContent2.text!!),
            TitleAndText(titleCampaignContent2.text!!, textCampaignContent1.text!!),
            TitleAndText(titleCampaignContent2.text!!, textCampaignContent2.text!!)
        )

        val adGroupIds = banners.map { it.adGroupId }.distinct()
        soft.assertThat(adGroupIds).hasSize(1)

        val keywordPhrases = keywordRepository.getKeywordsByAdGroupId(clientInfo.shard, adGroupIds[0])
            .map { it.phrase }
        soft.assertThat(keywordPhrases).hasSize(2)
        soft.assertThat(keywordPhrases).containsExactlyInAnyOrder(*KEYWORDS.toTypedArray())

        val ydbAdGroup = uacYdbAdGroupRepository.getDirectAdGroupByDirectAdGroupId(adGroupIds[0])
        soft.assertThat(ydbAdGroup).isNotNull
        soft.assertThat(ydbAdGroup!!.directCampaignId).isEqualTo(uacCampaignId)
        val ydbBanners = uacYdbAdRepository.getByDirectAdGroupId(listOf(ydbAdGroup.id), 0L, 1000L)
            .associateBy { it.directAdId }
        soft.assertThat(ydbBanners).hasSize(expectedYdbBannersCount)

        val contentIdsByTitles = listOf(titleCampaignContent1, titleCampaignContent2).associate { it.text to it.id }
        val contentIdsByTexts = listOf(textCampaignContent1, textCampaignContent2).associate { it.text to it.id }

        banners.forEach {
            val ydbBanner = ydbBanners[it.id]
            val titleId = contentIdsByTitles[it.title]
            val textId = contentIdsByTexts[it.body]
            soft.assertThat(ydbBanner!!.titleContentId).isEqualTo(titleId)
            soft.assertThat(ydbBanner.textContentId).isEqualTo(textId)
//            soft.assertThat(ydbBanner.status).isEqualTo(DirectAdStatus.CREATED) // TODO починить
        }
    }

    private fun checkDynamicBanners(soft: SoftAssertions, campaignId: Long) {
        val adGroups = adGroupService.getAdGroupsBySelectionCriteria(AdGroupsSelectionCriteria().withCampaignIds(setOf(campaignId)), LimitOffset.maxLimited(), false)
        soft.assertThat(adGroups).hasSize(1)
        val adGroup = adGroups!![0]
        val banners = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(campaignId)).map { it as DynamicBanner }
        soft.assertThat(banners).hasSize(2)
        val gotTitleAndTextList = banners.map { TitleAndText(it.title, it.body) }
        val dynamicTitle = "{Dynamic title}"
        // TODO понять, ожидаемое ли это поведение или должно быть всего 2
        soft.assertThat(gotTitleAndTextList).containsExactlyInAnyOrder(
            TitleAndText(dynamicTitle, textCampaignContent1.text!!),
            TitleAndText(dynamicTitle, textCampaignContent2.text!!)
        )

        val ydbAdGroup = uacYdbAdGroupRepository.getDirectAdGroupByDirectAdGroupId(adGroup.id)
        soft.assertThat(ydbAdGroup).isNotNull
        soft.assertThat(ydbAdGroup!!.directCampaignId).isEqualTo(uacCampaignId)

        val ydbBanners = uacYdbAdRepository.getByDirectAdGroupId(listOf(ydbAdGroup.id), 0L, 1000L)
            .associateBy { it.directAdId }
        soft.assertThat(ydbBanners).hasSize(2)

        val contentIdsByTexts = listOf(textCampaignContent1, textCampaignContent2).associate { it.text to it.id }

        banners.forEach {
            val ydbBanner = ydbBanners[it.id]
            val textId = contentIdsByTexts[it.body]
            soft.assertThat(ydbBanner!!.textContentId).isEqualTo(textId)
//            soft.assertThat(ydbBanner.status).isEqualTo(DirectAdStatus.CREATED) // TODO починить
        }
    }

    private fun checkDynamicBannersOnNewBackend(soft: SoftAssertions, campaignId: Long) {
        val banners = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(campaignId))
            .filterIsInstance<DynamicBanner>()
        soft.assertThat(banners).hasSize(2)
        val gotTitleAndTextList = banners.map { TitleAndText(it.title, it.body) }
        val dynamicTitle = "{Dynamic title}"
        soft.assertThat(gotTitleAndTextList).containsExactlyInAnyOrder(
            TitleAndText(dynamicTitle, textCampaignContent1.text!!),
            TitleAndText(dynamicTitle, textCampaignContent2.text!!)
        )

        val moderationStatuses = banners.map { it.statusModerate }.toSet()
        soft.assertThat(moderationStatuses).hasSize(1)
        soft.assertThat(moderationStatuses).allMatch { VALID_MODERATION_STATUSES.contains(it) }

        val adGroupIds = banners.map { it.adGroupId }.distinct()
        soft.assertThat(adGroupIds).hasSize(1)

        val ydbAdGroup = uacYdbAdGroupRepository.getDirectAdGroupByDirectAdGroupId(adGroupIds[0])
        soft.assertThat(ydbAdGroup).isNotNull
        soft.assertThat(ydbAdGroup!!.directCampaignId).isEqualTo(uacCampaignId)

        val ydbBanners = uacYdbAdRepository.getByDirectAdGroupId(listOf(ydbAdGroup.id), 0L, 1000L)
            .associateBy { it.directAdId }

        val contentIdsByTexts = listOf(textCampaignContent1, textCampaignContent2).associate { it.text to it.id }

        banners.forEach {
            val ydbBanner = ydbBanners[it.id]
            val textId = contentIdsByTexts[it.body]
            soft.assertThat(ydbBanner!!.textContentId).isEqualTo(textId)
        }
    }

    private fun checkSmartBanners(soft: SoftAssertions, campaignId: Long, creativeIds: List<Int>) {
        val adGroups = adGroupService.getAdGroupsBySelectionCriteria(AdGroupsSelectionCriteria().withCampaignIds(setOf(campaignId)), LimitOffset.maxLimited(), false)
        soft.assertThat(adGroups).hasSize(1)
        soft.assertThat(adGroups[0]).isInstanceOf(PerformanceAdGroup::class.java)
        val banners = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(campaignId)).map { it as PerformanceBanner }

        soft.assertThat(banners.map { it.creativeId.toInt() })
            .containsExactlyElementsOf(creativeIds)

        val ydbAdGroup = uacYdbAdGroupRepository.getDirectAdGroupByDirectAdGroupId(adGroups[0].id)
        soft.assertThat(ydbAdGroup).isNotNull
        soft.assertThat(ydbAdGroup!!.directCampaignId).isEqualTo(uacCampaignId)

        val ydbBanners = uacYdbAdRepository.getByDirectAdGroupId(listOf(ydbAdGroup.id), 0L, 1000L)
        soft.assertThat(ydbBanners).hasSize(creativeIds.size)
    }

    private fun checkSmartBannersOnNewBackend(soft: SoftAssertions, campaignId: Long) {
        val banners = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(campaignId))
            .filterIsInstance<PerformanceBannerMain>()

        val moderationStatuses = banners.map { it.statusModerate }.toSet()
        soft.assertThat(moderationStatuses).hasSize(1)
        soft.assertThat(moderationStatuses).allMatch { VALID_MODERATION_STATUSES.contains(it) }

        val adGroupIds = banners.map { it.adGroupId }.distinct()
        soft.assertThat(adGroupIds).hasSize(1)

        val ydbAdGroup = uacYdbAdGroupRepository.getDirectAdGroupByDirectAdGroupId(adGroupIds[0])
        soft.assertThat(ydbAdGroup).isNotNull
        soft.assertThat(ydbAdGroup!!.directCampaignId).isEqualTo(uacCampaignId)
    }

    data class TitleAndText(
        val title: String,
        val text: String,
    )
}
