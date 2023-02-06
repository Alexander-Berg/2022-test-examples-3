package ru.yandex.direct.web.entity.uac.controller

import com.fasterxml.jackson.databind.JsonNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.direct.core.entity.uac.model.Content
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.UacCampaignSteps
import ru.yandex.direct.core.testing.stub.CanvasClientStub
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.utils.fromJson
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.direct.web.entity.uac.model.CreateContentRequest

@GrutDirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacContentGrutCreateVideoControllerTest : UacContentCreateVideoControllerTestBase() {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var uacCampaignSteps: UacCampaignSteps

    @Autowired
    private lateinit var grutApiService: GrutApiService

    override fun checkDbContentExists(id: String) {
        assertThat(grutApiService.assetGrutApi.getAsset(id.toIdLong())).isNotNull
    }

    @Before
    fun grutBefore() {
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.UC_UAC_CREATE_BRIEF_IN_GRUT_INSTEAD_OF_YDB, true)
    }

    @Test
    fun testCampaignIsInYdb() {
        val campaignInfo = uacCampaignSteps.createMobileAppCampaign(userInfo.clientInfo!!)

        val sourceUrl = "https://example.com/somevideo.mp4"
        (canvasClient as CanvasClientStub).addCustomVideoUploadResponseWithUrl(videoUploadResponse, sourceUrl)
        val contentRequest = CreateContentRequest(
            type = ru.yandex.direct.core.entity.uac.model.MediaType.VIDEO,
            sourceUrl = sourceUrl,
            mdsUrl = null,
            thumb = null,
        )

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/content")
                .content(JsonUtils.toJson(contentRequest))
                .contentType(MediaType.APPLICATION_JSON)
                .param("ulogin", userInfo.clientInfo!!.login)
                .param("campaign_id", "${campaignInfo.campaign.campaignId}")
                .param("creative_type", "non_skippable_cpm")
        )

        val resultContent = result.andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val resultResult: JsonNode = JsonUtils.MAPPER.readTree(resultContent)["result"]
        val content: Content = fromJson(resultResult.toString())

        checkCommonContentFields(content)
        content.meta.checkEquals(contentMetaNonSkippableExpected)
        content.sourceUrl.checkEquals(sourceUrl)

        val contents = contentRepository.getContents(listOf(content.id))
        assertThat(contents).isNotEmpty
        contents.first { it.id == content.id }.meta["creative_type"].toString().checkEquals("non_skippable_cpm")
    }
}
