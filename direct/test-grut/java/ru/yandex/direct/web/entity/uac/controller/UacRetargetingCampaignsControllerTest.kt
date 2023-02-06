package ru.yandex.direct.web.entity.uac.controller

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.direct.core.entity.adgroup.container.ComplexMobileContentAdGroup
import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupAddOperationFactory
import ru.yandex.direct.core.entity.adgroup.service.complex.text.ComplexTextAdGroupAddOperation
import ru.yandex.direct.core.entity.banner.service.DatabaseMode
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.service.BaseCampaignService
import ru.yandex.direct.core.entity.client.service.ClientGeoService
import ru.yandex.direct.core.entity.mobileapp.model.MobileApp
import ru.yandex.direct.core.entity.mobilegoals.MobileAppGoalsService
import ru.yandex.direct.core.entity.retargeting.model.ConditionType
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest
import ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionService
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.UacRetargetingCondition
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRule
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoal
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignService
import ru.yandex.direct.core.entity.uac.service.UacAppInfoService
import ru.yandex.direct.core.entity.uac.service.UacRetargetingService
import ru.yandex.direct.core.testing.info.CampaignInfoConverter
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.multitype.entity.LimitOffset
import ru.yandex.direct.regions.GeoTree
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.uac.model.UacRetargetingCampaignResponse

@GrutDirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacRetargetingCampaignsControllerTest {
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
    private lateinit var steps: Steps

    @Autowired
    private lateinit var uacAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    private lateinit var uacAppInfoService: UacAppInfoService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var mobileAppGoalsService: MobileAppGoalsService

    @Autowired
    private lateinit var retargetingConditionService: RetargetingConditionService

    @Autowired
    private lateinit var complexAdGroupAddOperationFactory: ComplexAdGroupAddOperationFactory

    @Autowired
    private lateinit var clientGeoService: ClientGeoService

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var uacCampaignService: GrutUacCampaignService

    @Autowired
    private lateinit var baseCampaignService: BaseCampaignService

    private lateinit var userInfo: UserInfo
    private lateinit var mobileApp: MobileApp
    private lateinit var mobileAppGoalIds: List<Long>
    private lateinit var mobileAppGoalNameById: Map<Long, String>

    @Before
    fun before() {
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
        grutSteps.createClient(userInfo.clientInfo!!)
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.IN_APP_MOBILE_TARGETING, true)
        val ydbAppInfo = defaultAppInfo()
        uacAppInfoRepository.saveAppInfo(ydbAppInfo)
        val appInfo = uacAppInfoService.getAppInfo(ydbAppInfo)

        mobileApp = steps.mobileAppSteps().createMobileApp(userInfo.clientInfo!!, appInfo.url).mobileApp
        mobileAppGoalsService.updateMobileAppGoalsForExternalTracker(
            dslContextProvider.ppc(userInfo.shard), userInfo.clientId, listOf(mobileApp))
        mobileAppGoalNameById = mobileAppGoalsService
            .getGoalsByApps(userInfo.clientId, listOf(mobileApp)).associate { it.id to it.name }
        mobileAppGoalIds = mobileAppGoalNameById.keys.toList()
    }

    @Suppress("unused")
    private fun isFeatureEnabled() = listOf(true, false)

    @Test
    @TestCaseName("Test case for multiple ad groups feature enabled: {0}")
    @Parameters(method = "isFeatureEnabled")
    fun `get from campaign briefs`(isFeatureEnabled: Boolean) {
        steps.featureSteps()
            .addClientFeature(userInfo.clientId, FeatureName.UAC_MULTIPLE_AD_GROUPS_ENABLED, isFeatureEnabled)

        val mobileCampaignId = grutSteps.createMobileAppCampaign(userInfo.clientInfo!!, createInDirect = true)
        val directMobileCampaign = baseCampaignService
            .get(userInfo.clientId, userInfo.uid, listOf(mobileCampaignId))[0] as MobileContentCampaign
        val directMobileCampaignInfo = CampaignInfoConverter
            .toCampaignInfo(userInfo.clientInfo!!, directMobileCampaign)
        val textCampaignId = grutSteps.createTextCampaign(userInfo.clientInfo!!)
        val directTextCampaign = baseCampaignService
            .get(userInfo.clientId, userInfo.uid, listOf(textCampaignId))[0] as TextCampaign
        val directTextCampaignInfo = CampaignInfoConverter.toCampaignInfo(userInfo.clientInfo!!, directTextCampaign)

        val uacRetargetingCondition = createRetargetingCondition()
        val retargetingConditionId = uacRetargetingCondition.id!!
        val draftCampaign = grutSteps.createAndGetMobileAppCampaign(
            userInfo.clientInfo!!,
            startedAt = null,
            retargetingCondition = uacRetargetingCondition,
        )
        val retargetingCondition = retargetingConditionService.getRetargetingConditions(
            userInfo.clientId, listOf(retargetingConditionId), LimitOffset.maxLimited())[0]
        val textAdGroup = steps.adGroupSteps().createActiveTextAdGroup(directTextCampaignInfo)
        val mobileAdGroup = steps.adGroupSteps().createActiveMobileContentAdGroup(directMobileCampaignInfo)
        val targetInterest = TargetInterest().withRetargetingConditionId(retargetingConditionId)
        val complexMobileAdGroup = ComplexMobileContentAdGroup()
            .withAdGroup(mobileAdGroup.adGroup)
            .withTargetInterests(listOf(targetInterest))
        val complexTextAdGroup = ComplexTextAdGroup()
            .withAdGroup(textAdGroup.adGroup)
            .withRetargetingCondition(retargetingCondition)
            .withTargetInterests(listOf(targetInterest))

        val geoTree: GeoTree = clientGeoService.getClientTranslocalGeoTree(userInfo.clientId)
        val mobileOperation = complexAdGroupAddOperationFactory.createMobileContentAdGroupAddOperation(
            false, listOf(complexMobileAdGroup), geoTree, false, null,
            userInfo.uid, userInfo.clientId, userInfo.chiefUid
        )
        mobileOperation.prepareAndApply()

        val textOperation: ComplexTextAdGroupAddOperation = complexAdGroupAddOperationFactory
            .createTextAdGroupAddOperation(
                false, listOf(complexTextAdGroup), geoTree, false, null,
                userInfo.uid, userInfo.clientId, userInfo.chiefUid, DatabaseMode.ONLY_MYSQL
            )
        textOperation.prepareAndApply()

        val result = doRequest(retargetingConditionId)

        val response = JsonUtils.MAPPER.readTree(result)["result"]
        val soft = SoftAssertions()
        soft.assertThat(response).hasSize(3)
        soft.assertThat(response[0]["name"].asText()).isEqualTo(directMobileCampaign.name)
        soft.assertThat(response[0]["id"].asLong()).isEqualTo(directMobileCampaign.id)
        soft.assertThat(response[1]["name"].asText()).isEqualTo(directTextCampaign.name)
        soft.assertThat(response[1]["id"].asLong()).isEqualTo(directTextCampaign.id)
        soft.assertThat(response[2]["name"].asText()).isEqualTo(draftCampaign.name)
        soft.assertThat(response[2]["id"].asLong()).isEqualTo(draftCampaign.id.toIdLong())
        soft.assertAll()
    }

    /**
     * Одна кампания с условием ретаргетинга, но без групповых заявок,
     * вторая кампания с двумя групповыми заявками, на одной из которых есть условие ретаргетинга
     */
    @Test
    fun `get from ad group briefs`() {
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.UAC_MULTIPLE_AD_GROUPS_ENABLED, true)

        val retargetingCondition = createRetargetingCondition()
        val campaign1 = grutSteps.createAndGetMobileAppCampaign(
            userInfo.clientInfo!!,
            retargetingCondition = retargetingCondition
        )

        val campaign2 = grutSteps.createAndGetMobileAppCampaign(userInfo.clientInfo!!)
        grutSteps.createAdGroupBrief(campaign2.id.toIdLong())
        grutSteps.createAdGroupBrief(campaign2.id.toIdLong(), retargetingCondition = retargetingCondition)
        val result = doRequest(retargetingCondition.id!!)
        val response = JsonUtils.fromJson(result, UacRetargetingCampaignResponse::class.java)

        val soft = SoftAssertions()
        soft.assertThat(response.result).hasSize(2)
        val actualCampaignIds = response.result.map { it.id.toIdString() }
        val actualCampaignNames = response.result.map { it.name }
        soft.assertThat(actualCampaignIds).containsExactlyInAnyOrder(campaign1.id, campaign2.id)
        soft.assertThat(actualCampaignNames).containsExactlyInAnyOrder(campaign1.name, campaign2.name)
        soft.assertAll()
    }

    @Test
    fun testWithoutCampaigns() {
        val uacRetargetingCondition = createRetargetingCondition()

        val result = doRequest(uacRetargetingCondition.id!!)

        val response = JsonUtils.MAPPER.readTree(result)["result"]
        Assertions.assertThat(response).hasSize(0)
    }

    private fun createRetargetingCondition(): UacRetargetingCondition {
        val uacRetargetingCondition = UacRetargetingCondition(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    goals = listOf(UacRetargetingConditionRuleGoal(id = mobileAppGoalIds[0])),
                ),
            ),
            name = "Условие ретаргетинга 1"
        )
        val directRetargetingCondition = UacRetargetingService.toCoreRetargetingCondition(
            uacRetargetingCondition,
            userInfo.clientId.asLong(),
            type = ConditionType.metrika_goals
        )
        val retargetingConditionId = retargetingConditionService
            .addRetargetingConditions(listOf(directRetargetingCondition), userInfo.clientId)
            .get(0).result
        return UacRetargetingCondition(
            conditionRules = uacRetargetingCondition.conditionRules,
            name = uacRetargetingCondition.name,
            id = retargetingConditionId,
        )
    }

    private fun doRequest(retargetingConditionId: Long) =
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/retargeting/campaigns?ret_cond_id=$retargetingConditionId&ulogin=" +
                    userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString
}
