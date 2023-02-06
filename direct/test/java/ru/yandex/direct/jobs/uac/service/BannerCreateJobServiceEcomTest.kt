package ru.yandex.direct.jobs.uac.service

import com.nhaarman.mockitokotlin2.anyVararg
import com.nhaarman.mockitokotlin2.doReturn
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.bannerstorage.client.DummyBannerStorageClient
import ru.yandex.direct.bannerstorage.client.model.Template
import ru.yandex.direct.core.entity.adgroup.container.AdGroupsSelectionCriteria
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService
import ru.yandex.direct.core.entity.banner.model.DynamicBanner
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner
import ru.yandex.direct.core.entity.banner.model.PerformanceBannerMain
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.core.entity.banner.repository.BannerRelationsRepository
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.campaign.service.CampaignService
import ru.yandex.direct.core.entity.offerretargeting.repository.OfferRetargetingRepository
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.GoalType
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.createDirectContent
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.getCreativeGroup
import ru.yandex.direct.core.entity.uac.model.AdvType
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectAdGroupRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectAdGroup
import ru.yandex.direct.core.testing.data.TestFullGoals
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.repository.TestLalSegmentRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.UacCampaignSteps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.jobs.configuration.JobsTest
import ru.yandex.direct.jobs.uac.model.createUpdateAdsContainers
import ru.yandex.direct.jobs.uac.repository.AbstractUacRepositoryJobTest
import ru.yandex.direct.multitype.entity.LimitOffset
import ru.yandex.direct.test.utils.randomPositiveInt
import java.util.function.Consumer

private const val COUNTER_ID = 123
private const val SITE_VISIT_GOAL_ID = Goal.METRIKA_COUNTER_LOWER_BOUND + COUNTER_ID

@JobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled("Ходит в BannerStorage, поэтому в sandbox'е не пройдёт. Для ручного запуска нужен настоящий клиент BannerStorage")
class BannerCreateJobServiceEcomTest : AbstractUacRepositoryJobTest() {
    companion object {
        private val KEYWORDS = listOf("keyword1", "keyword2")
    }

    @Autowired
    private lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    private lateinit var uacYdbDirectContentRepository: UacYdbDirectContentRepository

    @Autowired
    private lateinit var bannerCreateJobService: YdbBannerCreateJobService

    @Autowired
    private lateinit var campaignService: CampaignService

    @Autowired
    private lateinit var campaignRepository: CampaignTypedRepository

    @Autowired
    private lateinit var adGroupService: AdGroupService

    @Autowired
    private lateinit var bannerRepository: BannerTypedRepository

    @Autowired
    private lateinit var offerRetargetingRepository: OfferRetargetingRepository

    @Autowired
    private lateinit var uacYdbCampaignRepository: UacYdbCampaignRepository

    @Autowired
    private lateinit var uacYdbDirectAdGroupRepository: UacYdbDirectAdGroupRepository

    @Autowired
    private lateinit var bannerRelationsRepository: BannerRelationsRepository

    @Autowired
    private lateinit var testLalSegmentRepository: TestLalSegmentRepository

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var metrikaClient: MetrikaClientStub

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var metrikaHelper: MetrikaHelperStub

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var dummyBannerStorageClient: DummyBannerStorageClient

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var uacCampaignSteps: UacCampaignSteps

    private lateinit var clientInfo: ClientInfo
    private lateinit var userInfo: UserInfo
    private lateinit var campaign: TextCampaign
    private lateinit var uacCampaign: UacYdbCampaign
    private var feedId: Long = 0

    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        userInfo = clientInfo.chiefUserInfo!!

        steps.trustedRedirectSteps().addValidCounters()

        metrikaClient.addUserCounter(userInfo.uid, COUNTER_ID)
        metrikaClient.addCounterGoal(COUNTER_ID, SITE_VISIT_GOAL_ID.toInt())
        metrikaClient.addGoals(userInfo.uid, setOf(TestFullGoals.defaultGoal(SITE_VISIT_GOAL_ID)))
        val lalSegment = TestFullGoals.defaultGoalByType(GoalType.LAL_SEGMENT).apply {
            parentId = SITE_VISIT_GOAL_ID
        }
        testLalSegmentRepository.addAll(listOf(lalSegment))

        feedId = steps.feedSteps().createDefaultFeed(clientInfo).feedId

        doReturn(Template(0, "", emptyList(), emptyList()))
            .`when`(dummyBannerStorageClient).getTemplate(ArgumentMatchers.anyInt(), anyVararg())

        val creativeIds = List(5) { randomPositiveInt() }
        doReturn(getCreativeGroup(creativeIds))
            .`when`(dummyBannerStorageClient).createSmartCreativeGroup(ArgumentMatchers.any())
    }

    @Test
    fun createNewAdsAndUpdateExist_ecomEntitiesCreated() {
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.UNIVERSAL_CAMPAIGNS_AUTORETARGETING_ENABLED, true)
        createEcomUcCampaignInfo(false)

        uacCampaign = createYdbCampaign(
                advType = AdvType.TEXT,
                href = "https://market.yandex.ru/",
                counterIds = listOf(COUNTER_ID),
                keywords = KEYWORDS,
                isEcom = true,
                feedId = feedId
        )
        uacYdbCampaignRepository.addCampaign(uacCampaign)
        val uacCampaignContents = listOf(
                createDefaultTitleContent(),
                createDefaultTextContent(),
        )
        uacYdbCampaignContentRepository.addCampaignContents(uacCampaignContents)
        val uacDirectContent = createDirectContent()
        uacYdbDirectContentRepository.addDirectContent(listOf(uacDirectContent))

        runCreateBannerFunction(uacCampaignContents)

        var adGroupIdsToBannerIds = bannerRelationsRepository.getAdGroupIdsToNonArchivedBannerIds(
                clientInfo.shard, campaign.id, listOf(TextBanner::class.java)
        )

        Assertions.assertThat(adGroupIdsToBannerIds.keys)
                .`as`("создалась одна группа")
                .hasSize(1)
        Assertions.assertThat(adGroupIdsToBannerIds.values.flatten())
                .`as`("создался один баннер")
                .hasSize(1)

        val subCampaignsIds = campaignService.getSubCampaignIdsWithMasterIds(setOf(campaign.id), clientInfo.clientId!!).keys
        val subCampaignsByType = campaignRepository.getTypedCampaigns(clientInfo.shard, subCampaignsIds)
                .map { it as CommonCampaign }
                .associateBy { it.type }

        val dynamicSubCampaign = subCampaignsByType[CampaignType.DYNAMIC]!!
        adGroupIdsToBannerIds = bannerRelationsRepository.getAdGroupIdsToNonArchivedBannerIds(
                clientInfo.shard, dynamicSubCampaign.id, listOf(DynamicBanner::class.java)
        )
        Assertions.assertThat(adGroupIdsToBannerIds.keys)
                .`as`("создалась одна динамическая группа")
                .hasSize(1)
        Assertions.assertThat(adGroupIdsToBannerIds.values.flatten())
                .`as`("создался один динамический баннер")
                .hasSize(1)

        val smartSubCampaign = subCampaignsByType[CampaignType.PERFORMANCE]!!
        adGroupIdsToBannerIds = bannerRelationsRepository.getAdGroupIdsToNonArchivedBannerIds(
                clientInfo.shard, smartSubCampaign.id, listOf(PerformanceBanner::class.java)
        )
        Assertions.assertThat(adGroupIdsToBannerIds.keys)
                .`as`("создалась одна смарт группа")
                .hasSize(1)
        Assertions.assertThat(adGroupIdsToBannerIds.values.flatten())
                .`as`("создались смарт баннеры")
                .isNotEmpty // Точное количество этих баннеров зависит от количества креативов, созданных по дефолтам
    }

    @Test
    fun createNewAdsAndUpdateExist_dynamicBannersUseLimitedCombinatorics() {
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.UNIVERSAL_CAMPAIGNS_AUTORETARGETING_ENABLED, true)
        createEcomUcCampaignInfo(false)

        uacCampaign = createYdbCampaign(
            advType = AdvType.TEXT,
            href = "https://market.yandex.ru/",
            counterIds = listOf(COUNTER_ID),
            keywords = KEYWORDS,
            isEcom = true,
            feedId = feedId
        )
        uacYdbCampaignRepository.addCampaign(uacCampaign)
        val uacCampaignContents = listOf(
            createDefaultTitleContent(title = "title1"),
            createDefaultTitleContent(title = "title2"),
            createDefaultTitleContent(title = "title3"),
            createDefaultTextContent(text = "text1"),
            createDefaultTextContent(text = "text2"),
            createDefaultTextContent(text = "text3"),
        )
        uacYdbCampaignContentRepository.addCampaignContents(uacCampaignContents)
        val uacDirectContent1 = createDirectContent()
        val uacDirectContent2 = createDirectContent()
        uacYdbDirectContentRepository.addDirectContent(listOf(uacDirectContent1, uacDirectContent2))

        runCreateBannerFunction(uacCampaignContents)

        val subCampaignsIds = campaignService.getSubCampaignIdsWithMasterIds(setOf(campaign.id), clientInfo.clientId!!).keys
        val subCampaignsByType = campaignRepository.getTypedCampaigns(clientInfo.shard, subCampaignsIds)
            .map { it as CommonCampaign }
            .associateBy { it.type }

        val dynamicSubCampaign = subCampaignsByType[CampaignType.DYNAMIC]!!
        val adGroupIdsToBannerIds = bannerRelationsRepository.getAdGroupIdsToNonArchivedBannerIds(
            clientInfo.shard, dynamicSubCampaign.id, listOf(DynamicBanner::class.java)
        )
        Assertions.assertThat(adGroupIdsToBannerIds.keys)
            .`as`("создалась одна динамическая группа")
            .hasSize(1)
        Assertions.assertThat(adGroupIdsToBannerIds.values.flatten())
            .`as`("создалось три динамических баннера")
            .hasSize(3)
    }

    @Test
    fun createNewAdsAndUpdateExist_dynamicBannersCreatedOnlyByNewTexts() {
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.UNIVERSAL_CAMPAIGNS_AUTORETARGETING_ENABLED, true)
        createEcomUcCampaignInfo(false)

        uacCampaign = createYdbCampaign(
            advType = AdvType.TEXT,
            href = "https://market.yandex.ru/",
            counterIds = listOf(COUNTER_ID),
            keywords = KEYWORDS,
            isEcom = true,
            feedId = feedId
        )
        val text1 = createDefaultTextContent(text = "text1")
        val text2 = createDefaultTextContent(text = "text2")
        val text3 = createDefaultTextContent(text = "text3")
        uacYdbCampaignRepository.addCampaign(uacCampaign)
        val uacCampaignContents = listOf(
            createDefaultTitleContent(title = "title1"),
            createDefaultTitleContent(title = "title2"),
            createDefaultTitleContent(title = "title3"),
            text1,
            text2,
            text3,
        )
        uacYdbCampaignContentRepository.addCampaignContents(uacCampaignContents)
        val uacDirectContent1 = createDirectContent()
        val uacDirectContent2 = createDirectContent()
        uacYdbDirectContentRepository.addDirectContent(listOf(uacDirectContent1, uacDirectContent2))

        runCreateBannerFunction(uacCampaignContents)

        // Меняем только заголовки
        val title4 = createDefaultTitleContent(title = "title4")
        val title5 = createDefaultTitleContent(title = "title5")
        val title6 = createDefaultTitleContent(title = "title6")
        val uacCampaignContents2 = listOf(
            title4,
            title5,
            title6,
            text1,
            text2,
            text3
        )
        uacYdbCampaignContentRepository.addCampaignContents(listOf(title4, title5, title6))
        val ydbAdGroups = uacYdbDirectAdGroupRepository.getDirectAdGroupsByCampaignId(uacCampaign.id)
        runCreateBannerFunction(uacCampaignContents2, ydbAdGroups)

        // Проверяем, что ДО баннеров ровно 3
        val subCampaignsIds = campaignService.getSubCampaignIdsWithMasterIds(setOf(campaign.id), clientInfo.clientId!!).keys
        val subCampaignsByType = campaignRepository.getTypedCampaigns(clientInfo.shard, subCampaignsIds)
            .map { it as CommonCampaign }
            .associateBy { it.type }

        val dynamicSubCampaign = subCampaignsByType[CampaignType.DYNAMIC]!!
        val adGroupIdsToBannerIds = bannerRelationsRepository.getAdGroupIdsToNonArchivedBannerIds(
            clientInfo.shard, dynamicSubCampaign.id, listOf(DynamicBanner::class.java)
        )
        Assertions.assertThat(adGroupIdsToBannerIds.keys)
            .`as`("создалась одна динамическая группа")
            .hasSize(1)
        Assertions.assertThat(adGroupIdsToBannerIds.values.flatten())
            .`as`("создалось три динамических баннера")
            .hasSize(3)
    }

    @Test
    fun createNewAdsAndUpdateExist_smartBannersCreatedOnlyOnce() {
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.UNIVERSAL_CAMPAIGNS_AUTORETARGETING_ENABLED, true)
        createEcomUcCampaignInfo(false)

        uacCampaign = createYdbCampaign(
                advType = AdvType.TEXT,
                href = "https://market.yandex.ru/",
                counterIds = listOf(COUNTER_ID),
                keywords = KEYWORDS,
                isEcom = true,
                feedId = feedId
        )
        uacYdbCampaignRepository.addCampaign(uacCampaign)
        val uacCampaignContents = listOf(
                createDefaultTitleContent(),
                createDefaultTextContent(),
        )
        uacYdbCampaignContentRepository.addCampaignContents(uacCampaignContents)
        val uacDirectContent = createDirectContent()
        uacYdbDirectContentRepository.addDirectContent(listOf(uacDirectContent))

        runCreateBannerFunction(uacCampaignContents)

        // Повторный запуск:
        val ydbAdGroups = uacYdbDirectAdGroupRepository.getDirectAdGroupsByCampaignId(uacCampaign.id)
        runCreateBannerFunction(uacCampaignContents, ydbAdGroups)

        val subCampaignsIds = campaignService.getSubCampaignIdsWithMasterIds(setOf(campaign.id), clientInfo.clientId!!).keys
        val subCampaignsByType = campaignRepository.getTypedCampaigns(clientInfo.shard, subCampaignsIds)
                .map { it as CommonCampaign }
                .associateBy { it.type }

        val smartSubCampaign = subCampaignsByType[CampaignType.PERFORMANCE]!!
        val adGroupIdsToBannerIds = bannerRelationsRepository.getAdGroupIdsToNonArchivedBannerIds(
                clientInfo.shard, smartSubCampaign.id, listOf(PerformanceBanner::class.java)
        )
        Assertions.assertThat(adGroupIdsToBannerIds.keys)
                .`as`("создалась одна смарт группа")
                .hasSize(1)
        Assertions.assertThat(adGroupIdsToBannerIds.values.flatten())
                .`as`("создались смарт баннеры")
                .isNotEmpty // Точное количество этих баннеров зависит от количества креативов, созданных по дефолтам
    }

    @Test
    fun createNewAdsAndUpdateExist_creativeFreeSmartBannersCreatedOnlyOnce() {
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.UNIVERSAL_CAMPAIGNS_AUTORETARGETING_ENABLED, true)
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.SMART_NO_CREATIVES, true)
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.CREATIVE_FREE_ECOM_UC, true)
        createEcomUcCampaignInfo(false)

        uacCampaign = createYdbCampaign(
                advType = AdvType.TEXT,
                href = "https://market.yandex.ru/",
                counterIds = listOf(COUNTER_ID),
                keywords = KEYWORDS,
                isEcom = true,
                feedId = feedId
        )
        uacYdbCampaignRepository.addCampaign(uacCampaign)
        val uacCampaignContents = listOf(
                createDefaultTitleContent(),
                createDefaultTextContent(),
        )
        uacYdbCampaignContentRepository.addCampaignContents(uacCampaignContents)
        val uacDirectContent = createDirectContent()
        uacYdbDirectContentRepository.addDirectContent(listOf(uacDirectContent))

        runCreateBannerFunction(uacCampaignContents)

        // Повторный запуск:
        val ydbAdGroups = uacYdbDirectAdGroupRepository.getDirectAdGroupsByCampaignId(uacCampaign.id)
        runCreateBannerFunction(uacCampaignContents, ydbAdGroups)

        val subCampaignsIds = campaignService.getSubCampaignIdsWithMasterIds(setOf(campaign.id), clientInfo.clientId!!).keys
        val subCampaignsByType = campaignRepository.getTypedCampaigns(clientInfo.shard, subCampaignsIds)
                .map { it as CommonCampaign }
                .associateBy { it.type }

        val smartSubCampaign = subCampaignsByType[CampaignType.PERFORMANCE]!!
        val adGroupIdsToBannerIds = bannerRelationsRepository.getAdGroupIdsToNonArchivedBannerIds(
                clientInfo.shard, smartSubCampaign.id, listOf(PerformanceBannerMain::class.java)
        )
        Assertions.assertThat(adGroupIdsToBannerIds.keys)
                .`as`("создалась одна смарт-группа")
                .hasSize(1)
        Assertions.assertThat(adGroupIdsToBannerIds.values.flatten())
                .`as`("создался родительский смарт-баннер")
                .hasSize(1)
    }

    @Test
    fun createNewAdsAndUpdateExist_bannersCreatedCorrectlyOnNewBackend() {
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.ECOM_UC_NEW_BACKEND_ENABLED, true)
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.SMART_NO_CREATIVES, true)
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.CREATIVE_FREE_ECOM_UC, true)

        createEcomUcCampaignInfo(true)

        val uacCampaignContents = listOf(
            createDefaultTitleContent(),
            createDefaultTextContent(),
        )
        uacYdbCampaignContentRepository.addCampaignContents(uacCampaignContents)
        val uacDirectContent = createDirectContent()
        uacYdbDirectContentRepository.addDirectContent(listOf(uacDirectContent))

        runCreateBannerFunction(uacCampaignContents)

        // Повторный запуск:
        val ydbAdGroups = uacYdbDirectAdGroupRepository.getDirectAdGroupsByCampaignId(uacCampaign.id)
        runCreateBannerFunction(uacCampaignContents, ydbAdGroups)

        val adGroupIds = adGroupService.getAdGroupIdsByCampaignIds(setOf(campaign.id))[campaign.id]!!
        val offerRetargetings = offerRetargetingRepository.getOfferRetargetingsByAdGroupIds(
            clientInfo.shard, clientInfo.clientId!!, adGroupIds
        )

        Assertions.assertThat(offerRetargetings)
            .`as`("В группе один офферный ретаргетинг")
            .hasSize(1)
    }

    @Test
    fun createNewAdsAndUpdateExist_trackingParams() {
        createEcomUcCampaignInfo(false)

        uacCampaign = createYdbCampaign(
                advType = AdvType.TEXT,
                href = "https://market.yandex.ru/",
                counterIds = listOf(COUNTER_ID),
                keywords = KEYWORDS,
                isEcom = true,
                feedId = feedId,
                trackingParams = "a=b&c=d"
        )
        uacYdbCampaignRepository.addCampaign(uacCampaign)
        val uacCampaignContents = listOf(
                createDefaultTitleContent(),
                createDefaultTextContent(),
        )
        uacYdbCampaignContentRepository.addCampaignContents(uacCampaignContents)
        val uacDirectContent = createDirectContent()
        uacYdbDirectContentRepository.addDirectContent(listOf(uacDirectContent))

        runCreateBannerFunction(uacCampaignContents)

        val textBanners = bannerRepository
                .getBannersByCampaignIdsAndClass(clientInfo.shard, setOf(campaign.id), TextBanner::class.java)
        Assertions.assertThat(textBanners)
                .describedAs("создались ТГО баннеры")
                .isNotEmpty
        Assertions.assertThat(textBanners)
                .describedAs("к ссылкам ТГО баннеров приклеены трекинговые параметры")
                .allSatisfy(Consumer {
                    Assertions.assertThat(it.href)
                            .describedAs("к ссылке приклеены трекинговые параметры")
                            .isEqualTo("https://market.yandex.ru/?a=b&c=d")
                })

        val subCampaignIds = campaignService.getSubCampaignIdsWithMasterIds(setOf(campaign.id), clientInfo.clientId!!).keys
        val subAdGroupByType = adGroupService.getAdGroupsBySelectionCriteria(AdGroupsSelectionCriteria()
                .withCampaignIds(subCampaignIds), LimitOffset.maxLimited(), true).associateBy { it.type }

        val dynamicSubAdGroup = subAdGroupByType[AdGroupType.DYNAMIC]
        Assertions.assertThat(dynamicSubAdGroup)
                .describedAs("создалась динамическая группа")
                .isNotNull
        Assertions.assertThat(dynamicSubAdGroup?.trackingParams)
                .describedAs("у динамической группы проставлены трекинговые параметры")
                .isEqualTo("a=b&c=d")

        val smartSubAdGroup = subAdGroupByType[AdGroupType.PERFORMANCE]
        Assertions.assertThat(smartSubAdGroup)
                .describedAs("создалась смарт группа")
                .isNotNull
        Assertions.assertThat(smartSubAdGroup?.trackingParams)
                .describedAs("у смарт группы проставлены трекинговые параметры")
                .isEqualTo("a=b&c=d")
    }

    private fun createEcomUcCampaignInfo(useNewBackend: Boolean) {
        val uacCampaignInfo = uacCampaignSteps.createEcomUcCampaign(clientInfo, useNewBackend=useNewBackend)
        val campaignInfo = uacCampaignInfo.campaign

        val campaigns = campaignRepository.getTypedCampaigns(campaignInfo.shard, setOf(campaignInfo.campaignId))
        campaign = campaigns[0] as TextCampaign
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
            text: String = "text",
            uacCampaignId: String = uacCampaign.id,
    ) = createCampaignContent(
            campaignId = uacCampaignId,
            type = MediaType.TEXT,
            text = text,
    )

    private fun runCreateBannerFunction(
        uacCampaignContents: List<UacYdbCampaignContent>,
        grutDirectAdGroups: Collection<UacYdbDirectAdGroup> = listOf(),
    ) {
        val containers = createUpdateAdsContainers(
            userInfo.uid,
            clientInfo.client!!,
            uacCampaign,
            uacAdGroupBrief = null,
            campaign,
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
