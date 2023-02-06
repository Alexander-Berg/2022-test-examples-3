package ru.yandex.direct.web.entity.uac.controller

import com.fasterxml.jackson.core.type.TypeReference
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
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.direct.web.entity.uac.model.CreateContentRequest

@GrutDirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacContentGrutCreateImageControllerTest : UacContentCreateImageControllerTestBase() {

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
    fun testUploadByUrlCampaignInYdb() {
        val campaignInfo = uacCampaignSteps.createMobileAppCampaign(userInfo.clientInfo!!)

        mockUploadAvatarsImageByUrl()

        val contentRequest = CreateContentRequest(
            type = ru.yandex.direct.core.entity.uac.model.MediaType.IMAGE,
            sourceUrl = "https://example.com/someimage.jpeg",
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
        )

        val resultContent = result.andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val resultResult: JsonNode = JsonUtils.MAPPER.readTree(resultContent)["result"]
        val content: Content = JsonUtils.fromJson(JsonUtils.toJson(resultResult), object : TypeReference<Content>() {})

        checkCommonContentFields(content)
        content.sourceUrl.checkEquals("https://example.com/someimage.jpeg")
        content.filename.checkEquals(null)

        assertThat(contentRepository.getContents(listOf(content.id))).isNotEmpty
    }
}
