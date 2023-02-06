package ru.yandex.direct.web.entity.uac.controller

import com.fasterxml.jackson.databind.JsonNode
import com.nhaarman.mockitokotlin2.eq
import java.math.BigDecimal
import java.util.Locale
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.nullValue
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.Mockito
import org.mockito.Mockito.anyString
import org.mockito.Mockito.doReturn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.audience.client.YaAudienceClient
import ru.yandex.direct.audience.client.model.AudienceSegment
import ru.yandex.direct.audience.client.model.SegmentStatus
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository
import ru.yandex.direct.core.entity.banner.type.href.BannerUrlCheckService
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes
import ru.yandex.direct.core.entity.uac.STORE_URL_FOR_APP_ID
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.AdvType
import ru.yandex.direct.core.entity.uac.model.AgePoint
import ru.yandex.direct.core.entity.uac.model.CampaignStatuses
import ru.yandex.direct.core.entity.uac.model.DeviceType
import ru.yandex.direct.core.entity.uac.model.DirectCampaignStatus
import ru.yandex.direct.core.entity.uac.model.Gender
import ru.yandex.direct.core.entity.uac.model.LimitPeriodType
import ru.yandex.direct.core.entity.uac.model.Sitelink
import ru.yandex.direct.core.entity.uac.model.Socdem
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.core.entity.uac.model.TargetStatus
import ru.yandex.direct.core.entity.uac.model.TargetType
import ru.yandex.direct.core.entity.uac.model.UacGoal
import ru.yandex.direct.core.entity.uac.model.UacRetargetingCondition
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRule
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoal
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoalType
import ru.yandex.direct.core.entity.uac.model.UacStrategy
import ru.yandex.direct.core.entity.uac.model.UacStrategyPlatform
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.service.appinfo.GooglePlayAppInfoGetter
import ru.yandex.direct.core.service.urlchecker.RedirectCheckResult
import ru.yandex.direct.core.testing.data.TestDomain
import ru.yandex.direct.core.testing.data.TestFullGoals
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.dbqueue.repository.DbQueueRepository
import ru.yandex.direct.dbqueue.steps.DbQueueSteps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.regions.Region
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.utils.fromJson
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.uac.model.CampaignStatusesRequest
import ru.yandex.direct.web.entity.uac.model.CreateCampaignRequest
import ru.yandex.direct.web.entity.uac.model.UacCampaign

abstract class UacCampaignTargetStatusControllerTestBase {
    protected lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    protected lateinit var steps: Steps

    @Autowired
    protected lateinit var dbQueueSteps: DbQueueSteps

    @Autowired
    protected lateinit var dbQueueRepository: DbQueueRepository

    @Autowired
    private lateinit var uacAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    protected lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    protected lateinit var bannerTypedRepository: BannerTypedRepository

    @Autowired
    private lateinit var googlePlayAppInfoGetter: GooglePlayAppInfoGetter

    @Autowired
    private lateinit var bannerUrlCheckService: BannerUrlCheckService

    protected lateinit var userInfo: UserInfo

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()

        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )

        // нужно для проверки регионов, так как из ручки возвращается регион в локали en
        LocaleContextHolder.setLocale(Locale.ENGLISH)

        doReturn(RedirectCheckResult.createSuccessResult(STORE_URL_FOR_APP_ID, ""))
            .`when`(bannerUrlCheckService).getRedirect(anyString(), anyString(), anyBoolean())

        dbQueueSteps.registerJobType(DbQueueJobTypes.UAC_UPDATE_ADS)
    }

    @After
    fun after() {
        LocaleContextHolder.setLocale(null)
    }

    abstract fun createAsset(mediaType: ru.yandex.direct.core.entity.uac.model.MediaType): String

    abstract fun getCampaign(campaignId: String): UacYdbCampaign?

    abstract fun getDirectCampaignId(uacCampaignId: String): Long

    abstract fun getDirectCampaignStatus(campaign: UacYdbCampaign): DirectCampaignStatus?

    fun checkUacCampaignState(
        uacCampaignId: String,
        expectedDirectStatusShow: Boolean,
        updateAdsJobExpected: Boolean,
        expectedUacCampaignStatus: Status,
        expectedDirectCampaignStatus: DirectCampaignStatus? = null,
        expectedBriefSynced: Boolean = !updateAdsJobExpected,
    ) {
        val uacCampaign = getCampaign(uacCampaignId)!!
        val directCampaignId = getDirectCampaignId(uacCampaign.id)
        val directCampaignStatus = getDirectCampaignStatus(uacCampaign)!!

        val mysqlCampaign = campaignTypedRepository.getTyped(
            userInfo.shard,
            listOf(directCampaignId)
        )[0] as MobileContentCampaign
        assertThat(
            "джоба создания в тестах не должна запускаться",
            bannerTypedRepository.getBannersByCampaignIds(userInfo.shard, listOf(mysqlCampaign.id)).isNullOrEmpty(),
            `is`(true)
        )
        val jobId = dbQueueSteps.getLastJobByType(userInfo.shard, DbQueueJobTypes.UAC_UPDATE_ADS)
        if (!updateAdsJobExpected) {
            assertThat("джоба не должна была поставиться в очередь", jobId, nullValue())
        } else {
            assertThat("джоба должна была появиться в очереди", jobId, notNullValue())
            val dbJob = dbQueueRepository.findJobById(userInfo.shard, DbQueueJobTypes.UAC_UPDATE_ADS, jobId)
            assertThat("джоба должна была появиться в очереди", dbJob, notNullValue())
            assertThat(
                "джоба в очереди имеет правильный идентификатор кампании в параметрах",
                dbJob?.args?.uacCampaignId,
                `is`(uacCampaignId)
            )
        }
        assertThat(
            "кампания в директовой базе должна иметь ожидаемый статус",
            mysqlCampaign.statusShow,
            `is`(expectedDirectStatusShow)
        )
        uacCampaign.apply {
            briefSynced.checkEquals(expectedBriefSynced)
        }
        // В базе у заявки нет поля status, поэтому проверяем вычисленное значение в ответе GET
        val getResult = doGetCampaignRequest(directCampaignId)
        assertThat(getResult["status"].asText(), `is`(expectedUacCampaignStatus.getTargetStatus()))
        if (expectedDirectCampaignStatus != null) {
            assertThat(
                directCampaignStatus,
                `is`(expectedDirectCampaignStatus)
            )
        }
    }

    @Test
    fun startUacDraftCampaignSuccess() {
        val campaignId = createDefaultUacContentCampaignReturnId()
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = true,
            expectedUacCampaignStatus = Status.DRAFT,
            updateAdsJobExpected = false,
            expectedDirectCampaignStatus = DirectCampaignStatus.DRAFT,
        )
        val response = changeCampaignTargetStatus(campaignId, TargetStatus.STARTED, true)
        assertThat(response?.status, `is`(Status.MODERATING))
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = true,
            expectedUacCampaignStatus = Status.MODERATING,
            updateAdsJobExpected = true,
            expectedDirectCampaignStatus = DirectCampaignStatus.CREATED,
        )
    }

    @Test
    fun startUacDraftCampaignSuccess_WithDirectId() {
        val campaignId = createDefaultUacContentCampaignReturnId()
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = true,
            expectedUacCampaignStatus = Status.DRAFT,
            updateAdsJobExpected = false,
            expectedDirectCampaignStatus = DirectCampaignStatus.DRAFT,
        )
        val response = changeCampaignTargetStatus(campaignId, TargetStatus.STARTED, true, true)
        assertThat(response?.status, `is`(Status.MODERATING))
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = true,
            expectedUacCampaignStatus = Status.MODERATING,
            updateAdsJobExpected = true,
            expectedDirectCampaignStatus = DirectCampaignStatus.CREATED,
        )
    }

    @Test
    fun startUacDraftCampaignForbidden_WithEmptyRegions() {
        val campaignId = createDefaultUacContentCampaignReturnId(regions = null)
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = true,
            expectedUacCampaignStatus = Status.DRAFT,
            updateAdsJobExpected = false,
            expectedDirectCampaignStatus = DirectCampaignStatus.DRAFT,
        )
        changeCampaignTargetStatus(campaignId, TargetStatus.STARTED, false)
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = true,
            expectedUacCampaignStatus = Status.DRAFT,
            updateAdsJobExpected = false,
            expectedDirectCampaignStatus = DirectCampaignStatus.DRAFT,
        )
    }

    @Test
    fun startUacDraftCampaignForbidden_WithEmptyTrackingUrl() {
        val campaignId = createDefaultUacContentCampaignReturnId(trackingUrl = null)
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = true,
            expectedUacCampaignStatus = Status.DRAFT,
            updateAdsJobExpected = false,
            expectedDirectCampaignStatus = DirectCampaignStatus.DRAFT,
        )
        changeCampaignTargetStatus(campaignId, TargetStatus.STARTED, false)
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = true,
            expectedUacCampaignStatus = Status.DRAFT,
            updateAdsJobExpected = false,
            expectedDirectCampaignStatus = DirectCampaignStatus.DRAFT,
        )
    }

    @Test
    fun stopUacDraftCampaignForbidden() {
        val campaignId = createDefaultUacContentCampaignReturnId()
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = true,
            expectedUacCampaignStatus = Status.DRAFT,
            updateAdsJobExpected = false,
            expectedDirectCampaignStatus = DirectCampaignStatus.DRAFT,
        )
        changeCampaignTargetStatus(campaignId, TargetStatus.STOPPED, false)
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = true,
            expectedUacCampaignStatus = Status.DRAFT,
            updateAdsJobExpected = false,
            expectedDirectCampaignStatus = DirectCampaignStatus.DRAFT,
        )
    }

    @Test
    fun stopUacDraftCampaignForbidden_WithDirectId() {
        val campaignId = createDefaultUacContentCampaignReturnId()
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = true,
            expectedUacCampaignStatus = Status.DRAFT,
            updateAdsJobExpected = false,
            expectedDirectCampaignStatus = DirectCampaignStatus.DRAFT,
        )
        changeCampaignTargetStatus(campaignId, TargetStatus.STOPPED, false, true)
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = true,
            expectedUacCampaignStatus = Status.DRAFT,
            updateAdsJobExpected = false,
            expectedDirectCampaignStatus = DirectCampaignStatus.DRAFT,
        )
    }

    @Test
    fun startUacNonDraftCampaignForbidden() {
        val campaignId = createDefaultUacContentCampaignReturnId()
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = true,
            expectedUacCampaignStatus = Status.DRAFT,
            updateAdsJobExpected = false,
            expectedDirectCampaignStatus = DirectCampaignStatus.DRAFT,
        )
        changeCampaignTargetStatus(campaignId, TargetStatus.STARTED, true)
        changeCampaignTargetStatus(campaignId, TargetStatus.STARTED, false)
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = true,
            expectedUacCampaignStatus = Status.MODERATING,
            updateAdsJobExpected = false,
            expectedDirectCampaignStatus = DirectCampaignStatus.CREATED,
            expectedBriefSynced = false,
        )
    }

    @Test
    fun startUacNonDraftCampaignForbidden_WithDirectId() {
        val campaignId = createDefaultUacContentCampaignReturnId()
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = true,
            expectedUacCampaignStatus = Status.DRAFT,
            updateAdsJobExpected = false,
            expectedDirectCampaignStatus = DirectCampaignStatus.DRAFT,
        )
        changeCampaignTargetStatus(campaignId, TargetStatus.STARTED, true, true)
        changeCampaignTargetStatus(campaignId, TargetStatus.STARTED, false, true)
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = true,
            expectedUacCampaignStatus = Status.MODERATING,
            updateAdsJobExpected = false,
            expectedDirectCampaignStatus = DirectCampaignStatus.CREATED,
            expectedBriefSynced = false,
        )
    }

    @Test
    fun stopUacNonDraftCampaignSuccess() {
        val campaignId = createDefaultUacContentCampaignReturnId()
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = true,
            expectedUacCampaignStatus = Status.DRAFT,
            updateAdsJobExpected = false,
            expectedDirectCampaignStatus = DirectCampaignStatus.DRAFT,
        )
        changeCampaignTargetStatus(campaignId, TargetStatus.STARTED, true)
        val response = changeCampaignTargetStatus(campaignId, TargetStatus.STOPPED, true)
        assertThat(response?.status, `is`(Status.MODERATING))
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = false,
            expectedUacCampaignStatus = Status.MODERATING,
            updateAdsJobExpected = false,
            expectedDirectCampaignStatus = DirectCampaignStatus.CREATED,
            expectedBriefSynced = false,
        )
    }

    @Test
    fun stopUacNonDraftCampaignSuccess_WithDirectId() {
        val campaignId = createDefaultUacContentCampaignReturnId()
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = true,
            expectedUacCampaignStatus = Status.DRAFT,
            updateAdsJobExpected = false,
            expectedDirectCampaignStatus = DirectCampaignStatus.DRAFT,
        )
        changeCampaignTargetStatus(campaignId, TargetStatus.STARTED, true, true)
        val response = changeCampaignTargetStatus(campaignId, TargetStatus.STOPPED, true, true)
        assertThat(response?.status, `is`(Status.MODERATING))
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = false,
            expectedUacCampaignStatus = Status.MODERATING,
            updateAdsJobExpected = false,
            expectedDirectCampaignStatus = DirectCampaignStatus.CREATED,
            expectedBriefSynced = false,
        )
    }

    @Test
    fun startUacDraftCampaign_ForbiddenWithoutTexts() {
        val campaignId = createDefaultUacContentCampaignReturnId(texts = listOf(), titles = listOf())
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = true,
            expectedUacCampaignStatus = Status.DRAFT,
            updateAdsJobExpected = false,
            expectedDirectCampaignStatus = DirectCampaignStatus.DRAFT,
        )
        changeCampaignTargetStatus(campaignId, TargetStatus.STOPPED, false)
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = true,
            expectedUacCampaignStatus = Status.DRAFT,
            updateAdsJobExpected = false,
            expectedDirectCampaignStatus = DirectCampaignStatus.DRAFT,
        )
    }

    @Test
    fun startUacDraftCampaign_SuccessWithOnlyHtml5Contents() {
        val contentId = createAsset(mediaType = ru.yandex.direct.core.entity.uac.model.MediaType.HTML5)
        val campaignId =
            createDefaultUacContentCampaignReturnId(texts = listOf(), titles = listOf(), contentIds = listOf(contentId))
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = true,
            expectedUacCampaignStatus = Status.DRAFT,
            updateAdsJobExpected = false,
            expectedDirectCampaignStatus = DirectCampaignStatus.DRAFT,
        )
        val response = changeCampaignTargetStatus(campaignId, TargetStatus.STARTED, true)
        assertThat(response?.status, `is`(Status.MODERATING))
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = true,
            expectedUacCampaignStatus = Status.MODERATING,
            updateAdsJobExpected = true,
            expectedDirectCampaignStatus = DirectCampaignStatus.CREATED,
        )
    }

    @Test
    fun startUacDraftCampaign_SuccessWithEmptyContent() {
        val campaignId = createDefaultUacContentCampaignReturnId(contentIds = listOf(), strategyPlatform = UacStrategyPlatform.SEARCH)
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = true,
            expectedUacCampaignStatus = Status.DRAFT,
            updateAdsJobExpected = false,
            expectedDirectCampaignStatus = DirectCampaignStatus.DRAFT,
        )
        val response = changeCampaignTargetStatus(campaignId, TargetStatus.STARTED, true)
        assertThat(response?.status, `is`(Status.MODERATING))
        checkUacCampaignState(
            campaignId,
            expectedDirectStatusShow = true,
            expectedUacCampaignStatus = Status.MODERATING,
            updateAdsJobExpected = true,
            expectedDirectCampaignStatus = DirectCampaignStatus.CREATED,
        )
    }

    fun changeCampaignTargetStatus(
        uacCampaignId: String,
        targetStatus: TargetStatus,
        isSuccessExpected: Boolean,
        isDirectId: Boolean = false,
    ): CampaignStatuses? {
        dbQueueSteps.clearQueue(DbQueueJobTypes.UAC_UPDATE_ADS)
        val login = userInfo.clientInfo!!.login
        val request = CampaignStatusesRequest(targetStatus = targetStatus)
        val directCampaignId = getDirectCampaignId(uacCampaignId)

        val urlTemplate = if (isDirectId) "/uac/campaign/direct/$directCampaignId/status?ulogin=$login"
        else "/uac/campaign/$uacCampaignId/status?ulogin=$login"
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post(urlTemplate)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(if (isSuccessExpected) MockMvcResultMatchers.status().isOk else MockMvcResultMatchers.status().isForbidden)
            .andReturn()
            .response
            .contentAsString

        return result.takeIf { isSuccessExpected }?.let { JsonUtils.fromJson(it, CampaignStatuses::class.java) }
    }

    protected fun createDefaultUacContentCampaignReturnId(
        regions: List<Long>? = listOf(Region.RUSSIA_REGION_ID, Region.BY_REGION_ID),
        trackingUrl: String? = "https://redirect.appmetrica.yandex.com/serve/1179849830915165578?c=ya_direct&c=ya_direct&google_aid_sha1={GOOGLE_AID_LC_SH1_HEX}&android_id_sha1={ANDROID_ID_LC_SH1_HEX}&device_type={DEVICE_TYPE}&source_type={STYPE}&source={SRC}&google_aid={google_aid}&click_id={logid}&search_term={PHRASE}&region_name={REGN_BS}&phrase_id={PHRASE_EXPORT_ID}&android_id={ANDROID_ID_LC}&position_type={PTYPE}&campaign_id=54494649",
        contentIds: List<String>? = null,
        texts: List<String>? = listOf("Some text for banner"),
        titles: List<String>? = listOf("Some title for banner"),
        strategyPlatform: UacStrategyPlatform? = null,
        retargetingCondition: UacRetargetingCondition? = null,
    ): String {
        dbQueueSteps.clearQueue(DbQueueJobTypes.UAC_UPDATE_ADS)
        val assetId = createAsset(ru.yandex.direct.core.entity.uac.model.MediaType.VIDEO)

        val request = createUacCampaignRequest(
            advType = AdvType.MOBILE_CONTENT,
            strategy = null,
            strategyPlatform = strategyPlatform,
            contentIds = contentIds ?: listOf(assetId),
            regions = regions,
            trackingUrl = trackingUrl,
            texts = texts,
            titles = titles,
            retargetingCondition = retargetingCondition,
        )

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        return resultJsonTree["result"]["id"].asText()
    }

    private fun createUacCampaignRequest(
        campaignName: String = "Test UAC campaign",
        advType: AdvType = AdvType.MOBILE_CONTENT,
        strategy: UacStrategy? = null,
        retargetingCondition: UacRetargetingCondition? = null,
        strategyPlatform: UacStrategyPlatform? = null,
        contentIds: List<String>? = null,
        regions: List<Long>? = listOf(Region.RUSSIA_REGION_ID, Region.BY_REGION_ID),
        minusRegions: List<Long>? = null,
        trackingUrl: String? = "https://redirect.appmetrica.yandex.com/serve/1179849830915165578?c=ya_direct&c=ya_direct&google_aid_sha1={GOOGLE_AID_LC_SH1_HEX}&android_id_sha1={ANDROID_ID_LC_SH1_HEX}&device_type={DEVICE_TYPE}&source_type={STYPE}&source={SRC}&google_aid={google_aid}&click_id={logid}&search_term={PHRASE}&region_name={REGN_BS}&phrase_id={PHRASE_EXPORT_ID}&android_id={ANDROID_ID_LC}&position_type={PTYPE}&campaign_id=54494649",
        texts: List<String>? = listOf("Some text for banner"),
        titles: List<String>? = listOf("Some title for banner"),
    ): CreateCampaignRequest {
        val ydbAppInfo = defaultAppInfo()
        uacAppInfoRepository.saveAppInfo(ydbAppInfo)

        val bannerHref = "https://" + TestDomain.randomDomain()
        val keywords = listOf("something keyword", "yet !another keyword")
        val minusKeywords = listOf("something minus keyword", "yet another minus keyword")
        val sitelink = Sitelink("sitelink title", "https://" + TestDomain.randomDomain(), "sitelink descr")
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
        val counters = listOf(RandomNumberUtils.nextPositiveInteger())

        // нужно создать мобильное приложение, иначе сервис попробует достать его из yt, к чему ci не готов
        val storeUrl = googlePlayAppInfoGetter.appPageUrl(ydbAppInfo.appId, ydbAppInfo.region, ydbAppInfo.language)
        steps.mobileAppSteps().createMobileApp(userInfo.clientInfo!!, storeUrl)
        return CreateCampaignRequest(
            displayName = campaignName,
            href = bannerHref,
            texts = texts,
            titles = titles,
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
            inventoryTypes = null,
            goals = goals,
            goalCreateRequest = null,
            counters = counters,
            permalinkId = null,
            phoneId = null,
            calltrackingPhones = emptyList(),
            sitelinks = listOf(sitelink),
            appId = ydbAppInfo.id,
            trackingUrl = trackingUrl,
            impressionUrl = null,
            targetId = TargetType.INSTALL,
            skadNetworkEnabled = null,
            adultContentEnabled = null,
            cpa = BigDecimal.valueOf(100000000L, 6),
            crr = null,
            timeTarget = null,
            strategy = strategy,
            retargetingCondition = retargetingCondition,
            videosAreNonSkippable = null,
            brandSurveyId = null,
            brandSurveyName = null,
            showsFrequencyLimit = null,
            strategyPlatform = strategyPlatform,
            adjustments = null,
            isEcom = null,
            feedId = null,
            feedFilters = null,
            trackingParams = null,
            cloneFromCampaignId = null,
            cpmAssets = null,
            campaignMeasurers = null,
            uacBrandsafety = null,
            uacDisabledPlaces = null,
            widgetPartnerId = null,
            source = null,
            mobileAppId = null,
            isRecommendationsManagementEnabled = false,
            isPriceRecommendationsManagementEnabled = false,
            relevanceMatch = null,
            showTitleAndBody = null,
            altAppStores = null,
            bizLandingId = null,
            searchLift = null,
        )
    }

    private fun doGetCampaignRequest(id: Long): JsonNode {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/campaign/direct/$id?ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andDo { System.err.println(it.response.contentAsString) }
            .andReturn()
            .response
            .contentAsString
        return JsonUtils.fromJson(result)["result"]
    }
}
