package ru.yandex.direct.web.entity.uac.controller

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import java.time.LocalDateTime
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.core.entity.uac.model.CampaignContentStatus
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.model.campaign_content.AssetStat
import ru.yandex.direct.core.entity.uac.repository.stat.AssetStatRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.uac.UacCampaignSteps
import ru.yandex.direct.model.KtModelChanges
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.uac.model.UacContentStatisticsResponse

open class UacCampaignControllerContentStatisticsBaseTest {

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    lateinit var uacCampaignSteps: UacCampaignSteps

    @Autowired
    lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    lateinit var assetStatRepository: AssetStatRepository

    @Autowired
    private lateinit var uacYdbCampaignRepository: UacYdbCampaignRepository

    lateinit var mockMvc: MockMvc
    lateinit var clientInfo: ClientInfo
    lateinit var uacCampaignInfo: UacCampaignSteps.UacCampaignInfo
    lateinit var titleAsset: UacYdbCampaignContent
    lateinit var textAsset: UacYdbCampaignContent
    lateinit var deletedTextAsset: UacYdbCampaignContent

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        clientInfo = testAuthHelper.createDefaultUser().clientInfo!!
        testAuthHelper.setOperatorAndSubjectUser(clientInfo.uid)
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )

        uacCampaignInfo = uacCampaignSteps.createTextCampaign(clientInfo)
        val assets = uacYdbCampaignContentRepository.getCampaignContents(uacCampaignInfo.uacCampaign.id)
        titleAsset = assets.first { it.type == MediaType.TITLE }
        textAsset = assets.first { it.type == MediaType.TEXT && it.status != CampaignContentStatus.DELETED }
        deletedTextAsset = assets.first { it.type == MediaType.TEXT && it.status == CampaignContentStatus.DELETED }

        MockitoAnnotations.openMocks(this)
    }

    fun getMockedDataFromStatTable(
        from: Long,
        to: Long,
        assetStats: List<AssetStat>,
    ) {
        whenever(assetStatRepository.getStatsByAssetIds(
            eq(setOf(titleAsset.id, textAsset.id, deletedTextAsset.id)),
            eq(uacCampaignInfo.campaign.campaign.orderId),
            // Статистика ищется начиная с from или даты создания кампании (выбирается позднее)
            eq(from),
            eq(to),
        )).thenReturn(assetStats)
    }

    fun sendAndGetStatisticResult(
        from: Long,
        to: Long,
    ): UacContentStatisticsResponse {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get(
                    "/uac/campaign/${uacCampaignInfo.campaign.campaignId}/content_statistics" +
                        "?ulogin=${clientInfo.login}" +
                        "&from=${from}" +
                        "&to=${to}")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        return JsonUtils.fromJson(result, UacContentStatisticsResponse::class.java)
    }

    fun setCreateAtToCampaign(createAt: LocalDateTime) {
        val campaignModelChanges = KtModelChanges<String, UacYdbCampaign>(uacCampaignInfo.uacCampaign.id)
        campaignModelChanges.process(UacYdbCampaign::createdAt, createAt)
        uacYdbCampaignRepository.update(campaignModelChanges)
    }
}
