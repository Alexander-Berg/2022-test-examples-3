package ru.yandex.direct.grid.processing.service.group

import com.nhaarman.mockitokotlin2.any
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.adgroup.generation.AdGroupKeywordRecommendationService
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatchCategory
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.api.GdDefect
import ru.yandex.direct.grid.processing.model.group.mutation.GdRelevanceMatchCategory
import ru.yandex.direct.grid.processing.model.showcondition.GdAdGroupGetKeywordRecommendationData
import ru.yandex.direct.grid.processing.model.showcondition.GdAdGroupGetKeywordRecommendationInput
import ru.yandex.direct.grid.processing.model.showcondition.GdKeywordsByCategory
import ru.yandex.direct.result.Result.successful

@GridProcessingTest
@RunWith(JUnitParamsRunner::class)
class GroupDataServiceRecommendedKeywordsByCategoryTest {

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    private lateinit var steps: Steps
    @Autowired
    private lateinit var groupDataService: GroupDataService
    @Autowired
    private lateinit var keywordGenerationService: AdGroupKeywordRecommendationService

    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId
    private var shard: Int = 0

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!
        shard = clientInfo.shard
    }

    fun casesForRecommendedKeywordsByCategory(): Array<Array<Any?>> {
        val byAdGroup = GdRelevanceMatchCategory.values()
            .map {
                arrayOf(
                    false,
                    true,
                    false,
                    listOf(RelevanceMatchCategory.valueOf(it.typedValue) to listOf("word")),
                    listOf(it to listOf("word")),
                    null
                )
            }
        val byAdGroupWithFeature = GdRelevanceMatchCategory.values()
            .map {
                arrayOf(
                    true,
                    true,
                    false,
                    listOf(RelevanceMatchCategory.valueOf(it.typedValue) to listOf("word")),
                    listOf(it to listOf("word")),
                    null
                )
            }
        val byCampaignWithFeature = GdRelevanceMatchCategory.values()
            .map {
                arrayOf(
                    false,
                    false,
                    true,
                    listOf(RelevanceMatchCategory.valueOf(it.typedValue) to listOf("word")),
                    emptyList<Pair<GdRelevanceMatchCategory, List<String>>>(),
                    GdDefect()
                        .withCode("FeatureDefectIds.CollectionDefectIds.FEATURE_IS_ALREADY_DISABLED_FOR_ROLE")
                        .withPath("")
                )
            }
        val byInvalidTypeCampaignWithFeature = GdRelevanceMatchCategory.values()
            .map {
                arrayOf(
                    false,
                    true,
                    false,
                    listOf(RelevanceMatchCategory.valueOf(it.typedValue) to listOf("word")),
                    emptyList<Pair<GdRelevanceMatchCategory, List<String>>>(),
                    GdDefect()
                        .withCode("CampaignDefectIds.CampaignTypeDefects.INVALID_CAMPAIGN_TYPE")
                        .withPath("")
                )
            }
        val byInvalidTypeAdGroupWithFeature = GdRelevanceMatchCategory.values()
            .map {
                arrayOf(
                    true,
                    true,
                    false,
                    listOf(RelevanceMatchCategory.valueOf(it.typedValue) to listOf("word")),
                    emptyList<Pair<GdRelevanceMatchCategory, List<String>>>(),
                    GdDefect()
                        .withCode("CampaignDefectIds.CampaignTypeDefects.INVALID_CAMPAIGN_TYPE")
                        .withPath("")
                )
            }
        return byAdGroup
            .plus(byAdGroupWithFeature)
            .plus(byCampaignWithFeature)
            .plus(byInvalidTypeCampaignWithFeature)
            .plus(byInvalidTypeAdGroupWithFeature)
            .plus(hardCases())
            .toTypedArray()
    }

    private fun hardCases(): Array<Array<Any?>> {
        val pairs = listOf(
            listOf(
                RelevanceMatchCategory.accessory_mark to
                    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
                RelevanceMatchCategory.alternative_mark to listOf("1", "2", "3", "4", "5")
            ) to listOf(
                GdRelevanceMatchCategory.ACCESSORY_MARK to listOf("6", "7", "8", "9", "10"),
                GdRelevanceMatchCategory.ALTERNATIVE_MARK to listOf("1", "2", "3", "4", "5")
            ),
            listOf(
                RelevanceMatchCategory.alternative_mark to
                    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
                RelevanceMatchCategory.accessory_mark to listOf("1", "2", "3", "4", "5")
            ) to listOf(
                GdRelevanceMatchCategory.ALTERNATIVE_MARK to listOf("6", "7", "8", "9", "10"),
                GdRelevanceMatchCategory.ACCESSORY_MARK to listOf("1", "2", "3", "4", "5")
            ),
            listOf(
                RelevanceMatchCategory.competitor_mark to
                    listOf("1", "2", "3", "4", "5", "6"),
                RelevanceMatchCategory.broader_mark to
                    listOf("1", "7", "8", "9", "10", "11"),
            ) to listOf(
                GdRelevanceMatchCategory.COMPETITOR_MARK to
                    listOf("2", "3", "4", "5", "6"),
                GdRelevanceMatchCategory.BROADER_MARK to
                    listOf("7", "8", "9", "10", "11"),
            ),

            // этот кейс может выдавать другой результат при изменении внутри deleteDuplicatedStringsAndCut()
            // в ru.yandex.direct.grid.processing.service.group.GroupDataService
            listOf(
                RelevanceMatchCategory.exact_mark to
                    listOf("a", "b", "c", "d", "e", "g", "q", "n"),
                RelevanceMatchCategory.accessory_mark to
                    listOf("r", "z", "r", "e", "l", "w", "p", "ii", "jj"),
                RelevanceMatchCategory.competitor_mark to
                    listOf("r", "e", "m", "j", "h", "y", "w", "i", "o", "oo"),
                RelevanceMatchCategory.alternative_mark to
                    listOf("aa", "t", "k", "e", "r", "re", "f", "w"),
                RelevanceMatchCategory.broader_mark to
                    listOf("m", "v", "u", "k", "tt", "ll", "ee", "bb", "l")
            ) to listOf(
                GdRelevanceMatchCategory.EXACT_MARK to
                    listOf("a", "b", "c", "d", "g"),
                GdRelevanceMatchCategory.ACCESSORY_MARK to
                    listOf("z", "p", "ii", "jj", "l"),
                GdRelevanceMatchCategory.COMPETITOR_MARK to
                    listOf("j", "h", "y", "i", "o"),
                GdRelevanceMatchCategory.ALTERNATIVE_MARK to
                    listOf("aa", "t", "re", "f", "k"),
                GdRelevanceMatchCategory.BROADER_MARK to
                    listOf("v", "u", "tt", "ll", "ee")
            )
        )
        var array = emptyArray<Array<Any?>>()
        pairs.forEach { (keywordsByCategory, expected) ->
            array = array.plus(
                arrayOf(
                    true,
                    true,
                    false,
                    keywordsByCategory,
                    expected,
                    null
                )
            )
        }
        return array
    }

    @Test
    @TestCaseName("isFeatureEnabled {0} withAdGroupId {1} withCampaignId {2} keywordsByCategory {3} expected {4} defect {5}")
    @Parameters(method = "casesForRecommendedKeywordsByCategory")
    fun recommendedKeywordsByCategoryTest(
        isFeatureEnabled: Boolean,
        withAdGroupId: Boolean,
        withCampaignId: Boolean,
        keywordsByCategoryList: List<Pair<RelevanceMatchCategory, List<String>>>,
        expectKeywordsByCategory: List<Pair<GdRelevanceMatchCategory, List<String>>>,
        defect: GdDefect?
    ) {
        val keywordsByCategory = keywordsByCategoryList.toMap()
        Mockito.doReturn(successful(keywordsByCategory))
            .`when`(keywordGenerationService).recommendedKeywords(
                any(), any()
            )
        steps.featureSteps().addClientFeature(
            clientId,
            FeatureName.RELEVANCE_MATCH_CATEGORIES_ALLOWED_IN_UC, isFeatureEnabled
        )
        val adGroupInfo =
            if (defect != null && defect.code.equals("CampaignDefectIds.CampaignTypeDefects.INVALID_CAMPAIGN_TYPE")) {
                steps.adGroupSteps().createActiveMobileContentAdGroup(clientInfo)
            } else {
                steps.adGroupSteps().createDefaultAdGroup(clientInfo)
            }
        val input: GdAdGroupGetKeywordRecommendationInput = createInput()
        if (withAdGroupId) {
            input.withAdGroupId(adGroupInfo.adGroupId)
        }
        if (withCampaignId) {
            input.withCampaignId(adGroupInfo.campaignId)
        }
        val result = groupDataService.recommendedKeywordsByCategory(shard, clientId, input)
        validateRecommendedKeywordsByCategoryTest(result, expectKeywordsByCategory.toMap(), defect)
    }

    private fun validateRecommendedKeywordsByCategoryTest(
        result: GdKeywordsByCategory,
        expected: Map<GdRelevanceMatchCategory, List<String>>,
        defect: GdDefect?
    ) {
        if (expected.isEmpty()) {
            SoftAssertions.assertSoftly { soft: SoftAssertions ->
                soft.assertThat(result.keywordByCategory).`as`("Empty keywordByCategory").isEmpty()
                soft.assertThat(result.validationResult).`as`("Not null validation").isNotNull
                soft.assertThat(result.validationResult.errors).`as`("Validation result").isEqualTo(listOf(defect))
            }
        } else {
            SoftAssertions.assertSoftly { soft: SoftAssertions ->
                soft.assertThat(result.validationResult).`as`("Null validation result").isNull()
                soft.assertThat(result.keywordByCategory).`as`("Keywords by category").containsExactlyInAnyOrderEntriesOf(expected)
            }
        }
    }

    private fun createInput(): GdAdGroupGetKeywordRecommendationInput {
        return GdAdGroupGetKeywordRecommendationInput()
            .withKeywordRecommendationData(
                GdAdGroupGetKeywordRecommendationData()
                    .withKeywords(listOf("plus"))
                    .withMinusKeywords(emptyList())
                    .withRegionIds(listOf(1))
            )
    }
}
