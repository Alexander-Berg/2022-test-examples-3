package ru.yandex.direct.web.entity.uac.controller

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.banner.repository.BannerModerationRepository
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMeaningfulGoalsWithRequiredFields
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds
import ru.yandex.direct.core.entity.client.service.ClientService
import ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes
import ru.yandex.direct.core.entity.feedfilter.model.FeedFilter
import ru.yandex.direct.core.entity.feedfilter.model.FeedFilterTab
import ru.yandex.direct.core.entity.uac.createDefaultImageContent
import ru.yandex.direct.core.entity.uac.createDefaultVideoContent
import ru.yandex.direct.core.entity.uac.model.AgePoint
import ru.yandex.direct.core.entity.uac.model.DeviceType
import ru.yandex.direct.core.entity.uac.model.Gender
import ru.yandex.direct.core.entity.uac.model.LimitPeriodType
import ru.yandex.direct.core.entity.uac.model.Socdem
import ru.yandex.direct.core.entity.uac.model.UacGoal
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatch
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbContent
import ru.yandex.direct.core.testing.data.TestDomain
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.repository.TestOrganizationRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.UacCampaignSteps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.dbqueue.steps.DbQueueSteps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.feature.FeatureName.CHANGE_BANNER_ORGANIZATION_ON_DEFAULT_CAMPAIGN_ORGANIZATION_CHANGE
import ru.yandex.direct.organizations.swagger.OrganizationsClient
import ru.yandex.direct.regions.Region
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.test.utils.checkNotEmpty
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.validation.defect.ids.CollectionDefectIds
import ru.yandex.direct.validation.defect.ids.NumberDefectIds
import ru.yandex.direct.validation.result.DefectId
import ru.yandex.direct.validation.result.DefectIds
import ru.yandex.direct.validation.result.Path
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.uac.model.CreateCampaignRequest
import ru.yandex.direct.web.entity.uac.model.PatchCampaignRequest
import ru.yandex.direct.web.entity.uac.service.emptyPatchRequest
import ru.yandex.direct.web.validation.model.WebValidationResult
import java.math.BigDecimal
import java.util.Locale
import java.util.function.Consumer

abstract class UacCampaignControllerUpdateUcTestBase {

    @Autowired
    protected lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    protected lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    protected lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    protected lateinit var steps: Steps

    @Autowired
    protected lateinit var campaignRepository: CampaignRepository

    @Autowired
    protected lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    protected lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Autowired
    protected lateinit var organizationsClientStub: OrganizationsClient

    @Autowired
    protected lateinit var dbQueueSteps: DbQueueSteps

    @Autowired
    protected lateinit var testOrganizationRepository: TestOrganizationRepository

    @Autowired
    protected lateinit var bannerTypedRepository: BannerTypedRepository

    @Autowired
    protected lateinit var bannerModerationRepository: BannerModerationRepository

    @Autowired
    private lateinit var uacCampaignSteps: UacCampaignSteps

    @Autowired
    private lateinit var clientService: ClientService

    @Autowired
    protected lateinit var metrikaClient: MetrikaClientStub

    protected lateinit var mockMvc: MockMvc
    protected lateinit var clientInfo: ClientInfo
    protected lateinit var imageContentFirst: UacYdbContent
    protected lateinit var imageContentSecond: UacYdbContent
    protected lateinit var videoContentFirst: UacYdbContent
    protected lateinit var videoContentSecond: UacYdbContent
    protected var feedId: Long = 0
    protected lateinit var uacCampaignId: String
    protected lateinit var uacEcomCampaignId: String
    private lateinit var uacEcomCampaignInfo: UacCampaignSteps.UacCampaignInfo
    protected lateinit var uacEcomNewBackendCampaignId: String

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        clientInfo = testAuthHelper.createDefaultUser().clientInfo!!
        testAuthHelper.setOperatorAndSubjectUser(clientInfo.uid)
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )

        // нужно для проверки регионов, так как из ручки возвращается регион в локали en
        LocaleContextHolder.setLocale(Locale.ENGLISH)

        dbQueueSteps.registerJobType(DbQueueJobTypes.UAC_UPDATE_ADS)

        feedId = steps.feedSteps().createDefaultFileFeed(clientInfo).feedId

        uacEcomCampaignInfo = uacCampaignSteps.createEcomUcCampaign(clientInfo)
        uacEcomCampaignId = uacEcomCampaignInfo.uacCampaign.id

        val newBackendEcomCampaign = uacCampaignSteps.createCampaign(clientInfo = clientInfo, isEcom = true).first
        uacEcomNewBackendCampaignId = newBackendEcomCampaign.uacCampaign.id

        // Нужно для честной проверки апдейта, иначе валидация работает как при добавлении кампании
        val filter = FeedFilter()
            .withTab(FeedFilterTab.ALL_PRODUCTS)
            .withConditions(listOf())
        val adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(newBackendEcomCampaign.campaign, feedId, filter)
        steps.bannerSteps().createActiveTextBanner(adGroupInfo)
        steps.bannerSteps().createActiveDynamicBanner(adGroupInfo)
        steps.performanceMainBannerSteps().createPerformanceMainBanner(adGroupInfo)

        steps.featureSteps().addClientFeature(
            clientInfo.clientId, CHANGE_BANNER_ORGANIZATION_ON_DEFAULT_CAMPAIGN_ORGANIZATION_CHANGE, true)
        steps.featureSteps().enableClientFeature(FeatureName.ALLOW_MEANINGFUL_GOAL_VALUE_FROM_METRIKA)
        steps.featureSteps().enableClientFeature(FeatureName.CRR_STRATEGY_ALLOWED)
        steps.featureSteps().enableClientFeature(FeatureName.FIX_CRR_STRATEGY_ALLOWED)
        steps.featureSteps().enableClientFeature(FeatureName.ALLOW_ALL_MEANINGFUL_GOALS_FOR_PAY_FOR_CONVERSION_STRATEGIES)
    }

    @After
    fun after() {
        LocaleContextHolder.setLocale(null)
    }

    protected abstract fun saveContents(contents: List<UacYdbContent>)

    protected fun doSuccessRequest(
        request: PatchCampaignRequest,
        expectTitleContentsCount: Int = 1,
        expectTextContentsCount: Int = 1,
        expectImageContentsCount: Int = 0,
        expectVideoContentsCount: Int = 0,
    ): Long {
        return doSuccessRequest(
            request,
            uacCampaignId,
            expectTitleContentsCount,
            expectTextContentsCount,
            expectImageContentsCount,
            expectVideoContentsCount)
    }

    abstract fun doSuccessRequest(
        request: PatchCampaignRequest,
        uacCampaignId: String,
        expectTitleContentsCount: Int = 1,
        expectTextContentsCount: Int = 1,
        expectImageContentsCount: Int = 0,
        expectVideoContentsCount: Int = 0,
    ): Long

    abstract fun getEcomCampaign(): UacYdbCampaign

    protected fun createContents() {
        val imageHashFirst = steps.bannerSteps().createWideImageFormat(clientInfo).imageHash
        val imageHashSecond = steps.bannerSteps().createWideImageFormat(clientInfo).imageHash

        var creativeCanvasIdFirst = steps.creativeSteps().nextCreativeId
        creativeCanvasIdFirst = steps.creativeSteps()
            .addDefaultVideoAdditionCreative(clientInfo, creativeCanvasIdFirst).creativeId

        var creativeCanvasIdSecond = steps.creativeSteps().nextCreativeId
        creativeCanvasIdSecond = steps.creativeSteps()
            .addDefaultVideoAdditionCreative(clientInfo, creativeCanvasIdSecond).creativeId

        videoContentFirst = createDefaultVideoContent(
            creativeId = creativeCanvasIdFirst,
            accountId = clientInfo.clientId!!.toString(),
        )
        videoContentSecond = createDefaultVideoContent(
            creativeId = creativeCanvasIdSecond,
            accountId = clientInfo.clientId!!.toString(),
        )
        imageContentFirst = createDefaultImageContent(
            imageHash = imageHashFirst,
            accountId = clientInfo.clientId!!.toString(),
        )
        imageContentSecond = createDefaultImageContent(
            imageHash = imageHashSecond,
            accountId = clientInfo.clientId!!.toString(),
        )
        saveContents(listOf(videoContentFirst, videoContentSecond, imageContentFirst, imageContentSecond))
    }

    @Test
    fun `update ecom uc campaign`() {
        val newName = "Brand new shiny name"
        val request = updateEcomCampaignRequest(name = newName)
        val directCampaignId = doSuccessRequest(request, uacEcomCampaignId)

        val hiddenCampaignIds = campaignRepository.getSubCampaignIdsWithMasterIds(clientInfo.shard,  setOf(directCampaignId))
        val hiddenCampaigns = campaignTypedRepository.getTypedCampaigns(clientInfo.shard, hiddenCampaignIds.keys)
            .filterIsInstance<CommonCampaign>()
        val uacYdbCampaign = getEcomCampaign()
        softly {
            assertThat(hiddenCampaigns).hasSize(2)
            assertThat(hiddenCampaigns[0].name).isEqualTo(newName)
            assertThat(hiddenCampaigns[1].name).isEqualTo(newName)
            assertThat(uacYdbCampaign.name).isEqualTo(newName)
        }
    }

    @Test
    fun `update ecom uc campaign on new backend`() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.ECOM_UC_NEW_BACKEND_ENABLED, true)

        val newName = "Brand new shiny name"
        val request = updateEcomCampaignRequest(name = newName)
        val directCampaignId = doSuccessRequest(request, uacEcomNewBackendCampaignId)

        val hiddenCampaignIds = campaignRepository.getSubCampaignIdsWithMasterIds(clientInfo.shard,  setOf(directCampaignId))
        val directCampaigns = campaignTypedRepository.getTypedCampaigns(clientInfo.shard, setOf(directCampaignId))
        softly {
            assertThat(hiddenCampaignIds).isEmpty()

            assertThat(directCampaigns).hasSize(1)
            assertThat((directCampaigns[0] as TextCampaign).name).isEqualTo(newName)
        }
    }

    @Test
    fun `update ecom uc campaign with too low weekly budget`() {
        val currency = clientService.getWorkCurrency(clientInfo.clientId!!)
        val request = updateEcomCampaignRequest(
                name = uacEcomCampaignInfo.uacCampaign.name,
                weekLimit = currency.minAutobudget) // Недельный бюджет должен быть x3 от минимального для валюты

        checkBadRequest(
                request,
                path = path(field(CreateCampaignRequest::weekLimit.name)),
                defectId = NumberDefectIds.MUST_BE_GREATER_THAN_OR_EQUAL_TO_MIN
        )
    }

    @Test
    fun `update ecom uc campaign without counters`() {
        val request = updateEcomCampaignRequest(name = "New name", counters = listOf())
        checkBadRequest(
            request,
            path = path(field(CreateCampaignRequest::counters.name)),
            defectId = CollectionDefectIds.Gen.CANNOT_BE_EMPTY
        )
    }

    @Test
    fun `update ecom uc campaign with unavailable counter`() {
        val unavailableCounterId = 123454321
        val request = updateEcomCampaignRequest(name = "New name", counters = listOf(unavailableCounterId))
        checkBadRequest(
            request,
            path = path(field(CreateCampaignRequest::counters.name)),
            defectId = CampaignDefectIds.Gen.METRIKA_COUNTER_IS_UNAVAILABLE
        )
    }

    @Test
    fun `update ecom uc campaign without goals`() {
        val request = updateEcomCampaignRequest(name = "New name", goals = listOf())
        checkBadRequest(
            request,
            path = path(field(CreateCampaignRequest::goals.name)),
            defectId = CollectionDefectIds.Size.SIZE_CANNOT_BE_LESS_THAN_MIN
        )
    }

    @Test
    fun `update ecom uc campaign with many goals`() {
        val goal1 = UacGoal(RandomNumberUtils.nextPositiveInteger().toLong(), cpa = BigDecimal.valueOf(200))
        val goal2 = UacGoal(RandomNumberUtils.nextPositiveInteger().toLong())
        val goals = listOf(goal1, goal2)

        val counterId = RandomNumberUtils.nextPositiveInteger()
        val counters = listOf(counterId)

        metrikaClient.addUserCounter(clientInfo.uid, counterId)
        metrikaClient.addCounterGoal(counterId, goal1.goalId.toInt())
        metrikaClient.addCounterGoal(counterId, goal2.goalId.toInt())

        val request = updateEcomCampaignRequest(name = "New name", counters = counters, goals = goals, crr = 20)

        val campaignId = doSuccessRequest(request, uacEcomCampaignId)
        val subCampaignIds = campaignRepository.getSubCampaignIdsWithMasterIds(clientInfo.shard, setOf(campaignId)).keys
        val campaigns = campaignTypedRepository.getTypedCampaigns(clientInfo.shard, subCampaignIds + campaignId)
            .filterIsInstance<CampaignWithMeaningfulGoalsWithRequiredFields>()
        assertThat(campaigns).allSatisfy(Consumer { campaign ->
            softly {
                assertThat(campaign.strategy.strategyData.goalId).isEqualTo(MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID)
                assertThat(campaign.strategy.strategyData.crr).isEqualTo(20)
                assertThat(campaign.meaningfulGoals).satisfiesExactlyInAnyOrder(Consumer {
                    softly {
                        assertThat(it.goalId).isEqualTo(goal1.goalId)
                        assertThat(it.conversionValue).isEqualTo(BigDecimal.valueOf(1000))
                        assertThat(it.isMetrikaSourceOfValue).isIn(false, null)
                    }
                }, {
                    softly {
                        assertThat(it.goalId).isEqualTo(goal2.goalId)
                        assertThat(it.isMetrikaSourceOfValue).isTrue()
                    }
                })
            }
        })
    }

    @Test
    fun `update ecom uc campaign with goal from unavailable counter`() {
        val goal = UacGoal(RandomNumberUtils.nextPositiveInteger().toLong())
        val goals = listOf(goal)
        val counterId = RandomNumberUtils.nextPositiveInteger()
        val counters = listOf(counterId)

        metrikaClient.addCounterGoal(counterId, goal.goalId.toInt())

        val request = updateEcomCampaignRequest(name = "New name", counters = counters, goals = goals)
        checkBadRequest(
            request,
            path = path(field(CreateCampaignRequest::counters.name)),
            defectId = CampaignDefectIds.Gen.METRIKA_COUNTER_IS_UNAVAILABLE
        )
    }

    @Test
    fun `update ecom uc campaign with invalid tracking params`() {
        val request = updateEcomCampaignRequest(name = "New name", trackingParams = "a=b space c=d")
        checkBadRequest(
            request,
            path = path(field(CreateCampaignRequest::trackingParams.name)),
            defectId = DefectIds.INVALID_VALUE
        )
    }

    protected fun checkBadRequest(
        request: PatchCampaignRequest,
        path: Path,
        defectId: DefectId<*>,
    ) {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/$uacEcomCampaignId?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString

        val validationResult = JsonUtils
            .fromJson(JsonUtils.fromJson(result)["validation_result"].toString(), WebValidationResult::class.java)
        validationResult.errors.checkNotEmpty()

        val codeToPath = validationResult.errors
            .associate { it.code to it.path }
        Assertions.assertThat(codeToPath).containsEntry(defectId.code, path.toString())
    }

    protected fun updateCampaignRequest(
        contentIds: List<String>? = null,
        texts: List<String>? = listOf("text"),
        titles: List<String>? = listOf("title"),
        permalinkId: Long? = null,
        goals: List<UacGoal>? = null,
        cpa: BigDecimal? = null,
        crr: Long? = null,
        minusRegions: List<Long>? = null,
        minusKeywords: List<String>? = null,
        relevanceMatch: UacRelevanceMatch? = null,
        showTitleAndBody: Boolean? = null,
    ): PatchCampaignRequest {
        val bannerHref = "https://" + TestDomain.randomDomain()
        val weekLimit = BigDecimal.valueOf(2300000000, 6)
        val regions = listOf(Region.RUSSIA_REGION_ID, Region.BY_REGION_ID)
        val socdem = Socdem(listOf(Gender.FEMALE), AgePoint.AGE_45, AgePoint.AGE_INF, Socdem.IncomeGrade.LOW, Socdem.IncomeGrade.PREMIUM)
        val actualGoals = goals ?: listOf(UacGoal(RandomNumberUtils.nextPositiveInteger().toLong(), null))
        val counters = listOf(RandomNumberUtils.nextPositiveInteger())

        return emptyPatchRequest().copy(
            titles = titles,
            texts = texts,
            displayName = "Text campaign",
            weekLimit = weekLimit,
            href = bannerHref,
            regions = regions,
            minusRegions = minusRegions,
            socdem = socdem,
            goals = actualGoals,
            counters = counters,
            limitPeriod = LimitPeriodType.MONTH,
            keywords = listOf("keyword1", "keyword2"),
            deviceTypes = setOf(DeviceType.ALL),
            permalinkId = permalinkId,
            cpa = if (goals != null) cpa else BigDecimal.valueOf(100000000L, 6),
            crr = crr,
            contentIds = contentIds,
            minusKeywords = minusKeywords,
            relevanceMatch = relevanceMatch,
            showTitleAndBody = showTitleAndBody,
        )
    }

    private fun updateEcomCampaignRequest(
        name: String,
        goals: List<UacGoal>? = null,
        cpa: BigDecimal? = null,
        crr: Long? = null,
        counters: List<Int>? = null,
        trackingParams: String? = null,
        weekLimit: BigDecimal = BigDecimal.valueOf(2300000000, 6),
        relevanceMatch: UacRelevanceMatch? = null,
    ): PatchCampaignRequest {
        val bannerHref = "https://" + TestDomain.randomDomain()
        val regions = listOf(Region.RUSSIA_REGION_ID, Region.BY_REGION_ID)
        val socdem = Socdem(listOf(Gender.FEMALE), AgePoint.AGE_45, AgePoint.AGE_INF, Socdem.IncomeGrade.LOW, Socdem.IncomeGrade.PREMIUM)
        val goal = UacGoal(RandomNumberUtils.nextPositiveInteger().toLong(), null)
        val actualGoals = goals ?: listOf(goal)
        val counterId = RandomNumberUtils.nextPositiveInteger()
        val actualCounters = counters ?: listOf(counterId)

        metrikaClient.addUserCounter(clientInfo.uid, counterId)
        metrikaClient.addCounterGoal(counterId, goal.goalId.toInt())

        return emptyPatchRequest().copy(
            displayName = name,
            titles = listOf("titles"),
            texts = listOf("text"),
            weekLimit = weekLimit,
            href = bannerHref,
            regions = regions,
            socdem = socdem,
            goals = actualGoals,
            counters = actualCounters,
            limitPeriod = LimitPeriodType.MONTH,
            keywords = listOf("keyword1", "keyword2"),
            deviceTypes = setOf(DeviceType.ALL),
            cpa = if (goals != null) cpa else BigDecimal.valueOf(100000000L, 6),
            crr = crr,
            contentIds = null,
            isEcom = true,
            feedId = feedId,
            trackingParams = trackingParams,
            relevanceMatch = relevanceMatch,
        )
    }
}
