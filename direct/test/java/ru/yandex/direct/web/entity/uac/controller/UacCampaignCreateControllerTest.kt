package ru.yandex.direct.web.entity.uac.controller

import com.google.common.base.Strings
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito
import org.mockito.Mockito.anyString
import org.mockito.Mockito.doReturn
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
import ru.yandex.direct.audience.client.YaAudienceClient
import ru.yandex.direct.audience.client.model.CreateExperimentResponse
import ru.yandex.direct.audience.client.model.CreateExperimentResponseEnvelope
import ru.yandex.direct.audience.client.model.ExperimentSegmentResponse
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefectIds
import ru.yandex.direct.core.entity.banner.type.href.BannerUrlCheckService
import ru.yandex.direct.core.entity.bidmodifier.AgeType
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics
import ru.yandex.direct.core.entity.bidmodifier.BidModifierGeo
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargeting
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType
import ru.yandex.direct.core.entity.bidmodifier.GenderType
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService
import ru.yandex.direct.core.entity.brandlift.repository.BrandSurveyRepository
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign
import ru.yandex.direct.core.entity.campaign.model.StrategyName
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds
import ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefectIds
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppAlternativeStore
import ru.yandex.direct.core.entity.mobileapp.model.MobileGoalConversions
import ru.yandex.direct.core.entity.mobileapp.repository.MobileAppConversionStatisticRepository
import ru.yandex.direct.core.entity.mobilegoals.MobileAppGoalsService
import ru.yandex.direct.core.entity.retargeting.model.ConditionType
import ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionService
import ru.yandex.direct.core.entity.uac.STORE_URL_FOR_APP_ID
import ru.yandex.direct.core.entity.uac.createDefaultHtml5Content
import ru.yandex.direct.core.entity.uac.createDefaultVideoContent
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.AdvType
import ru.yandex.direct.core.entity.uac.model.AgePoint
import ru.yandex.direct.core.entity.uac.model.AltAppStore
import ru.yandex.direct.core.entity.uac.model.CampaignStatuses
import ru.yandex.direct.core.entity.uac.model.DeviceType
import ru.yandex.direct.core.entity.uac.model.Gender
import ru.yandex.direct.core.entity.uac.model.InventoryType
import ru.yandex.direct.core.entity.uac.model.LimitPeriodType
import ru.yandex.direct.core.entity.uac.model.Sitelink
import ru.yandex.direct.core.entity.uac.model.Socdem
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.core.entity.uac.model.TargetStatus
import ru.yandex.direct.core.entity.uac.model.TargetType
import ru.yandex.direct.core.entity.uac.model.UacAdjustmentRequest
import ru.yandex.direct.core.entity.uac.model.UacGoal
import ru.yandex.direct.core.entity.uac.model.UacRetargetingCondition
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRule
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoal
import ru.yandex.direct.core.entity.uac.model.UacShowsFrequencyLimit
import ru.yandex.direct.core.entity.uac.model.UacStrategy
import ru.yandex.direct.core.entity.uac.model.UacStrategyData
import ru.yandex.direct.core.entity.uac.model.UacStrategyName
import ru.yandex.direct.core.entity.uac.model.UacStrategyPlatform
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAccountRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUserRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.CpmAssetButton
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacBannerMeasurerSystem
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacButtonAction
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacCpmAsset
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils
import ru.yandex.direct.core.entity.uac.repository.ydb.model.CounterType
import ru.yandex.direct.core.entity.uac.repository.ydb.model.TrackerAppEvent
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacDisabledPlaces
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacMeasurer
import ru.yandex.direct.core.entity.uac.service.UacAppInfoService
import ru.yandex.direct.core.entity.uac.service.UacRetargetingService
import ru.yandex.direct.core.entity.uac.service.appinfo.GooglePlayAppInfoGetter
import ru.yandex.direct.core.service.urlchecker.RedirectCheckResult
import ru.yandex.direct.core.testing.data.TestBanners
import ru.yandex.direct.core.testing.data.TestDomain
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.repository.TestUacYdbTrackerUrlStatRepository
import ru.yandex.direct.core.testing.repository.UacYdbTrackerUrlStat
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.regions.Region
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.test.utils.checkNotEmpty
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.validation.defect.ids.CollectionDefectIds
import ru.yandex.direct.validation.defect.ids.StringDefectIds
import ru.yandex.direct.validation.result.DefectIds
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.uac.model.CreateCampaignRequest
import ru.yandex.direct.web.entity.uac.model.PatchCampaignRequest
import ru.yandex.direct.web.entity.uac.service.YdbUacCampaignWebService
import ru.yandex.direct.web.entity.uac.toResponse
import ru.yandex.direct.web.validation.model.WebValidationResult
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@DirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacCampaignCreateControllerTest {
    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

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
    private lateinit var uacAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    private lateinit var uacCampaignRepository: UacYdbCampaignRepository

    @Autowired
    private lateinit var uacDirectCampaignRepository: UacYdbDirectCampaignRepository

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var uacAccountRepository: UacYdbAccountRepository

    @Autowired
    private lateinit var uacUserRepository: UacYdbUserRepository

    @Autowired
    private lateinit var ydbUacCampaignWebService: YdbUacCampaignWebService

    @Autowired
    private lateinit var googlePlayAppInfoGetter: GooglePlayAppInfoGetter

    @Autowired
    private lateinit var bidModifierService: BidModifierService

    private lateinit var userInfo: UserInfo

    @Autowired
    private lateinit var uacYdbContentRepository: UacYdbContentRepository

    @Autowired
    private lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    private lateinit var bannerUrlCheckService: BannerUrlCheckService

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var mobileAppGoalsService: MobileAppGoalsService

    @Autowired
    private lateinit var retargetingConditionService: RetargetingConditionService

    @Autowired
    private lateinit var uacAppInfoService: UacAppInfoService

    @Autowired
    private lateinit var yaAudienceClient: YaAudienceClient

    @Autowired
    private lateinit var brandSurveyRepository: BrandSurveyRepository

    @Autowired
    private lateinit var mobileAppConversionStatisticRepository: MobileAppConversionStatisticRepository

    @Autowired
    private lateinit var testUacYdbTrackerUrlStatRepository: TestUacYdbTrackerUrlStatRepository

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()

        val TRACKER_URL = "https://adidas.app.link/abcde"
        val TRACKER_URL_STAT = UacYdbTrackerUrlStat(
            updateTime = UacYdbUtils.toEpochSecond(LocalDateTime.now()),
            trackerUrl = TRACKER_URL,
            counterType = CounterType.CLICK,
            hitCount = 100,
            conversionsByEvent = mapOf(
                TrackerAppEvent.PURCHASED to 100
            )
        )
        val newCampaignStatuses = CampaignStatuses(Status.DRAFT, TargetStatus.STOPPED)
    }

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )

        steps.dbQueueSteps().registerJobType(DbQueueJobTypes.RECALC_BRAND_LIFT_CAMPAIGNS)

        steps.featureSteps()
            .addClientFeature(userInfo.clientId, FeatureName.DISABLE_BILLING_AGGREGATES, true)
        steps.featureSteps()
            .addClientFeature(userInfo.clientId, FeatureName.BRAND_LIFT, true)
        // нужно для проверки регионов, так как из ручки возвращается регион в локали en
        LocaleContextHolder.setLocale(Locale.ENGLISH)

        doReturn(RedirectCheckResult.createSuccessResult(STORE_URL_FOR_APP_ID, ""))
            .`when`(bannerUrlCheckService).getRedirect(anyString(), anyString(), anyBoolean())

        doReturn(listOf<MobileGoalConversions>())
            .`when`(mobileAppConversionStatisticRepository)
            .getConversionStats(anyString(), anyString(), anyList(), anyInt())
        testUacYdbTrackerUrlStatRepository.insertStat(TRACKER_URL_STAT)
    }

    @After
    fun after() {
        LocaleContextHolder.setLocale(null)
        testUacYdbTrackerUrlStatRepository.clean()
    }

    @Test
    fun createUacNonSkippbleCpmCampaignTest() {
        val content = createDefaultVideoContent()
        uacYdbContentRepository.saveContents(listOf(content))

        val request = createUacCampaignRequest(
            advType = AdvType.CPM_BANNER,

            strategy = UacStrategy(
                UacStrategyName.AUTOBUDGET_MAX_IMPRESSIONS,
                UacStrategyData(
                    BigDecimal.ZERO,
                    true,
                    BigDecimal.TEN,
                    LocalDate.now().plusMonths(6),
                    LocalDate.now(),
                    BigDecimal.valueOf(2100), BigDecimal.valueOf(100),
                    0
                )
            ),
            contentIds = listOf(content.id),
            titles = null,
            texts = null,
            keywords = null,
            nonSkippable = false,
            trackingUrl = null,
        )

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andDo { e -> System.err.println(e.response.contentAsString) }
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        val campaignId = resultJsonTree["result"]["id"].asText()
        val campaign = uacCampaignRepository.getCampaign(campaignId)
        val directCampaign = uacDirectCampaignRepository.getDirectCampaignById(campaignId)
        val filledCampaign =
            ydbUacCampaignWebService.fillCampaign(
                userInfo.user!!,
                userInfo.user!!,
                campaign!!,
                directCampaign!!.directCampaignId,
                newCampaignStatuses,
            )

        SoftAssertions.assertSoftly {
            it.assertThat(resultJsonTree)
                .isEqualTo(JsonUtils.MAPPER.readTree(JsonUtils.toJson(filledCampaign.toResponse())))

            // проверка, что пользователь и аккаунт создался
            val user = uacUserRepository.getUserByUid(userInfo.uid)
            val account = uacAccountRepository.getAccountByClientId(userInfo.clientId.asLong())
            it.assertThat(user).isNotNull
            it.assertThat(account).isNotNull
            val mysqlCampaign =
                campaignTypedRepository.getTyped(userInfo.shard, listOf(directCampaign!!.directCampaignId))[0]
            it.assertThat(mysqlCampaign).isNotNull
            it.assertThat(mysqlCampaign).isInstanceOf(CpmBannerCampaign::class.java)
        }
    }

    @Test
    fun createUacCpmCampaignTest() {
        val content = createDefaultVideoContent()
        uacYdbContentRepository.saveContents(listOf(content))

        val request = createUacCampaignRequest(
            advType = AdvType.CPM_BANNER,
            strategy = UacStrategy(
                UacStrategyName.AUTOBUDGET_MAX_IMPRESSIONS,
                UacStrategyData(
                    BigDecimal.ZERO,
                    true,
                    BigDecimal.TEN,
                    LocalDate.now().plusMonths(6),
                    LocalDate.now(),
                    BigDecimal.valueOf(2100), BigDecimal.ONE,
                    0
                )
            ),
            contentIds = listOf(content.id),
            titles = null,
            texts = null,
            keywords = null,
            nonSkippable = false,
            trackingUrl = null,
        )

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo {
                System.err.println("!!!"+it.response.contentAsString)
            }
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        val campaignId = resultJsonTree["result"]["id"].asText()
        val campaign = uacCampaignRepository.getCampaign(campaignId)
        val directCampaign = uacDirectCampaignRepository.getDirectCampaignById(campaignId)
        val filledCampaign = ydbUacCampaignWebService.fillCampaign(
            userInfo.user!!,
            userInfo.user!!,
            campaign!!,
            directCampaign!!.directCampaignId,
            newCampaignStatuses,
        )

        SoftAssertions.assertSoftly {
            it.assertThat(resultJsonTree)
                .isEqualTo(JsonUtils.MAPPER.readTree(JsonUtils.toJson(filledCampaign.toResponse())))

            // проверка, что пользователь и аккаунт создался
            val user = uacUserRepository.getUserByUid(userInfo.uid)
            val account = uacAccountRepository.getAccountByClientId(userInfo.clientId.asLong())
            it.assertThat(user).isNotNull
            it.assertThat(account).isNotNull
            val mysqlCampaign =
                campaignTypedRepository.getTyped(userInfo.shard, listOf(directCampaign!!.directCampaignId))[0]
            it.assertThat(mysqlCampaign).isNotNull
            it.assertThat(mysqlCampaign).isInstanceOf(CpmBannerCampaign::class.java)
        }
    }

    @Test
    fun createUacCpmCampaignWithRfTest() {
        val content = createDefaultVideoContent()
        uacYdbContentRepository.saveContents(listOf(content))

        val request = createUacCampaignRequest(
            advType = AdvType.CPM_BANNER,
            strategy = UacStrategy(
                UacStrategyName.AUTOBUDGET_MAX_IMPRESSIONS,
                UacStrategyData(
                    BigDecimal.ZERO,
                    true,
                    BigDecimal.TEN,
                    LocalDate.now().plusMonths(6),
                    LocalDate.now(),
                    BigDecimal.valueOf(2100), BigDecimal.ONE,
                    0
                )
            ),
            showsFrequencyLimit = UacShowsFrequencyLimit(impressionRateCount=12, impressionRateIntervalDays = 30),
            contentIds = listOf(content.id),
            titles = null,
            texts = null,
            keywords = null,
            nonSkippable = false,
            trackingUrl = null,
            inventoryTypes = null,
            )

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo { System.err.println(it.response.contentAsString) }
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        val campaignId = resultJsonTree["result"]["id"].asText()
        val campaign = uacCampaignRepository.getCampaign(campaignId)
        val directCampaign = uacDirectCampaignRepository.getDirectCampaignById(campaignId)
        val filledCampaign = ydbUacCampaignWebService.fillCampaign(
            userInfo.user!!,
            userInfo.user!!,
            campaign!!,
            directCampaign!!.directCampaignId,
            newCampaignStatuses,
        )

        SoftAssertions.assertSoftly {
            it.assertThat(resultJsonTree)
                .isEqualTo(JsonUtils.MAPPER.readTree(JsonUtils.toJson(filledCampaign.toResponse())))

            // проверка, что пользователь и аккаунт создался
            val user = uacUserRepository.getUserByUid(userInfo.uid)
            val account = uacAccountRepository.getAccountByClientId(userInfo.clientId.asLong())
            it.assertThat(user).isNotNull
            it.assertThat(account).isNotNull
            val mysqlCampaign =
                campaignTypedRepository.getTyped(userInfo.shard, listOf(directCampaign!!.directCampaignId))[0]
            it.assertThat(mysqlCampaign).isNotNull
            it.assertThat(mysqlCampaign).isInstanceOf(CpmBannerCampaign::class.java)

            val  cpmCampaign = mysqlCampaign as CpmBannerCampaign;

            it.assertThat(cpmCampaign.impressionRateCount).isEqualTo(12)
            it.assertThat(cpmCampaign.impressionRateIntervalDays).isEqualTo(30)
        }
    }



    @Test
    fun createUacCampaignTest() {
        val request = createUacCampaignRequest()

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo { e -> System.err.println("!!!!"+e.response.contentAsString) }
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        val campaignId = resultJsonTree["result"]["id"].asText()
        val campaign = uacCampaignRepository.getCampaign(campaignId)
        val directCampaign = uacDirectCampaignRepository.getDirectCampaignById(campaignId)
        val filledCampaign = ydbUacCampaignWebService.fillCampaign(
            userInfo.user!!,
            userInfo.user!!,
            campaign!!,
            directCampaign!!.directCampaignId,
            newCampaignStatuses,
        )

        SoftAssertions.assertSoftly {
            it.assertThat(resultJsonTree)
                .isEqualTo(JsonUtils.MAPPER.readTree(JsonUtils.toJson(filledCampaign.toResponse())))

            // проверка, что пользователь и аккаунт создался
            val user = uacUserRepository.getUserByUid(userInfo.uid)
            val account = uacAccountRepository.getAccountByClientId(userInfo.clientId.asLong())
            it.assertThat(user).isNotNull
            it.assertThat(account).isNotNull
            val mysqlCampaign =
                campaignTypedRepository.getTyped(userInfo.shard, listOf(directCampaign!!.directCampaignId))[0]
            it.assertThat(mysqlCampaign).isNotNull
            it.assertThat(mysqlCampaign).isInstanceOf(MobileContentCampaign::class.java)
        }
    }

    @Test
    fun createUacCampaignTest_WithoutTrackingUrlAndRegions() {
        val request = createUacCampaignRequest(trackingUrl = null, regions = null)

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo { e -> System.err.println(e.response.contentAsString) }
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        val campaignId = resultJsonTree["result"]["id"].asText()
        val campaign = uacCampaignRepository.getCampaign(campaignId)
        val directCampaign = uacDirectCampaignRepository.getDirectCampaignById(campaignId)
        val filledCampaign = ydbUacCampaignWebService.fillCampaign(
            userInfo.user!!,
            userInfo.user!!,
            campaign!!,
            directCampaign!!.directCampaignId,
            newCampaignStatuses,
        )

        SoftAssertions.assertSoftly {
            it.assertThat(resultJsonTree)
                .isEqualTo(JsonUtils.MAPPER.readTree(JsonUtils.toJson(filledCampaign.toResponse())))

            // проверка, что пользователь и аккаунт создался
            val user = uacUserRepository.getUserByUid(userInfo.uid)
            val account = uacAccountRepository.getAccountByClientId(userInfo.clientId.asLong())
            it.assertThat(user).isNotNull
            it.assertThat(account).isNotNull
            val mysqlCampaign =
                campaignTypedRepository.getTyped(userInfo.shard, listOf(directCampaign!!.directCampaignId))[0]
            it.assertThat(mysqlCampaign).isNotNull
            it.assertThat(mysqlCampaign).isInstanceOf(MobileContentCampaign::class.java)
        }
    }

    @Test
    fun createUacCampaignTest_WithoutRegionsTextsAndTitles() {
        val request = createUacCampaignRequest(texts = listOf(), regions = listOf(), titles = listOf())

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo { e -> System.err.println(e.response.contentAsString) }
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        val campaignId = resultJsonTree["result"]["id"].asText()
        val campaign = uacCampaignRepository.getCampaign(campaignId)
        val directCampaign = uacDirectCampaignRepository.getDirectCampaignById(campaignId)
        val filledCampaign = ydbUacCampaignWebService.fillCampaign(
            userInfo.user!!,
            userInfo.user!!,
            campaign!!,
            directCampaign!!.directCampaignId,
            newCampaignStatuses,
        )

        SoftAssertions.assertSoftly {
            it.assertThat(resultJsonTree)
                .isEqualTo(JsonUtils.MAPPER.readTree(JsonUtils.toJson(filledCampaign.toResponse())))

            // проверка, что пользователь и аккаунт создался
            val user = uacUserRepository.getUserByUid(userInfo.uid)
            val account = uacAccountRepository.getAccountByClientId(userInfo.clientId.asLong())
            it.assertThat(user).isNotNull
            it.assertThat(account).isNotNull
            val mysqlCampaign =
                campaignTypedRepository.getTyped(userInfo.shard, listOf(directCampaign!!.directCampaignId))[0]
            it.assertThat(mysqlCampaign).isNotNull
            it.assertThat(mysqlCampaign).isInstanceOf(MobileContentCampaign::class.java)
        }
    }

    @Test
    fun createUacCampaignTest_WithoutContents() {
        val request = createUacCampaignRequest(texts = listOf(), contentIds = listOf(), titles = listOf(), sitelinks = listOf())

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo { e -> System.err.println(e.response.contentAsString) }
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        val campaignId = resultJsonTree["result"]["id"].asText()
        val campaign = uacCampaignRepository.getCampaign(campaignId)
        val directCampaign = uacDirectCampaignRepository.getDirectCampaignById(campaignId)
        val filledCampaign = ydbUacCampaignWebService.fillCampaign(
            userInfo.user!!,
            userInfo.user!!,
            campaign!!,
            directCampaign!!.directCampaignId,
            newCampaignStatuses,
        )

        SoftAssertions.assertSoftly {
            it.assertThat(resultJsonTree)
                .isEqualTo(JsonUtils.MAPPER.readTree(JsonUtils.toJson(filledCampaign.toResponse())))

            // проверка, что пользователь и аккаунт создался
            val user = uacUserRepository.getUserByUid(userInfo.uid)
            val account = uacAccountRepository.getAccountByClientId(userInfo.clientId.asLong())
            it.assertThat(user).isNotNull
            it.assertThat(account).isNotNull
            val mysqlCampaign =
                campaignTypedRepository.getTyped(userInfo.shard, listOf(directCampaign!!.directCampaignId))[0]
            it.assertThat(mysqlCampaign).isNotNull
            it.assertThat(mysqlCampaign).isInstanceOf(MobileContentCampaign::class.java)
        }
    }

    @Test
    fun createUacEcomBadRequestTest() {
        val request = createUacCampaignRequest(isEcom = true)

        mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                        .content(JsonUtils.toJson(request))
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun createUacCampaignBadRequestTest() {
        val tooLongCampaignName = "Test text UC campaign".repeat(20)
        val request = createUacCampaignRequest(campaignName = tooLongCampaignName)

        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun createUacCampaignBadRequestWithCpmStrategyTest() {

        val cpmStrategy = UacStrategy(
            UacStrategyName.AUTOBUDGET_MAX_IMPRESSIONS,
            UacStrategyData(
                BigDecimal.TEN,
                false, BigDecimal.TEN,
                LocalDate.now(),
                LocalDate.now(),
                BigDecimal.ONE,
                BigDecimal.ONE,
                0
            )
        )

        val request = createUacCampaignRequest(strategy = cpmStrategy)

        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun createUacCampaignNoRightsTest() {
        val request = createUacCampaignRequest()
        val anotherUser = steps.userSteps().createDefaultUser()
        testAuthHelper.setSubjectUser(userInfo.uid)
        testAuthHelper.setOperator(anotherUser.uid)

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + anotherUser.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString
        assertThat(result).isEqualTo("No rights")
    }

    @Test
    fun createDraftUacCampaign_withoutTrackingUrl() {
        val campaignRequest = createUacCampaignRequest(
            trackingUrl = null,
        )

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(campaignRequest))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo { e -> System.err.println(e.response.contentAsString) }
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val jsonTree = JsonUtils.MAPPER.readTree(result)
        val id = jsonTree["result"]["id"].asText()
        val trackingUrl = jsonTree["result"]["tracking_url"].asText()
        val success = jsonTree["success"].asBoolean()

        SoftAssertions.assertSoftly{
            it.assertThat(uacCampaignRepository.getCampaign(id)).isNotNull
            it.assertThat(uacDirectCampaignRepository.getDirectCampaignById(id)).isNotNull
            it.assertThat(trackingUrl).isEqualTo("null")
            it.assertThat(success).isTrue
        }

    }

    @Test
    fun createUacCampaignWithStrategyPlatformTest() {
        val request = createUacCampaignRequest(strategyPlatform = UacStrategyPlatform.CONTEXT)

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo { e -> System.err.println(e.response.contentAsString) }
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        val campaignId = resultJsonTree["result"]["id"].asText()
        val campaign = uacCampaignRepository.getCampaign(campaignId)
        val directCampaign = uacDirectCampaignRepository.getDirectCampaignById(campaignId)
        val filledCampaign = ydbUacCampaignWebService.fillCampaign(
            userInfo.user!!,
            userInfo.user!!,
            campaign!!,
            directCampaign!!.directCampaignId,
            newCampaignStatuses,
        )

        SoftAssertions.assertSoftly {
            it.assertThat(resultJsonTree)
                .isEqualTo(JsonUtils.MAPPER.readTree(JsonUtils.toJson(filledCampaign.toResponse())))

            // проверка, что пользователь и аккаунт создался
            val user = uacUserRepository.getUserByUid(userInfo.uid)
            val account = uacAccountRepository.getAccountByClientId(userInfo.clientId.asLong())
            it.assertThat(user).isNotNull
            it.assertThat(account).isNotNull
            val mysqlCampaign =
                campaignTypedRepository.getTyped(
                    userInfo.shard,
                    listOf(directCampaign!!.directCampaignId)
                )[0] as MobileContentCampaign
            it.assertThat(mysqlCampaign).isNotNull
            it.assertThat(mysqlCampaign).isInstanceOf(MobileContentCampaign::class.java)
            it.assertThat(mysqlCampaign.strategy.platform).isEqualTo(CampaignsPlatform.CONTEXT)
        }
    }

    @Test
    fun createUacCpcCampaign() {
        val request = createUacCampaignRequest(targetId = TargetType.CPC)

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo { e -> System.err.println("!!!!" + e.response.contentAsString) }
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString
        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        val campaignId = resultJsonTree["result"]["id"].asText()
        val campaign = uacCampaignRepository.getCampaign(campaignId)
        val directCampaign = uacDirectCampaignRepository.getDirectCampaignById(campaignId)
        val filledCampaign = ydbUacCampaignWebService.fillCampaign(
            userInfo.user!!,
            userInfo.user!!,
            campaign!!,
            directCampaign!!.directCampaignId,
            newCampaignStatuses,
        )

        SoftAssertions.assertSoftly {
            it.assertThat(resultJsonTree)
                .isEqualTo(JsonUtils.MAPPER.readTree(JsonUtils.toJson(filledCampaign.toResponse())))

            // проверка, что пользователь и аккаунт создался
            val user = uacUserRepository.getUserByUid(userInfo.uid)
            val account = uacAccountRepository.getAccountByClientId(userInfo.clientId.asLong())
            it.assertThat(user).isNotNull
            it.assertThat(account).isNotNull
            val mysqlCampaign =
                campaignTypedRepository.getTyped(userInfo.shard, listOf(directCampaign!!.directCampaignId))[0]
            it.assertThat(mysqlCampaign).isNotNull
            it.assertThat(mysqlCampaign).isInstanceOf(MobileContentCampaign::class.java)
        }
    }

    @Test
    fun createUacCpcCampaign_withStrategyData() {
        val request = createUacCampaignRequest(
            targetId = TargetType.CPC,
            strategy = UacStrategy(
                UacStrategyName.AUTOBUDGET_AVG_CLICK,
                UacStrategyData(
                    avgBid = BigDecimal(100)
                )
            )
        )

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo { e -> System.err.println("!!!!" + e.response.contentAsString) }
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString
        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        val campaignId = resultJsonTree["result"]["id"].asText()
        val campaign = uacCampaignRepository.getCampaign(campaignId)
        val directCampaign = uacDirectCampaignRepository.getDirectCampaignById(campaignId)
        val filledCampaign = ydbUacCampaignWebService.fillCampaign(
            userInfo.user!!,
            userInfo.user!!,
            campaign!!,
            directCampaign!!.directCampaignId,
            newCampaignStatuses,
        )

        SoftAssertions.assertSoftly {
            it.assertThat(resultJsonTree)
                .isEqualTo(JsonUtils.MAPPER.readTree(JsonUtils.toJson(filledCampaign.toResponse())))

            // проверка, что пользователь и аккаунт создался
            val user = uacUserRepository.getUserByUid(userInfo.uid)
            val account = uacAccountRepository.getAccountByClientId(userInfo.clientId.asLong())
            it.assertThat(user).isNotNull
            it.assertThat(account).isNotNull
            val mysqlCampaign =
                campaignTypedRepository.getTyped(userInfo.shard, listOf(directCampaign!!.directCampaignId))[0]
            it.assertThat(mysqlCampaign).isNotNull
            it.assertThat(mysqlCampaign).isInstanceOf(MobileContentCampaign::class.java)
            val mobileContentCampaign = mysqlCampaign as MobileContentCampaign
            it.assertThat(mobileContentCampaign.strategy.strategyName).isEqualTo(StrategyName.AUTOBUDGET_AVG_CLICK)
            it.assertThat(mobileContentCampaign.strategy.strategyData.avgBid).isEqualTo(BigDecimal.valueOf(100))
        }
    }

    @Test
    fun createDraftUacCampaign_withBidModifiers() {
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.IN_APP_MOBILE_TARGETING, true)
        val ydbAppInfo = defaultAppInfo()
        uacAppInfoRepository.saveAppInfo(ydbAppInfo)
        val appInfo = uacAppInfoService.getAppInfo(ydbAppInfo)

        val mobileApp = steps.mobileAppSteps().createMobileApp(userInfo.clientInfo!!, appInfo.url).mobileApp
        mobileAppGoalsService.updateMobileAppGoalsForExternalTracker(
            dslContextProvider.ppc(userInfo.shard), userInfo.clientId, listOf(mobileApp))
        val mobileAppGoalIds =  mobileAppGoalsService.getGoalsByApps(userInfo.clientId, listOf(mobileApp)).map { it.id }

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

        val campaignRequest = createUacCampaignRequest(
            adjustments = listOf(
                UacAdjustmentRequest(region = 225, age = null, gender = null, percent = 50, retargetingConditionId = null),
                UacAdjustmentRequest(region = 1, age = null, gender = null, percent = -20, retargetingConditionId = null),
                UacAdjustmentRequest(region = null, age = null, gender = Gender.MALE, percent = 100, retargetingConditionId = null),
                UacAdjustmentRequest(region = null, age = AgePoint.AGE_25, gender = Gender.FEMALE, percent = 33, retargetingConditionId = null),
                UacAdjustmentRequest(region = null, age = null, gender = null, percent = 130, retargetingConditionId = retargetingConditionId),
            ),
        )
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(campaignRequest))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo { e -> System.err.println(e.response.contentAsString) }
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString


        val jsonTree = JsonUtils.MAPPER.readTree(result)
        val id = jsonTree["result"]["id"].asText()
        val success = jsonTree["success"].asBoolean()

        SoftAssertions.assertSoftly {
            it.assertThat(uacCampaignRepository.getCampaign(id)).isNotNull
            it.assertThat(uacDirectCampaignRepository.getDirectCampaignById(id)).isNotNull
            it.assertThat(success).isTrue
        }

        val directCampaignId = uacDirectCampaignRepository.getDirectCampaignById(id)!!.directCampaignId
        val bidModifiers = bidModifierService.getByCampaignIds(
            userInfo.clientId,
            mutableListOf(directCampaignId),
            setOf(BidModifierType.GEO_MULTIPLIER, BidModifierType.DEMOGRAPHY_MULTIPLIER, BidModifierType.RETARGETING_MULTIPLIER),
            setOf(BidModifierLevel.CAMPAIGN),
            userInfo.uid
        )
        val soft = SoftAssertions()

        soft.assertThat(jsonTree["result"]["adjustments"]).hasSize(5)
        soft.assertThat(bidModifiers).hasSize(3)

        val geoIndex = if (bidModifiers[0] is BidModifierGeo) 0 else if (bidModifiers[1] is BidModifierGeo) 1 else 2
        soft.assertThat(bidModifiers[geoIndex]).isInstanceOf(BidModifierGeo::class.java)
        val bidModifierGeo = bidModifiers[geoIndex] as BidModifierGeo
        soft.assertThat(bidModifierGeo.regionalAdjustments.filter { it.hidden == false }).hasSize(2)
        soft.assertThat(bidModifierGeo.regionalAdjustments[0].regionId).isEqualTo(225)
        soft.assertThat(bidModifierGeo.regionalAdjustments[0].percent).isEqualTo(150)
        soft.assertThat(bidModifierGeo.regionalAdjustments[1].regionId).isEqualTo(1)
        soft.assertThat(bidModifierGeo.regionalAdjustments[1].percent).isEqualTo(80)

        val demographicsIndex = if (bidModifiers[0] is BidModifierDemographics) 0 else if (bidModifiers[1] is BidModifierDemographics) 1 else 2
        soft.assertThat(bidModifiers[demographicsIndex]).isInstanceOf(BidModifierDemographics::class.java)
        val bidModifierDemographics = bidModifiers[demographicsIndex] as BidModifierDemographics
        soft.assertThat(bidModifierDemographics.demographicsAdjustments).hasSize(2)
        soft.assertThat(bidModifierDemographics.demographicsAdjustments[0].age).isNull()
        soft.assertThat(bidModifierDemographics.demographicsAdjustments[0].gender).isEqualTo(GenderType.MALE)
        soft.assertThat(bidModifierDemographics.demographicsAdjustments[0].percent).isEqualTo(200)
        soft.assertThat(bidModifierDemographics.demographicsAdjustments[1].age).isEqualTo(AgeType._25_34)
        soft.assertThat(bidModifierDemographics.demographicsAdjustments[1].gender).isEqualTo(GenderType.FEMALE)
        soft.assertThat(bidModifierDemographics.demographicsAdjustments[1].percent).isEqualTo(133)

        val retargetingIndex = if (bidModifiers[0] is BidModifierRetargeting) 0 else if (bidModifiers[1] is BidModifierRetargeting) 1 else 2
        soft.assertThat(bidModifiers[retargetingIndex]).isInstanceOf(BidModifierRetargeting::class.java)
        val bidModifierRetargeting = bidModifiers[retargetingIndex] as BidModifierRetargeting
        soft.assertThat(bidModifierRetargeting.retargetingAdjustments).hasSize(1)
        soft.assertThat(bidModifierRetargeting.retargetingAdjustments[0].retargetingConditionId).isEqualTo(retargetingConditionId)
        soft.assertThat(bidModifierRetargeting.retargetingAdjustments[0].percent).isEqualTo(230)
        soft.assertAll()
    }

    @Test
    fun createUacCampaignTestWithHtml5Content() {
        val content = createDefaultHtml5Content()
        uacYdbContentRepository.saveContents(listOf(content))
        val request = createUacCampaignRequest(contentIds = listOf(content.id))

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo { e -> System.err.println("!!!!"+e.response.contentAsString) }
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        val campaignId = resultJsonTree["result"]["id"].asText()

        val campaignContents = uacYdbCampaignContentRepository.getCampaignContents(campaignId).filter {
            it.type == ru.yandex.direct.core.entity.uac.model.MediaType.HTML5 }
        assertThat(campaignContents).hasSize(1)
        assertThat(campaignContents[0].contentId).isEqualTo(content.id)
    }

    @Test
    fun createUacCampaignWithInvalidKeyword() {
        val request = createUacCampaignRequest(
            keywords = listOf("оформить кредитную карту -тинькофф -онлайн -кредитный -блэк -банк -платинум -банковский")
        )

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString
        val validationResult =
            JsonUtils.fromJson(
                JsonUtils.fromJson(result)["validation_result"].toString(),
                WebValidationResult::class.java
            )
        validationResult.errors.checkNotEmpty()
        validationResult.errors[0].path
            .checkEquals(path(field(PatchCampaignRequest::keywords.name), index(0)).toString())
        validationResult.errors[0].code
            .checkEquals(PhraseDefectIds.String.MINUS_WORD_DELETE_PLUS_WORD.code)
    }

    @Test
    fun disabledPlacesWIthOversizeLists() {
        val content = createDefaultVideoContent()
        uacYdbContentRepository.saveContents(listOf(content))
        val request = createUacCampaignRequest(
            advType = AdvType.CPM_BANNER,
            contentIds = listOf(content.id),
            titles = null,
            texts = null,
            nonSkippable = false,
            trackingUrl = null,
            disabledPlaces = UacDisabledPlaces(
                disabledPlaces = (0..1000).map { l -> "$l.ru" }.toList(),
                disabledVideoAdsPlaces = (0..40).map { l -> "$l.ru" }.toList(),
                disabledIps = null,
                disallowedPageIds = (1L..1001L).toList()),
            inventoryTypes = setOf(InventoryType.ALL),
        )
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString
        val validationResult =
            JsonUtils.fromJson(
                JsonUtils.fromJson(result)["validation_result"].toString(),
                WebValidationResult::class.java
            )
        assertThat(validationResult.errors).hasSize(3)
        assertThat(validationResult.errors.map { webDefect -> webDefect.path })
            .containsAll(
                listOf(
                    path(field(PatchCampaignRequest::uacDisabledPlaces.name),field(UacDisabledPlaces::disabledPlaces.name)).toString(),
                    path(field(PatchCampaignRequest::uacDisabledPlaces.name),field(UacDisabledPlaces::disabledVideoAdsPlaces.name)).toString(),
                    path(field(PatchCampaignRequest::uacDisabledPlaces.name),field(UacDisabledPlaces::disallowedPageIds.name)).toString()))
        assertThat(validationResult.errors.map { webDefect -> webDefect.code })
            .containsOnly(CollectionDefectIds.Size.SIZE_CANNOT_BE_MORE_THAN_MAX.code)
    }

    @Test
    fun disabledPlacesWIthOversizeDomains() {
        val content = createDefaultVideoContent()
        uacYdbContentRepository.saveContents(listOf(content))
        val request = createUacCampaignRequest(
            advType = AdvType.CPM_BANNER,
            strategy = UacStrategy(
                UacStrategyName.AUTOBUDGET_MAX_IMPRESSIONS,
                UacStrategyData(
                    BigDecimal.ZERO,
                    true,
                    BigDecimal.TEN,
                    LocalDate.now().plusMonths(6),
                    LocalDate.now(),
                    BigDecimal.valueOf(2100), BigDecimal.valueOf(100),
                    0
                )
            ),
            contentIds = listOf(content.id),
            titles = null,
            texts = null,
            nonSkippable = true,
            trackingUrl = null,
            disabledPlaces = UacDisabledPlaces(
                disabledPlaces = listOf(Strings.repeat("a", CampaignConstants.MAX_DOMAIN_LENGTH + 1)),
                disabledVideoAdsPlaces = listOf(Strings.repeat("a", CampaignConstants.MAX_DOMAIN_LENGTH + 1)),
                disabledIps = null,
                disallowedPageIds = null)
        )
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString
        val validationResult =
            JsonUtils.fromJson(
                JsonUtils.fromJson(result)["validation_result"].toString(),
                WebValidationResult::class.java
            )
        assertThat(validationResult.errors).isNotEmpty
        assertThat(validationResult.errors.map { webDefect -> webDefect.path })
            .containsAll(
                listOf(
                    path(field(PatchCampaignRequest::uacDisabledPlaces.name),field(UacDisabledPlaces::disabledPlaces.name), index(0)).toString(),
                    path(field(PatchCampaignRequest::uacDisabledPlaces.name),field(UacDisabledPlaces::disabledVideoAdsPlaces.name), index(0)).toString()))
        assertThat(validationResult.errors.map { webDefect -> webDefect.code })
            .contains(StringDefectIds.LENGTH_CANNOT_BE_MORE_THAN_MAX.code)
    }

    @Test
    fun disabledPlacesWIthInvalidIds() {
        val content = createDefaultVideoContent()
        uacYdbContentRepository.saveContents(listOf(content))
        val request = createUacCampaignRequest(
            advType = AdvType.CPM_BANNER,
            contentIds = listOf(content.id),
            titles = null,
            texts = null,
            nonSkippable = false,
            trackingUrl = null,
            disabledPlaces = UacDisabledPlaces(
                disabledPlaces = null,
                disabledVideoAdsPlaces = null,
                disabledIps = null,
                disallowedPageIds = listOf(0L))
        )
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString
        val validationResult =
            JsonUtils.fromJson(
                JsonUtils.fromJson(result)["validation_result"].toString(),
                WebValidationResult::class.java
            )
        assertThat(validationResult.errors).hasSize(1)
        assertThat(validationResult.errors.map { webDefect -> webDefect.path })
            .containsAll(
                listOf(
                    path(field(PatchCampaignRequest::uacDisabledPlaces.name),field(UacDisabledPlaces::disallowedPageIds.name), index(0)).toString()))
        assertThat(validationResult.errors.map { webDefect -> webDefect.code })
            .containsOnly(DefectIds.MUST_BE_VALID_ID.code)
    }


    @Test
    fun disabledPlacesWIthNonUniqueLists() {
        val content = createDefaultVideoContent()
        uacYdbContentRepository.saveContents(listOf(content))
        val request = createUacCampaignRequest(
            advType = AdvType.CPM_BANNER,
            contentIds = listOf(content.id),
            titles = null,
            texts = null,
            nonSkippable = false,
            trackingUrl = null,
            disabledPlaces = UacDisabledPlaces(
                disabledPlaces = listOf("a.ru", "a.ru"),
                disabledVideoAdsPlaces = listOf("a.ru", "a.ru"),
                disabledIps = null ,
                disallowedPageIds = null)
        )

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString
        val validationResult =
            JsonUtils.fromJson(
                JsonUtils.fromJson(result)["validation_result"].toString(),
                WebValidationResult::class.java
            )
        assertThat(validationResult.errors).hasSize(2)
        assertThat(validationResult.errors.map { webDefect -> webDefect.path })
            .containsAll(
                listOf(
                    path(field(PatchCampaignRequest::uacDisabledPlaces.name),field(UacDisabledPlaces::disabledPlaces.name)).toString(),
                    path(field(PatchCampaignRequest::uacDisabledPlaces.name),field(UacDisabledPlaces::disabledVideoAdsPlaces.name)).toString()))
        assertThat(validationResult.errors.map { webDefect -> webDefect.code })
            .containsOnly(CampaignDefectIds.Subset.MUST_NOT_CONTAIN_DUPLICATED_STRINGS.code)
    }

    @Test
    fun disabledPlacesWIthNonUniqueListsButNotOneToOne() {
        val content = createDefaultVideoContent()
        uacYdbContentRepository.saveContents(listOf(content))
        val request = createUacCampaignRequest(
            advType = AdvType.CPM_BANNER,
            contentIds = listOf(content.id),
            titles = null,
            texts = null,
            nonSkippable = false,
            trackingUrl = null,
            disabledPlaces = UacDisabledPlaces(
                disabledPlaces = listOf("https://mail.ru", "https://mail.ru/"),
                disabledVideoAdsPlaces = listOf("https://mail.ru", "https://mail.ru/"),
                disabledIps = null ,
                disallowedPageIds = null)
        )

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString
        val validationResult =
            JsonUtils.fromJson(
                JsonUtils.fromJson(result)["validation_result"].toString(),
                WebValidationResult::class.java
            )
        assertThat(validationResult.errors).hasSize(2)
        assertThat(validationResult.errors.map { webDefect -> webDefect.path })
            .containsAll(
                listOf(
                    path(field(PatchCampaignRequest::uacDisabledPlaces.name),field(UacDisabledPlaces::disabledPlaces.name)).toString(),
                    path(field(PatchCampaignRequest::uacDisabledPlaces.name),field(UacDisabledPlaces::disabledVideoAdsPlaces.name)).toString()))
        assertThat(validationResult.errors.map { webDefect -> webDefect.code })
            .containsOnly(CampaignDefectIds.Subset.MUST_NOT_CONTAIN_DUPLICATED_STRINGS.code)
    }


    @Test
    fun cpmAssetsWithCustomButtonWithoutFeature() {
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.ASSET_BUTTON_CUSTOM_TEXT, false);

        val content = createDefaultVideoContent()
        uacYdbContentRepository.saveContents(listOf(content))

        var cpmAssetButton = CpmAssetButton(action = UacButtonAction.CUSTOM_TEXT,
            href = "https://ya.ru",
            customText = "hh");
        val cpmAssetWithButton = mapOf(content.id to UacCpmAsset(
            title = "Asset title",
            titleExtension = "Asset title extension",
            body = "Asset body",
            button = cpmAssetButton,
            logoImageHash = null,
            measurers = null,
            pixels = listOf("https:/asd/mc.yanasxasxsdex.ru/pixel/7429537502459335683?rnd=%25aw_random%25"),
            bannerHref = "https://ya.ru")
        );
        val request = createUacCampaignRequest(
            advType = AdvType.CPM_BANNER,
            contentIds = listOf(content.id),
            titles = null,
            texts = null,
            nonSkippable = false,
            trackingUrl = null,
            disabledPlaces = UacDisabledPlaces(
                disabledPlaces = listOf("https://mail.ru"),
                disabledVideoAdsPlaces = listOf("https://mail.ru"),
                disabledIps = null ,
                disallowedPageIds = null),
            cpmAssets = cpmAssetWithButton
        )

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString
        val validationResult =
            JsonUtils.fromJson(
                JsonUtils.fromJson(result)["validation_result"].toString(),
                WebValidationResult::class.java
            )
        assertThat(validationResult.errors).hasSize(2)
        assertThat(validationResult.errors.map { webDefect -> webDefect.path })
            .containsAll(
                listOf(
                    path(field(PatchCampaignRequest::cpmAssets), field(content.id), field(UacCpmAsset::button), field(CpmAssetButton::customText)).toString(),
                    path(field(PatchCampaignRequest::cpmAssets), field(content.id), field(UacCpmAsset::button), field(CpmAssetButton::action)).toString()))
        assertThat(validationResult.errors.map { webDefect -> webDefect.code })
            .containsAll(listOf(DefectIds.INVALID_VALUE.code, BannerDefectIds.Gen.UNSUPPORTED_BUTTON_ACTION.code))
    }

    @Test
    fun cpmAssetsWithCustomButtonWithFeature() {
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.ASSET_BUTTON_CUSTOM_TEXT, true);

        val content = createDefaultVideoContent()
        uacYdbContentRepository.saveContents(listOf(content))

        var cpmAssetButton = CpmAssetButton(action = UacButtonAction.CUSTOM_TEXT,
            href = "https://ya.ru",
            customText = "hh");
        val cpmAssetWithButton = mapOf(content.id to UacCpmAsset(
            title = "Asset title",
            titleExtension = "Asset title extension",
            body = "Asset body",
            button = cpmAssetButton,
            logoImageHash = null,
            measurers = null,
            pixels = listOf("https:/asd/mc.yanasxasxsdex.ru/pixel/7429537502459335683?rnd=%25aw_random%25"),
            bannerHref = "https://ya.ru")
        );
        val request = createUacCampaignRequest(
            advType = AdvType.CPM_BANNER,
            contentIds = listOf(content.id),
            titles = null,
            texts = null,
            nonSkippable = false,
            trackingUrl = null,
            disabledPlaces = UacDisabledPlaces(
                disabledPlaces = listOf("https://mail.ru"),
                disabledVideoAdsPlaces = listOf("https://mail.ru"),
                disabledIps = null ,
                disallowedPageIds = null),
            cpmAssets = cpmAssetWithButton
        )

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString
        val validationResult =
            JsonUtils.fromJson(
                JsonUtils.fromJson(result)["validation_result"].toString(),
                WebValidationResult::class.java
            )
        assertThat(validationResult.errors).hasSize(1)
        assertThat(validationResult.errors.map { webDefect -> webDefect.path })
            .containsAll(
                listOf(
                    path(field(PatchCampaignRequest::cpmAssets), field(content.id), field(UacCpmAsset::button), field(CpmAssetButton::customText)).toString()))
        assertThat(validationResult.errors.map { webDefect -> webDefect.code })
            .contains(DefectIds.INVALID_VALUE.code)
    }

    @Test
    fun createCpmBannerCampaign_WithLogo() {
        val imageHash = "imageHash"
        steps.bannerSteps().createBannerImageFormat(userInfo.clientInfo, TestBanners.defaultBannerImageFormat(imageHash))
        val content = createDefaultVideoContent()
        uacYdbContentRepository.saveContents(listOf(content))
        val request = createUacCampaignRequest(
            advType = AdvType.CPM_BANNER,
            strategy = UacStrategy(
                UacStrategyName.AUTOBUDGET_MAX_IMPRESSIONS,
                UacStrategyData(
                    BigDecimal.ZERO,
                    true,
                    BigDecimal.TEN,
                    LocalDate.now().plusMonths(6),
                    LocalDate.now(),
                    BigDecimal.valueOf(2100), BigDecimal.ONE,
                    0
                )
            ),
            contentIds = listOf(content.id),
            titles = null,
            texts = null,
            nonSkippable = false,
            trackingUrl = null,
            cpmAssets = mapOf(
                content.id to UacCpmAsset(
                    title = "Asset title",
                    titleExtension = "Asset title extension",
                    body = "Asset body",
                    button = CpmAssetButton(
                        action = UacButtonAction.BUY,
                        customText = null,
                        href = "https://yandex.ru",
                    ),
                    logoImageHash = imageHash,
                    measurers = listOf(UacMeasurer(measurerType = UacBannerMeasurerSystem.WEBORAMA,
                        params = "{\"account\":1,\"tte\":1,\"aap\":1}")),
                    pixels = null,
                    bannerHref = "https://yandex.ru",
                )
            )
        )

        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isCreated)
    }

    @Test
    fun createCpmBannerCampaign_WithLogoWithUnknowhHash() {
        val content = createDefaultVideoContent()
        uacYdbContentRepository.saveContents(listOf(content))
        val request = createUacCampaignRequest(
            advType = AdvType.CPM_BANNER,
            strategy = UacStrategy(
                UacStrategyName.AUTOBUDGET_MAX_IMPRESSIONS,
                UacStrategyData(
                    BigDecimal.ZERO,
                    true,
                    BigDecimal.TEN,
                    LocalDate.now().plusMonths(6),
                    LocalDate.now(),
                    BigDecimal.valueOf(2100), BigDecimal.ONE,
                    0
                )
            ),
            contentIds = listOf(content.id),
            titles = null,
            texts = null,
            nonSkippable = false,
            trackingUrl = null,
            cpmAssets = mapOf(
                content.id to UacCpmAsset(
                    title = "Asset title",
                    titleExtension = "Asset title extension",
                    body = "Asset body",
                    button = CpmAssetButton(
                        action = UacButtonAction.BUY,
                        customText = null,
                        href = "https://yandex.ru",
                    ),
                    logoImageHash = "imageHash",
                    measurers = listOf(UacMeasurer(measurerType = UacBannerMeasurerSystem.WEBORAMA,
                        params = "{\"account\":1,\"tte\":1,\"aap\":1}")),
                    pixels = null,
                    bannerHref = "https://yandex.ru",
                )
            )
        )

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString

        val validationResult =
            JsonUtils.fromJson(
                JsonUtils.fromJson(result)["validation_result"].toString(),
                WebValidationResult::class.java
            )
        assertThat(validationResult.errors).hasSize(1)
        assertThat(validationResult.errors.map { webDefect -> webDefect.code })
            .containsOnly(BannerDefectIds.Gen.IMAGE_NOT_FOUND.code)
    }

    @Test
    fun createCpmBannerCampaign_WithLogoWithNullHash() {
        val content = createDefaultVideoContent()
        uacYdbContentRepository.saveContents(listOf(content))
        val request = createUacCampaignRequest(
            advType = AdvType.CPM_BANNER,
            strategy = UacStrategy(
                UacStrategyName.AUTOBUDGET_MAX_IMPRESSIONS,
                UacStrategyData(
                    BigDecimal.ZERO,
                    true,
                    BigDecimal.TEN,
                    LocalDate.now().plusMonths(6),
                    LocalDate.now(),
                    BigDecimal.valueOf(2100), BigDecimal.ONE,
                    0
                )
            ),
            contentIds = listOf(content.id),
            titles = null,
            texts = null,
            nonSkippable = false,
            trackingUrl = null,
            cpmAssets = mapOf(
                content.id to UacCpmAsset(
                    title = "Asset title",
                    titleExtension = "Asset title extension",
                    body = "Asset body",
                    button = CpmAssetButton(
                        action = UacButtonAction.BUY,
                        customText = null,
                        href = "https://yandex.ru",
                    ),
                    logoImageHash = null,
                    measurers = listOf(UacMeasurer(measurerType = UacBannerMeasurerSystem.WEBORAMA,
                        params = "{\"account\":1,\"tte\":1,\"aap\":1}")),
                    pixels = null,
                    bannerHref = "https://yandex.ru",
                )
            )
        )

        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isCreated)
    }

    @Test
    fun createUacCrrCampaign() {
        doReturn(
            listOf(
                MobileGoalConversions(TargetType.BUY.goalId!!, 0, 100, 100)
            )
        ).`when`(mobileAppConversionStatisticRepository).getConversionStats(
            anyString(), anyString(), anyList(), anyInt()
        )
        steps.featureSteps()
            .addClientFeature(userInfo.clientId, FeatureName.CRR_STRATEGY_ALLOWED, true)
        steps.featureSteps()
            .addClientFeature(userInfo.clientId, FeatureName.FIX_CRR_STRATEGY_ALLOWED, true)
        steps.featureSteps()
            .addClientFeature(userInfo.clientId, FeatureName.IN_APP_EVENTS_IN_RMP_ENABLED, true)
        steps.featureSteps()
            .addClientFeature(userInfo.clientId, FeatureName.UAC_ROAS_STRATEGY, true)
        val request = createUacCampaignRequest(
            targetId = TargetType.BUY,
            cpa = null,
            trackingUrl = TRACKER_URL,
            strategy = UacStrategy(
                UacStrategyName.AUTOBUDGET_CRR,
                UacStrategyData(
                    payForConversion = true,
                    crr = 15
                )
            )
        )
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo { e -> System.err.println(e.response.contentAsString) }
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString
        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        val campaignId = resultJsonTree["result"]["id"].asText()
        val campaign = uacCampaignRepository.getCampaign(campaignId)
        val directCampaign = uacDirectCampaignRepository.getDirectCampaignById(campaignId)
        val filledCampaign = ydbUacCampaignWebService.fillCampaign(
            userInfo.user!!,
            userInfo.user!!,
            campaign!!,
            directCampaign!!.directCampaignId,
            newCampaignStatuses,
        )

        SoftAssertions.assertSoftly {
            it.assertThat(resultJsonTree)
                .isEqualTo(JsonUtils.MAPPER.readTree(JsonUtils.toJson(filledCampaign.toResponse())))

            // проверка, что пользователь и аккаунт создался
            val user = uacUserRepository.getUserByUid(userInfo.uid)
            val account = uacAccountRepository.getAccountByClientId(userInfo.clientId.asLong())
            it.assertThat(user).isNotNull
            it.assertThat(account).isNotNull
            val mysqlCampaign =
                campaignTypedRepository.getTyped(userInfo.shard, listOf(directCampaign!!.directCampaignId))[0]
            it.assertThat(mysqlCampaign).isNotNull
            it.assertThat(mysqlCampaign).isInstanceOf(MobileContentCampaign::class.java)
            it.assertThat((mysqlCampaign as MobileContentCampaign).strategy.strategyName).isEqualTo(StrategyName.AUTOBUDGET_CRR)
            it.assertThat((mysqlCampaign).strategy.strategyData.crr).isEqualTo(15)
        }
    }

    @Test
    fun createUacCpaCampaignUsingStrategy() {
        steps.featureSteps()
            .addClientFeature(userInfo.clientId, FeatureName.UAC_FIX_CPA_STRATEGY_ENABLED, true)
        steps.featureSteps()
            .addClientFeature(userInfo.clientId, FeatureName.IN_APP_EVENTS_IN_RMP_ENABLED, true)
        val request = createUacCampaignRequest(
            cpa = null,
            targetId = TargetType.BUY,
            strategy = UacStrategy(
                UacStrategyName.AUTOBUDGET_AVG_CPA,
                UacStrategyData(
                    avgCpa = BigDecimal.valueOf(15L),
                    payForConversion = true
                )
            )
        )
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo { e -> System.err.println(e.response.contentAsString) }
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString
        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        val campaignId = resultJsonTree["result"]["id"].asText()
        val campaign = uacCampaignRepository.getCampaign(campaignId)
        val directCampaign = uacDirectCampaignRepository.getDirectCampaignById(campaignId)
        val filledCampaign = ydbUacCampaignWebService.fillCampaign(
            userInfo.user!!,
            userInfo.user!!,
            campaign!!,
            directCampaign!!.directCampaignId,
            newCampaignStatuses,
        )

        SoftAssertions.assertSoftly {
            it.assertThat(resultJsonTree)
                .isEqualTo(JsonUtils.MAPPER.readTree(JsonUtils.toJson(filledCampaign.toResponse())))

            // проверка, что пользователь и аккаунт создался
            val user = uacUserRepository.getUserByUid(userInfo.uid)
            val account = uacAccountRepository.getAccountByClientId(userInfo.clientId.asLong())
            it.assertThat(user).isNotNull
            it.assertThat(account).isNotNull
            val mysqlCampaign =
                campaignTypedRepository.getTyped(userInfo.shard, listOf(directCampaign!!.directCampaignId))[0]
            it.assertThat(mysqlCampaign).isNotNull
            it.assertThat(mysqlCampaign).isInstanceOf(MobileContentCampaign::class.java)
            it.assertThat((mysqlCampaign as MobileContentCampaign).strategy.strategyName).isEqualTo(StrategyName.AUTOBUDGET_AVG_CPA)
            it.assertThat((mysqlCampaign).strategy.strategyData.avgCpa).isEqualTo(BigDecimal.valueOf(15))
            it.assertThat((mysqlCampaign).strategy.strategyData.payForConversion).isEqualTo(true)
        }
    }

    @Test
    fun createUacCpiCampaignUsingStrategy() {
        val request = createUacCampaignRequest(
            cpa = null,
            targetId = TargetType.INSTALL,
            strategy = UacStrategy(
                UacStrategyName.AUTOBUDGET_AVG_CPI,
                UacStrategyData(
                    payForConversion = true,
                    avgCpi = BigDecimal.valueOf(15L)
                )
            )
        )
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo { e -> System.err.println(e.response.contentAsString) }
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString
        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        val campaignId = resultJsonTree["result"]["id"].asText()
        val campaign = uacCampaignRepository.getCampaign(campaignId)
        val directCampaign = uacDirectCampaignRepository.getDirectCampaignById(campaignId)
        val filledCampaign = ydbUacCampaignWebService.fillCampaign(
            userInfo.user!!,
            userInfo.user!!,
            campaign!!,
            directCampaign!!.directCampaignId,
            newCampaignStatuses,
        )

        SoftAssertions.assertSoftly {
            it.assertThat(resultJsonTree)
                .isEqualTo(JsonUtils.MAPPER.readTree(JsonUtils.toJson(filledCampaign.toResponse())))

            // проверка, что пользователь и аккаунт создался
            val user = uacUserRepository.getUserByUid(userInfo.uid)
            val account = uacAccountRepository.getAccountByClientId(userInfo.clientId.asLong())
            it.assertThat(user).isNotNull
            it.assertThat(account).isNotNull
            val mysqlCampaign =
                campaignTypedRepository.getTyped(userInfo.shard, listOf(directCampaign!!.directCampaignId))[0]
            it.assertThat(mysqlCampaign).isNotNull
            it.assertThat(mysqlCampaign).isInstanceOf(MobileContentCampaign::class.java)
            it.assertThat((mysqlCampaign as MobileContentCampaign).strategy.strategyName).isEqualTo(StrategyName.AUTOBUDGET_AVG_CPI)
            it.assertThat((mysqlCampaign).strategy.strategyData.avgCpi).isEqualTo(BigDecimal.valueOf(15))
            it.assertThat((mysqlCampaign).strategy.strategyData.payForConversion).isEqualTo(true)
        }
    }

    @Test
    fun createUacCampaignBadRequestWithoutCpaAndStrategyTest() {
        val request = createUacCampaignRequest(
            strategy = null,
            cpa = null
        )

        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun createUacCampaignWithBrandLiftName() {
        val exp = CreateExperimentResponse()
            .withExperimentId(123L)
            .withExperimentSegments(listOf(ExperimentSegmentResponse().withSegmentId(123L)))
        Mockito.`when`(yaAudienceClient.createExperiment(anyString(), any()))
            .thenReturn(CreateExperimentResponseEnvelope().withCreateExperimentResponse(exp))

        val content = createDefaultVideoContent()
        uacYdbContentRepository.saveContents(listOf(content))
        val brandSurveyId = "brandSurveyId"
        val brandSurveyName = "brandSurveyName"
        val request = createUacCampaignRequest(
            brandSurveyId = brandSurveyId, brandSurveyName = brandSurveyName,
            advType = AdvType.CPM_BANNER,
            contentIds = listOf(content.id),
            titles = null,
            texts = null,
            nonSkippable = false,
            trackingUrl = null,
            strategy = UacStrategy(
                UacStrategyName.AUTOBUDGET_MAX_IMPRESSIONS,
                UacStrategyData(
                    BigDecimal.ZERO,
                    true,
                    BigDecimal.TEN,
                    LocalDate.now().plusMonths(6),
                    LocalDate.now(),
                    BigDecimal.valueOf(2100), BigDecimal.valueOf(100),
                    0
                )
            ),
        )

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val brandSurveys = brandSurveyRepository.getBrandSurvey(userInfo.shard, brandSurveyId)
        Assert.assertNotNull(brandSurveys)
        Assert.assertEquals(brandSurveys.size, 1)
        Assert.assertEquals(brandSurveys[0].name, brandSurveyName)
    }

    @Test
    fun createUacCpmCampaignWithMetricaCounters() {
        val counters = listOf(1, 2, 3)

        val content = createDefaultVideoContent()
        uacYdbContentRepository.saveContents(listOf(content))

        val request = createUacCampaignRequest(
            advType = AdvType.CPM_BANNER,
            contentIds = listOf(content.id),
            titles = null,
            texts = null,
            nonSkippable = false,
            trackingUrl = null,
            strategy = UacStrategy(
                UacStrategyName.AUTOBUDGET_MAX_IMPRESSIONS,
                UacStrategyData(
                    BigDecimal.ZERO,
                    true,
                    BigDecimal.TEN,
                    LocalDate.now().plusMonths(6),
                    LocalDate.now(),
                    BigDecimal.valueOf(2100), BigDecimal.valueOf(100),
                    0
                )
            ),
            counters = counters,
        )

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        val campaignId = resultJsonTree["result"]["id"].asText()
        val directCampaign = uacDirectCampaignRepository.getDirectCampaignById(campaignId)
        val cpmBannerCampaign = campaignTypedRepository
            .getTyped(userInfo.shard, listOf(directCampaign!!.directCampaignId))[0] as CpmBannerCampaign
        assertThat(cpmBannerCampaign.metrikaCounters).containsAll(counters.map { it.toLong() })
    }

    @Test
    fun testCreateUacCampaignWithRetargetingCondition() {
        testCreateUacCampaignWithRetargetingCondition(0)
    }

    @Test
    fun testCreateUacCampaignWithRetargetingConditionAndSearchRetargeting() {
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.SEARCH_RETARGETING_ENABLED, true)
        testCreateUacCampaignWithRetargetingCondition(1)
    }

    @Suppress("unused")
    fun testData() = listOf(
        listOf(
            true,
            setOf(
                AltAppStore.HUAWEI_APP_GALLERY, AltAppStore.VIVO_APP_STORE,
                AltAppStore.SAMSUNG_GALAXY_STORE, AltAppStore.XIAOMI_GET_APPS
            ),
            EnumSet.of(
                MobileAppAlternativeStore.HUAWEI_APP_GALLERY, MobileAppAlternativeStore.VIVO_APP_STORE,
                MobileAppAlternativeStore.SAMSUNG_GALAXY_STORE, MobileAppAlternativeStore.XIAOMI_GET_APPS
            ),
        ),
        listOf(true, null, null),
        listOf(false, null, null),
        listOf(
            true,
            setOf(AltAppStore.HUAWEI_APP_GALLERY, AltAppStore.VIVO_APP_STORE),
            EnumSet.of(MobileAppAlternativeStore.HUAWEI_APP_GALLERY, MobileAppAlternativeStore.VIVO_APP_STORE),
        ),
        listOf(
            true,
            setOf(AltAppStore.VIVO_APP_STORE),
            EnumSet.of(MobileAppAlternativeStore.VIVO_APP_STORE),
        ),
        listOf(
            true,
            emptySet<AltAppStore>(),
            null
        ),
    )

    @Test
    @Parameters(method = "testData")
    fun createUacCampaignWithAlternativeAppStoresTest(
        enableAltAppStores: Boolean,
        altAppStores: Set<AltAppStore>?,
        expectedAltAppStores: Set<MobileAppAlternativeStore>?,
    ) {
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.ENABLE_ALTERNATIVE_STORES_IN_UAC, enableAltAppStores)

        val request = createUacCampaignRequest(altAppStores = altAppStores)

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo { e -> System.err.println(e.response.contentAsString) }
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        val campaignId = resultJsonTree["result"]["id"].asText()
        val directCampaign = uacDirectCampaignRepository.getDirectCampaignById(campaignId)

        val mysqlCampaign = campaignTypedRepository.getTyped(userInfo.shard, listOf(directCampaign!!.directCampaignId))[0]
        assertThat((mysqlCampaign as MobileContentCampaign).alternativeAppStores).isEqualTo(expectedAltAppStores)
    }

    private fun testCreateUacCampaignWithRetargetingCondition(bidModifiersCount: Int) {
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.IN_APP_MOBILE_TARGETING, true)
        val ydbAppInfo = defaultAppInfo()
        uacAppInfoRepository.saveAppInfo(ydbAppInfo)
        val appInfo = uacAppInfoService.getAppInfo(ydbAppInfo)

        val mobileApp = steps.mobileAppSteps().createMobileApp(userInfo.clientInfo!!, appInfo.url).mobileApp
        mobileAppGoalsService.updateMobileAppGoalsForExternalTracker(
            dslContextProvider.ppc(userInfo.shard), userInfo.clientId, listOf(mobileApp))
        val mobileAppGoalIds =  mobileAppGoalsService.getGoalsByApps(userInfo.clientId, listOf(mobileApp)).map { it.id }

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
        val id = retargetingConditionService.addRetargetingConditions(
            listOf(directRetargetingCondition), userInfo.clientId)[0].result

        val request = createUacCampaignRequest(
            retargetingCondition = UacRetargetingCondition(
                conditionRules = retargetingCondition.conditionRules,
                name = retargetingCondition.name,
                id = id
            ),
            mobileAppId = mobileApp.id,
        )
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo { e -> System.err.println(e.response.contentAsString) }
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        val campaignId = resultJsonTree["result"]["id"].asText()
        val campaign = uacCampaignRepository.getCampaign(campaignId)

        val soft = SoftAssertions()
        soft.assertThat(campaign?.retargetingCondition?.id).isEqualTo(id)
        soft.assertThat(campaign?.retargetingCondition?.name).isEqualTo(retargetingCondition.name)
        soft.assertThat(campaign?.retargetingCondition?.conditionRules).isEqualTo(retargetingCondition.conditionRules)

        val directCampaignId = uacDirectCampaignRepository.getDirectCampaignById(campaignId)!!.directCampaignId
        val bidModifiers = bidModifierService.getByCampaignIds(
            userInfo.clientId,
            mutableListOf(directCampaignId),
            setOf(BidModifierType.RETARGETING_FILTER),
            setOf(BidModifierLevel.CAMPAIGN),
            userInfo.uid
        )
        soft.assertThat(bidModifiers).hasSize(bidModifiersCount)
        soft.assertAll()
    }
    private fun createUacCampaignRequest(
        campaignName: String = "Test text UC campaign",
        advType: AdvType = AdvType.MOBILE_CONTENT,
        strategy: UacStrategy? = null,
        cpa: BigDecimal? = BigDecimal.valueOf(100000000L, 6),
        retargetingCondition: UacRetargetingCondition? = null,
        contentIds: List<String>? = null,
        texts: List<String>? = listOf("Some text for banner"),
        titles: List<String>? = listOf("Some title for banner"),
        keywords: List<String>? = listOf("something keyword", "yet !another keyword"),
        minusKeywords: List<String>? = listOf("something minus keyword", "yet another minus keyword"),
        nonSkippable: Boolean? = null,
        regions: List<Long>? = listOf(Region.RUSSIA_REGION_ID, Region.BY_REGION_ID),
        minusRegions: List<Long>? = null,
        trackingUrl: String? = "https://redirect.appmetrica.yandex.com/serve/1179849830915165578?c=ya_direct&c=ya_direct&google_aid_sha1={GOOGLE_AID_LC_SH1_HEX}&android_id_sha1={ANDROID_ID_LC_SH1_HEX}&device_type={DEVICE_TYPE}&source_type={STYPE}&source={SRC}&google_aid={google_aid}&click_id={logid}&search_term={PHRASE}&region_name={REGN_BS}&phrase_id={PHRASE_EXPORT_ID}&android_id={ANDROID_ID_LC}&position_type={PTYPE}&campaign_id=54494649",
        showsFrequencyLimit: UacShowsFrequencyLimit? = null,
        strategyPlatform: UacStrategyPlatform? = UacStrategyPlatform.BOTH,
        sitelinks: List<Sitelink>? = listOf(Sitelink("sitelink title", "https://" + TestDomain.randomDomain(), "sitelink descr")),
        targetId: TargetType = TargetType.INSTALL,
        adjustments: List<UacAdjustmentRequest>? = null,
        isEcom: Boolean? = null,
        crr: Long? = null,
        mobileAppId: Long? = null,
        disabledPlaces: UacDisabledPlaces? = null,
        inventoryTypes: Set<InventoryType>? = null,
        brandSurveyId: String? = null,
        brandSurveyName: String? = null,
        cpmAssets: Map<String, UacCpmAsset>? = null,
        altAppStores: Set<AltAppStore>? = null,
        counters: List<Int>? = listOf(RandomNumberUtils.nextPositiveInteger()),
    ): CreateCampaignRequest {
        val ydbAppInfo = defaultAppInfo()
        uacAppInfoRepository.saveAppInfo(ydbAppInfo)

        val bannerHref = "https://" + TestDomain.randomDomain()
        val weekLimit = BigDecimal.valueOf(2300000000, 6)
        val socdem = Socdem(
            listOf(Gender.FEMALE),
            AgePoint.AGE_45,
            AgePoint.AGE_INF,
            Socdem.IncomeGrade.LOW,
            Socdem.IncomeGrade.PREMIUM
        )
        val deviceTypes = setOf(DeviceType.ALL)
        val goals = listOf(UacGoal(RandomNumberUtils.nextPositiveInteger().toLong(), null))

        // нужно создать мобильное приложение, иначе сервис попробует достать его из yt, к чему ci не готов
        val storeUrl = googlePlayAppInfoGetter.appPageUrl(ydbAppInfo.appId, ydbAppInfo.region, ydbAppInfo.language)
        steps.mobileAppSteps().createMobileApp(userInfo.clientInfo!!, storeUrl)
        return CreateCampaignRequest(
            displayName = campaignName,
            href = bannerHref,
            texts = texts,
            titles = titles,
            minusRegions = minusRegions,
            regions = regions,
            contentIds = contentIds,
            weekLimit = weekLimit,
            limitPeriod = LimitPeriodType.MONTH,
            advType = advType,
            hyperGeoId = 1234,
            keywords = keywords,
            minusKeywords = minusKeywords,
            socdem = socdem,
            deviceTypes = deviceTypes,
            inventoryTypes = inventoryTypes,
            goals = goals,
            goalCreateRequest = null,
            counters = counters,
            permalinkId = null,
            phoneId = null,
            calltrackingPhones = emptyList(),
            sitelinks = sitelinks,
            appId = ydbAppInfo.id,
            trackingUrl = trackingUrl,
            impressionUrl = null,
            targetId = targetId,
            skadNetworkEnabled = null,
            adultContentEnabled = null,
            cpa = cpa,
            crr = crr,
            timeTarget = null,
            strategy = strategy,
            retargetingCondition = retargetingCondition,
            videosAreNonSkippable = nonSkippable,
            brandSurveyId = brandSurveyId,
            brandSurveyName = brandSurveyName,
            showsFrequencyLimit = showsFrequencyLimit,
            strategyPlatform = strategyPlatform,
            adjustments = adjustments,
            isEcom = isEcom,
            feedId = null,
            feedFilters = null,
            trackingParams = null,
            cloneFromCampaignId = null,
            cpmAssets = cpmAssets,
            campaignMeasurers = null,
            uacBrandsafety = null,
            uacDisabledPlaces = disabledPlaces,
            widgetPartnerId = null,
            source = null,
            mobileAppId = mobileAppId,
            isRecommendationsManagementEnabled = null,
            isPriceRecommendationsManagementEnabled = null,
            relevanceMatch = null,
            showTitleAndBody = null,
            altAppStores = altAppStores,
            bizLandingId = null,
            searchLift = null,
        )
    }
}
