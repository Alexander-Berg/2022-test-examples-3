package ru.yandex.direct.grid.processing.service.showcondition

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher
import ru.yandex.direct.core.entity.adgroup.service.MinusKeywordPreparingTool
import ru.yandex.direct.core.entity.campaign.service.CampaignService
import ru.yandex.direct.core.entity.keyword.service.KeywordService
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignService
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.KeywordInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR
import ru.yandex.direct.grid.processing.configuration.GrutGridProcessingTest
import ru.yandex.direct.grid.processing.model.api.GdDefect
import ru.yandex.direct.grid.processing.model.api.GdValidationResult
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdMoveToMinusKeywordsPayload
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.test.utils.assertj.Conditions

private const val MUTATION_TEMPLATE_FOR_SEARCH_QUERY = """
    mutation {
      moveSearchQueryToMinusKeywords(input: {campaignId: %s, searchQuery: "%s"}) {
        movedKeywordIds
        validationResult {
          errors {
            code
          }
        }
      }
    }
    """

private const val VALID_SEARCH_QUERY = "купить слона"
private const val INVALID_SEARCH_QUERY = "1 2 3 4 5 6 7 8"

@GrutGridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class MoveSearchQueryToMinusKeywordGraphQlServiceTest {
    @Autowired
    private lateinit var steps: Steps
    @Autowired
    private lateinit var grutSteps: GrutSteps
    @Autowired
    private lateinit var keywordService: KeywordService
    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private lateinit var processor: GridGraphQLProcessor
    @Autowired
    private lateinit var campaignService: CampaignService
    @Autowired
    private lateinit var contextProvider: GridContextProvider
    @Autowired
    private lateinit var grutUacCampaignService: GrutUacCampaignService
    @Autowired
    private lateinit var minusKeywordPreparingTool: MinusKeywordPreparingTool

    private lateinit var clientInfo: ClientInfo
    private lateinit var adGroupInfo: AdGroupInfo
    private lateinit var keyword1: KeywordInfo
    private lateinit var keyword2: KeywordInfo

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        val user = clientInfo.chiefUserInfo!!.user!!
        TestAuthHelper.setDirectAuthentication(user)
        contextProvider.gridContext = ContextHelper.buildContext(user)

        grutSteps.createClient(clientInfo)

        adGroupInfo = AdGroupInfo().withClientInfo(clientInfo)
        keyword1 = steps.keywordSteps().createKeyword(adGroupInfo)
        keyword2 = steps.keywordSteps().createKeyword(adGroupInfo)
    }

    @Test
    fun moveSearchQueryToMinusKeywordsIfSameKeywordExists_mySqlCampaign() {
        processMoveSearchQueryAndAssertResponse(
            adGroupInfo.campaignId, keyword1.keyword.phrase,
            GdMoveToMinusKeywordsPayload().withMovedKeywordIds(listOf(keyword1.id))
        )

        val expectedKeywords = listOf(keyword2.keyword.phrase)
        val expectedMinusKeywords = minusKeywordPreparingTool.preprocess(listOf(keyword1.keyword.phrase))

        SoftAssertions.assertSoftly {
            assertMySqlKeywords(it, expectedKeywords, expectedMinusKeywords)
        }
    }

    @Test
    fun moveSearchQueryToMinusKeywordsIfSameKeywordDoesntExist_mySqlCampaign() {
        processMoveSearchQueryAndAssertResponse(
            adGroupInfo.campaignId, VALID_SEARCH_QUERY,
            GdMoveToMinusKeywordsPayload().withMovedKeywordIds(listOf())
        )

        val expectedKeywords = listOf(keyword1.keyword.phrase, keyword2.keyword.phrase)
        val expectedMinusKeywords = minusKeywordPreparingTool.preprocess(listOf(VALID_SEARCH_QUERY))

        SoftAssertions.assertSoftly {
            assertMySqlKeywords(it, expectedKeywords, expectedMinusKeywords)
        }
    }

    @Test
    fun invalidMinusKeyword_mySqlCampaign() {
        val defect = GdDefect()
            .withCode("MinusPhraseDefectIds.IndividualKeywordLength.MAX_COUNT_WORDS_IN_KEYWORD")

        processMoveSearchQueryAndAssertResponse(
            adGroupInfo.campaignId, INVALID_SEARCH_QUERY,
            GdMoveToMinusKeywordsPayload()
                .withMovedKeywordIds(listOf())
                .withValidationResult(GdValidationResult().withErrors(listOf(defect)))
        )

        val expectedKeywords = listOf(keyword1.keyword.phrase, keyword2.keyword.phrase)
        val expectedMinusKeywords = minusKeywordPreparingTool.preprocess(listOf())

        SoftAssertions.assertSoftly {
            assertMySqlKeywords(it, expectedKeywords, expectedMinusKeywords)
        }
    }

    @Test
    fun moveSearchQueryToMinusKeywordsIfSameKeywordExists_grutCampaign() {
        createGrutCampaign()

        processMoveSearchQueryAndAssertResponse(
            adGroupInfo.campaignId, keyword1.keyword.phrase,
            GdMoveToMinusKeywordsPayload().withMovedKeywordIds(listOf(keyword1.id))
        )

        val expectedKeywords = listOf(keyword2.keyword.phrase)
        val expectedMinusKeywords = minusKeywordPreparingTool.preprocess(listOf(keyword1.keyword.phrase))

        val campaign = grutUacCampaignService.getCampaignById(adGroupInfo.campaignId.toString())!!;
        val grutKeywords = campaign.keywords
        val grutMinusKeywords = campaign.minusKeywords

        SoftAssertions.assertSoftly {
            assertMySqlKeywords(it, expectedKeywords, expectedMinusKeywords)
            it.assertThat(grutKeywords).isEqualTo(expectedKeywords)
            it.assertThat(grutMinusKeywords).isEqualTo(listOf(keyword1.keyword.phrase))
        }
    }

    @Test
    fun invalidMinusKeyword_grutCampaign() {
        createGrutCampaign()

        val defect = GdDefect()
            .withCode("MinusPhraseDefectIds.IndividualKeywordLength.MAX_COUNT_WORDS_IN_KEYWORD")

        processMoveSearchQueryAndAssertResponse(
            adGroupInfo.campaignId, INVALID_SEARCH_QUERY,
            GdMoveToMinusKeywordsPayload()
                .withMovedKeywordIds(listOf())
                .withValidationResult(GdValidationResult().withErrors(listOf(defect)))
        )

        val expectedKeywords = listOf(keyword1.keyword.phrase, keyword2.keyword.phrase)
        val expectedMinusKeywords = minusKeywordPreparingTool.preprocess(listOf())

        val campaign = grutUacCampaignService.getCampaignById(adGroupInfo.campaignId.toString())!!;
        val grutKeywords = campaign.keywords
        val grutMinusKeywords = campaign.minusKeywords


        SoftAssertions.assertSoftly {
            assertMySqlKeywords(it, expectedKeywords, expectedMinusKeywords)
            it.assertThat(grutKeywords).isEqualTo(expectedKeywords)
            it.assertThat(grutMinusKeywords).isNull()
        }
    }

    @Test
    fun moveSearchQueryToMinusKeywordsIfSameKeywordDoesntExist_grutCampaign() {
        createGrutCampaign()

        processMoveSearchQueryAndAssertResponse(
            adGroupInfo.campaignId, VALID_SEARCH_QUERY,
            GdMoveToMinusKeywordsPayload().withMovedKeywordIds(listOf())
        )

        val expectedKeywords = listOf(keyword1.keyword.phrase, keyword2.keyword.phrase)
        val expectedMinusKeywords = minusKeywordPreparingTool.preprocess(listOf(VALID_SEARCH_QUERY))

        val campaign = grutUacCampaignService.getCampaignById(adGroupInfo.campaignId.toString())!!;
        val grutKeywords = campaign.keywords
        val grutMinusKeywords = campaign.minusKeywords


        SoftAssertions.assertSoftly {
            assertMySqlKeywords(it, expectedKeywords, expectedMinusKeywords)
            it.assertThat(grutKeywords).isEqualTo(expectedKeywords)
            it.assertThat(grutMinusKeywords).isEqualTo(listOf(VALID_SEARCH_QUERY))
        }
    }

    private fun createGrutCampaign() {
        val keywordPhrases = listOf(keyword1.keyword.phrase, keyword2.keyword.phrase)
        val uacYdbCampaign = createYdbCampaign(
            id = adGroupInfo.campaignId.toString(),
            keywords = keywordPhrases
        )
        grutSteps.createTextCampaign(clientInfo, uacYdbCampaign)
    }

    private fun processMoveSearchQueryAndAssertResponse(
        campaignId: Long, searchQuery: String, expectedPayload: GdMoveToMinusKeywordsPayload
    ) {
        val query = MUTATION_TEMPLATE_FOR_SEARCH_QUERY.format(campaignId, searchQuery)
        val result = processor.processQuery(null, query, null, contextProvider.gridContext)
        assertThat(result.errors).isEmpty()

        val data = result.getData<Map<String, Any>>()
        val payload = GraphQlJsonUtils.convertValue(
            data["moveSearchQueryToMinusKeywords"], GdMoveToMinusKeywordsPayload::class.java)
        assertThat(payload).`is`(Conditions.matchedBy(BeanDifferMatcher.beanDiffer(expectedPayload)))
    }

    private fun assertMySqlKeywords(
        softAssertions: SoftAssertions,
        expectedKeywords: List<String>,
        expectedMinusKeywords: List<String>
    ) {
        val campaignKeywords = keywordService.getKeywords(
            clientInfo.clientId!!, listOf(keyword1.id, keyword2.id)).map { it.phrase }
        val campaignMinusKeywords = campaignService.getMinusKeywordsByCampaignId(
            adGroupInfo.shard, adGroupInfo.campaignId)
        softAssertions.assertThat(campaignKeywords).containsExactlyInAnyOrderElementsOf(expectedKeywords)
        softAssertions.assertThat(campaignMinusKeywords).containsExactlyInAnyOrderElementsOf(expectedMinusKeywords)
    }
}
