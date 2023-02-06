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
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.direct.core.entity.uac.converter.UacGrutAdGroupBriefConverter.toAdGroupBriefGrutModel
import ru.yandex.direct.core.entity.uac.converter.UacGrutCampaignConverter.toUacYdbCampaign
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatch
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatchCategory
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbContent
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignService
import ru.yandex.direct.core.entity.uac.service.GrutUacContentService
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps.GrutCampaignInfo
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.feature.FeatureName.DISABLE_VIDEO_CREATIVE
import ru.yandex.direct.feature.FeatureName.RELEVANCE_MATCH_CATEGORIES_ALLOWED_IN_UC
import ru.yandex.direct.test.utils.checkNotNull
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.validation.result.DefectIds
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.direct.web.entity.uac.model.PatchCampaignRequest

@GrutDirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacCampaignGrutControllerUpdateUcTest : UacCampaignControllerUpdateUcTestBase() {

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var grutUacContentService: GrutUacContentService

    @Autowired
    private lateinit var uacCampaignService: GrutUacCampaignService

    @Autowired
    private lateinit var grutApiService: GrutApiService

    private lateinit var grutCampaignInfo: GrutCampaignInfo

    override fun saveContents(contents: List<UacYdbContent>) {
        grutUacContentService.insertContents(contents)
    }

    @Before
    fun grutBefore() {
        grutSteps.createClient(clientInfo)
        uacEcomCampaignId = grutSteps.createEcomUcCampaign(clientInfo).first.toIdString()
        grutCampaignInfo = grutSteps.createAndGetTextCampaign(clientInfo)
        uacCampaignId = grutCampaignInfo.uacCampaign.id

        uacEcomNewBackendCampaignId = grutSteps.createTextCampaign(clientInfo = clientInfo, isEcom = true).toIdString()

        createContents()
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
     * Проверяем, что UC кампания обновляется с автотаргетингом
     */
    @Test
    @TestCaseName("Active {0} categories {1}")
    @Parameters(method = "casesForAutotargetingCategories")
    fun `update uc campaign with autotargeting`(
        active: Boolean,
        categories: List<UacRelevanceMatchCategory>,
    ) {
        steps.featureSteps().addClientFeature(clientInfo.clientId, RELEVANCE_MATCH_CATEGORIES_ALLOWED_IN_UC, true)

        val relevanceMatch = UacRelevanceMatch(
            active = active,
            categories = categories.toSet(),
        )

        val request = updateCampaignRequest(relevanceMatch = relevanceMatch)
        val directCampaignId = doSuccessRequest(request)
        val ucCampaign = uacCampaignService.getCampaignById(directCampaignId.toString())

        Assertions.assertThat(ucCampaign!!.relevanceMatch)
            .`as`("автотаргетинг")
            .isEqualTo(relevanceMatch)
    }

    /**
     * Проверяем, что UC кампания не обновляется с автотаргетингом без фичи
     */
    @Test
    fun `update uc campaign with autotargeting and without feature`() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, RELEVANCE_MATCH_CATEGORIES_ALLOWED_IN_UC, false)

        val relevanceMatch = UacRelevanceMatch(
            active = true,
            categories = setOf(UacRelevanceMatchCategory.EXACT_MARK),
        )

        val request = updateCampaignRequest(relevanceMatch = relevanceMatch)
        checkBadRequest(
            request,
            path = path(field(PatchCampaignRequest::relevanceMatch)),
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
     * Проверяем, что UC кампания не обновляется с автотаргетингом и без EXACT_MARK категории
     */
    @Test
    @TestCaseName("Categories {0}")
    @Parameters(method = "casesForAutotargetingCategoriesWithoutExtra")
    fun `update uc campaign with autotargeting and without exact_mark category`(
        categories: List<UacRelevanceMatchCategory>,
    ) {
        steps.featureSteps().addClientFeature(clientInfo.clientId, RELEVANCE_MATCH_CATEGORIES_ALLOWED_IN_UC, true)

        val relevanceMatch = UacRelevanceMatch(
            active = true,
            categories = categories.toSet(),
        )

        val request = updateCampaignRequest(relevanceMatch = relevanceMatch)
        checkBadRequest(
            request,
            path = path(field(PatchCampaignRequest::relevanceMatch)),
            defectId = DefectIds.INVALID_VALUE
        )
    }

    fun casesForShowTitleAndBody() = arrayOf(arrayOf(true), arrayOf(false))

    @Test
    @TestCaseName("Show title and body {0}")
    @Parameters(method = "casesForShowTitleAndBody")
    fun `update uc campaign with show title and body`(
        showTitleAndBody: Boolean,
    ) {
        steps.featureSteps().addClientFeature(clientInfo.clientId, DISABLE_VIDEO_CREATIVE, true)
        val request = updateCampaignRequest(showTitleAndBody = showTitleAndBody)

        val directCampaignId = doSuccessRequest(request)
        val ucCampaign = uacCampaignService.getCampaignById(directCampaignId.toString())

        Assertions.assertThat(ucCampaign!!.showTitleAndBody)
            .`as`("флаг показа заголовков и текстов")
            .isEqualTo(showTitleAndBody)
    }

    @Test
    fun `update uc campaign with creating ad group brief`() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.UAC_MULTIPLE_AD_GROUPS_ENABLED, true)
        val adGroupId = steps.adGroupSteps().createActiveTextAdGroup(grutCampaignInfo.campaignInfo).adGroupId

        val request = updateCampaignRequest()
        val campaignId = doSuccessRequest(request)
        val adGroupBriefs = grutApiService.adGroupBriefGrutApi.selectAdGroupBriefsByCampaignId(campaignId)
        Assertions.assertThat(adGroupBriefs.size).`as`("Ожидается одна заявка на группу").isEqualTo(1)
        val adGroupBrief = adGroupBriefs[0]

        val campaignBrief = grutApiService.briefGrutApi.getBrief(campaignId)
        val campaign = campaignBrief!!.toUacYdbCampaign()
        val expectedAdGroupBrief = toAdGroupBriefGrutModel(
            campaignId,
            campaign,
            id = adGroupBrief.id,
            adGroupIds = listOf(adGroupId)
        )
        Assertions.assertThat(adGroupBrief).isEqualTo(expectedAdGroupBrief)
    }

    @Test
    fun `update uc campaign with updating ad group brief`() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.UAC_MULTIPLE_AD_GROUPS_ENABLED, true)
        grutApiService.adGroupBriefGrutApi.createAdGroupBriefs(
            listOf(toAdGroupBriefGrutModel(uacCampaignId.toIdLong(), grutCampaignInfo.uacCampaign)))

        val request = updateCampaignRequest()
        val campaignId = doSuccessRequest(request)
        val adGroupBriefs = grutApiService.adGroupBriefGrutApi.selectAdGroupBriefsByCampaignId(campaignId)
        Assertions.assertThat(adGroupBriefs.size).`as`("Ожидается одна заявка на группу").isEqualTo(1)
        val adGroupBrief = adGroupBriefs[0]

        val campaignBrief = grutApiService.briefGrutApi.getBrief(campaignId)
        val campaign = campaignBrief!!.toUacYdbCampaign()
        val expectedAdGroupBrief = toAdGroupBriefGrutModel(campaignId, campaign, id = adGroupBrief.id)
        Assertions.assertThat(adGroupBrief).isEqualTo(expectedAdGroupBrief)
    }

    override fun getEcomCampaign(): UacYdbCampaign {
        grutApiService.briefGrutApi.getBrief(uacEcomCampaignId.toIdLong())
        val campaignResponse = grutApiService.briefGrutApi.getBrief(uacEcomCampaignId.toIdLong())
        campaignResponse.checkNotNull()
        return campaignResponse!!.toUacYdbCampaign()
    }

    override fun doSuccessRequest(
        request: PatchCampaignRequest,
        uacCampaignId: String,
        expectTitleContentsCount: Int,
        expectTextContentsCount: Int,
        expectImageContentsCount: Int,
        expectVideoContentsCount: Int,
    ): Long {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/${uacCampaignId}?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andDo { System.err.println(it.response.contentAsString) }
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        val campaignId = resultJsonTree["result"]["id"].asText()
        val directCampaignId = campaignId.toIdLong()
        val campaignResponse = grutApiService.briefGrutApi.getBrief(directCampaignId)
        campaignResponse.checkNotNull()
        val campaign = campaignResponse!!.toUacYdbCampaign()

        val campaignContents = grutUacContentService.getCampaignContents(campaign)
        val titleContents = campaignContents
            .filter { it.type == ru.yandex.direct.core.entity.uac.model.MediaType.TITLE }
            .filter { it.removedAt == null }
        val textContents = campaignContents
            .filter { it.type == ru.yandex.direct.core.entity.uac.model.MediaType.TEXT }
            .filter { it.removedAt == null }
        val imageContents = campaignContents
            .filter { it.type == ru.yandex.direct.core.entity.uac.model.MediaType.IMAGE }
            .filter { it.removedAt == null }
        val videoContents = campaignContents
            .filter { it.type == ru.yandex.direct.core.entity.uac.model.MediaType.VIDEO }
            .filter { it.removedAt == null }

        SoftAssertions.assertSoftly {
            it.assertThat(titleContents.size)
                .`as`("Количество title ассетов")
                .isEqualTo(expectTitleContentsCount)
            it.assertThat(textContents.size)
                .`as`("Количество text ассетов")
                .isEqualTo(expectTextContentsCount)
            it.assertThat(imageContents.size)
                .`as`("Количество image ассетов")
                .isEqualTo(expectImageContentsCount)
            it.assertThat(videoContents.size)
                .`as`("Количество video ассетов")
                .isEqualTo(expectVideoContentsCount)
        }
        return directCampaignId
    }
}
