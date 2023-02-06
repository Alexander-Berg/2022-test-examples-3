package ru.yandex.direct.web.entity.uac.controller

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import java.util.function.Consumer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
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
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefectIds
import ru.yandex.direct.core.entity.campaign.model.CampaignMetatype
import ru.yandex.direct.core.entity.campaign.model.CampaignWithCustomStrategy
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMeaningfulGoalsWithRequiredFields
import ru.yandex.direct.core.entity.campaign.model.DbStrategy
import ru.yandex.direct.core.entity.campaign.model.StrategyData
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds
import ru.yandex.direct.core.entity.client.service.ClientService
import ru.yandex.direct.core.entity.metrikacounter.model.MetrikaCounterPermission
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterDefects
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.uac.createDefaultImageContent
import ru.yandex.direct.core.entity.uac.createDefaultVideoContent
import ru.yandex.direct.core.entity.uac.model.UacFeedFilter
import ru.yandex.direct.core.entity.uac.model.UacFeedFilterCondition
import ru.yandex.direct.core.entity.uac.model.UacFeedFilterOperator
import ru.yandex.direct.core.entity.uac.model.UacGoal
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatch
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbContent
import ru.yandex.direct.core.entity.uac.service.BaseUacCampaignService
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.metrika.client.model.response.CounterInfoDirect
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.test.utils.checkNotEmpty
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.validation.defect.ids.CollectionDefectIds
import ru.yandex.direct.validation.defect.ids.NumberDefectIds
import ru.yandex.direct.validation.result.DefectId
import ru.yandex.direct.validation.result.Path
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.uac.model.CreateCampaignRequest
import ru.yandex.direct.web.entity.uac.service.BaseUacCampaignWebService
import ru.yandex.direct.web.validation.model.WebValidationResult

abstract class UacCampaignControllerCreateUcTestBase {

    companion object {
        val DEFAULT_WEEK_LIMIT: BigDecimal = BigDecimal.valueOf(2300000000, 2).setScale(2)
    }

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    protected lateinit var steps: Steps

    @Autowired
    protected lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var campaignRepository: CampaignRepository

    protected abstract val uacCampaignWebService: BaseUacCampaignWebService

    protected abstract val uacCampaignService: BaseUacCampaignService

    @Autowired
    private lateinit var metrikaClient: MetrikaClientStub

    @Autowired
    private lateinit var clientService: ClientService

    protected lateinit var mockMvc: MockMvc
    protected lateinit var userInfo: UserInfo
    protected lateinit var imageContentFirst: UacYdbContent
    protected lateinit var imageContentSecond: UacYdbContent
    protected lateinit var videoContentFirst: UacYdbContent
    protected lateinit var videoContentSecond: UacYdbContent
    protected var feedId: Long = 0

    protected abstract fun saveContents(contents: List<UacYdbContent>)

    protected abstract fun checkSuccessRequest(
        request: CreateCampaignRequest,
        expectTitleContentsCount: Int = 1,
        expectTextContentsCount: Int = 1,
        expectImageContentsCount: Int = 0,
        expectVideoContentsCount: Int = 0,
        expectFeedFilters: List<UacFeedFilter>? = null,
    ): Long

    protected fun checkAssets(
        campaignContents: List<UacYdbCampaignContent>,
        softAssertions: SoftAssertions,
        expectTitleContentsCount: Int,
        expectTextContentsCount: Int,
        expectImageContentsCount: Int,
        expectVideoContentsCount: Int,
    ) {
        val titleContents = campaignContents.filter { it.type == ru.yandex.direct.core.entity.uac.model.MediaType.TITLE }
        val textContents = campaignContents.filter { it.type == ru.yandex.direct.core.entity.uac.model.MediaType.TEXT }
        val imageContents = campaignContents.filter { it.type == ru.yandex.direct.core.entity.uac.model.MediaType.IMAGE }
        val videoContents = campaignContents.filter { it.type == ru.yandex.direct.core.entity.uac.model.MediaType.VIDEO }

        softAssertions.assertThat(titleContents.size)
            .`as`("Количество title ассетов")
            .isEqualTo(expectTitleContentsCount)
        softAssertions.assertThat(textContents.size)
            .`as`("Количество text ассетов")
            .isEqualTo(expectTextContentsCount)
        softAssertions.assertThat(imageContents.size)
            .`as`("Количество image ассетов")
            .isEqualTo(expectImageContentsCount)
        softAssertions.assertThat(videoContents.size)
            .`as`("Количество video ассетов")
            .isEqualTo(expectVideoContentsCount)
    }

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )

        feedId = steps.feedSteps().createDefaultFileFeed(userInfo.clientInfo).feedId

        createContents()

        // нужно для проверки регионов, так как из ручки возвращается регион в локали en
        LocaleContextHolder.setLocale(Locale.ENGLISH)

        steps.featureSteps().enableClientFeature(FeatureName.ALLOW_MEANINGFUL_GOAL_VALUE_FROM_METRIKA)
        steps.featureSteps().enableClientFeature(FeatureName.CRR_STRATEGY_ALLOWED)
        steps.featureSteps().enableClientFeature(FeatureName.FIX_CRR_STRATEGY_ALLOWED)
        steps.featureSteps().enableClientFeature(FeatureName.ALLOW_ALL_MEANINGFUL_GOALS_FOR_PAY_FOR_CONVERSION_STRATEGIES)
    }

    private fun createContents() {
        val imageHashFirst = steps.bannerSteps().createWideImageFormat(userInfo.clientInfo).imageHash
        val imageHashSecond = steps.bannerSteps().createWideImageFormat(userInfo.clientInfo).imageHash

        var creativeCanvasIdFirst = steps.creativeSteps().nextCreativeId
        creativeCanvasIdFirst = steps.creativeSteps()
            .addDefaultVideoAdditionCreative(userInfo.clientInfo, creativeCanvasIdFirst).creativeId

        var creativeCanvasIdSecond = steps.creativeSteps().nextCreativeId
        creativeCanvasIdSecond = steps.creativeSteps()
            .addDefaultVideoAdditionCreative(userInfo.clientInfo, creativeCanvasIdSecond).creativeId

        videoContentFirst = createDefaultVideoContent(
            accountId = userInfo.clientId.toString(),
            creativeId = creativeCanvasIdFirst,
        )
        videoContentSecond = createDefaultVideoContent(
            accountId = userInfo.clientId.toString(),
            creativeId = creativeCanvasIdSecond,
        )
        imageContentFirst = createDefaultImageContent(
            accountId = userInfo.clientId.toString(),
            imageHash = imageHashFirst,
        )
        imageContentSecond = createDefaultImageContent(
            accountId = userInfo.clientId.toString(),
            imageHash = imageHashSecond,
        )
        saveContents(listOf(videoContentFirst, videoContentSecond, imageContentFirst, imageContentSecond))
    }

    @After
    fun after() {
        LocaleContextHolder.setLocale(null)
    }

    /**
     * Проверяем создание подкампании в Ecom сценарии
     */
    @Test
    fun `create ecom uc campaign`() {
        val request = createCampaignRequest(isEcom = true, feedId = feedId)
        val directCampaignId = checkSuccessRequest(request)

        val subCampaignIds = campaignRepository.getSubCampaignIdsWithMasterIds(userInfo.shard,  setOf(directCampaignId))
        val subCampaigns = campaignTypedRepository.getTypedCampaigns(userInfo.shard, subCampaignIds.keys)
        val expectedWeekLimit = DEFAULT_WEEK_LIMIT.divide(BigDecimal(3), DEFAULT_WEEK_LIMIT.scale(), RoundingMode.DOWN)
        SoftAssertions.assertSoftly {
            it.assertThat(subCampaigns).hasSize(2)

            val weekLimits = subCampaigns
                .asSequence()
                .filterIsInstance<CampaignWithCustomStrategy>()
                .map(CampaignWithCustomStrategy::getStrategy)
                .map(DbStrategy::getStrategyData)
                .map(StrategyData::getSum)
                .toSet()
            it.assertThat(weekLimits).hasSize(1)
            it.assertThat(weekLimits.iterator().next()).isEqualTo(expectedWeekLimit)
        }
    }

    /**
     * Проверяем создание Ecom кампании без подкампаний (на новом бэкенде)
     */
    @Test
    fun `create ecom uc campaign on new backend`() {
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.ECOM_UC_NEW_BACKEND_ENABLED, true)

        val request = createCampaignRequest(isEcom = true, feedId = feedId, href = "https://market.yandex.ru/")
        val directCampaignId = checkSuccessRequest(request)
        val directCampaigns = campaignTypedRepository.getTypedCampaigns(userInfo.shard, setOf(directCampaignId))

        val subCampaignIds = campaignRepository.getSubCampaignIdsWithMasterIds(userInfo.shard,  setOf(directCampaignId))

        SoftAssertions.assertSoftly {
            it.assertThat(directCampaigns).hasSize(1)
            it.assertThat(directCampaigns[0]).isInstanceOf(TextCampaign::class.java)

            val camp = directCampaigns[0] as TextCampaign
            it.assertThat(camp.metatype).isEqualTo(CampaignMetatype.ECOM)
            it.assertThat(camp.strategy.strategyData.sum).isEqualByComparingTo(DEFAULT_WEEK_LIMIT)

            it.assertThat(subCampaignIds).isEmpty()
        }
    }

    /**
     * Проверяем создание Ecom кампании с ECOMMERCE целью
     */
    @Test
    fun `create ecom uc campaign with ECOMMERCE goal`() {
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.COLD_START_FOR_ECOMMERCE_GOALS, true)

        val counterId = RandomNumberUtils.nextPositiveInteger(900_000_000)
        val counters = listOf(counterId)
        metrikaClient.addUserCounters(userInfo.uid, listOf(
            CounterInfoDirect()
                .withId(counterId)
                .withEcommerce(true)
                .withCounterPermission(MetrikaCounterPermission.OWN.name.lowercase())))

        val goal = UacGoal(Goal.METRIKA_ECOMMERCE_BASE + counterId, null)

        val request = createCampaignRequest(isEcom = true, feedId = feedId, counters = counters, goals = listOf(goal))
        checkSuccessRequest(request)
    }

    /**
     * Проверяем, что нельзя создать Ecom UC кампанию со слишком маленьким недельным бюджетом
     */
    @Test
    fun `create ecom uc campaign with too low weekly budget`() {
        val currency = clientService.getWorkCurrency(userInfo.clientId)
        val request = createCampaignRequest(
            isEcom = true,
            feedId = 12345,
            weekLimit = currency.minAutobudget) // Недельный бюджет должен быть x3 от минимального для валюты

        checkBadRequest(
            request,
            path = path(PathHelper.field(CreateCampaignRequest::weekLimit.name)),
            defectId = NumberDefectIds.MUST_BE_GREATER_THAN_OR_EQUAL_TO_MIN
        )
    }

    /**
     * Проверяем, что нельзя создать Ecom UC кампанию без счётчиков
     */
    @Test
    fun `create ecom uc campaign without counters`() {
        val request = createCampaignRequest(isEcom = true, feedId = 12345, counters = listOf())
        checkBadRequest(
            request,
            path = path(PathHelper.field(CreateCampaignRequest::counters.name)),
            defectId = CollectionDefectIds.Gen.CANNOT_BE_EMPTY
        )
    }

    /**
     * Проверяем, что нельзя создать Ecom UC кампанию, если счётчик недоступен
     */
    @Test
    fun `create ecom uc campaign with unavailable counter`() {
        val unavailableCounterId = 123454321
        val request = createCampaignRequest(isEcom = true, feedId = 12345, counters = listOf(unavailableCounterId))
        checkBadRequest(
            request,
            path = path(PathHelper.field(CreateCampaignRequest::counters.name)),
            defectId = CampaignDefectIds.Gen.METRIKA_COUNTER_IS_UNAVAILABLE
        )
    }

    /**
     * Проверяем, что нельзя создать Ecom UC кампанию бесцельно
     */
    @Test
    fun `create ecom uc campaign without goals`() {
        val request = createCampaignRequest(isEcom = true, feedId = 12345, goals = listOf())
        checkBadRequest(
            request,
            path = path(PathHelper.field(CreateCampaignRequest::goals.name)),
            defectId = CollectionDefectIds.Size.SIZE_CANNOT_BE_LESS_THAN_MIN
        )
    }

    /**
     * Проверяем, что можно создать Ecom UC кампанию с несколькими целями
     */
    @Test
    fun `create ecom uc campaign with many goals`() {
        val goal1 = UacGoal(RandomNumberUtils.nextPositiveInteger().toLong(),
            cpa = BigDecimal.valueOf(100).setScale(6))
        val goal2 = UacGoal(RandomNumberUtils.nextPositiveInteger().toLong(),
            cpa = BigDecimal.valueOf(200).setScale(6))
        val goals = listOf(goal1, goal2)

        val counterId = RandomNumberUtils.nextPositiveInteger()
        val counters = listOf(counterId)

        metrikaClient.addUserCounter(userInfo.uid, counterId)
        metrikaClient.addCounterGoal(counterId, goal1.goalId.toInt())
        metrikaClient.addCounterGoal(counterId, goal2.goalId.toInt())

        val request = createCampaignRequest(isEcom = true, feedId = feedId, counters = counters, goals = goals)

        val campaignId = checkSuccessRequest(request)
        val subCampaignIds = campaignRepository.getSubCampaignIdsWithMasterIds(userInfo.shard, setOf(campaignId)).keys
        val campaigns = campaignTypedRepository.getTypedCampaigns(userInfo.shard, subCampaignIds + campaignId)
            .filterIsInstance<CampaignWithMeaningfulGoalsWithRequiredFields>()
        assertThat(campaigns).allSatisfy(Consumer { campaign ->
            softly {
                assertThat(campaign.strategy.strategyData.goalId).isEqualTo(CampaignConstants.MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID)
                assertThat(campaign.strategy.strategyData.crr).isEqualTo(100)
                assertThat(campaign.meaningfulGoals).satisfiesExactlyInAnyOrder(Consumer {
                    softly {
                        assertThat(it.goalId).isEqualTo(goal1.goalId)
                        assertThat(it.conversionValue).isEqualTo(BigDecimal.valueOf(100))
                        assertThat(it.isMetrikaSourceOfValue).isIn(false, null)
                    }
                }, {
                    softly {
                        assertThat(it.goalId).isEqualTo(goal2.goalId)
                        assertThat(it.conversionValue).isEqualTo(BigDecimal.valueOf(200))
                        assertThat(it.isMetrikaSourceOfValue).isIn(false, null)
                    }
                })
            }
        })
    }

    /**
     * Проверяем, что нельзя создать Ecom UC кампанию, если цель соответствует недоступному счётчику
     */
    @Test
    fun `create ecom uc campaign with goal from unavailable counter`() {
        val goal = UacGoal(RandomNumberUtils.nextPositiveInteger().toLong())
        val goals = listOf(goal)
        val counterId = RandomNumberUtils.nextPositiveInteger()
        val counters = listOf(counterId)

        metrikaClient.addCounterGoal(counterId, goal.goalId.toInt())

        val request = createCampaignRequest(isEcom = true, feedId = 12345, counters = counters, goals = goals, crr = 20)
        checkBadRequest(
            request,
            path = path(PathHelper.field(CreateCampaignRequest::counters.name)),
            defectId = CampaignDefectIds.Gen.METRIKA_COUNTER_IS_UNAVAILABLE
        )
    }

    @Test
    fun `create ecom uc campaign with invalid condition operator in filter`() {
        val request = createCampaignRequest(isEcom = true, feedId = feedId, feedFilters = listOf(
            UacFeedFilter(listOf(
                UacFeedFilterCondition("age", UacFeedFilterOperator.GREATER, "[\"18\"]", listOf("18")),
            )),
        ))
        checkBadRequest(
            request,
            path = path(PathHelper.field("feedFilters[0].conditions[0].operator")),
            defectId = PerformanceFilterDefects.PerformanceFilterDefectIds.INVALID_OPERATOR
        )
    }

    @Test
    fun `create ecom uc campaign with invalid condition value filter`() {
        val request = createCampaignRequest(isEcom = true, feedId = feedId, feedFilters = listOf(
            UacFeedFilter(listOf(
                UacFeedFilterCondition("name", UacFeedFilterOperator.CONTAINS, "[\"|\"]", listOf("|")),
                UacFeedFilterCondition("categoryId", UacFeedFilterOperator.EQUALS, "[1]", listOf("1")),
            )),
        ))
        checkBadRequest(
            request,
            path = path(PathHelper.field("feedFilters[0].conditions[0].stringValue")),
            defectId = BannerDefectIds.Gen.RESTRICTED_CHARS_IN_FIELD
        )
    }

    @Test
    fun `create ecom uc campaign with duplicate filters`() {
        val request = createCampaignRequest(isEcom = true, feedId = feedId, feedFilters = listOf(
            UacFeedFilter(listOf(
                UacFeedFilterCondition("name", UacFeedFilterOperator.CONTAINS, "[\"b\"]", listOf("b")),
                UacFeedFilterCondition("categoryId", UacFeedFilterOperator.EQUALS, "[1]", listOf("1")),
            )),
            UacFeedFilter(listOf(
                UacFeedFilterCondition("name", UacFeedFilterOperator.CONTAINS, "[\"b\"]", listOf("b")),
                UacFeedFilterCondition("categoryId", UacFeedFilterOperator.EQUALS, "[1]", listOf("1")),
            )),
        ))
        checkBadRequest(
            request,
            path = path(PathHelper.field("feedFilters[0]")),
            defectId = CollectionDefectIds.Gen.MUST_NOT_CONTAIN_DUPLICATED_OBJECTS
        )
    }


    protected fun checkBadRequest(
        request: CreateCampaignRequest,
        path: Path,
        defectId: DefectId<*>,
    ) {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString

        val validationResult = JsonUtils
            .fromJson(JsonUtils.fromJson(result)["validation_result"].toString(), WebValidationResult::class.java)
        validationResult.errors.checkNotEmpty()
        assertThat(validationResult.errors.map { it.path })
            .contains(path.toString())
        assertThat(validationResult.errors.map { it.code })
            .contains(defectId.code)
    }

    protected fun createCampaignRequest(
        href: String? = null,
        contentIds: List<String>? = null,
        texts: List<String>? = listOf("Some text for banner"),
        titles: List<String>? = listOf("Some title for banner"),
        isEcom: Boolean? = null,
        feedId: Long? = null,
        goals: List<UacGoal>? = null,
        cpa: BigDecimal? = null,
        crr: Long? = null,
        counters: List<Int>? = null,
        weekLimit: BigDecimal = DEFAULT_WEEK_LIMIT,
        feedFilters: List<UacFeedFilter>? = null,
        trackingParams: String? = null,
        recommendationsManagementEnabled: Boolean? = null,
        priceRecommendationsManagementEnabled: Boolean? = null,
        minusRegions: List<Long>? = null,
        minusKeywords: List<String>? = null,
        relevanceMatch: UacRelevanceMatch? = null,
    ): CreateCampaignRequest {
        return UacCampaignRequestsCommon.createCampaignRequest(
            uid = userInfo.uid,
            metrikaClient = metrikaClient,
            href = href,
            contentIds = contentIds,
            texts = texts,
            titles = titles,
            isEcom = isEcom,
            feedId = feedId,
            goals = goals,
            cpa = cpa,
            crr = crr,
            counters = counters,
            weekLimit = weekLimit,
            feedFilters = feedFilters,
            trackingParams = trackingParams,
            recommendationsManagementEnabled = recommendationsManagementEnabled,
            priceRecommendationsManagementEnabled = priceRecommendationsManagementEnabled,
            minusRegions = minusRegions,
            minusKeywords = minusKeywords,
            relevanceMatch = relevanceMatch,
            showTitleAndBody = null,
        )
    }
}
