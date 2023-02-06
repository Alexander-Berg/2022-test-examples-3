package ru.yandex.direct.web.entity.uac.controller

import com.nhaarman.mockitokotlin2.any
import org.apache.commons.lang.RandomStringUtils
import org.assertj.core.api.Assertions
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.instanceOf
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.bidmodifier.AgeType
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics
import ru.yandex.direct.core.entity.bidmodifier.BidModifierGeo
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType
import ru.yandex.direct.core.entity.bidmodifier.GenderType
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign
import ru.yandex.direct.core.entity.campaign.model.StrategyName
import ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes
import ru.yandex.direct.core.entity.keyword.service.validation.KeywordDefectIds
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefectIds
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefectIds
import ru.yandex.direct.core.entity.mobileapp.model.MobileGoalConversions
import ru.yandex.direct.core.entity.mobileapp.repository.MobileAppConversionStatisticRepository
import ru.yandex.direct.core.entity.mobilegoals.MobileAppGoalsService
import ru.yandex.direct.core.entity.region.validation.RegionIdDefectIds
import ru.yandex.direct.core.entity.retargeting.model.ConditionType
import ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionService
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.AgePoint
import ru.yandex.direct.core.entity.uac.model.Gender
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.core.entity.uac.model.TargetType
import ru.yandex.direct.core.entity.uac.model.UacAdjustmentRequest
import ru.yandex.direct.core.entity.uac.model.UacRetargetingCondition
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRule
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoal
import ru.yandex.direct.core.entity.uac.model.UacStrategy
import ru.yandex.direct.core.entity.uac.model.UacStrategyData
import ru.yandex.direct.core.entity.uac.model.UacStrategyName
import ru.yandex.direct.core.entity.uac.model.UacStrategyPlatform
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils
import ru.yandex.direct.core.entity.uac.repository.ydb.model.CounterType
import ru.yandex.direct.core.entity.uac.repository.ydb.model.TrackerAppEvent
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacDisabledPlaces
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.service.UacAppInfoService
import ru.yandex.direct.core.entity.uac.service.UacRetargetingService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.repository.TestUacYdbTrackerUrlStatRepository
import ru.yandex.direct.core.testing.repository.UacYdbTrackerUrlStat
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.UacCampaignSteps
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.test.utils.checkNotEmpty
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.utils.JsonUtils.fromJson
import ru.yandex.direct.validation.defect.ids.CollectionDefectIds
import ru.yandex.direct.validation.defect.ids.StringDefectIds
import ru.yandex.direct.validation.result.DefectId
import ru.yandex.direct.validation.result.DefectIds
import ru.yandex.direct.validation.result.Path
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.uac.model.PatchCampaignRequest
import ru.yandex.direct.web.entity.uac.service.emptyPatchInternalRequest
import ru.yandex.direct.web.entity.uac.service.emptyPatchRequest
import ru.yandex.direct.web.validation.model.WebValidationResult
import java.math.BigDecimal
import java.time.LocalDateTime

abstract class UacCampaignUpdateControllerTestBase {
    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    protected lateinit var steps: Steps

    @Autowired
    private lateinit var bidModifierService: BidModifierService

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Autowired
    protected lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var mobileAppGoalsService: MobileAppGoalsService

    @Autowired
    protected lateinit var retargetingConditionService: RetargetingConditionService

    @Autowired
    private lateinit var uacAppInfoService: UacAppInfoService

    @Autowired
    private lateinit var uacAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    private lateinit var uacCampaignSteps: UacCampaignSteps

    @Autowired
    private lateinit var testUacYdbTrackerUrlStatRepository: TestUacYdbTrackerUrlStatRepository

    @Autowired
    private lateinit var mobileAppConversionStatisticRepository: MobileAppConversionStatisticRepository

    protected lateinit var mockMvc: MockMvc

    protected lateinit var clientInfo: ClientInfo
    protected lateinit var uacCampaignId: String
    protected lateinit var draftUacCampaignId: String
    protected lateinit var moderatingCampaignId: String
    protected lateinit var uacCampaignWithoutCpaId: String

    protected lateinit var uacTextCampaignId: String

    abstract fun getCampaign(id: String): UacYdbCampaign?
    abstract fun getDirectCampaignId(uacCampaignId: String): Long?
    abstract fun createBanner(uacCampaignId: String)
    abstract fun createHtml5Content(): String
    abstract fun getDirectCampaign(shard: Int, campaignId: String): MobileContentCampaign
    abstract fun generateMoney(value: Long): BigDecimal

    companion object {
        val TRACKER_URL_STAT = UacYdbTrackerUrlStat(
            updateTime = UacYdbUtils.toEpochSecond(LocalDateTime.now()),
            trackerUrl = "https://redirect.appmetrica.yandex.com/serve/1179849830915165578",
            counterType = CounterType.CLICK,
            hitCount = 100,
            conversionsByEvent = mapOf(
                TrackerAppEvent.PURCHASED to 100
            )
        )
    }

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        clientInfo = steps.clientSteps().createDefaultClient()
        testAuthHelper.setOperatorAndSubjectUser(clientInfo.uid)
        TestAuthHelper.setSecurityContextWithAuthentication(directWebAuthenticationSource.authentication)
        steps.dbQueueSteps().registerJobType(DbQueueJobTypes.UAC_UPDATE_ADS)
        steps.dbQueueSteps().registerJobType(DbQueueJobTypes.RECALC_BRAND_LIFT_CAMPAIGNS)

        Mockito.doReturn(listOf<MobileGoalConversions>()).`when`(mobileAppConversionStatisticRepository).getConversionStats(
            Mockito.anyString(), Mockito.anyString(), ArgumentMatchers.anyList(), ArgumentMatchers.anyInt()
        )
    }

    @After
    fun after() {
        testUacYdbTrackerUrlStatRepository.clean()
    }

    @Test
    fun updateNonExistentTest() {
        val request = emptyPatchInternalRequest()

        mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/1231231?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun updateEmptyCampaignName() {
        val request = emptyPatchRequest().copy(
            displayName = ""
        )

        doRequestAndExpectValidationError(
            request = request,
            path = path(field(PatchCampaignRequest::displayName.name)),
            defectId = StringDefectIds.CANNOT_BE_EMPTY,
        )
    }

    @Test
    fun updateTooLongCampaignName() {
        val request = emptyPatchRequest().copy(
            displayName = RandomStringUtils.randomAlphabetic(300)
        )

        doRequestAndExpectValidationError(
            request = request,
            path = path(field(PatchCampaignRequest::displayName.name)),
            defectId = StringDefectIds.LENGTH_CANNOT_BE_MORE_THAN_MAX,
        )
    }

    @Test
    fun updateEmptyHref() {
        val request = emptyPatchRequest().copy(
            href = ""
        )

        doRequestAndExpectValidationError(
            request = request,
            path = path(field(PatchCampaignRequest::href.name)),
            defectId = StringDefectIds.CANNOT_BE_EMPTY,
        )
    }

    @Test
    fun updateInvalidHref() {
        val request = emptyPatchRequest().copy(
            href = "hrgagfsdfsd"
        )

        doRequestAndExpectValidationError(
            request = request,
            path = path(field(PatchCampaignRequest::href.name)),
            defectId = DefectIds.INVALID_VALUE,
        )
    }

    @Test
    fun updateNonExistentAppId() {
        val request = emptyPatchRequest().copy(
            appId = "4234234233244"
        )

        doRequestAndExpectValidationError(
            campaignId = draftUacCampaignId,
            request = request,
            path = path(field(PatchCampaignRequest::appId.name)),
            defectId = DefectIds.OBJECT_NOT_FOUND,
        )
    }

    @Test
    fun updateEmptyTexts() {
        val request = emptyPatchRequest().copy(
            texts = emptyList()
        )

        doRequestAndExpectValidationError(
            request = request,
            path = path(field(PatchCampaignRequest::texts.name)),
            defectId = CollectionDefectIds.Gen.CANNOT_BE_EMPTY,
        )
    }

    @Test
    fun updateEmptyTitles() {
        val request = emptyPatchRequest().copy(
            titles = emptyList()
        )

        doRequestAndExpectValidationError(
            request = request,
            path = path(field(PatchCampaignRequest::titles.name)),
            defectId = CollectionDefectIds.Gen.CANNOT_BE_EMPTY,
        )
    }

    @Test
    fun updateNonExistentContentIds() {
        val request = emptyPatchRequest().copy(
            contentIds = listOf("12312312312")
        )

        doRequestAndExpectValidationError(
            request = request,
            path = path(field(PatchCampaignRequest::contentIds.name), index(0)),
            defectId = DefectIds.OBJECT_NOT_FOUND,
        )
    }

    @Test
    fun updateEmptyRegions() {
        val request = emptyPatchRequest().copy(
            regions = emptyList()
        )

        doRequestAndExpectValidationError(
            request = request,
            path = path(field(PatchCampaignRequest::regions.name)),
            defectId = RegionIdDefectIds.Gen.EMPTY_REGIONS,
        )
    }

    @Test
    fun updateTooManyTitles() {
        val request = emptyPatchRequest().copy(
            titles = List(5) { RandomStringUtils.randomAlphabetic(5) }
        )

        doRequestAndExpectValidationError(
            request = request,
            path = path(field(PatchCampaignRequest::titles.name)),
            defectId = CollectionDefectIds.Size.SIZE_CANNOT_BE_MORE_THAN_MAX,
        )
    }

    @Test
    fun updateTooManyTexts() {
        val request = emptyPatchRequest().copy(
            texts = List(5) { RandomStringUtils.randomAlphabetic(5) }
        )

        doRequestAndExpectValidationError(
            request = request,
            path = path(field(PatchCampaignRequest::texts.name)),
            defectId = CollectionDefectIds.Size.SIZE_CANNOT_BE_MORE_THAN_MAX,
        )
    }

    @Test
    fun updateTitlesWithProperty() {
        val directCampaignId = getDirectCampaignId(uacCampaignId)!!
        ppcPropertiesSupport.set(PpcPropertyNames.MAX_TITLES_IN_UAC, "5")
        val request = emptyPatchRequest().copy(
            titles = List(5) { RandomStringUtils.randomAlphabetic(5) }
        )

        mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/direct/$directCampaignId?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
        ppcPropertiesSupport.remove(PpcPropertyNames.MAX_TITLES_IN_UAC)
    }

    @Test
    fun updateTextsWithProperty() {
        val directCampaignId = getDirectCampaignId(uacCampaignId)!!
        ppcPropertiesSupport.set(PpcPropertyNames.MAX_TEXTS_IN_UAC, "5")
        val request = emptyPatchRequest().copy(
            texts = List(5) { RandomStringUtils.randomAlphabetic(5) }
        )

        mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/direct/$directCampaignId?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
        ppcPropertiesSupport.remove(PpcPropertyNames.MAX_TEXTS_IN_UAC)
    }

    @Test
    fun updateTooManyKeywords() {
        val request = emptyPatchRequest().copy(
            keywords = List(201) { RandomStringUtils.randomAlphabetic(15) }
        )

        doRequestAndExpectValidationError(
            request = request,
            path = path(field(PatchCampaignRequest::keywords.name), index(0)),
            defectId = KeywordDefectIds.Keyword.MAX_KEYWORDS_PER_AD_GROUP_EXCEEDED,
        )
    }


    @Test
    fun updateTooLongMinusKeyword() {
        val request = emptyPatchRequest().copy(
            minusKeywords = List(1) { RandomStringUtils.randomAlphabetic(36) }
        )

        doRequestAndExpectValidationError(
            request = request,
            path = path(field(PatchCampaignRequest::minusKeywords.name)),
            defectId = MinusPhraseDefectIds.IndividualKeywordLength.MAX_LENGTH_MINUS_WORD,
        )
    }

    @Test
    fun updateTooManyMinusKeywords() {
        val request = emptyPatchRequest().copy(
            minusKeywords = List(2000) { RandomStringUtils.randomAlphabetic(20) }
        )

        doRequestAndExpectValidationError(
            request = request,
            path = path(field(PatchCampaignRequest::minusKeywords.name)),
            defectId = MinusPhraseDefectIds.StringLength.MAX_LENGTH_MINUS_KEYWORDS,
        )
    }

    @Test
    fun updateLongMinusKeywords() {
        val request = emptyPatchRequest().copy(
            minusKeywords = List(1000) { RandomStringUtils.randomAlphabetic(20) }
        )

        doRequestAndReturnStatusInfo(campaignId = uacCampaignId, request = request)
            .checkEquals(Status.STARTED to false)
    }

    @Test
    fun updateDisplayNameCorrectlyWithDirectId() {
        steps
            .featureSteps()
            .addClientFeature(clientInfo.clientId, FeatureName.IN_APP_EVENTS_IN_RMP_ENABLED, true)
        val directCampaignId = getDirectCampaignId(uacCampaignId)!!
        val request = emptyPatchRequest().copy(
            displayName = "New campaign name",
        )

        val resultRaw = mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/direct/$directCampaignId?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(resultRaw)
        val campaign = getCampaign(resultJsonTree["result"]["id"].asText())
        assertThat(campaign?.name, `is`("New campaign name"))
    }

    @Test
    fun updateModeratingCampaignWithoutBanners_IsObsoleteIsFalse() {
        steps
            .featureSteps()
            .addClientFeature(clientInfo.clientId, FeatureName.IN_APP_EVENTS_IN_RMP_ENABLED, true)
        val request = emptyPatchRequest().copy(
            displayName = "New campaign name",
        )

        doRequestAndReturnStatusInfo(campaignId = moderatingCampaignId, request = request)
            .checkEquals(Status.MODERATING to false)
    }

    @Test
    fun updateDraftCampaign_IsObsoleteIsFalse() {
        steps
            .featureSteps()
            .addClientFeature(clientInfo.clientId, FeatureName.IN_APP_EVENTS_IN_RMP_ENABLED, true)
        val request = emptyPatchRequest().copy(
            displayName = "New campaign name",
        )

        doRequestAndReturnStatusInfo(campaignId = draftUacCampaignId, request = request)
            .checkEquals(Status.DRAFT to false)
    }

    @Test
    fun updateStartedCampaign_IsObsoleteIsTrue() {
        steps
            .featureSteps()
            .addClientFeature(clientInfo.clientId, FeatureName.IN_APP_EVENTS_IN_RMP_ENABLED, true)
        createBanner(uacCampaignId)

        val request = emptyPatchRequest().copy(
            displayName = "New campaign name",
        )

        doRequestAndReturnStatusInfo(campaignId = uacCampaignId, request = request)
            .checkEquals(Status.STARTED to true)
    }

    @Test
    fun updateStrategyPlatform() {
        steps
            .featureSteps()
            .addClientFeature(clientInfo.clientId, FeatureName.IN_APP_EVENTS_IN_RMP_ENABLED, true)
        val request = emptyPatchRequest().copy(
            strategyPlatform = UacStrategyPlatform.CONTEXT,
        )

        val resultRaw = mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/$uacCampaignId?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(resultRaw)
        val campaign = getCampaign(resultJsonTree["result"]["id"].asText())
        assertThat(campaign?.strategyPlatform, `is`(UacStrategyPlatform.CONTEXT))
    }

    @Test
    fun updateKeywords() {
        val request = emptyPatchRequest().copy(
            keywords = listOf("keyword1", "keyword2"),
        )

        val resultRaw = mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/$uacCampaignId?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(resultRaw)
        val campaign = getCampaign(resultJsonTree["result"]["id"].asText())
        assertThat(campaign?.keywords, `is`(listOf("keyword1", "keyword2")))
    }

    @Test
    fun updateMinusKeywords() {
        val request = emptyPatchRequest().copy(
            minusKeywords = listOf("minusKeyword1", "minusKeyword2"),
        )

        val resultRaw = mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/$uacCampaignId?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(resultRaw)
        val campaign = getCampaign(resultJsonTree["result"]["id"].asText())
        assertThat(campaign?.minusKeywords, `is`(listOf("minusKeyword1", "minusKeyword2")))
    }

    @Test
    fun updateAdjustments() {
        var request = emptyPatchRequest().copy(
            adjustments = listOf(
                UacAdjustmentRequest(
                    region = 225,
                    age = null,
                    gender = null,
                    percent = 50,
                    retargetingConditionId = null
                ),
                UacAdjustmentRequest(
                    region = null,
                    age = AgePoint.AGE_25,
                    gender = Gender.FEMALE,
                    percent = 100,
                    retargetingConditionId = null
                ),
            ),
        )

        mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/$uacCampaignId?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val directCampaignId = getDirectCampaignId(uacCampaignId)!!
        var bidModifiers = bidModifierService.getByCampaignIds(
            clientInfo.clientId!!,
            mutableListOf(directCampaignId),
            setOf(BidModifierType.GEO_MULTIPLIER, BidModifierType.DEMOGRAPHY_MULTIPLIER),
            setOf(BidModifierLevel.CAMPAIGN),
            clientInfo.uid
        )
        assertThat(bidModifiers, hasSize(2))
        var geoIndex = if (bidModifiers[0] is BidModifierGeo) 0 else 1
        assertThat(bidModifiers[geoIndex], instanceOf(BidModifierGeo::class.java))
        var bidModifierGeo = bidModifiers[geoIndex] as BidModifierGeo
        assertThat(bidModifierGeo.regionalAdjustments.filter { it.hidden == false }, hasSize(1))
        Assertions.assertThat(bidModifierGeo.regionalAdjustments[0].regionId).isEqualTo(225)
        Assertions.assertThat(bidModifierGeo.regionalAdjustments[0].percent).isEqualTo(150)

        assertThat(bidModifiers[1 - geoIndex], instanceOf(BidModifierDemographics::class.java))
        var bidModifierDemographics = bidModifiers[1 - geoIndex] as BidModifierDemographics
        assertThat(bidModifierDemographics.demographicsAdjustments, hasSize(1))
        assertThat(bidModifierDemographics.demographicsAdjustments[0].age, equalTo(AgeType._25_34))
        assertThat(bidModifierDemographics.demographicsAdjustments[0].gender, equalTo(GenderType.FEMALE))
        assertThat(bidModifierDemographics.demographicsAdjustments[0].percent, equalTo(200))

        request = emptyPatchRequest().copy(
            adjustments = listOf(
                UacAdjustmentRequest(
                    region = 225,
                    age = null,
                    gender = null,
                    percent = -25,
                    retargetingConditionId = null
                ),
                UacAdjustmentRequest(
                    region = 1,
                    age = null,
                    gender = null,
                    percent = -60,
                    retargetingConditionId = null
                ),
                UacAdjustmentRequest(
                    region = null,
                    age = null,
                    gender = Gender.MALE,
                    percent = 0,
                    retargetingConditionId = null
                ),
            ),
        )

        mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/$uacCampaignId?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        bidModifiers = bidModifierService.getByCampaignIds(
            clientInfo.clientId!!,
            mutableListOf(directCampaignId),
            setOf(BidModifierType.GEO_MULTIPLIER, BidModifierType.DEMOGRAPHY_MULTIPLIER),
            setOf(BidModifierLevel.CAMPAIGN),
            clientInfo.uid
        )
        assertThat(bidModifiers, hasSize(2))
        geoIndex = if (bidModifiers[0] is BidModifierGeo) 0 else 1
        assertThat(bidModifiers[geoIndex], instanceOf(BidModifierGeo::class.java))
        bidModifierGeo = bidModifiers[geoIndex] as BidModifierGeo
        val regionalAdjustments = bidModifierGeo.regionalAdjustments.filter { it.hidden == false }
        assertThat(regionalAdjustments, hasSize(2))
        val region1Index = if (regionalAdjustments[0].regionId == 1L) 0 else 1
        assertThat(regionalAdjustments[1 - region1Index].regionId, equalTo(225))
        assertThat(regionalAdjustments[1 - region1Index].percent, equalTo(75))
        assertThat(regionalAdjustments[region1Index].regionId, equalTo(1))
        assertThat(regionalAdjustments[region1Index].percent, equalTo(40))

        assertThat(bidModifiers[1 - geoIndex], instanceOf(BidModifierDemographics::class.java))
        bidModifierDemographics = bidModifiers[1 - geoIndex] as BidModifierDemographics
        assertThat(bidModifierDemographics.demographicsAdjustments, hasSize(1))
        assertThat(bidModifierDemographics.demographicsAdjustments[0].age, equalTo(null))
        assertThat(bidModifierDemographics.demographicsAdjustments[0].gender, equalTo(GenderType.MALE))
        assertThat(bidModifierDemographics.demographicsAdjustments[0].percent, equalTo(100))
    }

    @Test
    fun updateMinusRegions() {
        val request = emptyPatchRequest().copy(
            minusRegions = listOf(213, 977),
        )

        val resultRaw = mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/$uacCampaignId?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(resultRaw)
        val campaign = getCampaign(resultJsonTree["result"]["id"].asText())
        assertThat(campaign?.minusRegions, `is`(listOf(213L, 977L)))
    }

    @Test
    fun updateBadKeywords() {
        val request = emptyPatchRequest().copy(
            keywords = listOf("first keyword -first")
        )

        doRequestAndExpectValidationError(
            request = request,
            path = path(field(PatchCampaignRequest::keywords.name), index(0)),
            defectId = PhraseDefectIds.String.MINUS_WORD_DELETE_PLUS_WORD,
        )
    }

    @Test
    fun updateWithOnlyHtml5Contents() {
        val contentId = createHtml5Content()
        val request = emptyPatchRequest().copy(
            texts = listOf(), titles = listOf(), contentIds = listOf(contentId)
        )

        mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/$uacCampaignId?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun updateCrrStrategy() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.CRR_STRATEGY_ALLOWED, true)
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.FIX_CRR_STRATEGY_ALLOWED, true)
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.IN_APP_EVENTS_IN_RMP_ENABLED, true)
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.UAC_ROAS_STRATEGY, true)
        val uacCampaignInfo = uacCampaignSteps.createMobileAppCampaign(clientInfo, cpa = null)
        Mockito.doReturn(
            listOf(
                MobileGoalConversions(TargetType.PAYMENT.goalId!!, 0, 100, 100)
            )
        ).`when`(mobileAppConversionStatisticRepository).getConversionStats(
            any(), any(), any(), any()
        )

        testUacYdbTrackerUrlStatRepository.insertStat(TRACKER_URL_STAT)

        val request = emptyPatchRequest().copy(
            targetId = TargetType.PAYMENT,
            strategy = UacStrategy(
                UacStrategyName.AUTOBUDGET_CRR,
                UacStrategyData(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    crr = 15,
                    payForConversion = true
                )
            )
        )

        val res = mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/${uacCampaignWithoutCpaId}?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
        testUacYdbTrackerUrlStatRepository.clean()
        val resultRaw = res.andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(resultRaw)
        val campaign = getCampaign(resultJsonTree["result"]["id"].asText())
        assertThat(campaign?.strategy?.uacStrategyData?.crr, `is`(15))
        val directCampaign = getDirectCampaign(clientInfo.shard, campaign!!.id)
        softly {
            assertThat(directCampaign.strategy.strategyName).isEqualTo(StrategyName.AUTOBUDGET_CRR)
            assertThat(directCampaign.strategy.strategyData.goalId).isEqualTo(TargetType.PAYMENT.goalId)
            assertThat(directCampaign.strategy.strategyData.crr).isEqualTo(15L)
        }
    }

    @Test
    fun updateCpaStrategyUsingStrategyData() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.IN_APP_EVENTS_IN_RMP_ENABLED, true)
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.UAC_FIX_CPA_STRATEGY_ENABLED, true)
        val request = emptyPatchRequest().copy(
            targetId = TargetType.PAYMENT,
            strategy = UacStrategy(
                UacStrategyName.AUTOBUDGET_AVG_CPA,
                UacStrategyData(
                    avgCpa = BigDecimal.valueOf(15),
                    payForConversion = true
                )
            )
        )

        val res = mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/$uacCampaignId?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
        val resultRaw = res.andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(resultRaw)
        val campaign = getCampaign(resultJsonTree["result"]["id"].asText())
        assertThat(campaign?.strategy?.uacStrategyData?.avgCpa, `is`(generateMoney(15)))
        val directCampaign = getDirectCampaign(clientInfo.shard, campaign!!.id)
        softly {
            assertThat(directCampaign.strategy.strategyName).isEqualTo(StrategyName.AUTOBUDGET_AVG_CPA)
            assertThat(directCampaign.strategy.strategyData.goalId).isEqualTo(TargetType.PAYMENT.goalId)
            assertThat(directCampaign.strategy.strategyData.avgCpa).isEqualTo(BigDecimal.valueOf(15))
        }
    }

    @Test
    fun updateCpiStrategyUsingStrategyData() {
        steps.featureSteps()
            .addClientFeature(clientInfo.clientId, FeatureName.IN_APP_EVENTS_IN_RMP_ENABLED, true)
        val request = emptyPatchRequest().copy(
            targetId = TargetType.INSTALL,
            strategy = UacStrategy(
                UacStrategyName.AUTOBUDGET_AVG_CPI,
                UacStrategyData(
                    payForConversion = true,
                    avgCpi = BigDecimal.valueOf(15)
                )
            )
        )

        val res = mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/$uacCampaignId?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
        val resultRaw = res.andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(resultRaw)
        val campaign = getCampaign(resultJsonTree["result"]["id"].asText())
        assertThat(campaign?.strategy?.uacStrategyData?.avgCpi, `is`(generateMoney(15)))
        val directCampaign = getDirectCampaign(clientInfo.shard, campaign!!.id)
        softly {
            assertThat(directCampaign.strategy.strategyName).isEqualTo(StrategyName.AUTOBUDGET_AVG_CPI)
            assertThat(directCampaign.strategy.strategyData.goalId as Long?).isEqualTo(null)
            assertThat(directCampaign.strategy.strategyData.avgCpi).isEqualTo(BigDecimal.valueOf(15))
        }
    }

    @Test
    fun updateClickStrategyUsingStrategyData() {
        val request = emptyPatchRequest().copy(
            targetId = TargetType.CPC,
            strategy = UacStrategy(
                UacStrategyName.AUTOBUDGET_AVG_CLICK,
                UacStrategyData(
                    payForConversion = true,
                    avgBid = BigDecimal.valueOf(15)
                )
            )
        )

        val res = mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/$uacCampaignId?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
        val resultRaw = res.andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(resultRaw)
        val campaign = getCampaign(resultJsonTree["result"]["id"].asText())
        assertThat(campaign?.strategy?.uacStrategyData?.avgBid, `is`(generateMoney(15)))
        val directCampaign = getDirectCampaign(clientInfo.shard, campaign!!.id)
        softly {
            assertThat(directCampaign.strategy.strategyName).isEqualTo(StrategyName.AUTOBUDGET_AVG_CLICK)
            assertThat(directCampaign.strategy.strategyData.goalId as Long?).isEqualTo(null)
            assertThat(directCampaign.strategy.strategyData.avgBid).isEqualTo(BigDecimal.valueOf(15))
        }
    }

    @Test
    fun updateUacCampaignWithRetargetingConditionTest() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.IN_APP_MOBILE_TARGETING, true)
        val ydbAppInfo = defaultAppInfo()
        uacAppInfoRepository.saveAppInfo(ydbAppInfo)
        val appInfo = uacAppInfoService.getAppInfo(ydbAppInfo)

        val mobileApp = steps.mobileAppSteps().createMobileApp(clientInfo, appInfo.url).mobileApp
        mobileAppGoalsService.updateMobileAppGoalsForExternalTracker(
            dslContextProvider.ppc(clientInfo.shard), clientInfo.clientId, listOf(mobileApp)
        )
        val mobileAppGoalIds =
            mobileAppGoalsService.getGoalsByApps(clientInfo.clientId, listOf(mobileApp)).map { it.id }

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
            clientInfo.clientId!!.asLong(),
            type = ConditionType.metrika_goals
        )
        val id = retargetingConditionService.addRetargetingConditions(
            listOf(directRetargetingCondition), clientInfo.clientId
        )[0].result

        var request = emptyPatchRequest().copy(
            retargetingCondition = UacRetargetingCondition(
                conditionRules = retargetingCondition.conditionRules,
                name = retargetingCondition.name,
                id = id
            )
        )
        var resultRaw = mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/$uacCampaignId?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        var resultJsonTree = JsonUtils.MAPPER.readTree(resultRaw)
        var campaign = getCampaign(resultJsonTree["result"]["id"].asText())
        assertThat(campaign?.retargetingCondition?.id, `is`(id))
        assertThat(campaign?.retargetingCondition?.name, `is`("Условие ретаргетинга 1"))
        assertThat(campaign?.retargetingCondition?.conditionRules, `is`(retargetingCondition.conditionRules))

        request = emptyPatchRequest().copy(retargetingCondition = null)
        resultRaw = mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/$uacCampaignId?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        resultJsonTree = JsonUtils.MAPPER.readTree(resultRaw)
        campaign = getCampaign(resultJsonTree["result"]["id"].asText())
        assertThat(campaign?.retargetingCondition, equalTo(null))
    }

    @Test
    fun test_UpdateCampaign_WithDisabledPlaces() {
        val request = emptyPatchRequest().copy(
            uacDisabledPlaces = UacDisabledPlaces(listOf("http://profi.ru/", "example.com"), null, null, null),
            targetId = TargetType.INSTALL,
            strategy = UacStrategy(
                UacStrategyName.AUTOBUDGET_AVG_CPI,
                UacStrategyData(
                    payForConversion = true,
                    avgCpi = BigDecimal.valueOf(15)
                )
            )
        )

        val resultRaw = doRequestAndReturnResponse(uacCampaignId, request, MockMvcResultMatchers.status().isOk)

        val resultJsonTree = JsonUtils.MAPPER.readTree(resultRaw)
        val campaign = getCampaign(resultJsonTree["result"]["id"].asText())
        assertThat(campaign?.uacDisabledPlaces?.disabledPlaces, `is`(listOf("http://profi.ru/", "example.com")))
        val directCampaign = getDirectCampaign(clientInfo.shard, campaign!!.id)
        softly {
            assertThat(directCampaign.disabledDomains).isEqualTo(listOf("example.com", "profi.ru"))
        }
    }

    private fun doRequestAndReturnStatusInfo(
        campaignId: String = uacCampaignId,
        request: PatchCampaignRequest,
    ): Pair<Status, Boolean> {
        val resultRaw = doRequestAndReturnResponse(campaignId, request, MockMvcResultMatchers.status().isOk)

        val resultJsonNode = JsonUtils.MAPPER.readTree(resultRaw)["result"]
        return Status.valueOf(resultJsonNode["status"].asText().uppercase()) to
            resultJsonNode["status_obsolete"].booleanValue()
    }

    private fun doRequestAndExpectValidationError(
        campaignId: String = uacCampaignId,
        request: PatchCampaignRequest,
        path: Path,
        defectId: DefectId<*>,
    ) {
        val resultRaw = doRequestAndReturnResponse(campaignId, request, MockMvcResultMatchers.status().isBadRequest)

        val validationResult =
            fromJson(fromJson(resultRaw)["validation_result"].toString(), WebValidationResult::class.java)
        validationResult.errors.checkNotEmpty()
        validationResult.errors[0].path
            .checkEquals(path.toString())
        validationResult.errors[0].code
            .checkEquals(defectId.code)
    }

    private fun doRequestAndReturnResponse(
        campaignId: String = uacCampaignId,
        request: PatchCampaignRequest,
        resultMatcher: ResultMatcher,
    ): String {
        return mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/$campaignId?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(resultMatcher)
            .andReturn()
            .response
            .contentAsString
    }
}
