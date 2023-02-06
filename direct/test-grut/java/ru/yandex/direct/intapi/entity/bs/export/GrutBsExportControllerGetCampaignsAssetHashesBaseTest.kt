package ru.yandex.direct.intapi.entity.bs.export

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import ru.yandex.direct.core.entity.uac.createAdGroupBriefGrutModel
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.core.grut.api.AdGroupBriefGrutModel
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.UserSteps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.grut.objects.proto.Banner


open class GrutBsExportControllerGetCampaignsAssetHashesBaseTest {

    @Autowired
    lateinit var bsExportController: BsExportController

    @Autowired
    lateinit var grutSteps: GrutSteps

    @Autowired
    lateinit var userSteps: UserSteps

    @Autowired
    lateinit var grutApiService: GrutApiService

    @Autowired
    lateinit var steps: Steps

    lateinit var mockMvc: MockMvc
    lateinit var userInfo: UserInfo

    fun init(uacMultipleAdGroupsEnabled: Boolean) {
        mockMvc = MockMvcBuilders.standaloneSetup(bsExportController).build()
        userInfo = userSteps.createDefaultUser()
        grutSteps.createClient(userInfo.clientInfo!!)

        steps.featureSteps()
            .addClientFeature(userInfo.clientId, FeatureName.UAC_MULTIPLE_AD_GROUPS_ENABLED, uacMultipleAdGroupsEnabled)
    }

    fun performRequest(campaignIds: Collection<Long>): Map<String, Any> {
        val result = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/bsexport/get-campaigns-asset-hashes")
                    .content(campaignIds.toString())
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        return JsonUtils.fromJson(result, object : TypeReference<Map<String, Any>>() {})
    }


    fun createAsset(mediaType: ru.yandex.grut.objects.proto.MediaType.EMediaType): String {
        return when (mediaType) {
            ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_TITLE -> grutSteps.createTitleAsset(userInfo.clientId)
            ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_TEXT -> grutSteps.createDefaultTextAsset(userInfo.clientId)
            ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_IMAGE -> grutSteps.createDefaultImageAsset(userInfo.clientId)
            ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_VIDEO -> grutSteps.createDefaultVideoAsset(userInfo.clientId)
            ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_HTML5 -> grutSteps.createDefaultHtml5Asset(userInfo.clientId)
            else -> throw IllegalStateException("Unsupported asset type")
        }
    }

    fun createBanner(
        campaignId: Long,
        adGroupId: Long,
        assetIds: List<String>,
        assetLinkIds: List<String>? = null,
        status: Banner.TBannerSpec.EBannerStatus = Banner.TBannerSpec.EBannerStatus.BSS_CREATED,
    ): Long {
        return grutSteps.createBanner(
            campaignId = campaignId,
            adGroupId = adGroupId,
            assetIds = assetIds,
            assetLinkIds = assetLinkIds,
            status = status
        )
    }

    fun createAdGroupBrief(
        campaignId: Long,
        assetIds: List<String>,
    ): AdGroupBriefGrutModel {
        val uacAssets = assetIds
            .map {
                createCampaignContent(
                    id = it,
                    campaignId = campaignId.toIdString(),
                    contentId = it
                )
            }
        return createAdGroupBriefWithCustomAssets(campaignId, uacAssets)
    }

    fun createAdGroupBriefWithCustomAssets(
        campaignId: Long,
        uacAssets: List<UacYdbCampaignContent>,
    ): AdGroupBriefGrutModel {
        val adGroupBriefIds = grutApiService.adGroupBriefGrutApi.createAdGroupBriefs(
            listOf(
                createAdGroupBriefGrutModel(
                    campaignId,
                    assetLinks = uacAssets,
                )
            )
        )
        Assertions.assertThat(adGroupBriefIds)
            .`as`("Групповая заявка создана в груте")
            .isNotEmpty
        return grutApiService.adGroupBriefGrutApi.getAdGroupBrief(adGroupBriefIds[0])!!
    }

    fun setAssetIdsToCampaign(campaignId: Long, assetIds: List<String>) {
        grutSteps.setAssetLinksToCampaign(campaignId, assetIds)
    }
}
