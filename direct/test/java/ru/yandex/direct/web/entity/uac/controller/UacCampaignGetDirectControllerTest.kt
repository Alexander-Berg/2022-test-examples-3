package ru.yandex.direct.web.entity.uac.controller

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AdGroupStatesEnum
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.AggregatedStatusCampaignData
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.CampaignCounters
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.CampaignStatesEnum
import ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes
import ru.yandex.direct.core.entity.uac.UacErrorResponse
import ru.yandex.direct.core.entity.uac.createDirectCampaign
import ru.yandex.direct.core.entity.uac.createTextCampaignContent
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.TargetStatus
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectCampaignRepository
import ru.yandex.direct.core.entity.uac.service.UacCampaignService
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate
import ru.yandex.direct.dbschema.ppc.Tables.AGGR_STATUSES_CAMPAIGNS
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.utils.fromJson
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.uac.notOwnerResponse
import ru.yandex.direct.web.entity.uac.service.YdbUacCampaignWebService

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacCampaignGetDirectControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var ydbUacCampaignWebService: YdbUacCampaignWebService

    @Autowired
    private lateinit var uacCampaignService: UacCampaignService

    @Autowired
    private lateinit var uacCampaignRepository: UacYdbCampaignRepository

    @Autowired
    private lateinit var uacDirectCampaignRepository: UacYdbDirectCampaignRepository

    @Autowired
    private lateinit var uacCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    private lateinit var uacAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    private lateinit var userInfo: UserInfo

    @Before
    fun before() {
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
        // нужно для проверки регионов, так как из ручки возвращается регион в локали en
        LocaleContextHolder.setLocale(Locale.ENGLISH)
        steps.dbQueueSteps().registerJobType(DbQueueJobTypes.RECALC_BRAND_LIFT_CAMPAIGNS)
    }

    @After
    fun after() {
        LocaleContextHolder.setLocale(null)
    }

    @Test
    fun getDirectCampaignSuccessfulTest() {
        val campaignToCreate = TestCampaigns.activeMobileAppCampaign(null, null)
            .withStatusModerate(StatusModerate.SENT)
        val campaign = steps.campaignSteps().createCampaign(campaignToCreate, userInfo.clientInfo)
        val ydbAppInfo = defaultAppInfo()
        val ydbCampaign = createYdbCampaign(
            appId = ydbAppInfo.id,
            startedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
            targetStatus = TargetStatus.STOPPED,
        )
        val ydbDirectCampaign = createDirectCampaign(id = ydbCampaign.id, directCampaignId = campaign.campaignId)
        val campaignContent = createTextCampaignContent(campaignId = ydbCampaign.id)
        uacAppInfoRepository.saveAppInfo(ydbAppInfo)
        uacCampaignRepository.addCampaign(ydbCampaign)
        uacCampaignContentRepository.addCampaignContents(listOf(campaignContent))
        uacDirectCampaignRepository.saveDirectCampaign(ydbDirectCampaign)


        val aggrData = AggregatedStatusCampaignData(
            listOf(CampaignStatesEnum.PAYED, CampaignStatesEnum.DOMAIN_MONITORED),
            CampaignCounters(1, mapOf(GdSelfStatusEnum.RUN_WARN to 1), mapOf(AdGroupStatesEnum.BS_RARELY_SERVED to 1)),
            GdSelfStatusEnum.RUN_WARN,
            GdSelfStatusReason.CAMPAIGN_HAS_ADGROUPS_WITH_WARNINGS,
        )
        dslContextProvider.ppc(campaign.shard)
            .insertInto(AGGR_STATUSES_CAMPAIGNS, AGGR_STATUSES_CAMPAIGNS.CID, AGGR_STATUSES_CAMPAIGNS.AGGR_DATA)
            .values(campaign.campaignId, JsonUtils.toJson(aggrData))
            .execute()

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/campaign/direct/${campaign.campaignId}?ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val gotYdbCampaign = uacCampaignRepository.getCampaign(ydbCampaign.id)
        assertThat(gotYdbCampaign).isNotNull
        assertThat(gotYdbCampaign).isEqualTo(ydbCampaign)
        val statuses = uacCampaignService.getCampaignStatuses(userInfo.clientId, ydbDirectCampaign.directCampaignId,
            gotYdbCampaign!!)

        val filledCampaign = ydbUacCampaignWebService
            .fillCampaign(
                operator = userInfo.clientInfo?.chiefUserInfo?.user!!,
                subjectUser = userInfo.clientInfo?.chiefUserInfo?.user!!,
                ydbCampaign, ydbDirectCampaign.directCampaignId,
                campaignStatuses = statuses!!,
            )
        assertThat(JsonUtils.MAPPER.readTree(result)["result"]).isEqualTo(
            JsonUtils.MAPPER.readTree(
                JsonUtils.toJson(
                    filledCampaign
                )
            )
        )
    }

    @Test
    fun getDirectCampaign_updateStatusesWithEmptyStateReasonsTest() {
        val campaignToCreate = TestCampaigns.activeMobileAppCampaign(null, null)
            .withStatusModerate(StatusModerate.SENT)
        val campaign = steps.campaignSteps().createCampaign(campaignToCreate, userInfo.clientInfo)
        val ydbAppInfo = defaultAppInfo()
        val ydbCampaign = createYdbCampaign(
            appId = ydbAppInfo.id,
            startedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
            targetStatus = TargetStatus.STOPPED,
        )
        val ydbDirectCampaign = createDirectCampaign(id = ydbCampaign.id, directCampaignId = campaign.campaignId)
        val campaignContent = createTextCampaignContent(campaignId = ydbCampaign.id)
        uacAppInfoRepository.saveAppInfo(ydbAppInfo)
        uacCampaignRepository.addCampaign(ydbCampaign)
        uacCampaignContentRepository.addCampaignContents(listOf(campaignContent))
        uacDirectCampaignRepository.saveDirectCampaign(ydbDirectCampaign)


        val aggrData = AggregatedStatusCampaignData(
            listOf(CampaignStatesEnum.PAYED, CampaignStatesEnum.DOMAIN_MONITORED),
            CampaignCounters(1, mapOf(GdSelfStatusEnum.RUN_WARN to 1), mapOf(AdGroupStatesEnum.BS_RARELY_SERVED to 1)),
            GdSelfStatusEnum.RUN_WARN,
            listOf(),
        )
        dslContextProvider.ppc(campaign.shard)
            .insertInto(AGGR_STATUSES_CAMPAIGNS, AGGR_STATUSES_CAMPAIGNS.CID, AGGR_STATUSES_CAMPAIGNS.AGGR_DATA)
            .values(campaign.campaignId, JsonUtils.toJson(aggrData))
            .execute()

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/campaign/direct/${campaign.campaignId}?ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val gotYdbCampaign = uacCampaignRepository.getCampaign(ydbCampaign.id)
        assertThat(gotYdbCampaign).isNotNull
        assertThat(gotYdbCampaign).isEqualTo(ydbCampaign)

        val statuses = uacCampaignService.getCampaignStatuses(userInfo.clientId, ydbDirectCampaign.directCampaignId,
            gotYdbCampaign!!)
        val filledCampaign = ydbUacCampaignWebService
            .fillCampaign(
                operator = userInfo.clientInfo?.chiefUserInfo?.user!!,
                subjectUser = userInfo.clientInfo?.chiefUserInfo?.user!!,
                ydbCampaign, ydbDirectCampaign.directCampaignId,
                campaignStatuses = statuses!!,
            )
        assertThat(JsonUtils.MAPPER.readTree(result)["result"]).isEqualTo(
            JsonUtils.MAPPER.readTree(
                JsonUtils.toJson(
                    filledCampaign
                )
            )
        )
    }

    @Test
    fun getDirectCampaignNoRightsTest() {
        val anotherUserInfo = testAuthHelper.createDefaultUser()
        val campaignToCreate = TestCampaigns.activeMobileAppCampaign(null, null)
            .withStatusModerate(StatusModerate.SENT)
        val campaign = steps.campaignSteps().createCampaign(campaignToCreate, userInfo.clientInfo)
        val ydbAppInfo = defaultAppInfo()
        val ydbCampaign = createYdbCampaign(
            appId = ydbAppInfo.id,
            startedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
            targetStatus = TargetStatus.STOPPED,
        )
        val ydbDirectCampaign = createDirectCampaign(id = ydbCampaign.id, directCampaignId = campaign.campaignId)
        val campaignContent = createTextCampaignContent(campaignId = ydbCampaign.id)
        uacAppInfoRepository.saveAppInfo(ydbAppInfo)
        uacCampaignRepository.addCampaign(ydbCampaign)
        uacCampaignContentRepository.addCampaignContents(listOf(campaignContent))
        uacDirectCampaignRepository.saveDirectCampaign(ydbDirectCampaign)

        val resultRaw = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/campaign/direct/${campaign.campaignId}?ulogin=" + anotherUserInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isForbidden)
            .andReturn()
            .response
            .contentAsString
        val uacErrorResponse = fromJson<UacErrorResponse>(resultRaw)
        uacErrorResponse.checkEquals(fromJson(notOwnerResponse().body as String))

        val gotYdbCampaign = uacCampaignRepository.getCampaign(ydbCampaign.id)
        assertThat(gotYdbCampaign).isNotNull
        // проверяем, что ничего, кроме статусов, не изменилось
        assertThat(gotYdbCampaign)
            .isEqualTo(ydbCampaign)
    }
}
