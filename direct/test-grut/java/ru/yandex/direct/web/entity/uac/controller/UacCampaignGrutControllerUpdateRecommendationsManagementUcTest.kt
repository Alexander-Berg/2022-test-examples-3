package ru.yandex.direct.web.entity.uac.controller

import java.math.BigDecimal
import java.util.Locale
import org.assertj.core.api.Assertions
import org.assertj.core.api.SoftAssertions
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
import ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes
import ru.yandex.direct.core.entity.uac.converter.UacGrutCampaignConverter.toUacYdbCampaign
import ru.yandex.direct.core.entity.uac.model.UacGoal
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.dbqueue.steps.DbQueueSteps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.validation.result.DefectIds
import ru.yandex.direct.validation.result.Path
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.uac.model.CreateCampaignRequest
import ru.yandex.direct.web.entity.uac.model.PatchCampaignRequest
import ru.yandex.direct.web.entity.uac.updateCampaignRequest
import ru.yandex.direct.web.validation.model.WebValidationResult

@GrutDirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacCampaignGrutControllerUpdateRecommendationsManagementUcTest {

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var grutApiService: GrutApiService

    @Autowired
    private lateinit var dbQueueSteps: DbQueueSteps

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var metrikaClient: MetrikaClientStub

    private lateinit var mockMvc: MockMvc
    private lateinit var clientInfo: ClientInfo
    private lateinit var uacCampaignId: String

    private var feedId: Long = 0

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        clientInfo = testAuthHelper.createDefaultUser().clientInfo!!
        testAuthHelper.setOperatorAndSubjectUser(clientInfo.uid)
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )

        dbQueueSteps.registerJobType(DbQueueJobTypes.UAC_UPDATE_ADS)

        // нужно для проверки регионов, так как из ручки возвращается регион в локали en
        LocaleContextHolder.setLocale(Locale.ENGLISH)
        grutSteps.createClient(clientInfo)
        uacCampaignId = grutSteps.createTextCampaign(clientInfo).toIdString()
        feedId = steps.feedSteps().createDefaultFileFeed(clientInfo).feedId

        steps.featureSteps().addClientFeature(clientInfo.clientId,
            FeatureName.UC_UAC_CREATE_ECOM_BRIEF_IN_GRUT_INSTEAD_OF_YDB, true)
        steps.featureSteps().addClientFeature(clientInfo.clientId,
            FeatureName.UC_UAC_CREATE_BRIEF_IN_GRUT_INSTEAD_OF_YDB, true)
    }

    @After
    fun after() {
        LocaleContextHolder.setLocale(null)
    }

    /**
     * Проверяем включение управления от Яндекса
     */
    @Test
    fun `update uc campaign with recommendations management`() {
        val request = updateCampaignRequest(recommendationsManagementEnabled = true)
        doSuccessRequest(request, uacCampaignId)
    }

    /**
     * Проверяем включение управления ставками от Яндекса
     */
    @Test
    fun `update uc campaign with price recommendations management`() {
        val request = updateCampaignRequest(recommendationsManagementEnabled = true,
            priceRecommendationsManagementEnabled = true)
        doSuccessRequest(request, uacCampaignId)
    }

    /**
     * Проверяем отключение управления от Яндекса
     */
    @Test
    fun `update uc campaign with recommendations management off`() {
        val firstRequest = updateCampaignRequest(recommendationsManagementEnabled = true,
            priceRecommendationsManagementEnabled = true)
        doSuccessRequest(firstRequest, uacCampaignId)

        val request = updateCampaignRequest(recommendationsManagementEnabled = false,
            priceRecommendationsManagementEnabled = false)
        doSuccessRequest(request, uacCampaignId)
    }

    /**
     * Проверяем включение управления ставками от Яндекса без управления от Яндекса
     */
    @Test
    fun `update uc campaign with price recommendations management and recommendations management off`() {
        val request = updateCampaignRequest(recommendationsManagementEnabled = false,
            priceRecommendationsManagementEnabled = true)
        checkBadRequest(
            request,
            path = PathHelper.path(PathHelper.field(CreateCampaignRequest::isPriceRecommendationsManagementEnabled)),
        )
    }

    /**
     * Проверяем включение управления ставками от Яндекса без явного управления от Яндекса
     */
    @Test
    fun `update uc campaign with price recommendations management and no recommendations management`() {
        val request = updateCampaignRequest(priceRecommendationsManagementEnabled = true)
        checkBadRequest(
            request,
            path = PathHelper.path(PathHelper.field(CreateCampaignRequest::isPriceRecommendationsManagementEnabled)),
        )
    }

    /**
     * Проверяем отключение управления от Яндекса без явного отключения управления ставками от Яндекса
     */
    @Test
    fun `update uc campaign with recommendations management off and price recommendations management still on`() {
        val firstRequest = updateCampaignRequest(recommendationsManagementEnabled = true,
            priceRecommendationsManagementEnabled = true)
        doSuccessRequest(firstRequest, uacCampaignId)

        val request = updateCampaignRequest(recommendationsManagementEnabled = false)
        checkBadRequest(
            request,
            path = PathHelper.path(PathHelper.field(CreateCampaignRequest::isRecommendationsManagementEnabled)),
        )
    }

    /**
     * Проверяем игнорирование включения управления от Яндекса в ecom-кампаниях
     */
    @Test
    fun `update uc ecom campaign with recommendations management`() {
        val request = updateCampaignRequest(recommendationsManagementEnabled = true)
        doSuccessRequest(request,
            uacCampaignId = prepareEcomCampaign(goals = request.goals, counters = request.counters))
    }

    /**
     * Проверяем игнорирование включения управления ставками от Яндекса в ecom-кампаниях
     */
    @Test
    fun `update uc ecom campaign with price recommendations management`() {
        val request = updateCampaignRequest(recommendationsManagementEnabled = true,
            priceRecommendationsManagementEnabled = true)
        doSuccessRequest(request,
            uacCampaignId = prepareEcomCampaign(goals = request.goals, counters = request.counters))
    }

    /**
     * Проверяем игнорирование включения управления ставками от Яндекса в ecom-кампаниях без управления от Яндекса
     */
    @Test
    fun `update uc ecom campaign with price recommendations management and no recommendations management`() {
        val request = updateCampaignRequest(priceRecommendationsManagementEnabled = true)
        doSuccessRequest(request,
            uacCampaignId = prepareEcomCampaign(goals = request.goals, counters = request.counters))
    }

    private fun checkBadRequest(
        request: PatchCampaignRequest,
        path: Path,
        overrideCampaignId: String? = null,
    ) {
        val campaignId = overrideCampaignId ?: uacCampaignId
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/${campaignId}?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString

        val validationResult = JsonUtils
            .fromJson(JsonUtils.fromJson(result)["validation_result"].toString(), WebValidationResult::class.java)
        Assertions.assertThat(validationResult.errors).isNotEmpty
        Assertions.assertThat(validationResult.errors[0].path).isEqualTo(path.toString())
        Assertions.assertThat(validationResult.errors[0].code).isEqualTo(DefectIds.INCONSISTENT_STATE.code)
    }

    private fun prepareEcomCampaign(goals: List<UacGoal>?, counters: List<Int>?) : String {
        if (goals != null && counters != null && !goals.isEmpty() && !counters.isEmpty()) {
            metrikaClient.addUserCounter(clientInfo.uid, counters[0])
            metrikaClient.addCounterGoal(counters[0], goals[0].goalId.toInt())
        }
        val request = UacCampaignRequestsCommon.createCampaignRequest(
            metrikaClient = metrikaClient,
            uid = clientInfo.uid,
            isEcom = true,
            feedId = feedId,
            goals = goals,
            counters = counters,
            cpa = BigDecimal.valueOf(100)
        )
        val result = UacCampaignRequestsCommon.doSuccessCreateRequest(
            request = request,
            mockMvc = mockMvc,
            login = clientInfo.login,
        )
        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        return resultJsonTree["result"]["id"].asText()
    }

    private fun doSuccessRequest(
        request: PatchCampaignRequest,
        uacCampaignId: String,
    ) {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/${uacCampaignId}?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andDo { System.err.println(it.response.contentAsString) }
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        val campaignId = resultJsonTree["result"]["id"].asText()
        val directCampaignId = campaignId.toIdLong()
        val campaignResponse = grutApiService.briefGrutApi.getBrief(directCampaignId)
        Assertions.assertThat(campaignResponse).isNotNull
        val campaign = campaignResponse!!.toUacYdbCampaign()

        SoftAssertions.assertSoftly {
            if (campaign.isEcom == true) {
                it.assertThat(campaign.recommendationsManagementEnabled)
                    .`as`("галочка управления от Яндекса")
                    .isFalse
                it.assertThat(campaign.priceRecommendationsManagementEnabled)
                    .`as`("галочка управления ставками от Яндекса")
                    .isFalse
            } else {
                if (request.isRecommendationsManagementEnabled != null) {
                    it.assertThat(campaign.recommendationsManagementEnabled)
                        .`as`("галочка управления от Яндекса")
                        .isEqualTo(request.isRecommendationsManagementEnabled)
                }
                if (request.isPriceRecommendationsManagementEnabled != null) {
                    it.assertThat(campaign.priceRecommendationsManagementEnabled)
                        .`as`("галочка управления ставками от Яндекса")
                        .isEqualTo(request.isPriceRecommendationsManagementEnabled)
                }
            }
        }
    }
}
