package ru.yandex.direct.web.entity.uac.controller

import org.assertj.core.api.Assertions.assertThat
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
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AdGroupStatesEnum
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.AggregatedStatusCampaignData
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.CampaignCounters
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.CampaignStatesEnum
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService
import ru.yandex.direct.core.entity.mobilegoals.MobileAppGoalsService
import ru.yandex.direct.core.entity.retargeting.model.ConditionType
import ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionService
import ru.yandex.direct.core.entity.uac.AssetConstants.ASSET_TEXT
import ru.yandex.direct.core.entity.uac.UacErrorResponse
import ru.yandex.direct.core.entity.uac.converter.UacBidModifiersConverter
import ru.yandex.direct.core.entity.uac.converter.UacGrutCampaignConverter.toCampaignSpec
import ru.yandex.direct.core.entity.uac.converter.UacGrutCampaignConverter.toEAdCampaignType
import ru.yandex.direct.core.entity.uac.converter.UacGrutCampaignConverter.toUacYdbCampaign
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.InventoryType
import ru.yandex.direct.core.entity.uac.model.TargetStatus
import ru.yandex.direct.core.entity.uac.model.UacAdjustmentRequest
import ru.yandex.direct.core.entity.uac.model.UacRetargetingCondition
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRule
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoal
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignService
import ru.yandex.direct.core.entity.uac.service.GrutUacClientService
import ru.yandex.direct.core.entity.uac.service.UacAppInfoService
import ru.yandex.direct.core.entity.uac.service.UacRetargetingService
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.dbschema.ppc.Tables.AGGR_STATUSES_CAMPAIGNS
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.utils.fromJson
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.uac.notOwnerResponse
import ru.yandex.direct.web.entity.uac.service.GrutUacCampaignWebService
import ru.yandex.grut.objects.proto.client.Schema
import ru.yandex.grut.objects.proto.client.Schema.TCampaignMeta
import java.time.LocalDateTime.now
import java.time.temporal.ChronoUnit
import java.util.Locale

@GrutDirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacCampaignGrutGetDirectControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var uacCampaignService: GrutUacCampaignService

    @Autowired
    private lateinit var grutUacCampaignWebService: GrutUacCampaignWebService

    @Autowired
    private lateinit var uacClientService: GrutUacClientService

    @Autowired
    private lateinit var uacAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    private lateinit var grutApiService: GrutApiService

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var mobileAppGoalsService: MobileAppGoalsService

    @Autowired
    private lateinit var retargetingConditionService: RetargetingConditionService

    @Autowired
    private lateinit var uacAppInfoService: UacAppInfoService

    @Autowired
    private lateinit var bidModifierService: BidModifierService

    private lateinit var userInfo: UserInfo

    @Before
    fun before() {
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
        // нужно для проверки регионов, так как из ручки возвращается регион в локали en
        LocaleContextHolder.setLocale(Locale.ENGLISH)
    }

    @After
    fun after() {
        LocaleContextHolder.setLocale(null)
    }

    @Test
    fun getDirectNonExistentTest() {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/campaign/direct/1231231?ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun getDirectCampaignSuccessfulTest() {
        val operator = userInfo.clientInfo?.chiefUserInfo?.user!!
        val subjectUser = userInfo.clientInfo?.chiefUserInfo?.user!!
        uacClientService.getOrCreateClient(operator, subjectUser)

        val campaignToCreate = TestCampaigns.activeMobileAppCampaign(null, null)
            .withStatusModerate(StatusModerate.SENT)
        val campaign = steps.campaignSteps().createCampaign(campaignToCreate, userInfo.clientInfo)
        val directCampaignId = campaign.campaignId
        val appInfo = defaultAppInfo()
        uacAppInfoRepository.saveAppInfo(appInfo)

        val ydbCampaign = createCampaignInGrutAndGet(subjectUser, directCampaignId, appInfo.id)

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

        val gotYdbCampaign = grutApiService.briefGrutApi.getBrief(ydbCampaign.id.toIdLong())!!.toUacYdbCampaign()

        // проверяем, что ничего, кроме статусов, не изменилось
        assertThat(gotYdbCampaign).usingRecursiveComparison().ignoringFields(
            UacYdbCampaign::targetStatus.name,
            UacYdbCampaign::createdAt.name,
        ).isEqualTo(ydbCampaign)

        val updatesStatuses = grutUacCampaignWebService.recalcStatuses(
            userInfo.clientInfo!!.clientId!!, directCampaignId, ydbCampaign.isDraft
        )
        val statuses = uacCampaignService.getCampaignStatuses(userInfo.clientId, directCampaignId, gotYdbCampaign)!!
        val filledCampaign = grutUacCampaignWebService
            .fillCampaign(
                operator = operator,
                subjectUser = subjectUser,
                gotYdbCampaign, directCampaignId,
                statuses
            )
        val resultRaw = JsonUtils.MAPPER.readTree(result)["result"]
        resultRaw.checkEquals(
            filledCampaign,
            ignoredFieldNames = listOf("created_at"), // ORM выставляет свой timestamp в момент записи, следовательно,
                                                      // может разъехаться
        )

        val texts = fromJson<List<String>>(resultRaw["texts"].toString())
        assertThat(texts).hasSize(1)
        assertThat(texts).contains(ASSET_TEXT)
    }

    @Test
    fun getProtoDirectCampaignSuccessfulTest() {
        val operator = userInfo.clientInfo?.chiefUserInfo?.user!!
        val subjectUser = userInfo.clientInfo?.chiefUserInfo?.user!!
        uacClientService.getOrCreateClient(operator, subjectUser)

        val campaignToCreate = TestCampaigns.activeMobileAppCampaign(null, null)
            .withStatusModerate(StatusModerate.SENT)
        val campaign = steps.campaignSteps().createCampaign(campaignToCreate, userInfo.clientInfo)
        val directCampaignId = campaign.campaignId
        val appInfo = defaultAppInfo()
        uacAppInfoRepository.saveAppInfo(appInfo)

        createCampaignInGrutAndGet(subjectUser, directCampaignId, appInfo.id)

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

        val response = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/campaign/direct/${campaign.campaignId}?ulogin=${userInfo.clientInfo!!.login}" +
                    "&proto_format=true")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        System.err.println(response)
        // TODO(stasis93): proper testing (https://st.yandex-team.ru/DIRECT-172067)
    }

    @Test
    fun getDirectCampaignWithRetargetingConditionAdjustmentSuccessfulTest() {
        val operator = userInfo.clientInfo?.chiefUserInfo?.user!!
        val subjectUser = userInfo.clientInfo?.chiefUserInfo?.user!!
        uacClientService.getOrCreateClient(operator, subjectUser)

        val campaignToCreate = TestCampaigns.activeMobileAppCampaign(null, null)
            .withStatusModerate(StatusModerate.SENT)
        val campaign = steps.campaignSteps().createCampaign(campaignToCreate, userInfo.clientInfo)
        val directCampaignId = campaign.campaignId

        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.IN_APP_MOBILE_TARGETING, true)
        val ydbAppInfo = defaultAppInfo()
        uacAppInfoRepository.saveAppInfo(ydbAppInfo)
        val appInfo = uacAppInfoService.getAppInfo(ydbAppInfo)

        val mobileApp = steps.mobileAppSteps().createMobileApp(userInfo.clientInfo!!, appInfo.url).mobileApp
        mobileAppGoalsService.updateMobileAppGoalsForExternalTracker(
            dslContextProvider.ppc(userInfo.shard), userInfo.clientId, listOf(mobileApp))
        val mobileAppGoalIds = mobileAppGoalsService.getGoalsByApps(userInfo.clientId, listOf(mobileApp)).map { it.id }

        val retargetingCondition = UacRetargetingCondition(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    goals = listOf(UacRetargetingConditionRuleGoal(id = mobileAppGoalIds[0], name = "ACHIEVED_LEVEL")),
                ),
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.NOT,
                    goals = listOf(UacRetargetingConditionRuleGoal(id = mobileAppGoalIds[8], name = "PURCHASED")),
                ),
            ),
            name = "Условие ретаргетинга 1"
        )
        val directRetargetingCondition = UacRetargetingService.toCoreRetargetingCondition(
            retargetingCondition,
            userInfo.clientId.asLong(),
            type = ConditionType.metrika_goals
        )
        val retargetingConditionId = retargetingConditionService.addRetargetingConditions(
            listOf(directRetargetingCondition), userInfo.clientId)[0].result
        val adjustment = UacAdjustmentRequest(
            retargetingConditionId = retargetingConditionId,
            percent = 20,
            age = null,
            gender = null,
            region = null,
        )
        val bidModifiers = UacBidModifiersConverter.toBidModifiers(
            listOf(adjustment),
            null,
            directCampaignId,
            false,
        )
        bidModifierService.add(bidModifiers, subjectUser.clientId, operator.uid)

        createCampaignInGrutAndGet(subjectUser, directCampaignId, ydbAppInfo.id)

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/campaign/direct/${campaign.campaignId}?ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val resultRaw = JsonUtils.MAPPER.readTree(result)["result"]
        val soft = SoftAssertions()
        soft.assertThat(resultRaw["adjustments"]).hasSize(1)
        soft.assertThat(resultRaw["adjustments"][0]["retargeting_condition_id"].asInt())
            .isEqualTo(retargetingConditionId)
        soft.assertThat(resultRaw["adjustments"][0]["percent"].asInt()).isEqualTo(20)
        soft.assertAll()
    }

    @Test
    fun getDirectCampaignNoRightsTest() {
        val operator = userInfo.clientInfo?.chiefUserInfo?.user!!
        val subjectUser = userInfo.clientInfo?.chiefUserInfo?.user!!
        uacClientService.getOrCreateClient(operator, subjectUser)

        val anotherUserInfo = testAuthHelper.createDefaultUser()

        val campaignToCreate = TestCampaigns.activeMobileAppCampaign(null, null)
            .withStatusModerate(StatusModerate.SENT)
        val campaign = steps.campaignSteps().createCampaign(campaignToCreate, userInfo.clientInfo)
        val directCampaignId = campaign.campaignId
        val appInfo = defaultAppInfo()
        val ydbCampaign = createYdbCampaign(
            id = directCampaignId.toString(),
            accountId = subjectUser.clientId.toString(),
            appId = appInfo.id,
            startedAt = now().truncatedTo(ChronoUnit.SECONDS),
            targetStatus = TargetStatus.STOPPED,
            assetLinks = emptyList(),
            isEcom = false,
        )
        uacAppInfoRepository.saveAppInfo(appInfo)
        grutApiService.briefGrutApi.createObject(
            Schema.TCampaign.newBuilder().apply {
                meta = TCampaignMeta.newBuilder().apply {
                    id = ydbCampaign.id.toIdLong()
                    campaignType = ydbCampaign.advType.toEAdCampaignType()
                    clientId = subjectUser.clientId.asLong()
                }.build()
                spec = toCampaignSpec(ydbCampaign)
            }.build()
        )

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

        val gotYdbCampaign = grutApiService.briefGrutApi.getBrief(ydbCampaign.id.toIdLong())!!.toUacYdbCampaign()
        // проверяем, что ничего, кроме статусов, не изменилось
        assertThat(gotYdbCampaign)
            .usingRecursiveComparison()
            .ignoringFields(UacYdbCampaign::createdAt.name)
            .isEqualTo(ydbCampaign)
    }

    private fun createCampaignInGrutAndGet(
        subjectUser: User,
        directCampaignId: Long,
        ydbAppInfoId: String,
    ): UacYdbCampaign {
        val textAssetId = grutSteps.createDefaultTextAsset(subjectUser.clientId)
        val textAssetId2 = grutSteps.createTextAsset(subjectUser.clientId, "text asset 2")

        val ydbCampaign = createYdbCampaign(
            id = directCampaignId.toString(),
            accountId = subjectUser.clientId.toString(),
            appId = ydbAppInfoId,
            startedAt = now().truncatedTo(ChronoUnit.SECONDS),
            targetStatus = TargetStatus.STOPPED,
            assetLinks = listOf(
                createCampaignContent(id = textAssetId, campaignId = directCampaignId.toString(),
                    contentId = textAssetId, type = null),
                createCampaignContent(id = textAssetId2, campaignId = directCampaignId.toString(),
                    contentId = textAssetId2, type = null, removedAt = now().truncatedTo(ChronoUnit.SECONDS)),
            ),
            isEcom = false,
            inventoryTypes = setOf(InventoryType.INAPP, InventoryType.REWARDED),
        )

        grutApiService.briefGrutApi.createObject(
            Schema.TCampaign.newBuilder().apply {
                meta = TCampaignMeta.newBuilder().apply {
                    id = ydbCampaign.id.toIdLong()
                    campaignType = ydbCampaign.advType.toEAdCampaignType()
                    clientId = subjectUser.clientId.asLong()
                }.build()
                spec = toCampaignSpec(ydbCampaign)
            }.build()
        )
        return ydbCampaign
    }
}
