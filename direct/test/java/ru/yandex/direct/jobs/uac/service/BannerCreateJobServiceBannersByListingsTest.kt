package ru.yandex.direct.jobs.uac.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyVararg
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
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
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.core.entity.banner.repository.BannerRelationsRepository
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.client.model.Client
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.GoalType
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.createDirectContent
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.getCreativeGroup
import ru.yandex.direct.core.entity.uac.model.AdvType
import ru.yandex.direct.core.entity.uac.model.CampaignStatuses
import ru.yandex.direct.core.entity.uac.model.DirectCampaignStatus
import ru.yandex.direct.core.entity.uac.model.EcomOfferCatalog
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.core.entity.uac.model.TargetStatus
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatch
import ru.yandex.direct.core.entity.uac.repository.mysql.EcomOfferCatalogsRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectCampaign
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestFullGoals
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.repository.TestLalSegmentRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.UacCampaignSteps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.gemini.GeminiClient
import ru.yandex.direct.jobs.configuration.JobsTest
import ru.yandex.direct.jobs.uac.model.createUpdateAdsContainers
import ru.yandex.direct.regions.Region
import ru.yandex.direct.test.utils.randomPositiveInt

private const val COUNTER_ID = 123
private const val SITE_VISIT_GOAL_ID = Goal.METRIKA_COUNTER_LOWER_BOUND + COUNTER_ID
private val KEYWORDS = listOf("промокод озон", "озон промокод на скидку")
private const val HOST = "https://www.company.ru"
private const val CATALOG_1 = "catalog1"
private const val CATALOG_2 = "catalog2"

@JobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled("Ходит в BannerStorage, поэтому в sandbox'е не пройдёт. Для ручного запуска нужен настоящий клиент BannerStorage")
class BannerCreateJobServiceBannersByListingsTest {

    @Autowired
    private lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    private lateinit var uacYdbDirectContentRepository: UacYdbDirectContentRepository

    @Autowired
    private lateinit var campaignRepository: CampaignTypedRepository

    @Autowired
    private lateinit var bannerCreateJobService: YdbBannerCreateJobService

    @Autowired
    private lateinit var uacYdbCampaignRepository: UacYdbCampaignRepository

    @Autowired
    private lateinit var uacYdbDirectCampaignRepository: UacYdbDirectCampaignRepository

    @Autowired
    private lateinit var ecomOfferCatalogsRepository: EcomOfferCatalogsRepository

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

    @Autowired
    private lateinit var geminiClient: GeminiClient

    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId
    private lateinit var client: Client
    private var uid: Long = 0
    private lateinit var userInfo: UserInfo
    private lateinit var campaignInfo: CampaignInfo
    private lateinit var uacCampaign: UacYdbCampaign
    private lateinit var campaign: TextCampaign
    private var feedId: Long = 0

    private val campaignStatuses = CampaignStatuses(Status.STARTED, TargetStatus.STARTED)

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
        metrikaClient.addGoals(uid, setOf(TestFullGoals.defaultGoal(SITE_VISIT_GOAL_ID)))
        metrikaHelper.addGoalIds(uid, setOf(SITE_VISIT_GOAL_ID))
        val lalSegment = TestFullGoals.defaultGoalByType(GoalType.LAL_SEGMENT).apply {
            parentId = SITE_VISIT_GOAL_ID
        }
        testLalSegmentRepository.addAll(listOf(lalSegment))

        feedId = steps.feedSteps().createDefaultFeed(clientInfo).feedId

        val uacCampaignInfo = uacCampaignSteps.createEcomUcCampaign(clientInfo)
        campaignInfo = uacCampaignInfo.campaign

        val campaigns = campaignRepository.getTypedCampaigns(campaignInfo.shard, setOf(campaignInfo.campaignId))
        campaign = campaigns[0] as TextCampaign

        doReturn(Template(0, "", emptyList(), emptyList()))
            .`when`(dummyBannerStorageClient).getTemplate(ArgumentMatchers.anyInt(), anyVararg())

        val creativeIds = List(5) { randomPositiveInt() }
        doReturn(getCreativeGroup(creativeIds))
            .`when`(dummyBannerStorageClient).createSmartCreativeGroup(ArgumentMatchers.any())

        doReturn(mapOf(HOST to HOST))
            .`when`(geminiClient).getMainMirrors(eq(listOf(HOST)))

        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.ENABLED_BANNERS_BY_LISTINGS, true)
    }

    @Test
    fun createNewAdsAndUpdateExist_createBannersByListings() {
        doReturn(
            listOf(
                createEcomOfferCatalog(1, CATALOG_1, 10),
                createEcomOfferCatalog(2, CATALOG_2, 50)
            )
        ).`when`(ecomOfferCatalogsRepository).getByHosts(any())

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

        val containers = createUpdateAdsContainers(
            userInfo.uid,
            clientInfo.client!!,
            uacCampaign = uacCampaign,
            uacAdGroupBrief = null,
            campaign = TestCampaigns.defaultTextCampaign(),
        )
        bannerCreateJobService.createNewAdsAndUpdateExist(
            userInfo.clientInfo!!.client!!,
            containers,
            uacCampaign,
            uacDirectAdGroups = listOf(),
            uacAssetsByGroupBriefId = mapOf(null as Long? to uacCampaignContents),
            isItCampaignBrief = true,
        )

        val adGroupIdsToBannerIds = bannerRelationsRepository.getAdGroupIdsToNonArchivedBannerIds(
            clientInfo.shard, campaign.id, listOf(TextBanner::class.java)
        )

        Assertions.assertThat(adGroupIdsToBannerIds.keys)
            .`as`("создались 2 группы")
            .hasSize(2)
        Assertions.assertThat(adGroupIdsToBannerIds.values.flatten())
            .`as`("создались 3 баннера")
            .hasSize(3)
    }

    private fun createYdbUcCampaign(
        relevanceMatch: UacRelevanceMatch? = null,
        regions: List<Long>? = listOf(Region.RUSSIA_REGION_ID),
        hyperGeoId: Long? = null,
    ) = createYdbCampaign(
        advType = AdvType.TEXT,
        href = HOST,
        counterIds = listOf(COUNTER_ID),
        keywords = KEYWORDS,
        relevanceMatch = relevanceMatch,
        regions = regions,
        hyperGeoId = hyperGeoId,
        isEcom = true,
        feedId = feedId
    )

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

    private fun createYdbUcDirectCampaign(commonCampaign: CommonCampaign = campaign) {
        uacYdbDirectCampaignRepository.saveDirectCampaign(
            UacYdbDirectCampaign(
                id = uacCampaign.id,
                directCampaignId = commonCampaign.id,
                status = DirectCampaignStatus.CREATED,
                syncedAt = commonCampaign.lastChange,
                rejectReasons = null,
            )
        )
    }

    private fun createEcomOfferCatalog(id: Long, catalog: String, visitsCount: Long) = EcomOfferCatalog()
        .withId(id)
        .withHost(HOST)
        .withIsPermanent(false)
        .withCatalogPath(catalog)
        .withUrl("$HOST/$catalog")
        .withVisitsCount(visitsCount)
}
