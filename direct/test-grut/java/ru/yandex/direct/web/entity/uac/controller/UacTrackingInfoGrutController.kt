package ru.yandex.direct.web.entity.uac.controller

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.core.entity.uac.converter.UacGrutCampaignConverter
import ru.yandex.direct.core.entity.uac.converter.UacGrutCampaignConverter.toEAdCampaignType
import ru.yandex.direct.core.entity.uac.createAccount
import ru.yandex.direct.core.entity.uac.createDirectCampaign
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.TargetStatus
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAccountRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toEpochSecond
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignService
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.utils.DateTimeUtils
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.grut.objects.proto.client.Schema

@GrutDirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacTrackingInfoGrutControllerTest {
    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var uacAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    private lateinit var uacYdbAccountRepository: UacYdbAccountRepository

    @Autowired
    private lateinit var grutApiService: GrutApiService

    @Autowired
    private lateinit var grutUacCampaignService: GrutUacCampaignService

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var uacCampaignRepository: UacYdbCampaignRepository

    @Autowired
    private lateinit var uacDirectCampaignRepository: UacYdbDirectCampaignRepository

    private lateinit var userInfo: UserInfo

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Before
    fun before() {
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.UC_UAC_CREATE_MOBILE_CONTENT_BRIEF_IN_GRUT_INSTEAD_OF_YDB, true)
        grutSteps.createClient(userInfo.clientInfo!!)
    }

    @Test
    fun testGet() {
        val accountId = grutUacCampaignService.getOrCreateClient(userInfo.user!!, userInfo.user!!)
        val ydbAppInfo = defaultAppInfo()
        val appInfoId = ydbAppInfo.id.toIdLong()
        val campaignToCreate = TestCampaigns.activeMobileAppCampaign(null, null)
            .withOrderId(0L)
            .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB))
        val campaign = steps.campaignSteps().createCampaign(campaignToCreate, userInfo.clientInfo)
        val campaignId = grutSteps.createMobileAppCampaign(
            clientInfo = userInfo.clientInfo!!,
            campaignId = campaign.campaignId.toIdString(),
            startedAt = null,
            accountId = accountId,
            appId = ydbAppInfo.id,
            createdAt = LocalDateTime.now().minusSeconds(111),
        ).toIdString()

        val campaignToCreate2 = TestCampaigns.activeMobileAppCampaign(null, null)
            .withOrderId(0L)
            .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB))
        val campaign2 = steps.campaignSteps().createCampaign(campaignToCreate2, userInfo.clientInfo)
        val account = createAccount(userInfo.uid, userInfo.clientId.asLong())
        val ydbCampaignToCreate = createYdbCampaign(
            appId = ydbAppInfo.id,
            startedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
            targetStatus = TargetStatus.STOPPED,
            accountId = account.id,
        )
        val ydbDirectCampaign = createDirectCampaign(id = ydbCampaignToCreate.id, directCampaignId = campaign2.campaignId)
        uacAppInfoRepository.saveAppInfo(ydbAppInfo)
        uacCampaignRepository.addCampaign(ydbCampaignToCreate)
        uacDirectCampaignRepository.saveDirectCampaign(ydbDirectCampaign)
        uacYdbAccountRepository.saveAccount(account)

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/tracking_info?app_info.id=$appInfoId&ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val soft = SoftAssertions()
        soft.assertThat(grutUacCampaignService.getAppIdsByAccountId(accountId)).isEqualTo(listOf(ydbAppInfo.id))
        val ydbCampaign = grutUacCampaignService.getCampaignById(campaignId)
        val response = JsonUtils.MAPPER.readTree(result)["result"]
        soft.assertThat(response).hasSize(2)
        soft.assertThat(response[0]["tracking_url"].asText()).isEqualTo(ydbCampaign!!.trackingUrl)
        soft.assertThat(response[0]["tracker_name"].asText()).isEqualTo("APPMETRICA")
        soft.assertThat(response[0]["last_campaign_info"]["id"].asText()).isEqualTo(ydbCampaign.id)
        soft.assertThat(response[0]["last_campaign_info"]["name"].asText()).isEqualTo(ydbCampaign.name)
        soft.assertThat(response[0]["last_campaign_info"]["created_at"].asLong()).isEqualTo(toEpochSecond(ydbCampaign.createdAt))

        soft.assertThat(response[1]["tracking_url"].asText()).isEqualTo(ydbCampaignToCreate!!.trackingUrl)
        soft.assertThat(response[1]["tracker_name"].asText()).isEqualTo("APPMETRICA")
        soft.assertThat(response[1]["last_campaign_info"]["id"].asLong()).isEqualTo(campaign2.campaignId)
        soft.assertThat(response[1]["last_campaign_info"]["name"].asText()).isEqualTo(ydbCampaignToCreate.name)
        soft.assertThat(response[1]["last_campaign_info"]["created_at"].asLong()).isEqualTo(toEpochSecond(ydbCampaignToCreate.createdAt))
        soft.assertAll()
    }

    @Test
    fun testGetWithDuplicate() {
        val campaigns: MutableList<CampaignInfo> = mutableListOf()
        for (i in listOf(1, 2, 3)) {
            val campaignToCreate = TestCampaigns.activeMobileAppCampaign(null, null)
                .withStatusModerate(StatusModerate.SENT)
            val campaign = steps.campaignSteps().createCampaign(campaignToCreate, userInfo.clientInfo)
            campaigns.add(campaign)
        }
        val account = createAccount(userInfo.uid, userInfo.clientId.asLong())
        val ydbAppInfo = defaultAppInfo()
        val appInfoId = ydbAppInfo.id.toIdLong()
        val subjectUser = userInfo.clientInfo?.chiefUserInfo?.user!!
        val ydbCampaign1 = createYdbCampaign(
            appId = ydbAppInfo.id,
            startedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
            targetStatus = TargetStatus.STOPPED,
            accountId = account.id,
            id = campaigns[0].campaignId.toString(),
            createdAt = LocalDateTime.now().minusSeconds(111),
        )
        val ydbCampaign2 = createYdbCampaign(
            appId = ydbAppInfo.id,
            startedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
            targetStatus = TargetStatus.STOPPED,
            accountId = account.id,
            id = campaigns[1].campaignId.toString(),
        )
        val ydbCampaign3 = createYdbCampaign(
            appId = ydbAppInfo.id,
            startedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
            targetStatus = TargetStatus.STOPPED,
            accountId = account.id,
            impressionUrl = "https://view.adjust.com/impression/xyz4567?ya_click_id={TRACKID}&gps_adid={GOOGLE_AID_LC}&idfa={IDFA_LC}",
            trackingUrl = "https://app.adjust.com/nnaes87?campaign={campaign_id}_{campaign_name_lat}&idfa={IDFA_UC}&gps_adid={GOOGLE_AID_LC}&campaign_id={campaign_id}&creative_id={ad_id}&publisher_id={gbid}&ya_click_id={TRACKID}",
            id = campaigns[2].campaignId.toString(),
        )
        uacAppInfoRepository.saveAppInfo(ydbAppInfo)
        uacYdbAccountRepository.saveAccount(account)
        for (campaign in listOf(ydbCampaign1, ydbCampaign2, ydbCampaign3)) {
            grutApiService.briefGrutApi.createBrief(Schema.TCampaign.newBuilder().apply {
                meta = Schema.TCampaignMeta.newBuilder().apply {
                    id = campaign.id.toIdLong()
                    campaignType = campaign.advType.toEAdCampaignType()
                    clientId = subjectUser.clientId.asLong()
                    creationTime = campaign.createdAt.atZone(DateTimeUtils.MSK).toEpochSecond() * 1_000
                }.build()
                spec = UacGrutCampaignConverter.toCampaignSpec(campaign)
            }.build())
        }
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/tracking_info?app_info.id=$appInfoId&ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val response = JsonUtils.MAPPER.readTree(result)["result"]
        val soft = SoftAssertions()
        soft.assertThat(response).hasSize(2)
        soft.assertThat(response[0]["tracking_url"].asText()).isEqualTo(ydbCampaign1.trackingUrl)
        soft.assertThat(response[0]["tracker_name"].asText()).isEqualTo("APPMETRICA")
        soft.assertThat(response[0]["last_campaign_info"]["id"].asLong()).isEqualTo(campaigns[0].campaignId)
        soft.assertThat(response[0]["last_campaign_info"]["name"].asText()).isEqualTo(ydbCampaign1.name)
        soft.assertThat(response[0]["last_campaign_info"]["created_at"].asLong()).isEqualTo(toEpochSecond(ydbCampaign1.createdAt))

        soft.assertThat(response[1]["tracking_url"].asText()).isEqualTo(ydbCampaign3.trackingUrl)
        soft.assertThat(response[1]["impression_url"].asText()).isEqualTo(ydbCampaign3.impressionUrl)
        soft.assertThat(response[1]["tracker_name"].asText()).isEqualTo("ADJUST")
        soft.assertThat(response[1]["last_campaign_info"]["id"].asLong()).isEqualTo(campaigns[2].campaignId)
        soft.assertThat(response[1]["last_campaign_info"]["name"].asText()).isEqualTo(ydbCampaign3.name)
        soft.assertThat(response[1]["last_campaign_info"]["created_at"].asLong()).isEqualTo(toEpochSecond(ydbCampaign3.createdAt))
        soft.assertAll()
    }
}
