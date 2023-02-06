package ru.yandex.direct.web.entity.uac.controller

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes
import ru.yandex.direct.core.entity.uac.model.DirectCampaignStatus
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.core.entity.uac.model.TargetStatus
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.repository.TestCampaignRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.dbqueue.steps.DbQueueSteps
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.uac.model.CampaignStatusesRequest

@GrutDirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacCampaignControllerStatusesTest {

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var grutApiService: GrutApiService

    @Autowired
    private lateinit var dbQueueSteps: DbQueueSteps

    @Autowired
    private lateinit var campaignRepository: CampaignRepository

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var metrikaClient: MetrikaClientStub

    @Autowired
    private lateinit var testCampaignRepository: TestCampaignRepository

    private lateinit var mockMvc: MockMvc
    private lateinit var clientInfo: ClientInfo
    private var feedId: Long = 0

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        clientInfo = testAuthHelper.createDefaultUser().clientInfo!!
        testAuthHelper.setOperatorAndSubjectUser(clientInfo.uid)
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )

        // нужно для проверки регионов, так как из ручки возвращается регион в локали en
        LocaleContextHolder.setLocale(Locale.ENGLISH)

        dbQueueSteps.registerJobType(DbQueueJobTypes.UAC_UPDATE_ADS)

        feedId = steps.feedSteps().createDefaultFileFeed(clientInfo).feedId

        grutSteps.createClient(clientInfo)
    }

    @Test
    fun `test enable new UC campaign`() {
        val ucCampaignId = grutSteps.createTextCampaign(clientInfo, TargetStatus.STOPPED, startedAt = null)
        val textAsset = grutSteps.createDefaultTextAsset(clientInfo.clientId!!)
        val titleAsset = grutSteps.createDefaultTitleAsset(clientInfo.clientId!!)
        grutSteps.setAssetLinksToCampaign(ucCampaignId, listOf(textAsset, titleAsset))

        val request = CampaignStatusesRequest(targetStatus = TargetStatus.STARTED)
        doSuccessRequest(request, ucCampaignId.toIdString(), Status.MODERATING, TargetStatus.STARTED, true)
    }

    @Test
    fun `test disable running UC campaign`() {
        val ucCampaignId = grutSteps.createTextCampaign(clientInfo, TargetStatus.STARTED, DirectCampaignStatus.CREATED,
            startedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
        val textAsset = grutSteps.createDefaultTextAsset(clientInfo.clientId!!)
        val titleAsset = grutSteps.createDefaultTitleAsset(clientInfo.clientId!!)
        grutSteps.setAssetLinksToCampaign(ucCampaignId, listOf(textAsset, titleAsset))

        val request = CampaignStatusesRequest(targetStatus = TargetStatus.STOPPED)
        doSuccessRequest(request, ucCampaignId.toIdString(), Status.STOPPED, TargetStatus.STOPPED, false)
    }

    @Test
    fun `test enable new Ecom UC campaign`() {
        val counterIds = initMetrikaCounter()
        val feedId = steps.feedSteps().createDefaultFeed(clientInfo).feedId
        val ucCampaignId = grutSteps.createEcomUcCampaign(
            clientInfo = clientInfo,
            feedId = feedId,
            startedAt = null,
            targetStatus = TargetStatus.STOPPED,
            directCampaignStatus = DirectCampaignStatus.DRAFT,
            counterIds = counterIds).first
        val textAsset = grutSteps.createDefaultTextAsset(clientInfo.clientId!!)
        val titleAsset = grutSteps.createDefaultTitleAsset(clientInfo.clientId!!)
        grutSteps.setAssetLinksToCampaign(ucCampaignId, listOf(textAsset, titleAsset))

        val request = CampaignStatusesRequest(targetStatus = TargetStatus.STARTED)
        doSuccessRequest(request, ucCampaignId.toIdString(), Status.MODERATING, TargetStatus.STARTED, true)
    }

    @Test
    fun `test disable Ecom UC campaign`() {
        val counterIds = initMetrikaCounter()
        val feedId = steps.feedSteps().createDefaultFeed(clientInfo).feedId
        val ucCampaignId = grutSteps.createEcomUcCampaign(
            clientInfo = clientInfo,
            feedId = feedId,
            targetStatus = TargetStatus.STARTED,
            directCampaignStatus = DirectCampaignStatus.CREATED,
            counterIds = counterIds).first
        val textAsset = grutSteps.createDefaultTextAsset(clientInfo.clientId!!)
        val titleAsset = grutSteps.createDefaultTitleAsset(clientInfo.clientId!!)
        grutSteps.setAssetLinksToCampaign(ucCampaignId, listOf(textAsset, titleAsset))

        val request = CampaignStatusesRequest(targetStatus = TargetStatus.STOPPED)
        doSuccessRequest(request, ucCampaignId.toIdString(), Status.STOPPED, TargetStatus.STOPPED, false)
    }

    fun statusParams() = listOf(false, true)

    @Test
    @TestCaseName("Campaign status show: {0}")
    @Parameters(method = "statusParams")
    fun `test enable new UC campaign with different status show of campaign`(statusShow: Boolean) {
        val ucCampaignId = grutSteps.createTextCampaign(
            clientInfo,
            TargetStatus.STARTED,
            DirectCampaignStatus.CREATED,
            startedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
        )
        val textAsset = grutSteps.createDefaultTextAsset(clientInfo.clientId!!)
        val titleAsset = grutSteps.createDefaultTitleAsset(clientInfo.clientId!!)
        grutSteps.setAssetLinksToCampaign(ucCampaignId, listOf(textAsset, titleAsset))

        testCampaignRepository.updateStatusShow(clientInfo.shard, ucCampaignId, statusShow)

        val request = CampaignStatusesRequest(targetStatus = TargetStatus.STARTED)
        if (statusShow == true) {
            doBadRequest(request, ucCampaignId.toIdString())
        } else {
            doSuccessRequest(request, ucCampaignId.toIdString(), Status.STOPPED, TargetStatus.STARTED, true)
        }
    }

    @Test
    @TestCaseName("Campaign status show: {0}")
    @Parameters(method = "statusParams")
    fun `test disable UC campaign with different status show of campaign`(statusShow: Boolean) {
        val ucCampaignId = grutSteps.createTextCampaign(
            clientInfo,
            TargetStatus.STOPPED,
            DirectCampaignStatus.CREATED,
            startedAt = null
        )
        val textAsset = grutSteps.createDefaultTextAsset(clientInfo.clientId!!)
        val titleAsset = grutSteps.createDefaultTitleAsset(clientInfo.clientId!!)
        grutSteps.setAssetLinksToCampaign(ucCampaignId, listOf(textAsset, titleAsset))

        testCampaignRepository.updateStatusShow(clientInfo.shard, ucCampaignId, statusShow)

        val request = CampaignStatusesRequest(targetStatus = TargetStatus.STOPPED)
        if (statusShow == true) {
            doSuccessRequest(request, ucCampaignId.toIdString(), Status.DRAFT, TargetStatus.STOPPED, false)
        } else {
            doBadRequest(request, ucCampaignId.toIdString())
        }
    }


    @Test
    fun `test enable cpm banner campaign with disabled feature`() {
        val cpmCampaignId = grutSteps.createCpmBannerCampaign(
            clientInfo = clientInfo,
            startedAt = null,
        )
        val textAsset = grutSteps.createDefaultTextAsset(clientInfo.clientId!!)
        val titleAsset = grutSteps.createDefaultTitleAsset(clientInfo.clientId!!)
        grutSteps.setAssetLinksToCampaign(cpmCampaignId, listOf(textAsset, titleAsset))

        val request = CampaignStatusesRequest(targetStatus = TargetStatus.STARTED)
        doBadRequest(request, cpmCampaignId.toIdString())
    }

    private fun initMetrikaCounter(): List<Int> {
        val counterId1 = RandomNumberUtils.nextPositiveInteger(900_000_000)
        val counterId2 = RandomNumberUtils.nextPositiveInteger(900_000_000)
        metrikaClient.addUserCounter(clientInfo.uid, counterId1)
        metrikaClient.addUserCounter(clientInfo.uid, counterId2)
        return listOf(counterId1, counterId2)
    }

    private fun doSuccessRequest(
        request: CampaignStatusesRequest,
        campaignId: String,
        expectedStatus: Status,
        expectedTargetStatus: TargetStatus,
        expectedStatusShow: Boolean
    ) {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaign/direct/${campaignId}/status?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andDo { System.err.println(it.response.contentAsString) }
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(result)

        val newStatusNode = resultJsonTree["status"]
        val newStatus = JsonUtils.MAPPER.treeToValue(newStatusNode, Status::class.java)
        assertEquals(expectedStatus, newStatus)

        val newTargetStatusNode = resultJsonTree["target_status"]
        val newTargetStatus = JsonUtils.MAPPER.treeToValue(newTargetStatusNode, TargetStatus::class.java)
        assertEquals(expectedTargetStatus, newTargetStatus)

        val directCampaignId = campaignId.toIdLong()
        val campaignResponse = grutApiService.briefGrutApi.getBrief(directCampaignId)
        assertNotNull(campaignResponse)

        // Дальше проверяем актуальные статусы директовых кампаний
        val directCampaigns = campaignTypedRepository.getTypedCampaigns(clientInfo.shard, listOf(directCampaignId))
        assertEquals(1, directCampaigns.size)
        val directCampaign = directCampaigns[0] as CommonCampaign
        assertEquals(expectedStatusShow, directCampaign.statusShow)

        val hiddenCampaignIds = campaignRepository.getSubCampaignIdsWithMasterIds(clientInfo.shard, setOf(directCampaignId))
        if (hiddenCampaignIds.isNotEmpty()) {
            val hiddenCampaigns = campaignTypedRepository.getTypedCampaigns(clientInfo.shard, hiddenCampaignIds.keys)
                .filterIsInstance<CommonCampaign>()
                .toList()
            hiddenCampaigns.forEach {
                assertEquals("Campaign ${it.id} status show", expectedStatusShow, it.statusShow)
            }
        }
    }

    private fun doBadRequest(
        request: CampaignStatusesRequest,
        campaignId: String,
    ) {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaign/direct/${campaignId}/status?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andDo { System.err.println(it.response.contentAsString) }
            .andExpect(MockMvcResultMatchers.status().isForbidden)
    }
}
