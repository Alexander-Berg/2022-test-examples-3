package ru.yandex.direct.jobs.uac.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.core.entity.banner.repository.BannerRelationsRepository
import ru.yandex.direct.core.entity.campaign.model.CampaignSource
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.entity.client.model.Client
import ru.yandex.direct.core.entity.hypergeo.service.HyperGeoService
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatchCategory
import ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchRepository
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.GoalType
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.createDirectAd
import ru.yandex.direct.core.entity.uac.createDirectAdGroup
import ru.yandex.direct.core.entity.uac.createDirectContent
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.model.AdvType
import ru.yandex.direct.core.entity.uac.model.DirectCampaignStatus
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.model.direct_ad.DirectAdStatus
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatch
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatchCategory
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectAdGroupRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectAdRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectAdGroup
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectCampaign
import ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaignWithSystemFields
import ru.yandex.direct.core.testing.data.TestCampaigns.simpleStrategy
import ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoal
import ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.TypedCampaignInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.repository.TestCampaignRepository
import ru.yandex.direct.core.testing.repository.TestLalSegmentRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.TypedCampaignStepsUnstubbed
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName.RELEVANCE_MATCH_CATEGORIES_ALLOWED_IN_UC
import ru.yandex.direct.feature.FeatureName.UNIVERSAL_CAMPAIGNS_AUTORETARGETING_ENABLED
import ru.yandex.direct.jobs.configuration.JobsTest
import ru.yandex.direct.jobs.uac.model.createUpdateAdsContainers
import ru.yandex.direct.jobs.uac.repository.AbstractUacRepositoryJobTest
import ru.yandex.direct.regions.Region
import ru.yandex.direct.test.utils.checkContainsKey
import ru.yandex.direct.test.utils.checkEquals

private const val COUNTER_ID = 123
private val SITE_VISIT_GOAL_ID = Goal.METRIKA_COUNTER_LOWER_BOUND + COUNTER_ID

@JobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BannerCreateJobServiceTextCampaignTest : AbstractUacRepositoryJobTest() {
    companion object {
        private val KEYWORDS_FOR_ADD = listOf("промокод озон",
            "озон промокод на скидку")
        private val EXPECTED_KEYWORDS_IN_DB = listOf("промокод озон -скидка",
            "озон промокод на скидку")
    }

    @Autowired
    private lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    private lateinit var uacYdbDirectContentRepository: UacYdbDirectContentRepository

    @Autowired
    private lateinit var bannerCreateJobService: YdbBannerCreateJobService

    @Autowired
    private lateinit var typedCampaignStepsUnstubbed: TypedCampaignStepsUnstubbed

    @Autowired
    private lateinit var uacYdbCampaignRepository: UacYdbCampaignRepository

    @Autowired
    private lateinit var uacYdbDirectCampaignRepository: UacYdbDirectCampaignRepository

    @Autowired
    private lateinit var bannerRelationsRepository: BannerRelationsRepository

    @Autowired
    private lateinit var retargetingRepository: RetargetingRepository

    @Autowired
    private lateinit var retargetingConditionRepository: RetargetingConditionRepository

    @Autowired
    private lateinit var testLalSegmentRepository: TestLalSegmentRepository

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var metrikaClient: MetrikaClientStub

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var metrikaHelper: MetrikaHelperStub

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Autowired
    private lateinit var hyperGeoService: HyperGeoService

    @Autowired
    private lateinit var testCampaignRepository: TestCampaignRepository

    @Autowired
    private lateinit var uacYdbDirectAdGroupRepository: UacYdbDirectAdGroupRepository

    @Autowired
    private lateinit var uacYdbDirectAdRepository: UacYdbDirectAdRepository

    @Autowired
    private lateinit var keywordRepository: KeywordRepository

    @Autowired
    private lateinit var relevanceMatchRepository: RelevanceMatchRepository

    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId
    private lateinit var client: Client
    private var uid: Long = 0
    private lateinit var userInfo: UserInfo
    private lateinit var campaignInfo: TypedCampaignInfo
    private lateinit var uacCampaign: UacYdbCampaign

    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        userInfo = clientInfo.chiefUserInfo!!
        uid = userInfo.uid
        clientId = clientInfo.clientId!!
        client = clientInfo.client!!

        steps.trustedRedirectSteps().addValidCounters()

        metrikaClient.addUserCounter(uid, COUNTER_ID)
        metrikaClient.addCounterGoal(COUNTER_ID, SITE_VISIT_GOAL_ID.toInt())
        metrikaClient.addGoals(uid, setOf(defaultGoal(SITE_VISIT_GOAL_ID)))
        metrikaHelper.addGoalIds(uid, setOf(SITE_VISIT_GOAL_ID))
        val lalSegment = defaultGoalByType(GoalType.LAL_SEGMENT).apply {
            parentId = SITE_VISIT_GOAL_ID
        }
        testLalSegmentRepository.addAll(listOf(lalSegment))

        val textCampaign = defaultTextCampaignWithSystemFields(clientInfo).apply {
            strategy = simpleStrategy()
        }
        campaignInfo = typedCampaignStepsUnstubbed.createTextCampaign(userInfo, clientInfo, textCampaign)
        testCampaignRepository.setSource(campaignInfo.shard, campaignInfo.id, CampaignSource.UAC)
        campaignInfo.campaign
            .withSource(CampaignSource.UAC)
    }

    @Test
    fun createNewAdsAndUpdateExist_WithAutoRetargeting() {
        steps.featureSteps().addClientFeature(userInfo.clientId, UNIVERSAL_CAMPAIGNS_AUTORETARGETING_ENABLED, true)

        uacCampaign = createYdbUcCampaign()
        uacYdbCampaignRepository.addCampaign(uacCampaign)
        val uacCampaignContents = listOf(
            createDefaultTitleContent(),
            createDefaultTextContent(),
        )
        createYdbUcDirectCampaign()

        uacYdbCampaignContentRepository.addCampaignContents(uacCampaignContents)
        val uacDirectContent = createDirectContent()
        uacYdbDirectContentRepository.addDirectContent(listOf(uacDirectContent))

        runCreateBannerFunction(uacCampaignContents)

        val adGroupIdsToBannerIds = bannerRelationsRepository.getAdGroupIdsToNonArchivedBannerIds(
            clientInfo.shard, campaignInfo.id, listOf(TextBanner::class.java)
        )

        assertThat(adGroupIdsToBannerIds.keys)
            .`as`("создалась одна группа")
            .hasSize(1)
        assertThat(adGroupIdsToBannerIds.values.flatten())
            .`as`("создался один баннер")
            .hasSize(1)

        checkAutoRetargetingExisting()

        val uacYdbDirectAdGroups = uacYdbDirectAdGroupRepository.getDirectAdGroupsByCampaignId(uacCampaign.id)
        runCreateBannerFunction(uacCampaignContents, uacYdbDirectAdGroups)

        checkAutoRetargetingExisting()
    }

    /**
     * Проверяем обновление фраз у группы
     */
    @Test
    fun updateExistedAdGroupWithNewKeywords() {
        ppcPropertiesSupport.remove(PpcPropertyNames.MAX_BANNERS_IN_UAC_TEXT_AD_GROUP)

        val textCampaign = defaultTextCampaignWithSystemFields(clientInfo)
            .apply {
                strategy = simpleStrategy()
            }
        val textCampaignInfo = typedCampaignStepsUnstubbed.createTextCampaign(userInfo, clientInfo, textCampaign)
        val adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(textCampaignInfo.toCampaignInfo())

        uacCampaign = createYdbUcCampaign()
        uacYdbCampaignRepository.addCampaign(uacCampaign)
        val uacCampaignContents = listOf(
            createDefaultTitleContent(),
            createDefaultTextContent(),
        )
        uacYdbCampaignContentRepository.addCampaignContents(uacCampaignContents)
        createYdbUcDirectCampaign(textCampaign)

        val uacDirectAdGroup = createDirectAdGroup(
            directCampaignId = uacCampaign.id,
            directAdGroupId = adGroupInfo.adGroupId,
        )
        uacYdbDirectAdGroupRepository.saveDirectAdGroup(uacDirectAdGroup)

        val phrasesBeforeUpdate = keywordRepository.getKeywordTextsByAdGroupIds(
            clientInfo.shard, clientId, listOf(adGroupInfo.adGroupId))

        val soft = SoftAssertions()
        soft.assertThat(phrasesBeforeUpdate)
            .`as`("нет фраз до обновления")
            .isEmpty()

        runCreateBannerFunction(
            uacCampaignContents,
            listOf(uacDirectAdGroup),
            directCampaign = textCampaignInfo.campaign,
        )

        val actualUacAdGroups = uacYdbDirectAdGroupRepository.getDirectAdGroupsByCampaignId(uacCampaign.id)
        val actualAdGroupIds = actualUacAdGroups.map { it.directAdGroupId }
        val actualGroupIdToPhrases = keywordRepository.getKeywordTextsByAdGroupIds(
            clientInfo.shard, clientId, actualAdGroupIds
        ).mapValues {
            it.value.map { k -> k.phrase }
        }

        val expectAdGroupIdToKeywords = actualAdGroupIds
            .associateBy({ it }, { EXPECTED_KEYWORDS_IN_DB })
        val keywordsInYdb = uacYdbCampaignRepository.getCampaign(uacCampaign.id)?.keywords!!

        soft.assertThat(actualUacAdGroups)
            .`as`("количество групп в ydb")
            .hasSize(1)
        soft.assertThat(actualGroupIdToPhrases)
            .`as`("Фразы у групп")
            .isEqualTo(expectAdGroupIdToKeywords)
        soft.assertThat(keywordsInYdb)
            .`as`("Фразы в ydb")
            .isEqualTo(EXPECTED_KEYWORDS_IN_DB)
        soft.assertAll()
    }

    @Test
    fun createNewAds_WithHyperlocalAdGroup() {
        val hyperGeo = steps.hyperGeoSteps().createHyperGeo(clientInfo)

        uacCampaign = createYdbUcCampaign(
            regions = null,
            hyperGeoId = hyperGeo.id,
        )
        uacYdbCampaignRepository.addCampaign(uacCampaign)
        createYdbUcDirectCampaign()

        val uacCampaignContents = listOf(
            createDefaultTitleContent(),
            createDefaultTextContent(),
        )
        uacYdbCampaignContentRepository.addCampaignContents(uacCampaignContents)
        val uacDirectContent = createDirectContent()
        uacYdbDirectContentRepository.addDirectContent(listOf(uacDirectContent))

        runCreateBannerFunction(uacCampaignContents)

        val adGroupIdsToBannerIds = bannerRelationsRepository.getAdGroupIdsToNonArchivedBannerIds(
            clientInfo.shard, campaignInfo.id, listOf(TextBanner::class.java)
        )

        assertThat(adGroupIdsToBannerIds.keys)
            .`as`("создалась одна группа")
            .hasSize(1)
        assertThat(adGroupIdsToBannerIds.values.flatten())
            .`as`("создался один баннер")
            .hasSize(1)

        val adGroupId = adGroupIdsToBannerIds.keys.first()
        val hyperGeoByAdGroupId = hyperGeoService.getHyperGeoByAdGroupId(clientId, listOf(adGroupId))

        hyperGeoByAdGroupId
            .checkContainsKey(adGroupId)
        hyperGeoByAdGroupId[adGroupId].checkEquals(hyperGeo)
    }

    /**
     * Проверяем создание нескольких групп
     */
    @Test
    fun createMultipleAdGroups() {
        ppcPropertiesSupport.set(PpcPropertyNames.MAX_BANNERS_IN_UAC_TEXT_AD_GROUP, "1")

        uacCampaign = createYdbUcCampaign()
        uacYdbCampaignRepository.addCampaign(uacCampaign)
        createYdbUcDirectCampaign()

        val uacCampaignContents = listOf(
            createDefaultTitleContent("title_1"),
            createDefaultTitleContent("title_2"),
            createDefaultTextContent(),
        )
        uacYdbCampaignContentRepository.addCampaignContents(uacCampaignContents)
        val uacDirectContent = createDirectContent()
        uacYdbDirectContentRepository.addDirectContent(listOf(uacDirectContent))

        runCreateBannerFunction(uacCampaignContents)

        val adGroupIdsToBannerIds = bannerRelationsRepository.getAdGroupIdsToNonArchivedBannerIds(
            clientInfo.shard, campaignInfo.id, listOf(TextBanner::class.java)
        ).mapValues { it.value.size }

        val soft = SoftAssertions()
        soft.assertThat(adGroupIdsToBannerIds)
            .`as`("создалось две группы c одним банером в каждой")
            .hasSize(2)
            .containsValues(1, 1)

        val expectAdGroupIdToKeywords = adGroupIdsToBannerIds.keys
            .associateBy({ it }, { EXPECTED_KEYWORDS_IN_DB })
        val keywordsInYdb = uacYdbCampaignRepository.getCampaign(uacCampaign.id)?.keywords!!
        val actualGroupIdToPhrases = keywordRepository.getKeywordTextsByAdGroupIds(
            clientInfo.shard, clientId, adGroupIdsToBannerIds.keys
        ).mapValues {
            it.value.map { k -> k.phrase }
        }

        soft.assertThat(actualGroupIdToPhrases)
            .`as`("Фразы у групп")
            .isEqualTo(expectAdGroupIdToKeywords)
        soft.assertThat(keywordsInYdb)
            .`as`("Фразы в ydb")
            .isEqualTo(EXPECTED_KEYWORDS_IN_DB)
        soft.assertAll()
    }

    /**
     * Проверяем что при расчете количества баннеров на группу не учитываем удаленные
     */
    @Test
    fun createNewAds_AdGroupWithDeletedAds_AdsAddToTheAdGroup() {
        val adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo.toCampaignInfo())

        uacCampaign = createYdbUcCampaign()
        uacYdbCampaignRepository.addCampaign(uacCampaign)
        createYdbUcDirectCampaign()

        val uacCampaignContents = listOf(
            createDefaultTitleContent("title_1"),
            createDefaultTextContent(),
        )

        val uacDirectAdGroup = createDirectAdGroup(
            directCampaignId = uacCampaign.id,
            directAdGroupId = adGroupInfo.adGroupId,
        )
        uacYdbDirectAdGroupRepository.saveDirectAdGroup(uacDirectAdGroup)

        val deletedDirectAd = createDirectAd(
            directAdGroupId = uacDirectAdGroup.id,
            status = DirectAdStatus.DELETED,
        )
        uacYdbDirectAdRepository.saveDirectAd(deletedDirectAd)

        runCreateBannerFunction(uacCampaignContents, listOf(uacDirectAdGroup))

        val adGroupIdsToBannerIds = bannerRelationsRepository.getAdGroupIdsToNonArchivedBannerIds(
            clientInfo.shard, campaignInfo.id, listOf(TextBanner::class.java)
        ).mapValues { it.value.size }

        assertThat(adGroupIdsToBannerIds)
            .`as`("баннер добавился в уже существующую группу")
            .hasSize(1)
            .containsEntry(adGroupInfo.adGroupId, 1)
    }

    fun casesForAutotargetingCategories(): Array<Array<Any>> {
        val activeCategories = UacRelevanceMatchCategory.values()
            .map { arrayOf(true, true, setOf(it, UacRelevanceMatchCategory.EXACT_MARK).sorted()) }
        val activeCategoriesWithoutFeature = UacRelevanceMatchCategory.values()
            .map { arrayOf(false, true, setOf(it, UacRelevanceMatchCategory.EXACT_MARK).sorted()) }
        val inActiveCategories = UacRelevanceMatchCategory.values()
            .map { arrayOf(true, false, listOf(it)) }
        val inActiveCategoriesWithoutFeature = UacRelevanceMatchCategory.values()
            .map { arrayOf(false, false, listOf(it)) }
        return activeCategories
            .plus(activeCategoriesWithoutFeature)
            .plus(inActiveCategories)
            .plus(inActiveCategoriesWithoutFeature)
            .plus(arrayOf(arrayOf(true, true, UacRelevanceMatchCategory.values().sorted())))
            .plus(arrayOf(arrayOf(true, false, UacRelevanceMatchCategory.values().sorted())))
            .plus(arrayOf(arrayOf(false, true, UacRelevanceMatchCategory.values().sorted())))
            .plus(arrayOf(arrayOf(false, false, UacRelevanceMatchCategory.values().sorted())))
            .toTypedArray()
    }

    /**
     * Проверяем создание UC группы с автотаргетингом
     */
    @ParameterizedTest(name = "Active {1} featureEnabled {0} categories {2}")
    @MethodSource("casesForAutotargetingCategories")
    fun createNewGroupAndAds_WithAutotargeting(
        featureEnabled: Boolean,
        active: Boolean,
        categories: List<UacRelevanceMatchCategory>,
    ) {
        steps.featureSteps()
            .addClientFeature(userInfo.clientId, RELEVANCE_MATCH_CATEGORIES_ALLOWED_IN_UC, featureEnabled)

        val relevanceMatch = UacRelevanceMatch(
            active = active,
            categories = categories.toSet(),
        )
        uacCampaign = createYdbUcCampaign(relevanceMatch = relevanceMatch)
        uacYdbCampaignRepository.addCampaign(uacCampaign)
        val uacCampaignContents: List<UacYdbCampaignContent> = listOf(
            createDefaultTitleContent(),
            createDefaultTextContent(),
        )
        val adGroupIds = createAndCheckYdbCampaignDataAndGetAdGroupIds(uacCampaignContents = uacCampaignContents)

        // Проверяем список категорий автотаргетинга
        val expectRelevanceMatchCategories = if (!active || !featureEnabled) emptySet() else categories
            .map { RelevanceMatchCategory.fromTypedValue(it.typedValue) }
            .toSet()
        checkAutotargeting(adGroupIds, expectRelevanceMatchCategories)
    }

    /**
     * Проверяем обновление автотаргетинга c категориями у группы, когда у группы ранее не был включен автотаргетинг
     */
    @ParameterizedTest(name = "Active {1} featureEnabled {0} categories {2}")
    @MethodSource("casesForAutotargetingCategories")
    fun updateNewGroupAndAds_AddAutotargeting(
        featureEnabled: Boolean,
        active: Boolean,
        categories: List<UacRelevanceMatchCategory>,
    ) {
        steps.featureSteps()
            .addClientFeature(userInfo.clientId, RELEVANCE_MATCH_CATEGORIES_ALLOWED_IN_UC, featureEnabled)

        uacCampaign = createYdbUcCampaign()
        uacYdbCampaignRepository.addCampaign(uacCampaign)
        val uacCampaignContents: List<UacYdbCampaignContent> = listOf(
            createDefaultTitleContent(),
            createDefaultTextContent(),
        )
        val adGroupIds = createAndCheckYdbCampaignDataAndGetAdGroupIds(uacCampaignContents = uacCampaignContents)
        checkAutotargeting(adGroupIds, emptySet())

        // Обновляем мли отключаем категории автотаргетинга
        val uacYdbDirectAdGroups = uacYdbDirectAdGroupRepository.getDirectAdGroupsByCampaignId(uacCampaign.id)
        val newRelevanceMatch = UacRelevanceMatch(
            active = active,
            categories = categories.toSet(),
        )
        val newUacCampaign = uacCampaign.copy(relevanceMatch = newRelevanceMatch)

        runCreateBannerFunction(uacCampaignContents, uacYdbDirectAdGroups, newUacCampaign)

        // Проверяем что список категорий автотаргетинга изменился
        val expectRelevanceMatchCategories = if (!active || !featureEnabled) emptySet() else categories
            .map { RelevanceMatchCategory.fromTypedValue(it.typedValue) }
            .toSet()
        checkAutotargeting(adGroupIds, expectRelevanceMatchCategories)
    }

    /**
     * Проверяем обновление/удаление категорий автотаргетинга у группы
     */
    @ParameterizedTest(name = "Active {1} featureEnabled {0} categories {2}")
    @MethodSource("casesForAutotargetingCategories")
    fun updateAndDeleteAutotargeting(
        featureEnabled: Boolean,
        active: Boolean,
        categories: List<UacRelevanceMatchCategory>,
    ) {
        steps.featureSteps()
            .addClientFeature(userInfo.clientId, RELEVANCE_MATCH_CATEGORIES_ALLOWED_IN_UC, true)

        val relevanceMatch = UacRelevanceMatch(
            active = true,
            categories = setOf(UacRelevanceMatchCategory.EXACT_MARK),
        )

        uacCampaign = createYdbUcCampaign(relevanceMatch = relevanceMatch)
        uacYdbCampaignRepository.addCampaign(uacCampaign)
        val uacCampaignContents: List<UacYdbCampaignContent> = listOf(
            createDefaultTitleContent(),
            createDefaultTextContent(),
        )
        val adGroupIds = createAndCheckYdbCampaignDataAndGetAdGroupIds(uacCampaignContents = uacCampaignContents)

        checkAutotargeting(adGroupIds, setOf(RelevanceMatchCategory.exact_mark))

        // Кейс когда кампания была создана с включенной фичей и затем мы ее выключили/оставили
        steps.featureSteps()
            .addClientFeature(userInfo.clientId, RELEVANCE_MATCH_CATEGORIES_ALLOWED_IN_UC, featureEnabled)

        // Обновляем мли отключаем категории автотаргетинга
        val uacYdbDirectAdGroups = uacYdbDirectAdGroupRepository.getDirectAdGroupsByCampaignId(uacCampaign.id)
        val newRelevanceMatch = UacRelevanceMatch(
            active = active,
            categories = categories.toSet(),
        )
        val newUacCampaign = uacCampaign.copy(relevanceMatch = newRelevanceMatch)

        runCreateBannerFunction(uacCampaignContents, uacYdbDirectAdGroups, newUacCampaign)

        // Проверяем что список категорий автотаргетинга изменился
        var expectRelevanceMatchCategories =
            if (!featureEnabled) setOf(RelevanceMatchCategory.exact_mark)
            else categories
                .map { RelevanceMatchCategory.fromTypedValue(it.typedValue) }
                .toSet()
        expectRelevanceMatchCategories =
            if (!active && featureEnabled) setOf()
            else expectRelevanceMatchCategories
        checkAutotargeting(adGroupIds, expectRelevanceMatchCategories)
    }

    private fun checkAutoRetargetingExisting() {
        val retargetings = retargetingRepository.getRetargetingsByCampaigns(
            clientInfo.shard, listOf(campaignInfo.id))

        val soft = SoftAssertions()
        soft.assertThat(retargetings)
            .`as`("создался один ретаргетинг")
            .hasSize(1)

        val retargetingConditions = retargetingConditionRepository.getConditions(
            clientInfo.shard, retargetings.map { it.retargetingConditionId })
        soft.assertThat(retargetingConditions)
            .`as`("создалось одно условие ретаргетинга")
            .hasSize(1)

        val retargetingCondition = retargetingConditions[0]
        soft.assertThat(retargetingCondition.autoRetargeting).isTrue
        soft.assertAll()
    }

    private fun checkAutotargeting(
        adGroupIds: Collection<Long>,
        expectCategories: Set<RelevanceMatchCategory>?,
    ) {
        val relevanceMatches = relevanceMatchRepository
            .getRelevanceMatchesByAdGroupIds(clientInfo.shard, clientId, adGroupIds, true)
            .values
            .toList()

        val soft = SoftAssertions()
        soft.assertThat(relevanceMatches)
            .`as`("Количество автотаргетингов")
            .hasSize(1)
        soft.assertThat(relevanceMatches[0].isDeleted)
            .`as`("Удаленный")
            .isEqualTo(false)
        soft.assertThat(relevanceMatches[0].relevanceMatchCategories)
            .`as`("Категории")
            .isEqualTo(expectCategories)
        soft.assertAll()
    }

    private fun createAndCheckYdbCampaignDataAndGetAdGroupIds(
        uacCampaignContents: List<UacYdbCampaignContent>,
    ): Set<Long> {
        createYdbUcDirectCampaign()

        uacYdbCampaignContentRepository.addCampaignContents(uacCampaignContents)
        val uacDirectContent = createDirectContent()
        uacYdbDirectContentRepository.addDirectContent(listOf(uacDirectContent))

        runCreateBannerFunction(uacCampaignContents)

        val adGroupIdsToBannerIds = bannerRelationsRepository.getAdGroupIdsToNonArchivedBannerIds(
            clientInfo.shard, campaignInfo.id, listOf(TextBanner::class.java)
        )

        val soft = SoftAssertions()
        soft.assertThat(adGroupIdsToBannerIds.keys)
            .`as`("создалась одна группа")
            .hasSize(1)
        soft.assertThat(adGroupIdsToBannerIds.values.flatten())
            .`as`("создался один баннер")
            .hasSize(1)
        soft.assertAll()

        return adGroupIdsToBannerIds.keys
    }

    private fun createDefaultTitleContent(
        title: String = "title",
        uacCampaignId: String = uacCampaign.id,
    ) = createCampaignContent(
        campaignId = uacCampaignId,
        type = MediaType.TITLE,
        text = title,
    )

    private fun createDefaultTextContent(
        uacCampaignId: String = uacCampaign.id,
    ) = createCampaignContent(
        campaignId = uacCampaignId,
        type = MediaType.TEXT,
        text = "text",
    )

    private fun createYdbUcCampaign(
        relevanceMatch: UacRelevanceMatch? = null,
        regions: List<Long>? = listOf(Region.RUSSIA_REGION_ID),
        hyperGeoId: Long? = null,
    ) = createYdbCampaign(
        advType = AdvType.TEXT,
        href = "https://www.yandex.ru/company",
        counterIds = listOf(COUNTER_ID),
        keywords = KEYWORDS_FOR_ADD,
        relevanceMatch = relevanceMatch,
        regions = regions,
        hyperGeoId = hyperGeoId,
    )

    private fun createYdbUcDirectCampaign(commonCampaign: CommonCampaign = campaignInfo.campaign) {
        uacYdbDirectCampaignRepository.saveDirectCampaign(UacYdbDirectCampaign(
            id = uacCampaign.id,
            directCampaignId = commonCampaign.id,
            status = DirectCampaignStatus.CREATED,
            syncedAt = commonCampaign.lastChange,
            rejectReasons = null,
        ))
    }

    private fun runCreateBannerFunction(
        uacCampaignContents: List<UacYdbCampaignContent>,
        grutDirectAdGroups: Collection<UacYdbDirectAdGroup> = listOf(),
        uacCampaign: UacYdbCampaign = this.uacCampaign,
        directCampaign: CommonCampaign = campaignInfo.campaign,
    ) {
        val containers = createUpdateAdsContainers(
            userInfo.uid,
            clientInfo.client!!,
            uacCampaign,
            uacAdGroupBrief = null,
            directCampaign,
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
