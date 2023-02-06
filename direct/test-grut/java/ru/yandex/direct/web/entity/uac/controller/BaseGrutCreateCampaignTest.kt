package ru.yandex.direct.web.entity.uac.controller

import org.junit.After
import org.junit.Before
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.audience.client.YaAudienceClient
import ru.yandex.direct.audience.client.model.CreateExperimentResponse
import ru.yandex.direct.audience.client.model.CreateExperimentResponseEnvelope
import ru.yandex.direct.audience.client.model.ExperimentSegmentResponse
import ru.yandex.direct.core.entity.banner.type.href.BannerUrlCheckService
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.mobileapp.model.MobileGoalConversions
import ru.yandex.direct.core.entity.mobileapp.repository.MobileAppConversionStatisticRepository
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.AdvType
import ru.yandex.direct.core.entity.uac.model.AgePoint
import ru.yandex.direct.core.entity.uac.model.DeviceType
import ru.yandex.direct.core.entity.uac.model.Gender
import ru.yandex.direct.core.entity.uac.model.InventoryType
import ru.yandex.direct.core.entity.uac.model.LimitPeriodType
import ru.yandex.direct.core.entity.uac.model.Sitelink
import ru.yandex.direct.core.entity.uac.model.Socdem
import ru.yandex.direct.core.entity.uac.model.TargetType
import ru.yandex.direct.core.entity.uac.model.UacAdjustmentRequest
import ru.yandex.direct.core.entity.uac.model.UacGoal
import ru.yandex.direct.core.entity.uac.model.UacRetargetingCondition
import ru.yandex.direct.core.entity.uac.model.UacShowsFrequencyLimit
import ru.yandex.direct.core.entity.uac.model.UacStrategy
import ru.yandex.direct.core.entity.uac.model.UacStrategyData
import ru.yandex.direct.core.entity.uac.model.UacStrategyName
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacCpmAsset
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacDisabledPlaces
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacSearchLift
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAppInfo
import ru.yandex.direct.core.entity.uac.service.appinfo.GooglePlayAppInfoGetter
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.service.urlchecker.RedirectCheckResult
import ru.yandex.direct.core.testing.data.TestDomain
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.regions.Region
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.uac.createUcCampaignRequest
import ru.yandex.direct.web.entity.uac.model.CreateCampaignRequest
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

abstract class BaseGrutCreateCampaignTest {

    @Autowired
    protected lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    protected lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    protected lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    protected lateinit var steps: Steps

    @Autowired
    protected lateinit var uacAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    protected lateinit var grutApiSerive: GrutApiService

    @Autowired
    protected lateinit var googlePlayAppInfoGetter: GooglePlayAppInfoGetter

    @Autowired
    protected lateinit var bannerUrlCheckService: BannerUrlCheckService

    @Autowired
    protected lateinit var mobileAppConversionStatisticRepository: MobileAppConversionStatisticRepository

    @Autowired
    protected lateinit var grutSteps: GrutSteps

    @Autowired
    protected lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    protected lateinit var grutApiService: GrutApiService

    @Autowired
    protected lateinit var yaAudienceClient: YaAudienceClient

    protected lateinit var mockMvc: MockMvc

    protected lateinit var userInfo: UserInfo

    protected lateinit var clientInfo: ClientInfo

    protected lateinit var imageContentId: String

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userInfo = testAuthHelper.createDefaultUser()
        clientInfo = userInfo.clientInfo!!
        grutSteps.createClient(clientInfo)
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
        steps.featureSteps()
            .addClientFeature(userInfo.clientId, FeatureName.UC_UAC_CREATE_BRIEF_IN_GRUT_INSTEAD_OF_YDB, true)
        steps.featureSteps()
            .addClientFeature(userInfo.clientId, FeatureName.DISABLE_BILLING_AGGREGATES, true)
        steps.featureSteps().addClientFeature(
            userInfo.clientId,
            FeatureName.UC_UAC_CREATE_MOBILE_CONTENT_BRIEF_IN_GRUT_INSTEAD_OF_YDB,
            true
        )
        steps.featureSteps()
            .addClientFeature(userInfo.clientId, FeatureName.UC_UAC_CREATE_MEDIA_BRIEF_IN_GRUT_INSTEAD_OF_YDB, true)
        steps.featureSteps()
            .addClientFeature(userInfo.clientId, FeatureName.SMARTTV_BID_MODIFIER_ENABLED, true)

        // нужно для проверки регионов, так как из ручки возвращается регион в локали en
        LocaleContextHolder.setLocale(Locale.ENGLISH)

        Mockito.doReturn(
            RedirectCheckResult.createSuccessResult(
                "https://play.google.com/store/apps/details?id=com.yandex.browser", ""
            )
        )
            .`when`(bannerUrlCheckService).getRedirect(Mockito.anyString(), Mockito.anyString(), ArgumentMatchers.anyBoolean())
        Mockito.doReturn(listOf<MobileGoalConversions>())
            .`when`(mobileAppConversionStatisticRepository)
            .getConversionStats(Mockito.anyString(), Mockito.anyString(), ArgumentMatchers.anyList(), ArgumentMatchers.anyInt())

        Mockito.doReturn(CreateExperimentResponseEnvelope()
            .withCreateExperimentResponse(CreateExperimentResponse()
                .withExperimentId(1)
                .withExperimentSegments(listOf(
                    ExperimentSegmentResponse().withSegmentId(2),
                    ExperimentSegmentResponse().withSegmentId(3))
                )
            )).`when`(yaAudienceClient).createExperiment(any(), any())

        imageContentId = grutSteps.createDefaultImageAsset(clientInfo.clientId!!)

    }

    @After
    fun after() {
        LocaleContextHolder.setLocale(null)
    }

    protected fun createUacCampaignRequest(
        contentIds: List<String>,
        adjustments: List<UacAdjustmentRequest>? = null,
        uacDisabledPlaces: UacDisabledPlaces? = null,
        showTitleAndBody: Boolean? = null,
        adultContentEnabled: Boolean? = null,
        regions: List<Long>? = listOf(Region.BY_REGION_ID),
        app: UacYdbAppInfo? = null,
        goals: List<UacGoal>? = null,
    ): CreateCampaignRequest {
        val appInfo = app ?: defaultAppInfo(
            appId = "com.yandex.browser",
            bundleId = "com.yandex.browser",
        )

        if(app == null) {
            uacAppInfoRepository.saveAppInfo(appInfo)
            // нужно создать мобильное приложение, иначе сервис попробует достать его из yt, к чему ci не готов
            val storeUrl = googlePlayAppInfoGetter.appPageUrl(appInfo.appId, appInfo.region, appInfo.language)
            steps.mobileAppSteps().createMobileApp(userInfo.clientInfo!!, storeUrl)
        }

        return createUcCampaignRequest(
            displayName = "Яндекс.Браузер — с Алисой (Android)",
            href = "https://play.google.com/store/apps/details?id=com.yandex.browser",
            texts = listOf("text 1", "text 2", "text 3"),
            titles = listOf("title 1", "title 2", "title 3"),
            regions = regions,
            minusRegions = null,
            contentIds = contentIds,
            weekLimit = BigDecimal.valueOf(2300),
            limitPeriod = LimitPeriodType.WEEK,
            advType = AdvType.MOBILE_CONTENT,
            appId = appInfo.id,
            trackingUrl = "https://redirect.appmetrica.yandex.com/serve/1179849830915165578?c=ya_direct&c=ya_direct&google_aid_sha1={GOOGLE_AID_LC_SH1_HEX}&android_id_sha1={ANDROID_ID_LC_SH1_HEX}&device_type={DEVICE_TYPE}&source_type={STYPE}&source={SRC}&google_aid={google_aid}&click_id={logid}&search_term={PHRASE}&region_name={REGN_BS}&phrase_id={PHRASE_EXPORT_ID}&android_id={ANDROID_ID_LC}&position_type={PTYPE}&campaign_id=54494649",
            targetId = TargetType.INSTALL,
            cpa = BigDecimal.valueOf(500),
            keywords = null,
            minusKeywords = null,
            socdem = null,
            deviceTypes = null,
            sitelinks = null,
            adultContentEnabled = adultContentEnabled,
            timeTarget = null,
            adjustments = adjustments,
            isEcom = null,
            uacDisabledPlaces = uacDisabledPlaces,
            showTitleAndBody = showTitleAndBody,
            goals = goals,
        )
    }

    protected fun createCpmBannerCampaignRequest(
        contentId: String,
        strategyStartDate: LocalDate,
        strategyFinishDate: LocalDate,
        retargetingCondition: UacRetargetingCondition,
        showsFrequencyLimit: UacShowsFrequencyLimit,
        cpmAssets: Map<String, UacCpmAsset>,
        nonSkippable: Boolean = false,
        searchLift: UacSearchLift? = null,
    ): CreateCampaignRequest {
        return createUcCampaignRequest(
            displayName = "cpm banner campaign",
            href = "https://mediyka.ru",
            regions = listOf(Region.TURKEY_REGION_ID),
            minusRegions = null,
            contentIds = listOf(contentId),
            limitPeriod = LimitPeriodType.MONTH,
            advType = AdvType.CPM_BANNER,
            strategy = UacStrategy(
                UacStrategyName.AUTOBUDGET_MAX_IMPRESSIONS,
                UacStrategyData(
                    BigDecimal.ZERO,
                    true,
                    BigDecimal.valueOf(300),
                    strategyFinishDate,
                    strategyStartDate,
                    BigDecimal.valueOf(2100),
                    null,
                    null,
                )
            ),
            socdem = Socdem(
                genders = listOf(Gender.MALE),
                ageLower = AgePoint.AGE_25,
                ageUpper = AgePoint.AGE_55,
                incomeLower = Socdem.IncomeGrade.LOW,
                incomeUpper = Socdem.IncomeGrade.MIDDLE,
            ),
            retargetingCondition = retargetingCondition,
            weekLimit = null,
            cpa = null,
            texts = null,
            titles = null,
            keywords = null,
            minusKeywords = null,
            deviceTypes = setOf(DeviceType.DESKTOP, DeviceType.PHONE_IOS),
            inventoryTypes = setOf(InventoryType.INSTREAM),
            sitelinks = null,
            timeTarget = null,
            showsFrequencyLimit = showsFrequencyLimit,
            isEcom = null,
            cpmAssets = cpmAssets,
            videosAreNonSkippable = nonSkippable,
            searchLift = searchLift,
        )
    }

    protected fun createCampaignRequest(
        campaignName: String = "Test text UC campaign",
        advType: AdvType = AdvType.MOBILE_CONTENT,
        strategy: UacStrategy? = null,
        retargetingCondition: UacRetargetingCondition? = null,
        contentIds: List<String>? = null,
        adultContentEnabled: Boolean? = null
    ): CreateCampaignRequest {
        val ydbAppInfo = defaultAppInfo()
        uacAppInfoRepository.saveAppInfo(ydbAppInfo)

        val bannerHref = "https://" + TestDomain.randomDomain()
        val bannerText = "Some text for banner"
        val bannerTitle = "Some title for banner"
        val keywords = listOf("something keyword", "yet !another keyword")
        val minusKeywords = listOf("something minus keyword", "yet another minus keyword")
        val sitelink = Sitelink("sitelink title", "https://" + TestDomain.randomDomain(), "sitelink descr")
        val weekLimit = BigDecimal.valueOf(2300000000, 6)
        val regions = listOf(Region.RUSSIA_REGION_ID, Region.BY_REGION_ID)
        val minusRegions = listOf(Region.MOSCOW_REGION_ID)
        val socdem =
            Socdem(listOf(Gender.FEMALE), AgePoint.AGE_45, AgePoint.AGE_INF, Socdem.IncomeGrade.LOW, Socdem.IncomeGrade.PREMIUM)
        val deviceTypes = setOf(DeviceType.ALL)
        val goals = listOf(UacGoal(RandomNumberUtils.nextPositiveInteger().toLong()))
        val counters = listOf(RandomNumberUtils.nextPositiveInteger())
        val trackingUrl =
            "https://redirect.appmetrica.yandex.com/serve/1179849830915165578?c=ya_direct&c=ya_direct&google_aid_sha1={GOOGLE_AID_LC_SH1_HEX}&android_id_sha1={ANDROID_ID_LC_SH1_HEX}&device_type={DEVICE_TYPE}&source_type={STYPE}&source={SRC}&google_aid={google_aid}&click_id={logid}&search_term={PHRASE}&region_name={REGN_BS}&phrase_id={PHRASE_EXPORT_ID}&android_id={ANDROID_ID_LC}&position_type={PTYPE}&campaign_id=54494649"

        // нужно создать мобильное приложение, иначе сервис попробует достать его из yt, к чему ci не готов
        val storeUrl = googlePlayAppInfoGetter.appPageUrl(ydbAppInfo.appId, ydbAppInfo.region, ydbAppInfo.language)
        steps.mobileAppSteps().createMobileApp(userInfo.clientInfo!!, storeUrl)

        return createUcCampaignRequest(
            displayName = campaignName,
            href = bannerHref,
            texts = listOf(bannerText),
            titles = listOf(bannerTitle),
            regions = regions,
            minusRegions = minusRegions,
            contentIds = contentIds,
            weekLimit = weekLimit,
            limitPeriod = LimitPeriodType.MONTH,
            advType = advType,
            hyperGeoId = 1234,
            keywords = keywords,
            minusKeywords = minusKeywords,
            socdem = socdem,
            deviceTypes = deviceTypes,
            goals = goals,
            counters = counters,
            sitelinks = listOf(sitelink),
            appId = ydbAppInfo.id,
            trackingUrl = trackingUrl,
            targetId = TargetType.INSTALL,
            adultContentEnabled = adultContentEnabled,
            cpa = BigDecimal.valueOf(100000000L, 6),
            timeTarget = null,
            strategy = strategy,
            retargetingCondition = retargetingCondition,
        )
    }
}
