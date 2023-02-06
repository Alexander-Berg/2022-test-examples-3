package ru.yandex.direct.web.entity.uac.controller

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefectIds
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.uac.converter.UacGrutAdGroupBriefConverter.toAdGroupBriefGrutModel
import ru.yandex.direct.core.entity.uac.converter.UacGrutCampaignConverter.toUacYdbCampaign
import ru.yandex.direct.core.entity.uac.model.CampaignStatuses
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.core.entity.uac.model.TargetStatus
import ru.yandex.direct.core.entity.uac.model.UacFeedFilter
import ru.yandex.direct.core.entity.uac.model.UacFeedFilterCondition
import ru.yandex.direct.core.entity.uac.model.UacFeedFilterOperator
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatch
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatchCategory
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbContent
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignService
import ru.yandex.direct.core.entity.uac.service.GrutUacClientService
import ru.yandex.direct.core.entity.uac.service.GrutUacContentService
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.feature.FeatureName.RELEVANCE_MATCH_CATEGORIES_ALLOWED_IN_UC
import ru.yandex.direct.feature.FeatureName.UAC_MULTIPLE_AD_GROUPS_ENABLED
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.test.utils.checkNotNull
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.validation.result.DefectIds
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.direct.web.entity.uac.controller.UacCampaignRequestsCommon.doSuccessCreateRequest
import ru.yandex.direct.web.entity.uac.model.CreateCampaignRequest
import ru.yandex.direct.web.entity.uac.model.PatchCampaignRequest
import ru.yandex.direct.web.entity.uac.service.GrutUacCampaignWebService

@GrutDirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacCampaignGrutControllerCreateUcTest : UacCampaignControllerCreateUcTestBase() {

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    override lateinit var uacCampaignWebService: GrutUacCampaignWebService

    @Autowired
    override lateinit var uacCampaignService: GrutUacCampaignService

    @Autowired
    private lateinit var uacContentService: GrutUacContentService

    @Autowired
    private lateinit var uacClientService: GrutUacClientService

    @Autowired
    private lateinit var grutApiService: GrutApiService

    @Before
    fun grutBefore() {
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.UC_UAC_CREATE_ECOM_BRIEF_IN_GRUT_INSTEAD_OF_YDB, true)
        steps.featureSteps().addClientFeature(userInfo.clientId,
            FeatureName.UC_UAC_CREATE_BRIEF_IN_GRUT_INSTEAD_OF_YDB, true)
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.UAC_ENABLE_MINUS_KEYWORDS_TGO, true)
    }

    override fun saveContents(contents: List<UacYdbContent>) {
        uacClientService.getOrCreateClient(userInfo.user!!, userInfo.user!!)
        uacContentService.insertContents(contents)
    }

    override fun checkSuccessRequest(request: CreateCampaignRequest,
                                     expectTitleContentsCount: Int,
                                     expectTextContentsCount: Int,
                                     expectImageContentsCount: Int,
                                     expectVideoContentsCount: Int,
                                     expectFeedFilters: List<UacFeedFilter>?
    ): Long {
        val result = doSuccessCreateRequest(request, mockMvc, userInfo.clientInfo!!.login)

        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        val campaignId = resultJsonTree["result"]["id"].asText()

        val directCampaignId = campaignId.toIdLong()
        val campaignResponse = grutApiService.briefGrutApi.getBrief(directCampaignId)
        campaignResponse.checkNotNull()
        val campaign = campaignResponse!!.toUacYdbCampaign()
        val filledCampaign = uacCampaignWebService.fillCampaign(userInfo.user!!, userInfo.user!!, campaign, directCampaignId,
            CampaignStatuses(Status.DRAFT, TargetStatus.STOPPED))
        val campaignContents = uacContentService.getCampaignContents(campaign)

        if (expectFeedFilters != null) {
            filledCampaign.feedFilters.checkEquals(expectFeedFilters)
        }

        resultJsonTree["result"].checkEquals(
            filledCampaign,
            ignoredFieldNames = listOfNotNull(
                "feed_filters".takeIf { expectFeedFilters != null },
                "created_at", // ORM выставляет свой timestamp в момент записи, следовательно, может разъехаться
                "relevance_match", // проверяется отдельно, т.к. у списка категорий сортировка не статична
                "relevance_match_categories", // используется только для выдачи на фронт
            ),
        )

        SoftAssertions.assertSoftly {
            // Проверка кампании в mysql
            val mysqlCampaign = campaignTypedRepository.getTyped(userInfo.shard, listOf(directCampaignId))[0]
            it.assertThat(mysqlCampaign)
                .isNotNull
            it.assertThat(mysqlCampaign)
                .`as`("Тип кампании")
                .isInstanceOf(TextCampaign::class.java)
            // Проверка двух галок, которые могут в ответе быть не такими, как в запросе
            it.assertThat(filledCampaign.isRecommendationsManagementEnabled)
                .`as`("Получившееся значение recommendationsManagement")
                .isEqualTo(if (request.isEcom == true) false else request.isRecommendationsManagementEnabled)
            it.assertThat(filledCampaign.isPriceRecommendationsManagementEnabled)
                .`as`("Получившееся значение priceRecommendationsManagement")
                .isEqualTo(if (request.isEcom == true) false else request.isPriceRecommendationsManagementEnabled)

            checkAssets(campaignContents, it, expectTitleContentsCount, expectTextContentsCount, expectImageContentsCount, expectVideoContentsCount)
        }
        return directCampaignId
    }

    /**
     * Проверяем создание Ecom кампании с трекинговыми параметрами
     */
    @Test
    fun `create ecom uc campaign with tracking params`() {
        val request = createCampaignRequest(isEcom = true, feedId = feedId, trackingParams = "a=b&c={d}")
        checkSuccessRequest(request)
    }

    /**
     * Проверяем, что нельзя создать Ecom UC кампанию, указав невалидную ссылку
     */
    @Test
    fun `create ecom uc campaign with invalid href`() {
        val request = createCampaignRequest(isEcom = true, feedId = feedId, href = "https://invalid href")
        checkBadRequest(
            request,
            path = PathHelper.path(PathHelper.field(CreateCampaignRequest::href.name)),
            defectId = BannerDefectIds.Gen.INVALID_HREF
        )
    }

    /**
     * Проверяем, что нельзя создать Ecom UC кампанию, указав ломающие ссылку трекинговые параметры
     */
    @Test
    fun `create ecom uc campaign with invalid tracking params`() {
        val request = createCampaignRequest(isEcom = true, feedId = feedId, trackingParams = "a=b space c=d")
        checkBadRequest(
            request,
            path = PathHelper.path(PathHelper.field(CreateCampaignRequest::trackingParams.name)),
            defectId = DefectIds.INVALID_VALUE
        )
    }

    /**
     * Проверяем создание кампании с отключенным управлением от Яндекса
     */
    @Test
    fun `create uc campaign with recommendations management off`() {
        val request = createCampaignRequest(isEcom = false, recommendationsManagementEnabled = false,
            priceRecommendationsManagementEnabled = false)
        checkSuccessRequest(request)
    }

    /**
     * Проверяем создание кампании с управлением от Яндекса
     */
    @Test
    fun `create uc campaign with recommendations management`() {
        val request = createCampaignRequest(isEcom = false, recommendationsManagementEnabled = true)
        checkSuccessRequest(request)
    }

    /**
     * Проверяем создание кампании с управлением ставками от Яндекса
     */
    @Test
    fun `create uc campaign with price recommendations management`() {
        val request = createCampaignRequest(isEcom = false, recommendationsManagementEnabled = true,
            priceRecommendationsManagementEnabled = true)
        checkSuccessRequest(request)
    }

    /**
     * Проверяем создание кампании с управлением ставками от Яндекса без управления от Яндекса
     */
    @Test
    fun `create uc campaign with price recommendations management and recommendations management off`() {
        val request = createCampaignRequest(isEcom = false, recommendationsManagementEnabled = false,
            priceRecommendationsManagementEnabled = true)
        checkBadRequest(
            request,
            path = path(PathHelper.field(CreateCampaignRequest::isPriceRecommendationsManagementEnabled)),
            defectId = DefectIds.INCONSISTENT_STATE
        )
    }

    /**
     * Проверяем создание кампании с управлением ставками от Яндекса без явного управления от Яндекса
     */
    @Test
    fun `create uc campaign with price recommendations management and no recommendations management`() {
        val request = createCampaignRequest(isEcom = false, recommendationsManagementEnabled = null,
            priceRecommendationsManagementEnabled = true)
        checkBadRequest(
            request,
            path = path(PathHelper.field(CreateCampaignRequest::isPriceRecommendationsManagementEnabled)),
            defectId = DefectIds.INCONSISTENT_STATE
        )
    }

    /**
     * Проверяем возможность создания ecom-кампании с управлением от Яндекса (отключенным при сохранении)
     */
    @Test
    fun `create uc ecom campaign with recommendations management`() {
        val request = createCampaignRequest(isEcom = true, feedId = feedId, recommendationsManagementEnabled = true,
            priceRecommendationsManagementEnabled = false)
        checkSuccessRequest(request)
    }

    /**
     * Проверяем возможность создания ecom-кампании с управлением ставками от Яндекса (отключенным при сохранении)
     */
    @Test
    fun `create uc ecom campaign with price recommendations management`() {
        val request = createCampaignRequest(isEcom = true, feedId = feedId, recommendationsManagementEnabled = true,
            priceRecommendationsManagementEnabled = true)
        checkSuccessRequest(request)
    }

    /**
     * Проверяем возможность создания ecom-кампании с управлением ставками от Яндекса без управления от Яндекса
     * (управление автоматически отключается при сохранении)
     */
    @Test
    fun `create uc ecom campaign with price recommendations management and recommendations management off`() {
        val request = createCampaignRequest(isEcom = true, feedId = feedId, recommendationsManagementEnabled = false,
            priceRecommendationsManagementEnabled = true)
        checkSuccessRequest(request)
    }

    /**
     * Проверяем создание Ecom кампании с фильтрами со старой типизацией
     */
    @Test
    fun `create ecom uc campaign with old feed filters`() {
        val request = createCampaignRequest(isEcom = true, feedId = feedId, feedFilters = listOf(
            UacFeedFilter(listOf(
                UacFeedFilterCondition("name", UacFeedFilterOperator.CONTAINS, "[\"b\"]", null),
                UacFeedFilterCondition("categoryId", UacFeedFilterOperator.EQUALS, "[1]", null),
            )),
        ))
        checkSuccessRequest(request, expectFeedFilters = listOf(
            UacFeedFilter(listOf(
                UacFeedFilterCondition("name", UacFeedFilterOperator.CONTAINS, "[\"b\"]", listOf("b")),
                UacFeedFilterCondition("categoryId", UacFeedFilterOperator.EQUALS, "[1]", listOf("1")),
            )),
        ))
    }

    /**
     * Проверяем создание Ecom кампании с фильтрами с новой типизацией
     */
    @Test
    fun `create ecom uc campaign with new feed filters`() {
        val request = createCampaignRequest(isEcom = true, feedId = feedId, feedFilters = listOf(
            UacFeedFilter(listOf(
                UacFeedFilterCondition("name", UacFeedFilterOperator.CONTAINS, "[\"b\"]", listOf("b")),
                UacFeedFilterCondition("categoryId", UacFeedFilterOperator.EQUALS, "[1]", listOf("1")),
            )),
        ))
        checkSuccessRequest(request, expectFeedFilters = listOf(
            UacFeedFilter(listOf(
                UacFeedFilterCondition("name", UacFeedFilterOperator.CONTAINS, "[\"b\"]", listOf("b")),
                UacFeedFilterCondition("categoryId", UacFeedFilterOperator.EQUALS, "[\"1\"]", listOf("1")),
            )),
        ))
    }

    fun casesForAutotargetingCategories(): Array<Array<Any>> {
        val activeCategories = UacRelevanceMatchCategory.values()
            .map { arrayOf(true, setOf(it, UacRelevanceMatchCategory.EXACT_MARK).sorted()) }
        val inActiveCategories = UacRelevanceMatchCategory.values()
            .map { arrayOf(false, listOf(it)) }
        return activeCategories
            .plus(inActiveCategories)
            .plus(arrayOf(arrayOf(true, UacRelevanceMatchCategory.values().sorted())))
            .plus(arrayOf(arrayOf(false, UacRelevanceMatchCategory.values().sorted())))
            .toTypedArray()
    }

    /**
     * Проверяем создание UC кампании с автотаргетингом
     */
    @Test
    @TestCaseName("Active {0} categories {1}")
    @Parameters(method = "casesForAutotargetingCategories")
    fun `create uc campaign with autotargeting`(
        active: Boolean,
        categories: List<UacRelevanceMatchCategory>,
    ) {
        steps.featureSteps().addClientFeature(userInfo.clientId, RELEVANCE_MATCH_CATEGORIES_ALLOWED_IN_UC, true)

        val relevanceMatch = UacRelevanceMatch(
            active = active,
            categories = categories.toSortedSet(compareBy { it.id }),
        )

        val request = createCampaignRequest(isEcom = false, relevanceMatch = relevanceMatch)
        val directCampaignId = checkSuccessRequest(request)
        val ucCampaign = uacCampaignService.getCampaignById(directCampaignId.toString())

        val actualRelevanceMatchData = mapOf(ucCampaign!!.relevanceMatch!!.active
            to ucCampaign.relevanceMatch!!.categories.toSortedSet(compareBy { it.id }))
        val expectRelevanceMatchData = mapOf(relevanceMatch.active
            to relevanceMatch.categories.toSortedSet(compareBy { it.id }))

        Assertions.assertThat(actualRelevanceMatchData)
            .`as`("автотаргетинг")
            .isEqualTo(expectRelevanceMatchData)
    }

    /**
     * Проверяем, что UC кампания не создается с автотаргетингом без фичи
     */
    @Test
    fun `create uc campaign with autotargeting and without feature`() {
        steps.featureSteps().addClientFeature(userInfo.clientId, RELEVANCE_MATCH_CATEGORIES_ALLOWED_IN_UC, false)

        val relevanceMatch = UacRelevanceMatch(
            active = true,
            categories = setOf(UacRelevanceMatchCategory.EXACT_MARK),
        )

        val request = createCampaignRequest(isEcom = false, relevanceMatch = relevanceMatch)
        checkBadRequest(
            request,
            path = path(PathHelper.field(PatchCampaignRequest::relevanceMatch)),
            defectId = DefectIds.MUST_BE_NULL
        )
    }

    fun casesForAutotargetingCategoriesWithoutExtra(): Array<Any> {
        val data = UacRelevanceMatchCategory.values()
            .filter { it != UacRelevanceMatchCategory.EXACT_MARK }
            .map { listOf(it) }
        return data
            .plus(arrayOf(UacRelevanceMatchCategory.values()
                .filter { it != UacRelevanceMatchCategory.EXACT_MARK }
                .sorted())
            ).toTypedArray()
    }

    /**
     * Проверяем, что UC кампания не создается с автотаргетингом и без EXACT_MARK категории
     */
    @Test
    @TestCaseName("Categories {0}")
    @Parameters(method = "casesForAutotargetingCategoriesWithoutExtra")
    fun `create uc campaign with autotargeting and without exact_mark category`(
        categories: List<UacRelevanceMatchCategory>,
    ) {
        steps.featureSteps().addClientFeature(userInfo.clientId, RELEVANCE_MATCH_CATEGORIES_ALLOWED_IN_UC, true)

        val relevanceMatch = UacRelevanceMatch(
            active = true,
            categories = categories.toSet(),
        )

        val request = createCampaignRequest(isEcom = false, relevanceMatch = relevanceMatch)
        checkBadRequest(
            request,
            path = path(PathHelper.field(PatchCampaignRequest::relevanceMatch)),
            defectId = DefectIds.INVALID_VALUE
        )
    }

    @Test
    fun `create uc campaign with ad group brief`() {
        steps.featureSteps().addClientFeature(userInfo.clientId, UAC_MULTIPLE_AD_GROUPS_ENABLED, true)

        val request = createCampaignRequest(isEcom = false)
        val campaignId = checkSuccessRequest(request)
        val adGroupBriefs = grutApiService.adGroupBriefGrutApi.selectAdGroupBriefsByCampaignId(campaignId)
        Assertions.assertThat(adGroupBriefs.size).`as`("Ожидается одна заявка на группу").isEqualTo(1)
        val adGroupBrief = adGroupBriefs[0]

        val campaignBrief = grutApiService.briefGrutApi.getBrief(campaignId)
        val campaign = campaignBrief!!.toUacYdbCampaign()
        val expectedAdGroupBrief = toAdGroupBriefGrutModel(campaignId, campaign, id = adGroupBrief.id)
        Assertions.assertThat(adGroupBrief).isEqualTo(expectedAdGroupBrief)
    }
}
