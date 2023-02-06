package ru.yandex.direct.web.entity.uac.controller

import org.assertj.core.api.Assertions
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
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.uac.createDirectCampaign
import ru.yandex.direct.core.entity.uac.createTextCampaignContent
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectCampaignRepository
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacCampaignDeleteControllerTest {

    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var uacCampaignRepository: UacYdbCampaignRepository

    @Autowired
    private lateinit var uacDirectCampaignRepository: UacYdbDirectCampaignRepository

    @Autowired
    private lateinit var uacCampaignContentRepository: UacYdbCampaignContentRepository

    private lateinit var userInfo: UserInfo

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
        testAuthHelper.setOperatorAndSubjectUser(userInfo.uid)
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
        val (campaignId, directCampaignId) = createCampaign(draft = false)
        mockMvc.perform(
            MockMvcRequestBuilders
                .delete("/uac/campaign/$campaignId?ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isForbidden)

        val gotYdbCampaign = uacCampaignRepository.getCampaign(campaignId)
        val gotYdbDirectCampaign = uacDirectCampaignRepository.getDirectCampaignById(campaignId)
        val gotYdbContents = uacCampaignContentRepository.getCampaignContents(campaignId)
        val gotCampaign = campaignTypedRepository.getSafely(userInfo.shard, listOf(directCampaignId), CommonCampaign::class.java)[0]
        Assertions.assertThat(gotYdbCampaign).isNotNull
        Assertions.assertThat(gotYdbDirectCampaign).isNotNull
        Assertions.assertThat(gotYdbContents).isNotEmpty
        Assertions.assertThat(gotCampaign.statusEmpty).isFalse
    }

    @Test
    fun deleteSuccessfulTest() {
        val (campaignId, directCampaignId) = createCampaign(draft = true)

        mockMvc.perform(
            MockMvcRequestBuilders
                .delete("/uac/campaign/$campaignId?ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNoContent)

        val gotYdbCampaign = uacCampaignRepository.getCampaign(campaignId)
        val gotYdbDirectCampaign = uacDirectCampaignRepository.getDirectCampaignById(campaignId)
        val gotYdbContents = uacCampaignContentRepository.getCampaignContents(campaignId)
        val gotCampaign = campaignTypedRepository.getSafely(userInfo.shard, listOf(directCampaignId), CommonCampaign::class.java)[0]
        Assertions.assertThat(gotYdbCampaign).isNull()
        Assertions.assertThat(gotYdbDirectCampaign).isNull()
        Assertions.assertThat(gotYdbContents).isEmpty()
        Assertions.assertThat(gotCampaign.statusEmpty).isTrue
    }

    @Test
    fun deleteSuccessfulTest_WithDirectCampaignId() {
        val (campaignId, directCampaignId) = createCampaign(draft = true)

        mockMvc.perform(
            MockMvcRequestBuilders
                .delete("/uac/campaign/direct/$directCampaignId?ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNoContent)

        val gotYdbCampaign = uacCampaignRepository.getCampaign(campaignId)
        val gotYdbDirectCampaign = uacDirectCampaignRepository.getDirectCampaignById(campaignId)
        val gotYdbContents = uacCampaignContentRepository.getCampaignContents(campaignId)
        val gotCampaign = campaignTypedRepository.getSafely(userInfo.shard, listOf(directCampaignId), CommonCampaign::class.java)[0]
        Assertions.assertThat(gotYdbCampaign).isNull()
        Assertions.assertThat(gotYdbDirectCampaign).isNull()
        Assertions.assertThat(gotYdbContents).isEmpty()
        Assertions.assertThat(gotCampaign.statusEmpty).isTrue
    }

    @Test
    fun deleteNoRightsTest() {
        val anotherUserInfo = testAuthHelper.createDefaultUser()
        val (campaignId, directCampaignId) = createCampaign(draft = true)
        mockMvc.perform(
            MockMvcRequestBuilders
                .delete("/uac/campaign/$campaignId?ulogin=" + anotherUserInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)

        val gotYdbCampaign = uacCampaignRepository.getCampaign(campaignId)
        val gotYdbDirectCampaign = uacDirectCampaignRepository.getDirectCampaignById(campaignId)
        val gotYdbContents = uacCampaignContentRepository.getCampaignContents(campaignId)
        val gotCampaign = campaignTypedRepository.getSafely(userInfo.shard, listOf(directCampaignId), CommonCampaign::class.java)[0]
        Assertions.assertThat(gotYdbCampaign).isNotNull
        Assertions.assertThat(gotYdbDirectCampaign).isNotNull
        Assertions.assertThat(gotYdbContents).isNotEmpty
        Assertions.assertThat(gotCampaign.statusEmpty).isFalse
    }

    fun createCampaign(draft: Boolean): CampaignIdWithDirectCampaignId {
        val campaignToCreate = TestCampaigns.activeMobileAppCampaign(null, null)
            .withOrderId(0L)
            .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB))
        val campaign = steps.campaignSteps().createCampaign(campaignToCreate, userInfo.clientInfo)
        val ydbCampaign = createYdbCampaign(startedAt = if (draft) null else LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
        val ydbDirectCampaign = createDirectCampaign(id = ydbCampaign.id, directCampaignId = campaign.campaignId)
        val campaignContent = createTextCampaignContent(campaignId = ydbCampaign.id)
        uacCampaignRepository.addCampaign(ydbCampaign)
        uacCampaignContentRepository.addCampaignContents(listOf(campaignContent))
        uacDirectCampaignRepository.saveDirectCampaign(ydbDirectCampaign)
        return CampaignIdWithDirectCampaignId(ydbCampaign.id, campaign.campaignId)
    }

    data class CampaignIdWithDirectCampaignId(
        val campaignId: String,
        val directCampaignId: Long
    )
}
