package ru.yandex.direct.web.entity.adgeneration.controller

import java.time.LocalDateTime.now
import java.util.stream.Collectors.toList
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.bangenproxy.client.BanGenProxyClient
import ru.yandex.direct.bangenproxy.client.model.TextInfoCombinatorics
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.model.MediaType.TEXT
import ru.yandex.direct.core.entity.uac.model.MediaType.TITLE
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.core.entity.uac.service.GrutUacClientService
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.campaign.CampAdditionalDataSteps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.core.testing.steps.uac.UacCampaignSteps
import ru.yandex.direct.test.utils.checkContains
import ru.yandex.direct.utils.fromJson
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.adgeneration.model.response.WebGenerateTextSuggestionsResult

@GrutDirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class AdGenerationControllerTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var uacCampaignSteps: UacCampaignSteps

    @Autowired
    private lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    private lateinit var campAdditionalDataSteps: CampAdditionalDataSteps

    @Autowired
    private lateinit var grutUacClientService: GrutUacClientService

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var banGenProxyClient: BanGenProxyClient

    private lateinit var mockMvc: MockMvc
    private lateinit var userInfo: UserInfo
    private lateinit var campaignInfo: CampaignInfo
    private var directCampaignId: Long = 0

    private val titleAsset = "title asset"
    private val textAsset = "text asset"
    private val titleAsset2 = "title asset 2"
    private val textAsset2 = "text asset 2"

    @Before
    fun before() {
        MockitoAnnotations.openMocks(this)
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(directWebAuthenticationSource.authentication)

        val user = userInfo.clientInfo?.chiefUserInfo?.user!!
        grutUacClientService.getOrCreateClient(user, user)
        campaignInfo = steps.campaignSteps().createActiveCampaign(userInfo.clientInfo)
        directCampaignId = campaignInfo.campaignId
    }

    @Test
    fun testCampaignInGrut() {
        val titleAsset = "title asset"
        val textAsset = "text asset"
        val titleAsset2 = "title asset 2"
        val textAsset2 = "text asset 2"

        val titleAssetId = grutSteps.createTitleAsset(userInfo.clientId, titleAsset)
        val textAssetId = grutSteps.createTextAsset(userInfo.clientId, textAsset)
        val titleAssetId2 = grutSteps.createTitleAsset(userInfo.clientId, titleAsset2)
        val textAssetId2 = grutSteps.createTextAsset(userInfo.clientId, textAsset2)

        val uacCampaign = createYdbCampaign(
            id = directCampaignId.toString(),
            accountId = userInfo.clientId.toString(),
            assetLinks = listOf(
                createCampaignContent(id = titleAssetId, campaignId = directCampaignId.toString(), contentId = titleAssetId),
                createCampaignContent(id = textAssetId, campaignId = directCampaignId.toString(), contentId = textAssetId),
                createCampaignContent(id = titleAssetId2, campaignId = directCampaignId.toString(), contentId = titleAssetId2, removedAt = now()),
                createCampaignContent(id = textAssetId2, campaignId = directCampaignId.toString(), contentId = textAssetId2, removedAt = now()),
            ),
        )

        grutSteps.createTextCampaign(userInfo.clientInfo!!, uacCampaign)

        mockRequestAndCheck()
    }

    @Test
    fun testCampaignInUacYdb_new() {
        val uacCampaignInfo = uacCampaignSteps.createTextCampaign(userInfo.clientInfo!!, campaignInfo.campaign)

        val contentIds = uacCampaignInfo.contents.stream().map(UacYdbCampaignContent::id).collect(toList())
        uacYdbCampaignContentRepository.delete(contentIds)

        val uacYdbCampaignId = uacCampaignInfo.uacCampaign.id
        uacYdbCampaignContentRepository.addCampaignContents(listOf(
            createCampaignContent(campaignId = uacYdbCampaignId, type = TITLE, text = titleAsset),
            createCampaignContent(campaignId = uacYdbCampaignId, type = TITLE, text = titleAsset2, removedAt = now()),
            createCampaignContent(campaignId = uacYdbCampaignId, type = TEXT, text = textAsset),
            createCampaignContent(campaignId = uacYdbCampaignId, type = TEXT, text = textAsset2, removedAt = now())))

        mockRequestAndCheck()
    }

    private fun mockRequestAndCheck(
        url: String = "https://ya.ru"
    ) {
        doReturn(TextInfoCombinatorics("", "", "", listOf(titleAsset2), listOf(textAsset2)))
            .`when`(banGenProxyClient).getUrlInfoForCombinatorics(eq(url), eq(null), eq(listOf(titleAsset)), eq(listOf(textAsset)))
        campAdditionalDataSteps.addHref(campaignInfo.shard, campaignInfo.campaignId, url)
        val response = makeRequestAndGetResponse(directCampaignId)
        response.titles!!.checkContains(titleAsset2)
        response.bodies!!.checkContains(textAsset2)
    }

    private fun makeRequestAndGetResponse(
        campaignId: Long,
        url: String = "",
    ): WebGenerateTextSuggestionsResult {
        val responseRaw = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/ad_generation/ad_generation/generate_text_suggestions?" +
                    "ulogin=${userInfo.clientInfo!!.login}&url=$url&campaignId=$campaignId")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        return fromJson(responseRaw)
    }
}
