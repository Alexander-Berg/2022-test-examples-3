package ru.yandex.direct.web.entity.uac.controller

import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignService
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import java.time.LocalDateTime.now
import java.time.temporal.ChronoUnit

@GrutDirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacCampaignGrutDeleteControllerTest {

    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var grutUacCampaignService: GrutUacCampaignService

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var grutSteps: GrutSteps
    private lateinit var userInfo: UserInfo

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
        grutSteps.createClient(userInfo.clientInfo!!)
    }

    @BeforeEach
    fun beforeEach() {
        testAuthHelper.setOperatorAndSubjectUser(userInfo.uid)
    }

    @Test
    fun deleteNonExistentTest() {
        mockMvc.perform(
            MockMvcRequestBuilders
                .delete("/uac/campaign/1231231?ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun deleteNonValidStringIdTest() {
        mockMvc.perform(
            MockMvcRequestBuilders
                .delete("/uac/campaign/uac?ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun deleteNonValidMoreThanUint64IdTest() {
        mockMvc.perform(
            MockMvcRequestBuilders
                .delete("/uac/campaign/109467396721177068399?ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun deleteNotDraftCampaignTest() {
        val campaignId = createCampaign(draft = false)
        mockMvc.perform(
            MockMvcRequestBuilders
                .delete("/uac/campaign/$campaignId?ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isForbidden)


        val gotYdbCampaign = grutUacCampaignService.getCampaignById(campaignId)
        val gotCampaign = campaignTypedRepository.getSafely(userInfo.shard, listOf(campaignId.toLong()), CommonCampaign::class.java)[0]
        Assertions.assertThat(gotYdbCampaign).isNotNull
        Assertions.assertThat(gotCampaign.statusEmpty).isFalse
    }

    @Test
    fun deleteSuccessfulTest() {
        val campaignId = createCampaign(draft = true)
        mockMvc.perform(
            MockMvcRequestBuilders
                .delete("/uac/campaign/$campaignId?ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNoContent)

        val gotYdbCampaign = grutUacCampaignService.getCampaignById(campaignId)
        val gotCampaign = campaignTypedRepository.getSafely(userInfo.shard, listOf(campaignId.toLong()), CommonCampaign::class.java)[0]
        Assertions.assertThat(gotYdbCampaign).isNull()
        Assertions.assertThat(gotCampaign.statusEmpty).isTrue
    }

    @Test
    fun deleteNoRightsTest() {
        val anotherUserInfo = testAuthHelper.createDefaultUser()
        grutSteps.createClient(anotherUserInfo.clientInfo!!)
        val campaignId = createCampaign(draft = true)
        mockMvc.perform(
            MockMvcRequestBuilders
                .delete("/uac/campaign/$campaignId?ulogin=" + anotherUserInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)

        val gotYdbCampaign = grutUacCampaignService.getCampaignById(campaignId)
        val gotCampaign = campaignTypedRepository.getSafely(userInfo.shard, listOf(campaignId.toLong()), CommonCampaign::class.java)[0]
        Assertions.assertThat(gotYdbCampaign).isNotNull
        Assertions.assertThat(gotCampaign.statusEmpty).isFalse
    }

    fun createCampaign(draft: Boolean): String {
        val campaignToCreate = TestCampaigns.activeMobileAppCampaign(null, null)
            .withOrderId(0L)
            .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB))
        val campaign = steps.campaignSteps().createCampaign(campaignToCreate, userInfo.clientInfo)
        val startedAt = if (draft) null else now().truncatedTo(ChronoUnit.SECONDS)
        return grutSteps.createMobileAppCampaign(userInfo.clientInfo!!, campaign.campaignId.toIdString(), startedAt = startedAt).toIdString()
    }
}
