package ru.yandex.direct.web.entity.uac.controller

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.direct.core.entity.uac.model.Platform
import ru.yandex.direct.core.entity.uac.service.trackingurl.AdjustTrackingUrlParser
import ru.yandex.direct.core.service.urlchecker.RedirectCheckResult
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.utils.UrlUtils
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.direct.web.entity.uac.model.ValidateTrackingUrlRequest
import ru.yandex.direct.web.entity.uac.model.ValidationType
import ru.yandex.direct.web.entity.uac.service.emptyPatchRequest

@GrutDirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacCampaignControllerTrackingValidationDisabledTest : BaseGrutCreateCampaignTest() {

    @Autowired
    private lateinit var adjustTrackingUrlParser: AdjustTrackingUrlParser

    companion object {
        private const val LANDING_TRACKING_URL = "https://app.adjust.com/a35uigk?ya_click_id={logid}" +
            "&gps_adid={google_aid}" +
            "&creative={campaign_name}" +
            "&deeplink=sbermegamarket%3A%2F%2Fhome%26utm_source%3Dyandex" +
            "%26utm_medium%3Dcpc%26utm_campaign%3D{campaign_name}"

        private const val SHORT_LANDING_TRACKING_URL = "https://app.adjust.com/a35uigk?ya_click_id=" +
            "&gps_adid=" +
            "&creative=" +
            "&deeplink=sbermegamarket%3A%2F%2Fhome%26utm_source%3Dyandex" +
            "%26utm_medium%3Dcpc%26utm_campaign%3D{campaign_name}"
    }

    @Before
    fun init() {
        whenever(bannerUrlCheckService.getRedirect(eq(SHORT_LANDING_TRACKING_URL), anyString(), anyBoolean()))
            .thenReturn(RedirectCheckResult.createFailResult())
    }

    @Test
    fun ignoreTrackingUrl_updateCampaign_featureEnabledSuccess() {
        steps.featureSteps().setCurrentClient(clientInfo.clientId!!)
        steps.featureSteps().enableClientFeature(FeatureName.IGNORE_TRACKING_URL_APP_VALIDATION)

        val urlParts = UrlUtils.laxParseUrl(LANDING_TRACKING_URL)
        val trackingUrlLanding = adjustTrackingUrlParser.parse(urlParts, Platform.ANDROID)

        val (sourceCampaignId, sourceTrackingUrl, rawResult) = updateCampaignRequest()
        val changedResult = rawResult.response.contentAsString

        val changedCampaignId = JsonUtils.fromJson(changedResult)["result"]["id"].asText()
        val changedTrackingUrl = JsonUtils.fromJson(changedResult)["result"]["tracking_url"].asText()

        SoftAssertions().apply {
            assertThat(sourceCampaignId)
                .`as`("Assert campaignId")
                .isEqualTo(changedCampaignId)

            assertThat(changedTrackingUrl)
                .`as`("Assert new trackingUrl")
                .isNotNull
                .isNotEqualTo(sourceTrackingUrl)
                .isEqualTo(trackingUrlLanding.getUrl())
        }.assertAll()
    }

    @Test
    fun ignoreTrackingUrl_updateCampaign_noFeature_badRequestExpected() {
        updateCampaignRequest(400)
    }

    @Test
    fun ignoreTrackingUrl_validateTrackingUrl_featureEnabledSuccess() {
        steps.featureSteps().setCurrentClient(clientInfo.clientId!!)
        steps.featureSteps().enableClientFeature(FeatureName.IGNORE_TRACKING_URL_APP_VALIDATION)

        val urlParts = UrlUtils.laxParseUrl(LANDING_TRACKING_URL)
        val trackingUrlLanding = adjustTrackingUrlParser.parse(urlParts, Platform.ANDROID)
        validateTrackingUrl()
            .andExpect(jsonPath("$.result.url").value(trackingUrlLanding.getUrl()))
            .andExpect(jsonPath("$.result.system").value(trackingUrlLanding.system.getSystem()))
            .andExpect(jsonPath("$.result.tracker_id").value(trackingUrlLanding.trackerId!!))
    }

    @Test
    fun ignoreTrackingUrl_validateTrackingUrl_noFeature_badRequestExpected() {
        validateTrackingUrl(400)
    }

    private fun validateTrackingUrl(code: Int = 200): ResultActions {
        val request = createUacCampaignRequest(
            listOf(grutSteps.createDefaultImageAsset(clientInfo.clientId!!))
        )
        val resultRaw = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val appId = JsonUtils.fromJson(resultRaw)["result"]["app_info"]["app_id"].asText()
        val appInfoId = JsonUtils.fromJson(resultRaw)["result"]["app_info"]["id"].asText()

        val validateRequest = ValidateTrackingUrlRequest(
            LANDING_TRACKING_URL,
            ValidationType.TRACKING_URL,
            appInfoId,
            appId
        )
        return mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/tracking_url/validate")
                .param("ulogin", clientInfo.login)
                .content(JsonUtils.toJson(validateRequest))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().`is`(code))

    }

    private fun updateCampaignRequest(code: Int = 200): Triple<String, String, MvcResult> {
        val request = createUacCampaignRequest(
            listOf(grutSteps.createDefaultImageAsset(clientInfo.clientId!!))
        )

        val resultRaw = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val sourceCampaignId = JsonUtils.fromJson(resultRaw)["result"]["id"].asText()
        val sourceTrackingUrl = JsonUtils.fromJson(resultRaw)["result"]["tracking_url"].asText()

        val patchRequest = emptyPatchRequest().copy(
            trackingUrl = LANDING_TRACKING_URL
        )

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/$sourceCampaignId")
                .param("ulogin", clientInfo.login)
                .content(JsonUtils.toJson(patchRequest))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().`is`(code))
            .andReturn()
        return Triple(sourceCampaignId, sourceTrackingUrl, result)
    }
}
