package ru.yandex.direct.web.entity.uac.controller

import com.fasterxml.jackson.databind.node.ObjectNode
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions
import org.assertj.core.api.SoftAssertions
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.region.validation.RegionIdDefectIds
import ru.yandex.direct.core.entity.uac.model.CampaignStatuses
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.core.entity.uac.model.TargetStatus
import ru.yandex.direct.core.entity.uac.model.UacFeedFilter
import ru.yandex.direct.core.entity.uac.model.UacFeedFilterCondition
import ru.yandex.direct.core.entity.uac.model.UacFeedFilterOperator
import ru.yandex.direct.core.entity.uac.model.UacGoal
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatch
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatchCategory
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAccountRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUserRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbContent
import ru.yandex.direct.core.entity.uac.service.UacCampaignService
import ru.yandex.direct.core.entity.uac.validation.ContentDefectIds
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.feature.FeatureName.RELEVANCE_MATCH_CATEGORIES_ALLOWED_IN_UC
import ru.yandex.direct.regions.Region
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.test.utils.checkContainsInAnyOrder
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.test.utils.checkSize
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.validation.defect.ids.CollectionDefectIds
import ru.yandex.direct.validation.result.DefectIds
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.entity.uac.controller.UacCampaignRequestsCommon.doSuccessCreateRequest
import ru.yandex.direct.web.entity.uac.model.CreateCampaignRequest
import ru.yandex.direct.web.entity.uac.model.PatchCampaignRequest
import ru.yandex.direct.web.entity.uac.service.YdbUacCampaignWebService
import ru.yandex.direct.web.entity.uac.toResponse

@DirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacCampaignControllerCreateUcTest : UacCampaignControllerCreateUcTestBase() {

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    private lateinit var uacCampaignRepository: UacYdbCampaignRepository

    @Autowired
    private lateinit var uacDirectCampaignRepository: UacYdbDirectCampaignRepository

    @Autowired
    private lateinit var uacAccountRepository: UacYdbAccountRepository

    @Autowired
    private lateinit var uacUserRepository: UacYdbUserRepository

    @Autowired
    override lateinit var uacCampaignWebService: YdbUacCampaignWebService

    @Autowired
    override lateinit var uacCampaignService: UacCampaignService

    @Autowired
    private lateinit var uacYdbContentRepository: UacYdbContentRepository

    @Autowired
    private lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    override fun saveContents(contents: List<UacYdbContent>) {
        uacYdbContentRepository.saveContents(contents)
    }

    override fun checkSuccessRequest(
        request: CreateCampaignRequest,
        expectTitleContentsCount: Int,
        expectTextContentsCount: Int,
        expectImageContentsCount: Int,
        expectVideoContentsCount: Int,
        expectFeedFilters: List<UacFeedFilter>?
    ): Long {
        val result = doSuccessCreateRequest(request, mockMvc, userInfo.clientInfo!!.login)

        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        val campaignId = resultJsonTree["result"]["id"].asText()
        val campaign = uacCampaignRepository.getCampaign(campaignId)
        val directCampaign = uacDirectCampaignRepository.getDirectCampaignById(campaignId)
        val filledCampaign = uacCampaignWebService.fillCampaign(
            userInfo.user!!,
            userInfo.user!!,
            campaign!!,
            directCampaign!!.directCampaignId,
            CampaignStatuses(Status.DRAFT, TargetStatus.STOPPED),
        )

        val campaignContents = uacYdbCampaignContentRepository.getCampaignContents(campaignId)

        SoftAssertions.assertSoftly {
            val filledJsonTree = JsonUtils.MAPPER.readTree(JsonUtils.toJson(filledCampaign.toResponse()))

            if (expectFeedFilters != null) {
                it.assertThat(filledCampaign.feedFilters)
                    .isEqualTo(expectFeedFilters)

                if (resultJsonTree is ObjectNode) {
                    resultJsonTree.remove("feed_filters")
                }

                if (filledJsonTree is ObjectNode) {
                    filledJsonTree.remove("feed_filters")
                }
            }

            // эти настройки могут меняться при обработке запроса и проверяются в UacCampaignGrutControllerCreateUcTest
            if (filledCampaign.isEcom == true && filledJsonTree is ObjectNode && resultJsonTree is ObjectNode) {
                val filledResultTree = filledJsonTree.get("result")
                val resultResultTree = resultJsonTree.get("result")
                if (filledResultTree is ObjectNode && resultResultTree is ObjectNode) {
                    filledResultTree.remove("price_recommendations_management_enabled")
                    filledResultTree.remove("recommendations_management_enabled")
                    resultResultTree.remove("price_recommendations_management_enabled")
                    resultResultTree.remove("recommendations_management_enabled")
                }
            }

            it.assertThat(resultJsonTree)
                .isEqualTo(filledJsonTree)

            // Проверка пользователя
            val user = uacUserRepository.getUserByUid(userInfo.uid)
            val account = uacAccountRepository.getAccountByClientId(userInfo.clientId.asLong())
            it.assertThat(user).isNotNull
            it.assertThat(account).isNotNull

            // Проверка кампании в mysql
            val mysqlCampaign =
                campaignTypedRepository.getTyped(userInfo.shard, listOf(directCampaign.directCampaignId))[0]
            it.assertThat(mysqlCampaign)
                .isNotNull
            it.assertThat(mysqlCampaign)
                .`as`("Тип кампании")
                .isInstanceOf(TextCampaign::class.java)

            checkAssets(
                campaignContents,
                it,
                expectTitleContentsCount,
                expectTextContentsCount,
                expectImageContentsCount,
                expectVideoContentsCount
            )
        }
        return directCampaign.directCampaignId
    }

    /**
     * Проверяем создание UC кампании при запросе с одним image и video
     */
    @Test
    fun `create uc campaign with contents`() {
        val request = createCampaignRequest(
            contentIds = listOf(videoContentFirst.id, imageContentFirst.id),
        )

        checkSuccessRequest(
            request,
            expectTitleContentsCount = 1,
            expectTextContentsCount = 1,
            expectImageContentsCount = 1,
            expectVideoContentsCount = 1,
        )
    }

    /**
     * Проверяем создание UC кампании при запросе без контента
     */
    @Test
    fun `create uc campaign without contents`() {
        val request = createCampaignRequest()

        checkSuccessRequest(request)
    }

    @Test
    fun createUcCampaignWithGoalsWithoutConversionValue() {
        val defaultConversionValue = Currencies.getCurrency(CurrencyCode.RUB).ucDefaultConversionValue
        val goal1 = UacGoal(RandomNumberUtils.nextPositiveInteger().toLong(), null)
        val goal2 = UacGoal(RandomNumberUtils.nextPositiveInteger().toLong(), null)
        val request = createCampaignRequest(goals = listOf(goal1, goal2)).copy(cpa = null)

        val campaignId = checkSuccessRequest(request)

        val textCampaign = campaignTypedRepository.getTyped(userInfo.shard, listOf(campaignId))[0] as TextCampaign
        textCampaign.meaningfulGoals.checkSize(2)
        textCampaign.meaningfulGoals.forEach { it.conversionValue.checkEquals(defaultConversionValue) }
    }

    @Test
    fun createUcCampaignWithGoalsWithConversionValue() {
        val conversionValue = 1234.toBigDecimal()
        val goal1 = UacGoal(RandomNumberUtils.nextPositiveInteger().toLong(), conversionValue = conversionValue)
        val goal2 = UacGoal(RandomNumberUtils.nextPositiveInteger().toLong(), conversionValue = conversionValue)
        val request = createCampaignRequest(goals = listOf(goal1, goal2)).copy(cpa = null)

        val campaignId = checkSuccessRequest(request)

        val textCampaign = campaignTypedRepository.getTyped(userInfo.shard, listOf(campaignId))[0] as TextCampaign
        textCampaign.meaningfulGoals.checkSize(2)
        textCampaign.meaningfulGoals.forEach { it.conversionValue.checkEquals(conversionValue) }
    }

    @Test
    fun createUcCampaignWithGoalsWithAndWithoutConversionValue() {
        val conversionValue = 1234.toBigDecimal()
        val defaultConversionValue = Currencies.getCurrency(CurrencyCode.RUB).ucDefaultConversionValue
        val goal1 = UacGoal(RandomNumberUtils.nextPositiveInteger().toLong(), conversionValue = conversionValue)
        val goal2 = UacGoal(RandomNumberUtils.nextPositiveInteger().toLong())
        val request = createCampaignRequest(goals = listOf(goal1, goal2)).copy(cpa = null)

        val campaignId = checkSuccessRequest(request)

        val textCampaign = campaignTypedRepository.getTyped(userInfo.shard, listOf(campaignId))[0] as TextCampaign
        textCampaign.meaningfulGoals.checkSize(2)
        textCampaign.meaningfulGoals.map { it.conversionValue }
            .checkContainsInAnyOrder(conversionValue, defaultConversionValue)
    }

    /**
     * Проверяем что UC кампания не создается при запросе с двумя video ассетами
     */
    @Test
    fun `create uc campaign with two video contents`() {
        val request = createCampaignRequest(
            contentIds = listOf(videoContentFirst.id, videoContentSecond.id),
        )

        checkBadRequest(
            request,
            path = path(PathHelper.field(PatchCampaignRequest::contentIds.name)),
            defectId = ContentDefectIds.Gen.SIZE_OF_VIDEO_CONTENTS_CANNOT_BE_MORE_THAN_MAX,
        )
    }

    /**
     * Проверяем что UC кампания не создается при запросе с двумя image ассетами
     */
    @Test
    fun `create uc campaign with two image contents`() {
        val request = createCampaignRequest(
            contentIds = listOf(imageContentFirst.id, imageContentSecond.id),
        )

        checkBadRequest(
            request,
            path = path(PathHelper.field(PatchCampaignRequest::contentIds.name)),
            defectId = ContentDefectIds.Gen.SIZE_OF_IMAGE_CONTENTS_CANNOT_BE_MORE_THAN_MAX,
        )
    }

    /**
     * Проверяем что UC кампания не создается при запросе с двумя title ассетами
     */
    @Test
    fun `create uc campaign with two title contents`() {
        val request = createCampaignRequest(
            titles = listOf("title1", "title2"),
        )

        checkBadRequest(
            request,
            path = path(PathHelper.field(PatchCampaignRequest::titles.name)),
            defectId = CollectionDefectIds.Size.SIZE_MUST_BE_IN_INTERVAL,
        )
    }

    /**
     * Проверяем что UC кампания не создается при запросе с двумя text ассетами
     */
    @Test
    fun `create uc campaign with two text contents`() {
        val request = createCampaignRequest(
            texts = listOf("text1", "text2"),
        )

        checkBadRequest(
            request,
            path = path(PathHelper.field(PatchCampaignRequest::texts.name)),
            defectId = CollectionDefectIds.Size.SIZE_MUST_BE_IN_INTERVAL,
        )
    }

    /**
     * Проверяем что UC кампания создается при запросе с несколькими text/title/video/image ассетами
     * и c включенной фичой UC_MULTIPLE_ADS_ENABLED
     */
    @Test
    fun `create uc campaign with multiple contents and with feature`() {
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.UC_MULTIPLE_ADS_ENABLED, true)

        val request = createCampaignRequest(
            titles = listOf("title1", "title2"),
            texts = listOf("text1", "text2"),
            contentIds = listOf(
                videoContentFirst.id,
                videoContentSecond.id,
                imageContentFirst.id,
                imageContentSecond.id
            ),
        )

        checkSuccessRequest(
            request,
            expectTitleContentsCount = 2,
            expectTextContentsCount = 2,
            expectImageContentsCount = 2,
            expectVideoContentsCount = 2,
        )
    }

    /**
     * Проверяем создание Ecom кампании с фильтрами со старой и новой типизацией
     */
    @Test
    fun `create ecom uc campaign with old and new feed filters`() {
        val request = createCampaignRequest(isEcom = true, feedId = feedId, feedFilters = listOf(
            UacFeedFilter(listOf(
                UacFeedFilterCondition("name", UacFeedFilterOperator.CONTAINS, "[\"b\"]", null),
                UacFeedFilterCondition("categoryId", UacFeedFilterOperator.EQUALS, "[1]", listOf("1")),
            )),
        ))
        checkSuccessRequest(request, expectFeedFilters = listOf(
            UacFeedFilter(listOf(
                UacFeedFilterCondition("name", UacFeedFilterOperator.CONTAINS, "[\"b\"]", null),
                UacFeedFilterCondition("categoryId", UacFeedFilterOperator.EQUALS, "[1]", listOf("1")),
            )),
        ))
    }

    /**
     * Проверяем что UC кампания не создается, если регионы не содержат минус-регионы
     */
    @Test
    fun `create uc campaign with bad minus regions`() {
        val request = createCampaignRequest(
            minusRegions = listOf(Region.UZBEKISTAN_REGION_ID),
        )

        checkBadRequest(
            request,
            path = path(PathHelper.field(PatchCampaignRequest::regions.name)),
            defectId = RegionIdDefectIds.Regions.MINUS_REGIONS_WITHOUT_PLUS_REGIONS,
        )
    }

    /**
     * Проверяем, что UC кампания создается, если указаны минус-фразы
     */
    @Test
    fun `create uc campaign with minus keywords`() {
        val minusKeywords = listOf("minusKeyword", "anotherMinusKeyword")
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.UC_ENABLE_OPTIONAL_KEYWORDS, true)
        val request = createCampaignRequest(
            minusKeywords = minusKeywords
        )
        val campaignId = checkSuccessRequest(request)
        val campaign = campaignTypedRepository.getTyped(userInfo.shard, listOf(campaignId))[0] as TextCampaign
        Assertions.assertThat(campaign.minusKeywords).containsExactlyInAnyOrderElementsOf(minusKeywords)
    }

    fun casesForAutotargetingCategories(): List<List<Any>> {
        val activeCategories = UacRelevanceMatchCategory.values()
            .map { listOf(true, setOf(it, UacRelevanceMatchCategory.EXACT_MARK).sorted()) }
        val inActiveCategories = UacRelevanceMatchCategory.values()
            .map { listOf(false, listOf(it)) }
        return activeCategories
            .plus(inActiveCategories)
            .plus(listOf(listOf(true, UacRelevanceMatchCategory.values().sorted())))
            .plus(listOf(listOf(false, UacRelevanceMatchCategory.values().sorted())))
    }

    /**
     * Проверяем, что UC кампания создается с автотаргетингом
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
            categories = categories.toSet(),
        )

        val request = createCampaignRequest(relevanceMatch = relevanceMatch)
        val directCampaignId = checkSuccessRequest(request)
        val directCampaign = uacDirectCampaignRepository.getDirectCampaignByDirectCampaignId(directCampaignId)
        val ucCampaign = uacCampaignRepository.getCampaign(directCampaign!!.id)!!

        Assertions.assertThat(ucCampaign.relevanceMatch)
            .`as`("автотаргетинг")
            .isEqualTo(relevanceMatch)
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

        val request = createCampaignRequest(relevanceMatch = relevanceMatch)
        checkBadRequest(
            request,
            path = path(PathHelper.field(PatchCampaignRequest::relevanceMatch)),
            defectId = DefectIds.MUST_BE_NULL
        )
    }

    fun casesForAutotargetingCategoriesWithoutExtra(): List<List<Any>> {
        val data = UacRelevanceMatchCategory.values()
            .filter { it != UacRelevanceMatchCategory.EXACT_MARK }
            .map { listOf(listOf(it)) }
        return data
            .plus(listOf(
                listOf(UacRelevanceMatchCategory.values()
                    .filter { it != UacRelevanceMatchCategory.EXACT_MARK }
                    .sorted()
                )
            ))
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

        val request = createCampaignRequest(relevanceMatch = relevanceMatch)
        checkBadRequest(
            request,
            path = path(PathHelper.field(PatchCampaignRequest::relevanceMatch)),
            defectId = DefectIds.INVALID_VALUE
        )
    }
}
