package ru.yandex.direct.jobs.uac.service

import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository
import ru.yandex.direct.core.entity.banner.model.ImageBanner
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner
import ru.yandex.direct.core.entity.banner.model.NewMobileContentPrimaryAction
import ru.yandex.direct.core.entity.banner.repository.BannerRelationsRepository
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository
import ru.yandex.direct.core.entity.mobilegoals.MobileAppGoalsService
import ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchRepository
import ru.yandex.direct.core.entity.retargeting.model.ConditionType
import ru.yandex.direct.core.entity.retargeting.model.TargetingCategory
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository
import ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionService
import ru.yandex.direct.core.entity.retargeting.service.uc.UcRetargetingConditionService
import ru.yandex.direct.core.entity.uac.STORE_URL
import ru.yandex.direct.core.entity.uac.UacCommonUtils.CREATIVE_ID_KEY
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.createDefaultHtml5Content
import ru.yandex.direct.core.entity.uac.createDirectContent
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.DirectCampaignStatus
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.model.UacRetargetingCondition
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRule
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoal
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoalType
import ru.yandex.direct.core.entity.uac.model.direct_content.DirectContentStatus
import ru.yandex.direct.core.entity.uac.model.direct_content.DirectContentType
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectAdGroupRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAccount
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAppInfo
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectAdGroup
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectCampaign
import ru.yandex.direct.core.entity.uac.service.UacAppInfoService
import ru.yandex.direct.core.entity.uac.service.UacRetargetingService
import ru.yandex.direct.core.testing.data.TestCampaigns.defaultMobileContentCampaignWithSystemFields
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.TypedCampaignInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.repository.TestTargetingCategoriesRepository
import ru.yandex.direct.core.testing.steps.CreativeSteps
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.TypedCampaignStepsUnstubbed
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.jobs.configuration.JobsTest
import ru.yandex.direct.jobs.uac.model.createUpdateAdsContainers
import ru.yandex.direct.jobs.uac.repository.AbstractUacRepositoryJobTest
import ru.yandex.direct.model.KtModelChanges

@JobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BannerCreateJobServiceMobileCampaignTest : AbstractUacRepositoryJobTest() {
    @Autowired
    private lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    private lateinit var uacYdbDirectAdGroupRepository: UacYdbDirectAdGroupRepository

    @Autowired
    private lateinit var uacYdbDirectContentRepository: UacYdbDirectContentRepository

    @Autowired
    private lateinit var uacYdbContentRepository: UacYdbContentRepository

    @Autowired
    private lateinit var bannerCreateJobService: YdbBannerCreateJobService

    @Autowired
    private lateinit var typedCampaignStepsUnstubbed: TypedCampaignStepsUnstubbed

    @Autowired
    private lateinit var uacYdbCampaignRepository: UacYdbCampaignRepository

    @Autowired
    private lateinit var bannerTypedRepository: BannerTypedRepository

    @Autowired
    private lateinit var bannerRelationsRepository: BannerRelationsRepository

    @Autowired
    private lateinit var retargetingRepository: RetargetingRepository

    @Autowired
    private lateinit var retargetingConditionRepository: RetargetingConditionRepository

    @Autowired
    private lateinit var uacYdbAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    private lateinit var keywordRepository: KeywordRepository

    @Autowired
    private lateinit var adGroupRepository: AdGroupRepository

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var creativeSteps: CreativeSteps

    @Autowired
    private lateinit var testTargetingCategoriesRepository: TestTargetingCategoriesRepository

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var uacAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    private lateinit var mobileAppGoalsService: MobileAppGoalsService

    @Autowired
    private lateinit var retargetingConditionService: RetargetingConditionService

    @Autowired
    private lateinit var uacAppInfoService: UacAppInfoService

    @Autowired
    private lateinit var relevanceMatchRepository: RelevanceMatchRepository
    @Autowired
    private lateinit var uacYdbDirectCampaignRepository: UacYdbDirectCampaignRepository

    @Autowired
    private lateinit var ucRetargetingConditionService: UcRetargetingConditionService

    private lateinit var clientInfo: ClientInfo
    private lateinit var userInfo: UserInfo
    private lateinit var campaignInfo: TypedCampaignInfo
    private lateinit var uacAppInfo: UacYdbAppInfo
    private lateinit var uacAccount: UacYdbAccount
    private lateinit var uacCampaign: UacYdbCampaign

    @BeforeEach
    fun before() {
        testTargetingCategoriesRepository.addTargetingCategory(
            TargetingCategory(41, null, "Экшен", "NOT_DEFINED", 21672505.toBigInteger(), true)
        )
        testTargetingCategoriesRepository.addTargetingCategory(
            TargetingCategory(63, 81, "Другие игры", "GAME_OTHER", 21672500.toBigInteger(), true)
        )
        clientInfo = steps.clientSteps().createDefaultClient()
        userInfo = clientInfo.chiefUserInfo!!

        steps.trustedRedirectSteps().addValidCounters()

        val mobileAppInfo = steps.mobileAppSteps().createMobileApp(clientInfo, STORE_URL)
        campaignInfo = typedCampaignStepsUnstubbed.createMobileContentCampaign(
            userInfo, clientInfo,
            defaultMobileContentCampaignWithSystemFields(clientInfo)
                .withStrategy(TestCampaignsStrategy.defaultAutobudgetStrategy())
                .withMobileAppId(mobileAppInfo.mobileAppId)
        )

        uacAppInfo = defaultAppInfo()
        uacYdbAppInfoRepository.saveAppInfo(uacAppInfo)
        uacCampaign = createYdbCampaign(
            appId = uacAppInfo.id,
            keywords = listOf("keyword1", "keyword2"),
            minusKeywords = listOf("minusKeyword"),
            trackingUrl = "https://app.adjust.com/123456",
            impressionUrl = "https://view.adjust.com/impression/123456",
            minusRegions = listOf(213L),
        )
        uacYdbCampaignRepository.addCampaign(uacCampaign)
        uacYdbDirectCampaignRepository.saveDirectCampaign(
            UacYdbDirectCampaign(
                id = uacCampaign.id, directCampaignId = campaignInfo.id,
                status = DirectCampaignStatus.CREATED,
                syncedAt = LocalDateTime.now(),
                rejectReasons = null,
            )
        )

        uacAccount = steps.uacAccountSteps().createAccount(clientInfo)
    }

    @AfterEach
    fun after() {
        ppcPropertiesSupport.remove(PpcPropertyNames.MAX_BANNERS_IN_UAC_AD_GROUP)
    }

    @Test
    fun createNewAds_WithTargetInterests() {
        ppcPropertiesSupport.set(PpcPropertyNames.UAC_TARGET_INTERESTS_ENABLED, "true")

        val uacCampaignContents = listOf(
            createCampaignContent(
                campaignId = uacCampaign.id,
                type = MediaType.TITLE,
                text = "title",
            ),
            createCampaignContent(
                campaignId = uacCampaign.id,
                type = MediaType.TEXT,
                text = "text",
            ),
        )
        uacYdbCampaignContentRepository.addCampaignContents(uacCampaignContents)

        val uacDirectContent = createDirectContent()
        uacYdbDirectContentRepository.addDirectContent(listOf(uacDirectContent))

        runCreateBannerFunction(uacCampaignContents)

        val adGroupIdsToBannerIds = bannerRelationsRepository.getAdGroupIdsToNonArchivedBannerIds(
            clientInfo.shard, campaignInfo.id, listOf(MobileAppBanner::class.java)
        )

        assertThat(adGroupIdsToBannerIds.keys)
            .`as`("создалась одна группа")
            .hasSize(1)
        assertThat(adGroupIdsToBannerIds.values.flatten())
            .`as`("создался один баннер")
            .hasSize(1)

        val adGroupId: Long = adGroupIdsToBannerIds.keys.first()
        val keywords = keywordRepository.getKeywordsByAdGroupId(clientInfo.shard, adGroupId)
        assertThat(keywords).`as`("создалось два киворда").hasSize(2)
        val adGroup = adGroupRepository.getAdGroups(clientInfo.shard, listOf(adGroupId)).first()
        assertThat(adGroup.minusKeywords).`as`("создалось одно минус-слово").isEqualTo(listOf("minusKeyword"))
        assertThat(adGroup.geo).isEqualTo(listOf(225L, -213L, 977L))

        val retargetings = retargetingRepository.getRetargetingsByCampaigns(
            clientInfo.shard, listOf(campaignInfo.id)
        )
        assertThat(retargetings)
            .`as`("создалось 2 ретаргетинга")
            .hasSize(2)
        val retargetingConditions = retargetingConditionRepository.getConditions(
            clientInfo.shard, retargetings.map { it.retargetingConditionId })
        assertThat(retargetingConditions)
            .`as`("создалось 2 условия ретаргетинга")
            .hasSize(2)
        assertThat(retargetingConditions[0].collectGoals()[0].id).isEqualTo(21672505L)
        assertThat(retargetingConditions[1].collectGoals()[0].id).isEqualTo(21672500L)
    }

    @Test
    fun createNewAds_MultipleAdGroups_WithTargetInterests() {
        ppcPropertiesSupport.set(PpcPropertyNames.UAC_TARGET_INTERESTS_ENABLED, "true")
        ppcPropertiesSupport.set(PpcPropertyNames.MAX_BANNERS_IN_UAC_AD_GROUP, "1")

        val uacCampaignContents = listOf(
            createCampaignContent(
                campaignId = uacCampaign.id,
                type = MediaType.TITLE,
                text = "title",
            ),
            createCampaignContent(
                campaignId = uacCampaign.id,
                type = MediaType.TITLE,
                text = "title 2",
            ),
            createCampaignContent(
                campaignId = uacCampaign.id,
                type = MediaType.TEXT,
                text = "text",
            ),
            createCampaignContent(
                campaignId = uacCampaign.id,
                type = MediaType.TEXT,
                text = "text 2",
            ),
        )
        uacYdbCampaignContentRepository.addCampaignContents(uacCampaignContents)

        val uacDirectContent = createDirectContent()
        uacYdbDirectContentRepository.addDirectContent(listOf(uacDirectContent))

        runCreateBannerFunction(uacCampaignContents)

        val adGroupIdsToBannerIds = bannerRelationsRepository.getAdGroupIdsToNonArchivedBannerIds(
            clientInfo.shard, campaignInfo.id, listOf(MobileAppBanner::class.java)
        )

        assertThat(adGroupIdsToBannerIds.keys)
            .`as`("создалось 4 группы")
            .hasSize(4)
        assertThat(adGroupIdsToBannerIds.values.flatten())
            .`as`("создалось 4 баннера")
            .hasSize(4)

        val retargetings = retargetingRepository.getRetargetingsByCampaigns(
            clientInfo.shard, listOf(campaignInfo.id)
        )
        assertThat(retargetings)
            .`as`("создалось 8 ретаргетингов")
            .hasSize(8)
        val retargetingConditions = retargetingConditionRepository.getConditions(
            clientInfo.shard, retargetings.map { it.retargetingConditionId })
        assertThat(retargetingConditions)
            .`as`("создалось 2 условия ретаргетинга")
            .hasSize(2)
        assertThat(retargetingConditions[0].collectGoals()[0].id).isEqualTo(21672505L)
        assertThat(retargetingConditions[1].collectGoals()[0].id).isEqualTo(21672500L)
    }

    @Test
    fun createNewAds_WithoutTargetInterests_AndThenUpdateCampaignUrls() {
        ppcPropertiesSupport.set(PpcPropertyNames.UAC_TARGET_INTERESTS_ENABLED, "false")

        val uacCampaignContents = listOf(
            createCampaignContent(
                campaignId = uacCampaign.id,
                type = MediaType.TITLE,
                text = "title",
            ),
            createCampaignContent(
                campaignId = uacCampaign.id,
                type = MediaType.TEXT,
                text = "text",
            ),
        )
        uacYdbCampaignContentRepository.addCampaignContents(uacCampaignContents)

        val uacDirectContent = createDirectContent()
        uacYdbDirectContentRepository.addDirectContent(listOf(uacDirectContent))
        runCreateBannerFunction(uacCampaignContents)

        val adGroupIdsToBannerIds = bannerRelationsRepository.getAdGroupIdsToNonArchivedBannerIds(
            clientInfo.shard, campaignInfo.id, listOf(MobileAppBanner::class.java)
        )

        assertThat(adGroupIdsToBannerIds.keys)
            .`as`("создалась одна группа")
            .hasSize(1)
        assertThat(adGroupIdsToBannerIds.values.flatten())
            .`as`("создался один баннер")
            .hasSize(1)

        var banner: MobileAppBanner = bannerTypedRepository.getSafely(
            clientInfo.shard, adGroupIdsToBannerIds.values.first(), MobileAppBanner::class.java
        )[0]
        assertThat(banner.href)
            .`as`("ссылка на баннере верна")
            .isEqualTo("https://app.adjust.com/123456")
        assertThat(banner.impressionUrl)
            .`as`("ссылка для атрибуции к показу верна")
            .isEqualTo("https://view.adjust.com/impression/123456")

        val retargetings = retargetingRepository.getRetargetingsByCampaigns(
            clientInfo.shard, listOf(campaignInfo.id)
        )
        assertThat(retargetings)
            .`as`("ретаргетинг не создался")
            .hasSize(0)
        val retargetingConditions = retargetingConditionRepository.getConditions(
            clientInfo.shard, retargetings.map { it.retargetingConditionId })
        assertThat(retargetingConditions)
            .`as`("условие ретаргетинга не создалось")
            .hasSize(0)

        var newUacCampaign = createYdbCampaign(
            id = uacCampaign.id,
            appId = uacCampaign.appId,
            keywords = uacCampaign.keywords,
            minusKeywords = uacCampaign.minusKeywords,
            trackingUrl = "https://app.adjust.com/qwerty",
            impressionUrl = "https://view.adjust.com/impression/qwerty"
        )

        var grutDirectAdGroups = uacYdbDirectAdGroupRepository.getDirectAdGroupsByCampaignId(uacCampaign.id)
        runCreateBannerFunction(uacCampaignContents, newUacCampaign, grutDirectAdGroups)

        banner = bannerTypedRepository.getSafely(
            clientInfo.shard, adGroupIdsToBannerIds.values.first(), MobileAppBanner::class.java
        )[0]
        assertThat(banner.href)
            .`as`("ссылка на баннере обновилась")
            .isEqualTo("https://app.adjust.com/qwerty")
        assertThat(banner.impressionUrl)
            .`as`("ссылка для атрибуции к показу обновилась")
            .isEqualTo("https://view.adjust.com/impression/qwerty")

        newUacCampaign = createYdbCampaign(
            id = uacCampaign.id,
            appId = uacCampaign.appId,
            keywords = uacCampaign.keywords,
            minusKeywords = uacCampaign.minusKeywords,
            trackingUrl = "https://app.adjust.com/qwerty",
            impressionUrl = null
        )

        grutDirectAdGroups = uacYdbDirectAdGroupRepository.getDirectAdGroupsByCampaignId(uacCampaign.id)
        runCreateBannerFunction(uacCampaignContents, newUacCampaign, grutDirectAdGroups)

        banner = bannerTypedRepository.getSafely(
            clientInfo.shard, adGroupIdsToBannerIds.values.first(), MobileAppBanner::class.java
        )[0]
        assertThat(banner.href)
            .`as`("ссылка на баннере не изменилась")
            .isEqualTo("https://app.adjust.com/qwerty")
        assertThat(banner.impressionUrl)
            .`as`("ссылка для атрибуции к показу удалена")
            .isNull()
    }

    @Test
    fun createNewAds_WithHtml5Banner() {
        val html5Content = createDefaultHtml5Content()
        uacYdbContentRepository.saveContents(listOf(html5Content))
        creativeSteps.addDefaultHtml5Creative(clientInfo, html5Content.meta[CREATIVE_ID_KEY] as Long)
        val uacCampaignContents = listOf(
            createCampaignContent(
                campaignId = uacCampaign.id,
                type = MediaType.HTML5,
                contentId = html5Content.id
            ),
        )
        uacYdbCampaignContentRepository.addCampaignContents(uacCampaignContents)

        val uacDirectContent = createDirectContent(
            id = html5Content.id,
            status = DirectContentStatus.CREATED,
            type = DirectContentType.HTML5,
            directImageHash = null,
            directVideoId = null,
            directHtml5Id = html5Content.meta[CREATIVE_ID_KEY] as Long,
        )
        uacYdbDirectContentRepository.addDirectContent(listOf(uacDirectContent))
        runCreateBannerFunction(uacCampaignContents)

        val adGroupIdsToBannerIds = bannerRelationsRepository.getAdGroupIdsToNonArchivedBannerIds(
            clientInfo.shard, campaignInfo.id, listOf(ImageBanner::class.java)
        )

        assertThat(adGroupIdsToBannerIds.keys)
            .`as`("создалась одна группа")
            .hasSize(1)
        assertThat(adGroupIdsToBannerIds.values.flatten())
            .`as`("создался один баннер")
            .hasSize(1)
    }

    @Test
    fun createNewAds_WithRetargetingCondition() {
        ppcPropertiesSupport.set(PpcPropertyNames.UAC_TARGET_INTERESTS_ENABLED, "true")
        ppcPropertiesSupport.set(PpcPropertyNames.MAX_BANNERS_IN_UAC_AD_GROUP, "1")

        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.IN_APP_MOBILE_TARGETING, true)
        val ydbAppInfo = defaultAppInfo()
        uacAppInfoRepository.saveAppInfo(ydbAppInfo)
        val appInfo = uacAppInfoService.getAppInfo(ydbAppInfo)

        val mobileApp = steps.mobileAppSteps().createMobileApp(userInfo.clientInfo!!, appInfo.url).mobileApp
        mobileAppGoalsService.updateMobileAppGoalsForExternalTracker(
            dslContextProvider.ppc(userInfo.shard), userInfo.clientId, listOf(mobileApp)
        )
        val mobileAppGoalIds = mobileAppGoalsService.getGoalsByApps(userInfo.clientId, listOf(mobileApp)).map { it.id }

        val newUacCampaign = createYdbCampaign(
            appId = ydbAppInfo.id,
            trackingUrl = "https://app.adjust.com/123456",
            impressionUrl = "https://view.adjust.com/impression/123456",
        )
        uacYdbCampaignRepository.addCampaign(newUacCampaign)
        val uacCampaignContents = listOf(
            createCampaignContent(
                campaignId = uacCampaign.id,
                type = MediaType.TITLE,
                text = "title",
            ),
            createCampaignContent(
                campaignId = uacCampaign.id,
                type = MediaType.TITLE,
                text = "title 2",
            ),
            createCampaignContent(
                campaignId = uacCampaign.id,
                type = MediaType.TEXT,
                text = "text",
            ),
            createCampaignContent(
                campaignId = uacCampaign.id,
                type = MediaType.TEXT,
                text = "text 2",
            ),
        )
        uacYdbCampaignContentRepository.addCampaignContents(uacCampaignContents)

        val uacDirectContent = createDirectContent()
        uacYdbDirectContentRepository.addDirectContent(listOf(uacDirectContent))
        runCreateBannerFunction(uacCampaignContents, newUacCampaign)

        val adGroupIdsToBannerIds = bannerRelationsRepository.getAdGroupIdsToNonArchivedBannerIds(
            clientInfo.shard, campaignInfo.id, listOf(MobileAppBanner::class.java)
        )
        var relevanceMatches = relevanceMatchRepository
            .getRelevanceMatchesByAdGroupIds(clientInfo.shard, clientInfo.clientId, adGroupIdsToBannerIds.keys, true)
            .values
            .toList()

        val soft = SoftAssertions()
        soft.assertThat(adGroupIdsToBannerIds.keys)
            .`as`("создались 4 группы")
            .hasSize(4)
        soft.assertThat(adGroupIdsToBannerIds.values.flatten())
            .`as`("создались 4 баннера")
            .hasSize(4)
        soft.assertThat(relevanceMatches)
            .`as`("создались 4 автотаргетинга")
            .hasSize(4)
        soft.assertThat(relevanceMatches.filter { it.isDeleted == false })
            .`as`("автотаргетинги не удалились")
            .hasSize(4)

        for (bannerId in adGroupIdsToBannerIds.values) {
            val banner = bannerTypedRepository.getSafely(
                clientInfo.shard, bannerId, MobileAppBanner::class.java
            )[0]
            soft.assertThat(banner.primaryAction)
                .`as`("верная кнопка действия")
                .isEqualTo(NewMobileContentPrimaryAction.GET)
        }

        val retargetings = retargetingRepository.getRetargetingsByCampaigns(
            clientInfo.shard, listOf(campaignInfo.id)
        )
        soft.assertThat(retargetings)
            .`as`("создались 8 ретаргетингов")
            .hasSize(8)
        val retargetingConditions = retargetingConditionRepository.getConditions(
            clientInfo.shard, retargetings.map { it.retargetingConditionId })
        soft.assertThat(retargetingConditions)
            .`as`("создались 2 условия ретаргетинга")
            .hasSize(2)

        val retargetingCondition = UacRetargetingCondition(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    goals = listOf(UacRetargetingConditionRuleGoal(id = mobileAppGoalIds[0], name = "ACHIEVED_LEVEL")),
                ),
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.NOT,
                    goals = listOf(UacRetargetingConditionRuleGoal(id = mobileAppGoalIds[8], name = "PURCHASED")),
                ),
            ),
            name = "Условие ретаргетинга 1"
        )
        val directRetargetingCondition = UacRetargetingService.toCoreRetargetingCondition(
            retargetingCondition,
            userInfo.clientId.asLong(),
            type = ConditionType.metrika_goals
        )
        val id = retargetingConditionService.addRetargetingConditions(
            listOf(directRetargetingCondition), userInfo.clientId
        )[0].result
        val campaignModelChanges = KtModelChanges<String, UacYdbCampaign>(newUacCampaign.id)
        campaignModelChanges.process(
            UacYdbCampaign::retargetingCondition, UacRetargetingCondition(
                conditionRules = retargetingCondition.conditionRules,
                name = retargetingCondition.name,
                id = id,
            )
        )
        uacYdbCampaignRepository.update(campaignModelChanges)

        val ydbCampaign = uacYdbCampaignRepository.getCampaign(newUacCampaign.id)!!
        soft.assertThat(ydbCampaign.retargetingCondition?.id).isEqualTo(id)

        val adGroups = uacYdbDirectAdGroupRepository.getDirectAdGroupsByCampaignId(newUacCampaign.id)
        runCreateBannerFunction(uacCampaignContents, ydbCampaign, adGroups)

        val retargetings2 = retargetingRepository.getRetargetingsByCampaigns(
            clientInfo.shard, listOf(campaignInfo.id)
        )
        soft.assertThat(retargetings2)
            .`as`("создались 4 ретаргетинга, удалились ретаргетинги по интересам")
            .hasSize(4)
        val retargetingConditions2 = retargetingConditionRepository.getConditions(
            clientInfo.shard, retargetings2.map { it.retargetingConditionId })
        soft.assertThat(retargetingConditions2)
            .`as`("создалось условие ретаргетинга пользователя, удалились таргетинги по интересам")
            .hasSize(1)

        relevanceMatches = relevanceMatchRepository
            .getRelevanceMatchesByAdGroupIds(
                clientInfo.shard,
                clientInfo.clientId,
                adGroups.map { it.directAdGroupId },
                true
            )
            .values
            .toList()
        soft.assertThat(relevanceMatches)
            .`as`("создались 4 автотаргетинга")
            .hasSize(4)
        soft.assertThat(relevanceMatches.filter { it.isDeleted == false })
            .`as`("все автотаргетинги удалились")
            .hasSize(0)

        for (bannerId in adGroupIdsToBannerIds.values) {
            val banner = bannerTypedRepository.getSafely(
                clientInfo.shard, bannerId, MobileAppBanner::class.java
            )[0]
            soft.assertThat(banner.primaryAction)
                .`as`("верная кнопка действия")
                .isEqualTo(NewMobileContentPrimaryAction.OPEN)
        }

        soft.assertThat(retargetingConditions2[0].rules).hasSize(2)
        soft.assertThat(retargetingConditions2[0].rules[0].goals).hasSize(1)
        soft.assertThat(retargetingConditions2[0].rules[1].goals).hasSize(1)

        soft.assertThat(retargetingConditions2[0].rules[0].goals[0].id).isEqualTo(mobileAppGoalIds[0])
        soft.assertThat(retargetingConditions2[0].rules[1].goals[0].id).isEqualTo(mobileAppGoalIds[8])
        soft.assertAll()
    }

    @Test
    fun createNewAds_WithLalRetargetingCondition() {
        ppcPropertiesSupport.set(PpcPropertyNames.UAC_TARGET_INTERESTS_ENABLED, "true")

        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.IN_APP_MOBILE_TARGETING, true)
        val ydbAppInfo = defaultAppInfo()
        uacAppInfoRepository.saveAppInfo(ydbAppInfo)
        val appInfo = uacAppInfoService.getAppInfo(ydbAppInfo)

        // Создаем цели для ретаргетинга
        val mobileApp = steps.mobileAppSteps().createMobileApp(userInfo.clientInfo!!, appInfo.url).mobileApp
        mobileAppGoalsService.updateMobileAppGoalsForExternalTracker(
            dslContextProvider.ppc(userInfo.shard), userInfo.clientId, listOf(mobileApp)
        )
        val mobileAppGoals = mobileAppGoalsService.getGoalsByApps(userInfo.clientId, listOf(mobileApp))
        val mobileAppGoalIds = mobileAppGoals.map { it.id }

        // Создаем кампанию
        val newUacCampaign = createYdbCampaign(
            appId = ydbAppInfo.id,
            trackingUrl = "https://app.adjust.com/123456",
            impressionUrl = "https://view.adjust.com/impression/123456",
        )
        uacYdbCampaignRepository.addCampaign(newUacCampaign)
        val uacCampaignContents = listOf(
            createCampaignContent(
                campaignId = uacCampaign.id,
                type = MediaType.TITLE,
                text = "title",
            ),
            createCampaignContent(
                campaignId = uacCampaign.id,
                type = MediaType.TEXT,
                text = "text",
            ),
        )
        uacYdbCampaignContentRepository.addCampaignContents(uacCampaignContents)

        val uacDirectContent = createDirectContent()
        uacYdbDirectContentRepository.addDirectContent(listOf(uacDirectContent))

        // Запускаем для кампании создание баннеров и баннерных групп
        runCreateBannerFunction(uacCampaignContents, newUacCampaign)

        val adGroupIdsToBannerIds = bannerRelationsRepository.getAdGroupIdsToNonArchivedBannerIds(
            clientInfo.shard, campaignInfo.id, listOf(MobileAppBanner::class.java)
        )
        var relevanceMatches = relevanceMatchRepository
            .getRelevanceMatchesByAdGroupIds(clientInfo.shard, clientInfo.clientId, adGroupIdsToBannerIds.keys, true)
            .values
            .toList()

        val soft = SoftAssertions()
        soft.assertThat(adGroupIdsToBannerIds.keys)
            .`as`("создалась группа")
            .hasSize(1)
        soft.assertThat(adGroupIdsToBannerIds.values.flatten())
            .`as`("создался баннер")
            .hasSize(1)
        soft.assertThat(relevanceMatches)
            .`as`("создался авторетаргетинг")
            .hasSize(1)
        soft.assertThat(relevanceMatches.filter { it.isDeleted == false })
            .`as`("автотаргетинг не удалился")
            .hasSize(1)

        for (bannerId in adGroupIdsToBannerIds.values) {
            val banner = bannerTypedRepository.getSafely(
                clientInfo.shard, bannerId, MobileAppBanner::class.java
            )[0]
            soft.assertThat(banner.primaryAction)
                .`as`("верная кнопка действия")
                .isEqualTo(NewMobileContentPrimaryAction.GET)
        }

        val retargetings = retargetingRepository.getRetargetingsByCampaigns(
            clientInfo.shard, listOf(campaignInfo.id)
        )
        soft.assertThat(retargetings)
            .`as`("создались 2 ретаргетинга")
            .hasSize(2)
        val retargetingConditions = retargetingConditionRepository.getConditions(
            clientInfo.shard, retargetings.map { it.retargetingConditionId })
        soft.assertThat(retargetingConditions)
            .`as`("создалось одно условие ретаргетинг")
            .hasSize(2)

        // Создаем условие ретаргетинга
        val lalGoal = ucRetargetingConditionService
            .getOrCreateLalRetargetingGoals(clientInfo.clientId!!, setOf(mobileAppGoalIds[0]), mobileAppGoals)[0]
        val retargetingCondition = UacRetargetingCondition(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    goals = listOf(UacRetargetingConditionRuleGoal(id = mobileAppGoalIds[0], type = UacRetargetingConditionRuleGoalType.LAL, time = 540)),
                ),
            ),
            name = "Условие ретаргетинга"
        )
        val directRetargetingCondition = UacRetargetingService.toCoreRetargetingCondition(
            retargetingCondition,
            userInfo.clientId.asLong(),
            type = ConditionType.metrika_goals,
            parentIdToLalSegmentId = mapOf(mobileAppGoalIds[0] to lalGoal.id)
        )
        val id = retargetingConditionService.addRetargetingConditions(
            listOf(directRetargetingCondition), userInfo.clientId
        )[0].result

        // Обновляем кампанию, добавляем ей условие ретаргетинга только с LaL-целями
        val campaignModelChanges = KtModelChanges<String, UacYdbCampaign>(newUacCampaign.id)
        campaignModelChanges.process(
            UacYdbCampaign::retargetingCondition, UacRetargetingCondition(
                conditionRules = retargetingCondition.conditionRules,
                name = retargetingCondition.name,
                id = id,
            )
        )
        uacYdbCampaignRepository.update(campaignModelChanges)

        val ydbCampaign = uacYdbCampaignRepository.getCampaign(newUacCampaign.id)!!
        soft.assertThat(ydbCampaign.retargetingCondition?.id).isEqualTo(id)

        // Повторно запускаем процесс обновления баннеров и групп
        val adGroups = uacYdbDirectAdGroupRepository.getDirectAdGroupsByCampaignId(newUacCampaign.id)
        runCreateBannerFunction(uacCampaignContents, ydbCampaign, adGroups)

        val retargetings2 = retargetingRepository.getRetargetingsByCampaigns(
            clientInfo.shard, listOf(campaignInfo.id)
        )
        soft.assertThat(retargetings2)
            .`as`("создался етаргетинг, ретаргетинги по интересам остались")
            .hasSize(3)
        val retargetingConditions2 = retargetingConditionRepository.getConditions(
            clientInfo.shard, retargetings2.map { it.retargetingConditionId })
        soft.assertThat(retargetingConditions2)
            .`as`("создалось условие ретаргетинга пользователя, таргетинги по интересам остались")
            .hasSize(3)

        relevanceMatches = relevanceMatchRepository
            .getRelevanceMatchesByAdGroupIds(
                clientInfo.shard,
                clientInfo.clientId,
                adGroups.map { it.directAdGroupId },
                true
            )
            .values
            .toList()
        soft.assertThat(relevanceMatches)
            .`as`("создался автотаргетинг")
            .hasSize(1)
        soft.assertThat(relevanceMatches.filter { it.isDeleted == false })
            .`as`("автотаргетинг не удалился")
            .hasSize(1)

        for (bannerId in adGroupIdsToBannerIds.values) {
            val banner = bannerTypedRepository.getSafely(
                clientInfo.shard, bannerId, MobileAppBanner::class.java
            )[0]
            soft.assertThat(banner.primaryAction)
                .`as`("верная кнопка действия")
                .isEqualTo(NewMobileContentPrimaryAction.GET)
        }
        soft.assertAll()
    }

    @Test
    fun createNewAds_AndThenUpdateMinusRegions() {
        ppcPropertiesSupport.set(PpcPropertyNames.UAC_TARGET_INTERESTS_ENABLED, "false")

        val uacCampaignContents = listOf(
            createCampaignContent(
                campaignId = uacCampaign.id,
                type = MediaType.TITLE,
                text = "title",
            ),
            createCampaignContent(
                campaignId = uacCampaign.id,
                type = MediaType.TEXT,
                text = "text",
            ),
        )
        uacYdbCampaignContentRepository.addCampaignContents(uacCampaignContents)

        val uacDirectContent = createDirectContent()
        uacYdbDirectContentRepository.addDirectContent(listOf(uacDirectContent))
        runCreateBannerFunction(uacCampaignContents)

        val adGroupIdsToBannerIds = bannerRelationsRepository.getAdGroupIdsToNonArchivedBannerIds(
            clientInfo.shard, campaignInfo.id, listOf(MobileAppBanner::class.java)
        )

        assertThat(adGroupIdsToBannerIds.keys)
            .`as`("создалась одна группа")
            .hasSize(1)
        assertThat(adGroupIdsToBannerIds.values.flatten())
            .`as`("создался один баннер")
            .hasSize(1)

        val adGroupId: Long = adGroupIdsToBannerIds.keys.first()
        var adGroup = adGroupRepository.getAdGroups(clientInfo.shard, listOf(adGroupId)).first()
        assertThat(adGroup.geo).isEqualTo(listOf(225L, -213L, 977L))

        val newUacCampaign = createYdbCampaign(
            id = uacCampaign.id,
            appId = uacCampaign.appId,
            keywords = uacCampaign.keywords,
            minusKeywords = uacCampaign.minusKeywords,
            regions = listOf(225L),
            minusRegions = listOf(977L, 213L, 2L)
        )
        runCreateBannerFunction(
            uacCampaignContents,
            newUacCampaign,
            uacYdbDirectAdGroupRepository.getDirectAdGroupsByCampaignId(uacCampaign.id),
        )

        adGroup = adGroupRepository.getAdGroups(clientInfo.shard, listOf(adGroupId)).first()
        assertThat(adGroup.geo).isEqualTo(listOf(225L, -213L, -2L))
    }

    private fun runCreateBannerFunction(
        uacCampaignContents: List<UacYdbCampaignContent>,
        uacCampaign: UacYdbCampaign = this.uacCampaign,
        grutDirectAdGroups: Collection<UacYdbDirectAdGroup> = listOf(),
    ) {
        val containers = createUpdateAdsContainers(
            userInfo.uid,
            clientInfo.client!!,
            uacCampaign,
            uacAdGroupBrief = null,
            campaignInfo.campaign,
        )
        bannerCreateJobService.createNewAdsAndUpdateExist(
            userInfo.clientInfo!!.client!!,
            containers,
            uacCampaign,
            uacDirectAdGroups = grutDirectAdGroups,
            uacAssetsByGroupBriefId = mapOf(null as Long? to uacCampaignContents),
            isItCampaignBrief = true,
        )
    }
}
