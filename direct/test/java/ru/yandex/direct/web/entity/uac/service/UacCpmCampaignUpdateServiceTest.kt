package ru.yandex.direct.web.entity.uac.service

import com.google.common.collect.ImmutableList
import org.assertj.core.api.Assertions
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.nullValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.Mockito.anyString
import org.mockito.Mockito.doReturn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.banner.type.href.BannerUrlCheckService
import ru.yandex.direct.core.entity.brandSurvey.BrandSurvey
import ru.yandex.direct.core.entity.brandlift.repository.BrandSurveyRepository
import ru.yandex.direct.core.entity.client.model.ClientMeasurerSettings
import ru.yandex.direct.core.entity.client.model.ClientMeasurerSystem
import ru.yandex.direct.core.entity.client.model.MediascopeClientMeasurerSettings
import ru.yandex.direct.core.entity.client.repository.ClientMeasurerSettingsRepository
import ru.yandex.direct.core.entity.client.service.MediascopeClientSettingsService
import ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes
import ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.UAC_UPDATE_ADS
import ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType
import ru.yandex.direct.core.entity.uac.STORE_URL_FOR_APP_ID
import ru.yandex.direct.core.entity.uac.createDefaultVideoContent
import ru.yandex.direct.core.entity.uac.createMediaCampaignContent
import ru.yandex.direct.core.entity.uac.model.AgePoint
import ru.yandex.direct.core.entity.uac.model.CampaignContentStatus
import ru.yandex.direct.core.entity.uac.model.Gender
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.model.Socdem
import ru.yandex.direct.core.entity.uac.model.UacRetargetingCondition
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRule
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoal
import ru.yandex.direct.core.entity.uac.model.UacStrategy
import ru.yandex.direct.core.entity.uac.model.UacStrategyData
import ru.yandex.direct.core.entity.uac.model.UacStrategyName
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.CpmAssetButton
import ru.yandex.direct.core.entity.uac.repository.ydb.model.CpmAssetLogo
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacBannerMeasurerSystem
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacBrandsafety
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacButtonAction
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacCpmAsset
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacDisabledPlaces
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacMeasurer
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAccount
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbContent
import ru.yandex.direct.core.entity.uac.service.CpmBannerCampaignService
import ru.yandex.direct.core.service.urlchecker.RedirectCheckResult
import ru.yandex.direct.core.testing.data.TestBanners
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.BannerSteps
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.UacAccountSteps
import ru.yandex.direct.core.testing.steps.uac.UacCampaignSteps
import ru.yandex.direct.core.testing.steps.uac.UacContentSteps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.libs.mirrortools.utils.HostingsHandler
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.test.utils.checkContainsKey
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.test.utils.checkNotNull
import ru.yandex.direct.test.utils.checkNull
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.web.configuration.DirectWebTest
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime.now

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacCpmCampaignUpdateServiceTest {

    @Autowired
    private lateinit var uacCampaignSteps: UacCampaignSteps

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var uacYdbCampaignRepository: UacYdbCampaignRepository

    @Autowired
    private lateinit var cpmBannerCampaignService: CpmBannerCampaignService

    @Autowired
    private lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    private lateinit var uacContentSteps: UacContentSteps

    @Autowired
    private lateinit var uacAccountSteps: UacAccountSteps

    @Autowired
    private lateinit var bannerUrlCheckService: BannerUrlCheckService

    @Autowired
    private lateinit var webYdbUacCampaignUpdateService: WebYdbUacCampaignUpdateService

    @Autowired
    private lateinit var uacYdbContentRepository: UacYdbContentRepository

    @Autowired
    private lateinit var hostingsHandler: HostingsHandler

    @Autowired
    private lateinit var clientMeasurerSettingsRepository: ClientMeasurerSettingsRepository

    @Autowired
    private lateinit var mediascopeClientSettingsService: MediascopeClientSettingsService

    @Autowired
    private lateinit var brandSurveyRepository: BrandSurveyRepository

    private lateinit var uacCampaignInfo: UacCampaignSteps.UacCampaignInfo
    private lateinit var clientInfo: ClientInfo
    private lateinit var uacAccount: UacYdbAccount
    private lateinit var content: UacYdbContent

    @Before
    fun before() {
        content = createDefaultVideoContent()
        uacYdbContentRepository.saveContents(listOf(content))
        clientInfo = steps.clientSteps().createDefaultClient()
        uacAccount = uacAccountSteps.createAccount(clientInfo)
        uacCampaignInfo = uacCampaignSteps.createCpmCampaign(clientInfo)
        steps.dbQueueSteps().registerJobType(UAC_UPDATE_ADS)

        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.IN_APP_EVENTS_IN_RMP_ENABLED, true)
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.BRANDSAFETY_POLITICS, true)
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.BRAND_LIFT, true)

        steps.dbQueueSteps().registerJobType(DbQueueJobTypes.RECALC_BRAND_LIFT_CAMPAIGNS)

        doReturn(RedirectCheckResult.createSuccessResult(STORE_URL_FOR_APP_ID, ""))
            .`when`(bannerUrlCheckService).getRedirect(anyString(), anyString(), anyBoolean())
    }

    @Test
    fun updateCampaignNameTest() {
        val newCampaignName = "Brand new name"

        val result = webYdbUacCampaignUpdateService.updateCampaign(
            uacCampaignInfo.uacCampaign, uacCampaignInfo.uacDirectCampaign.directCampaignId,
            emptyPatchInternalRequest().copy(
                displayName = newCampaignName,
                href = uacCampaignInfo.uacCampaign.storeUrl,
                contentIds = listOf(content.id),
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
                videosAreNonSkippable = false,
                socdem = Socdem(
                    genders = listOf(Gender.MALE),
                    ageLower = AgePoint.AGE_25,
                    ageUpper = AgePoint.AGE_55,
                    incomeLower = Socdem.IncomeGrade.LOW,
                    incomeUpper = Socdem.IncomeGrade.MIDDLE
                )
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())
        assertThat((result.result as UacYdbCampaign).name, `is`(newCampaignName))

        uacYdbCampaignRepository.getCampaign(uacCampaignInfo.uacCampaign.id)?.name
            .checkEquals(newCampaignName)

        cpmBannerCampaignService.getCpmBannerCampaign(
            clientInfo.clientId!!,
            uacCampaignInfo.uacDirectCampaign.directCampaignId
        )?.name
            .checkEquals(newCampaignName)
    }

    @Test
    fun removeCampaignMeasurerTest() {
        val newCampaignName = "Brand new name"

        val preCampaign = uacYdbCampaignRepository.getCampaign(uacCampaignInfo.uacCampaign.id)
        preCampaign?.campaignMeasurers.checkNotNull()

        val result = webYdbUacCampaignUpdateService.updateCampaign(
            uacCampaignInfo.uacCampaign, uacCampaignInfo.uacDirectCampaign.directCampaignId,
            emptyPatchInternalRequest().copy(
                displayName = newCampaignName,
                href = uacCampaignInfo.uacCampaign.storeUrl,
                contentIds = listOf(content.id),
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
                videosAreNonSkippable = false,
                socdem = Socdem(
                    genders = listOf(Gender.MALE),
                    ageLower = AgePoint.AGE_25,
                    ageUpper = AgePoint.AGE_55,
                    incomeLower = Socdem.IncomeGrade.LOW,
                    incomeUpper = Socdem.IncomeGrade.MIDDLE
                ),
                campaignMeasurers = null
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())
        assertThat((result.result as UacYdbCampaign).name, `is`(newCampaignName))

        val campaign = uacYdbCampaignRepository.getCampaign(uacCampaignInfo.uacCampaign.id)
        campaign?.campaignMeasurers.checkNull()

        cpmBannerCampaignService.getCpmBannerCampaign(
            clientInfo.clientId!!,
            uacCampaignInfo.uacDirectCampaign.directCampaignId
        )?.measurers.isNullOrEmpty()
    }

    @Test
    fun updateCampaignAssetsTest() {
        val imageHash = "lERiGotdR9SRHjOmKuF2Gg";
        val bannerImageFormat = steps.bannerSteps()
            .createBannerImageFormat(clientInfo, TestBanners.defaultBannerImageFormat(imageHash))
        //добавить настройку медиаскоп
        val clientMeasurerSettingsList = listOf(
            ClientMeasurerSettings()
                .withClientId(clientInfo.clientId?.asLong())
                .withClientMeasurerSystem(ClientMeasurerSystem.MEDIASCOPE)
                .withSettings(mediascopeClientSettingsService.encryptSettings(
                    JsonUtils.toJson(
                        MediascopeClientMeasurerSettings()
                            .withAccessToken("1")
                            .withRefreshToken("2")
                            .withTmsecprefix("prefix")
                            .withExpiresAt(Instant.now().epochSecond + 8000)
                    )
                ))
        )
        clientMeasurerSettingsRepository.insertOrUpdate(clientInfo.shard, clientMeasurerSettingsList)

        val newCampaignName = "Brand new name"

        val result = webYdbUacCampaignUpdateService.updateCampaign(
            uacCampaignInfo.uacCampaign, uacCampaignInfo.uacDirectCampaign.directCampaignId,
            emptyPatchInternalRequest().copy(
                displayName = newCampaignName,
                href = uacCampaignInfo.uacCampaign.storeUrl,
                contentIds = listOf(content.id),
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
                videosAreNonSkippable = false,
                socdem = Socdem(
                    genders = listOf(Gender.MALE),
                    ageLower = AgePoint.AGE_25,
                    ageUpper = AgePoint.AGE_55,
                    incomeLower = Socdem.IncomeGrade.LOW,
                    incomeUpper = Socdem.IncomeGrade.MIDDLE
                ),
                cpmAssets = mapOf(
                    content.id to UacCpmAsset(
                        title = "Asset title",
                        titleExtension = "Asset title extension",
                        body = "Asset body",
                        button = CpmAssetButton(
                            action = UacButtonAction.BUY,
                            customText = null,
                            href = "https://ya.ru",
                        ),
                        logoImageHash = imageHash,
                        measurers = listOf(UacMeasurer(measurerType = UacBannerMeasurerSystem.WEBORAMA,
                            params = "{\"account\":1,\"tte\":1,\"aap\":1}"),
                            UacMeasurer(measurerType = UacBannerMeasurerSystem.MEDIASCOPE, params = "")),
                        pixels = listOf("https://mc.yandex.ru/pixel/7429537502459335683?rnd=%25aw_random%25"),
                        bannerHref = "https://ya.ru",
                    )
                )
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())
        assertThat((result.result as UacYdbCampaign).name, `is`(newCampaignName))

        val campaign = uacYdbCampaignRepository.getCampaign(uacCampaignInfo.uacCampaign.id)
        campaign?.name.checkEquals(newCampaignName)
        campaign?.cpmAssets.checkNotNull()
        campaign?.cpmAssets?.checkContainsKey(content.id)
        campaign?.cpmAssets!![content.id].checkEquals(
            UacCpmAsset(
                title = "Asset title",
                titleExtension = "Asset title extension",
                body = "Asset body",
                button = CpmAssetButton(
                    action = UacButtonAction.BUY,
                    customText = null,
                    href = "https://ya.ru",
                ),
                logoImageHash = imageHash,
                logoImage = CpmAssetLogo(
                    hash = imageHash,
                    height = 0, width = 0, path = "",
                    name = String.format(BannerSteps.DEFAULT_IMAGE_NAME_TEMPLATE, imageHash),
                    namespace = bannerImageFormat.avatarNamespace.toString(),
                    mdsGroupId = bannerImageFormat.mdsGroupId),
                measurers = listOf(UacMeasurer(measurerType = UacBannerMeasurerSystem.WEBORAMA,
                    params = "{\"account\":1,\"tte\":1,\"aap\":1}"),
                    UacMeasurer(measurerType = UacBannerMeasurerSystem.MEDIASCOPE, params = "")),
                pixels = listOf("https://mc.yandex.ru/pixel/7429537502459335683?rnd=%25aw_random%25"),
                bannerHref = "https://ya.ru",
            )
        )

        cpmBannerCampaignService.getCpmBannerCampaign(
            clientInfo.clientId!!,
            uacCampaignInfo.uacDirectCampaign.directCampaignId
        )?.name
            .checkEquals(newCampaignName)
    }

    @Test
    fun updateCampaignMediascopeHasNotSettingTest() {
        //удалить настройку медиаскоп
        clientMeasurerSettingsRepository.deleteByClientIdAndSystem(clientInfo.shard,
            clientInfo.clientId?.asLong(), ClientMeasurerSystem.MEDIASCOPE)
        val newCampaignName = "Brand new name"
        val oldName = uacCampaignInfo.uacCampaign.name

        val result = webYdbUacCampaignUpdateService.updateCampaign(
            uacCampaignInfo.uacCampaign, uacCampaignInfo.uacDirectCampaign.directCampaignId,
            emptyPatchInternalRequest().copy(
                displayName = newCampaignName,
                href = uacCampaignInfo.uacCampaign.storeUrl,
                contentIds = listOf(content.id),
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
                videosAreNonSkippable = false,
                socdem = Socdem(
                    genders = listOf(Gender.MALE),
                    ageLower = AgePoint.AGE_25,
                    ageUpper = AgePoint.AGE_55,
                    incomeLower = Socdem.IncomeGrade.LOW,
                    incomeUpper = Socdem.IncomeGrade.MIDDLE
                ),
                cpmAssets = mapOf(
                    content.id to UacCpmAsset(
                        title = "Asset title",
                        titleExtension = "Asset title extension",
                        body = "Asset body",
                        button = null,
                        logoImageHash = null,
                        measurers = listOf(UacMeasurer(measurerType = UacBannerMeasurerSystem.MEDIASCOPE, params = "")),
                        pixels = listOf("https://mc.yandex.ru/pixel/7429537502459335683?rnd=%25aw_random%25"),
                        bannerHref = "https://ya.ru",
                    )
                )
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(false))
        assertThat(result.validationResult, notNullValue())
        assertThat(result.result, nullValue())
        Assertions.assertThat(result.validationResult).`is`(matchedBy(hasDefectDefinitionWith<Any>(validationError(
            path(field("cpmAssets"), field(content.id), field("measurers"), index(0)),
            CommonDefects.invalidValue()
        ))))

        uacYdbCampaignRepository.getCampaign(uacCampaignInfo.uacCampaign.id)?.name
            .checkEquals(oldName)
    }

    @Test
    fun updateCampaignBadCpmAssetsTest() {
        val newCampaignName = "Brand new name"
        val oldName = uacCampaignInfo.uacCampaign.name;

        val result = webYdbUacCampaignUpdateService.updateCampaign(
            uacCampaignInfo.uacCampaign, uacCampaignInfo.uacDirectCampaign.directCampaignId,
            emptyPatchInternalRequest().copy(
                displayName = newCampaignName,
                href = uacCampaignInfo.uacCampaign.storeUrl,
                contentIds = listOf(content.id),
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
                videosAreNonSkippable = false,
                socdem = Socdem(
                    genders = listOf(Gender.MALE),
                    ageLower = AgePoint.AGE_25,
                    ageUpper = AgePoint.AGE_55,
                    incomeLower = Socdem.IncomeGrade.LOW,
                    incomeUpper = Socdem.IncomeGrade.MIDDLE
                ),
                cpmAssets = mapOf(
                    content.id to UacCpmAsset(
                        title = "Asset title loooooooooooooooooooooooooooooooooooooooooooooooong",
                        titleExtension = "Asset title extension",
                        body = "Asset body",
                        button = null,
                        logoImageHash = null,
                        measurers = listOf(UacMeasurer(measurerType = UacBannerMeasurerSystem.WEBORAMA,
                            params = "{\"account\":1,\"tte\":1,\"aap\":1}")),
                        pixels = listOf("https://mc.yandex.ru/pixel/7429537502459335683?rnd=%25aw_random%25"),
                        bannerHref = null,
                    )
                )
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(false))
        assertThat(result.validationResult, notNullValue())
        assertThat(result.result, nullValue())

        uacYdbCampaignRepository.getCampaign(uacCampaignInfo.uacCampaign.id)?.name
            .checkEquals(oldName)
    }

    @Test
    fun updateCampaignBadBannerHrefTest() {
        val newCampaignName = "Brand new name"
        val oldName = uacCampaignInfo.uacCampaign.name;

        val result = webYdbUacCampaignUpdateService.updateCampaign(
            uacCampaignInfo.uacCampaign, uacCampaignInfo.uacDirectCampaign.directCampaignId,
            emptyPatchInternalRequest().copy(
                displayName = newCampaignName,
                href = uacCampaignInfo.uacCampaign.storeUrl,
                contentIds = listOf(content.id),
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
                videosAreNonSkippable = false,
                socdem = Socdem(
                    genders = listOf(Gender.MALE),
                    ageLower = AgePoint.AGE_25,
                    ageUpper = AgePoint.AGE_55,
                    incomeLower = Socdem.IncomeGrade.LOW,
                    incomeUpper = Socdem.IncomeGrade.MIDDLE
                ),
                cpmAssets = mapOf(
                    content.id to UacCpmAsset(
                        title = "Asset title",
                        titleExtension = "Asset title extension",
                        body = "Asset body",
                        button = null,
                        logoImageHash = null,
                        measurers = listOf(UacMeasurer(measurerType = UacBannerMeasurerSystem.WEBORAMA,
                            params = "{\"account\":1,\"tte\":1,\"aap\":1}")),
                        pixels = null,
                        bannerHref = "ya.ru",
                    )
                )
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(false))
        assertThat(result.validationResult, notNullValue())
        assertThat(result.result, nullValue())

        uacYdbCampaignRepository.getCampaign(uacCampaignInfo.uacCampaign.id)?.name
            .checkEquals(oldName)
    }

    @Test
    fun updateCampaignBadButtonHrefTest() {
        val newCampaignName = "Brand new name"
        val oldName = uacCampaignInfo.uacCampaign.name;

        val result = webYdbUacCampaignUpdateService.updateCampaign(
            uacCampaignInfo.uacCampaign, uacCampaignInfo.uacDirectCampaign.directCampaignId,
            emptyPatchInternalRequest().copy(
                displayName = newCampaignName,
                href = uacCampaignInfo.uacCampaign.storeUrl,
                contentIds = listOf(content.id),
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
                videosAreNonSkippable = false,
                socdem = Socdem(
                    genders = listOf(Gender.MALE),
                    ageLower = AgePoint.AGE_25,
                    ageUpper = AgePoint.AGE_55,
                    incomeLower = Socdem.IncomeGrade.LOW,
                    incomeUpper = Socdem.IncomeGrade.MIDDLE
                ),
                cpmAssets = mapOf(
                    content.id to UacCpmAsset(
                        title = "Asset title",
                        titleExtension = "Asset title extension",
                        body = "Asset body",
                        button = CpmAssetButton(
                            action = UacButtonAction.BUY,
                            customText = null,
                            href = "ya.ru",
                        ),
                        logoImageHash = null,
                        measurers = listOf(UacMeasurer(measurerType = UacBannerMeasurerSystem.WEBORAMA,
                            params = "{\"account\":1,\"tte\":1,\"aap\":1}")),
                        pixels = null,
                        bannerHref = null,
                    )
                )
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(false))
        assertThat(result.validationResult, notNullValue())
        assertThat(result.result, nullValue())

        uacYdbCampaignRepository.getCampaign(uacCampaignInfo.uacCampaign.id)?.name
            .checkEquals(oldName)
    }

    @Test
    fun updateCampaignSocdemWithoutIncomeTest() {
        val newCampaignName = "Brand new name"

        val result = webYdbUacCampaignUpdateService.updateCampaign(
            uacCampaignInfo.uacCampaign, uacCampaignInfo.uacDirectCampaign.directCampaignId,
            emptyPatchInternalRequest().copy(
                displayName = newCampaignName,
                href = uacCampaignInfo.uacCampaign.storeUrl,
                contentIds = listOf(content.id),
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
                videosAreNonSkippable = false,
                socdem = Socdem(
                    genders = listOf(Gender.MALE),
                    incomeUpper = null,
                    incomeLower = null,
                    ageUpper = AgePoint.AGE_25,
                    ageLower = AgePoint.AGE_18,
                )
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
    }

    @Test
    fun updateCampaignBadSocdemTest() {
        val newCampaignName = "Brand new name"
        val oldName = uacCampaignInfo.uacCampaign.name;

        val result = webYdbUacCampaignUpdateService.updateCampaign(
            uacCampaignInfo.uacCampaign, uacCampaignInfo.uacDirectCampaign.directCampaignId,
            emptyPatchInternalRequest().copy(
                displayName = newCampaignName,
                href = uacCampaignInfo.uacCampaign.storeUrl,
                contentIds = listOf(content.id),
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
                videosAreNonSkippable = false,
                socdem = Socdem(
                    genders = listOf(Gender.MALE),
                    incomeUpper = null,
                    incomeLower = null,
                    ageUpper = AgePoint.AGE_18,
                    ageLower = AgePoint.AGE_25,
                )
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(false))
        assertThat(result.validationResult, notNullValue())
        assertThat(result.result, nullValue())

        uacYdbCampaignRepository.getCampaign(uacCampaignInfo.uacCampaign.id)?.name
            .checkEquals(oldName)
    }

    @Test
    fun updateCampaignBadRetargetingTest() {
        val newCampaignName = "Brand new name"

        val result = webYdbUacCampaignUpdateService.updateCampaign(
            uacCampaignInfo.uacCampaign, uacCampaignInfo.uacDirectCampaign.directCampaignId,
            emptyPatchInternalRequest().copy(
                displayName = newCampaignName,
                href = uacCampaignInfo.uacCampaign.storeUrl,
                contentIds = listOf(content.id),
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
                videosAreNonSkippable = false,
                socdem = Socdem(
                    genders = listOf(Gender.MALE),
                    ageLower = AgePoint.AGE_25,
                    ageUpper = AgePoint.AGE_55,
                    incomeLower = Socdem.IncomeGrade.LOW,
                    incomeUpper = Socdem.IncomeGrade.MIDDLE
                ),
                retargetingCondition = UacRetargetingCondition(conditionRules = listOf())

            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(false))
        assertThat(result.validationResult, notNullValue())
    }

    @Test
    fun updateCampaignRetargetingTest() {
        val newCampaignName = "Brand new name"

        val result = webYdbUacCampaignUpdateService.updateCampaign(
            uacCampaignInfo.uacCampaign, uacCampaignInfo.uacDirectCampaign.directCampaignId,
            emptyPatchInternalRequest().copy(
                displayName = newCampaignName,
                href = uacCampaignInfo.uacCampaign.storeUrl,
                contentIds = listOf(content.id),
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
                videosAreNonSkippable = false,
                socdem = Socdem(
                    genders = listOf(Gender.MALE),
                    ageLower = AgePoint.AGE_25,
                    ageUpper = AgePoint.AGE_55,
                    incomeLower = Socdem.IncomeGrade.LOW,
                    incomeUpper = Socdem.IncomeGrade.MIDDLE
                ),
                retargetingCondition = UacRetargetingCondition(
                    conditionRules = listOf(
                        UacRetargetingConditionRule(
                            interestType = CryptaInterestType.all,
                            type = UacRetargetingConditionRule.RuleType.OR,
                            goals = listOf(UacRetargetingConditionRuleGoal(2499001184)),
                        ),
                        UacRetargetingConditionRule(
                            type = UacRetargetingConditionRule.RuleType.OR,
                            goals = listOf(UacRetargetingConditionRuleGoal(2499000002)),
                        ),
                        UacRetargetingConditionRule(
                            type = UacRetargetingConditionRule.RuleType.OR,
                            goals = listOf(
                                UacRetargetingConditionRuleGoal(2499000006),
                                UacRetargetingConditionRuleGoal(2499000004)
                            )
                        ),
                        UacRetargetingConditionRule(
                            type = UacRetargetingConditionRule.RuleType.OR,
                            goals = listOf(UacRetargetingConditionRuleGoal(2499000013))
                        )

                    )
                )

            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())
    }

    @Test
    fun updateCampaignBrandSafetyTest() {
        val newCampaignName = "Brand new name"
        val brandSafetyPoliticsId = 4294967302L//для политики фича включена

        val result = webYdbUacCampaignUpdateService.updateCampaign(
            uacCampaignInfo.uacCampaign, uacCampaignInfo.uacDirectCampaign.directCampaignId,
            emptyPatchInternalRequest().copy(
                displayName = newCampaignName,
                href = uacCampaignInfo.uacCampaign.storeUrl,
                contentIds = listOf(content.id),
                strategy = UacStrategy(
                    UacStrategyName.AUTOBUDGET_MAX_IMPRESSIONS,
                    UacStrategyData(
                        BigDecimal.ZERO,
                        true,
                        BigDecimal.TEN,
                        LocalDate.now().plusMonths(6),
                        LocalDate.now(),
                        BigDecimal.valueOf(2100), BigDecimal.valueOf(100),
                        null
                    )
                ),
                videosAreNonSkippable = false,
                socdem = Socdem(
                    genders = listOf(Gender.MALE),
                    ageLower = AgePoint.AGE_25,
                    ageUpper = AgePoint.AGE_55,
                    incomeLower = Socdem.IncomeGrade.LOW,
                    incomeUpper = Socdem.IncomeGrade.MIDDLE
                ),
                uacBrandsafety = UacBrandsafety(true, listOf(brandSafetyPoliticsId))
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())

        val directCampaign = cpmBannerCampaignService.getCpmBannerCampaign(
            clientInfo.clientId!!,
            uacCampaignInfo.uacDirectCampaign.directCampaignId
        )

        assertThat(directCampaign?.brandSafetyCategories, notNullValue())
        assertThat(directCampaign?.brandSafetyCategories, hasSize(8))
        assertThat(directCampaign?.brandSafetyCategories, hasItem(brandSafetyPoliticsId))
    }

    @Test
    fun updateCampaignBrandSafetyInvalidTest() {
        val newCampaignName = "Brand new name"
        val brandSafetyReligionId = 4294967307L//для религии фича не включена, нельзя добавлять

        val result = webYdbUacCampaignUpdateService.updateCampaign(
            uacCampaignInfo.uacCampaign, uacCampaignInfo.uacDirectCampaign.directCampaignId,
            emptyPatchInternalRequest().copy(
                displayName = newCampaignName,
                href = uacCampaignInfo.uacCampaign.storeUrl,
                contentIds = listOf(content.id),
                strategy = UacStrategy(
                    UacStrategyName.AUTOBUDGET_MAX_IMPRESSIONS,
                    UacStrategyData(
                        BigDecimal.ZERO,
                        true,
                        BigDecimal.TEN,
                        LocalDate.now().plusMonths(6),
                        LocalDate.now(),
                        BigDecimal.valueOf(2100), BigDecimal.valueOf(100),
                        null
                    )
                ),
                videosAreNonSkippable = false,
                socdem = Socdem(
                    genders = listOf(Gender.MALE),
                    ageLower = AgePoint.AGE_25,
                    ageUpper = AgePoint.AGE_55,
                    incomeLower = Socdem.IncomeGrade.LOW,
                    incomeUpper = Socdem.IncomeGrade.MIDDLE
                ),
                uacBrandsafety = UacBrandsafety(true, listOf(brandSafetyReligionId))
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(false))
        assertThat(result.validationResult, notNullValue())
        assertThat(result.result, nullValue())
    }

    @Test
    fun updateCampaignBlackListTest() {
        val newCampaignName = "Brand new name"

        val result = webYdbUacCampaignUpdateService.updateCampaign(
            uacCampaignInfo.uacCampaign, uacCampaignInfo.uacDirectCampaign.directCampaignId,
            emptyPatchInternalRequest().copy(
                displayName = newCampaignName,
                href = uacCampaignInfo.uacCampaign.storeUrl,
                contentIds = listOf(content.id),
                strategy = UacStrategy(
                    UacStrategyName.AUTOBUDGET_MAX_IMPRESSIONS,
                    UacStrategyData(
                        BigDecimal.ZERO,
                        true,
                        BigDecimal.TEN,
                        LocalDate.now().plusMonths(6),
                        LocalDate.now(),
                        BigDecimal.valueOf(2100),
                        BigDecimal.valueOf(100),
                        null
                    )
                ),
                videosAreNonSkippable = false,
                socdem = Socdem(
                    genders = listOf(Gender.MALE),
                    ageLower = AgePoint.AGE_25,
                    ageUpper = AgePoint.AGE_55,
                    incomeLower = Socdem.IncomeGrade.LOW,
                    incomeUpper = Socdem.IncomeGrade.MIDDLE
                ),
                uacDisabledPlaces = UacDisabledPlaces(
                    ImmutableList.of("http://profi.ru/"),
                    ImmutableList.of("http://profi.ru/"),
                    null,
                    null
                )
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())

        val directCampaign = cpmBannerCampaignService.getCpmBannerCampaign(
            clientInfo.clientId!!,
            uacCampaignInfo.uacDirectCampaign.directCampaignId
        )

        assertThat(directCampaign?.disabledVideoPlacements, notNullValue());
        assertThat(directCampaign?.disabledDomains, notNullValue());
        assertThat(directCampaign?.disabledDomains, contains("profi.ru"));
        assertThat(directCampaign?.disabledVideoPlacements, contains("profi.ru"));
    }

    @Test
    fun updateMediaContentHtml5Test() {
        val html5ContentIdToKeep = uacContentSteps.createHtml5Content(clientInfo, uacAccount.id).id
        val html5ContentIdToDelete = uacContentSteps.createHtml5Content(clientInfo, uacAccount.id).id
        val html5ContentIdToRestore = uacContentSteps.createHtml5Content(clientInfo, uacAccount.id).id
        val html5ContentIdToAdd = uacContentSteps.createHtml5Content(clientInfo, uacAccount.id).id
        val videoContentIdToKeep = uacContentSteps.createVideoContent(clientInfo, uacAccount.id).id
        val videoContentIdToDelete = uacContentSteps.createVideoContent(clientInfo, uacAccount.id).id
        val videoContentIdToRestore = uacContentSteps.createVideoContent(clientInfo, uacAccount.id).id

        val html5CampaignContents = listOf(
            html5ContentIdToKeep,
            html5ContentIdToDelete,
            html5ContentIdToRestore
        ).mapIndexed { index, contentId ->
            createMediaCampaignContent(
                campaignId = uacCampaignInfo.uacCampaign.id,
                type = MediaType.HTML5,
                order = index,
                contentId = contentId,
                status = if (index == 2) {
                    CampaignContentStatus.DELETED
                } else {
                    CampaignContentStatus.CREATED
                },
                removedAt = if (index == 2) {
                    now()
                } else {
                    null
                },
            )
        }

        val videoCampaignContents = listOf(
            videoContentIdToKeep,
            videoContentIdToDelete,
            videoContentIdToRestore
        ).mapIndexed { index, contentId ->
            createMediaCampaignContent(
                campaignId = uacCampaignInfo.uacCampaign.id,
                type = MediaType.VIDEO,
                order = index,
                contentId = contentId,
                status = if (index == 2) {
                    CampaignContentStatus.DELETED
                } else {
                    CampaignContentStatus.CREATED
                },
                removedAt = if (index == 2) {
                    now()
                } else {
                    null
                },
            )
        }
        uacYdbCampaignContentRepository.addCampaignContents(html5CampaignContents + videoCampaignContents)

        val result = webYdbUacCampaignUpdateService.updateCampaign(
            uacCampaignInfo.uacCampaign, uacCampaignInfo.uacDirectCampaign.directCampaignId,
            emptyPatchInternalRequest().copy(
                displayName = uacCampaignInfo.uacCampaign.name,
                href = uacCampaignInfo.uacCampaign.storeUrl,
                contentIds = listOf(
                    html5ContentIdToKeep, html5ContentIdToRestore, html5ContentIdToAdd
                ),
                videosAreNonSkippable = false,
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
                socdem = Socdem(
                    genders = listOf(Gender.MALE),
                    ageLower = AgePoint.AGE_25,
                    ageUpper = AgePoint.AGE_55,
                    incomeLower = Socdem.IncomeGrade.LOW,
                    incomeUpper = Socdem.IncomeGrade.MIDDLE
                )
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())

        val updateCampaignContents = uacYdbCampaignContentRepository.getCampaignContents(uacCampaignInfo.uacCampaign.id)
        updateCampaignContents
            .filter { it.type == MediaType.HTML5 }
            .filter { it.status == CampaignContentStatus.CREATED }
            .sortedBy { it.order }
            .map { it.contentId }
            .checkEquals(listOf(html5ContentIdToKeep, html5ContentIdToRestore, html5ContentIdToAdd))
    }

    @Test
    fun updateMediaContentTest() {
        val imageContentIdToKeep = uacContentSteps.createImageContent(clientInfo, uacAccount.id).id
        val imageContentIdToDelete = uacContentSteps.createImageContent(clientInfo, uacAccount.id).id
        val imageContentIdToRestore = uacContentSteps.createImageContent(clientInfo, uacAccount.id).id
        val imageContentIdToAdd = uacContentSteps.createImageContent(clientInfo, uacAccount.id).id
        val videoContentIdToKeep = uacContentSteps.createVideoContent(clientInfo, uacAccount.id).id
        val videoContentIdToDelete = uacContentSteps.createVideoContent(clientInfo, uacAccount.id).id
        val videoContentIdToRestore = uacContentSteps.createVideoContent(clientInfo, uacAccount.id).id
        val videoContentIdToAdd = uacContentSteps.createVideoContent(clientInfo, uacAccount.id).id

        val imageCampaignContents = listOf(
            imageContentIdToKeep,
            imageContentIdToDelete,
            imageContentIdToRestore
        ).mapIndexed { index, contentId ->
            createMediaCampaignContent(
                campaignId = uacCampaignInfo.uacCampaign.id,
                type = MediaType.IMAGE,
                order = index,
                contentId = contentId,
                status = if (index == 2) {
                    CampaignContentStatus.DELETED
                } else {
                    CampaignContentStatus.CREATED
                },
                removedAt = if (index == 2) {
                    now()
                } else {
                    null
                },
            )
        }

        val videoCampaignContents = listOf(
            videoContentIdToKeep,
            videoContentIdToDelete,
            videoContentIdToRestore
        ).mapIndexed { index, contentId ->
            createMediaCampaignContent(
                campaignId = uacCampaignInfo.uacCampaign.id,
                type = MediaType.VIDEO,
                order = index,
                contentId = contentId,
                status = if (index == 2) {
                    CampaignContentStatus.DELETED
                } else {
                    CampaignContentStatus.CREATED
                },
                removedAt = if (index == 2) {
                    now()
                } else {
                    null
                },
            )
        }
        uacYdbCampaignContentRepository.addCampaignContents(imageCampaignContents + videoCampaignContents)

        val result = webYdbUacCampaignUpdateService.updateCampaign(
            uacCampaignInfo.uacCampaign, uacCampaignInfo.uacDirectCampaign.directCampaignId,
            emptyPatchInternalRequest().copy(
                displayName = uacCampaignInfo.uacCampaign.name,
                href = uacCampaignInfo.uacCampaign.storeUrl,
                contentIds = listOf(
                    videoContentIdToKeep, videoContentIdToRestore, videoContentIdToAdd
                ),
                videosAreNonSkippable = false,
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
                socdem = Socdem(
                    genders = listOf(Gender.MALE),
                    ageLower = AgePoint.AGE_25,
                    ageUpper = AgePoint.AGE_55,
                    incomeLower = Socdem.IncomeGrade.LOW,
                    incomeUpper = Socdem.IncomeGrade.MIDDLE
                )
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())

        val updateCampaignContents = uacYdbCampaignContentRepository.getCampaignContents(uacCampaignInfo.uacCampaign.id)
        updateCampaignContents
            .filter { it.type == MediaType.VIDEO }
            .filter { it.status == CampaignContentStatus.CREATED }
            .sortedBy { it.order }
            .map { it.contentId }
            .checkEquals(listOf(videoContentIdToKeep, videoContentIdToRestore, videoContentIdToAdd))
    }

    @Test
    fun updateCampaignBrandLiftName() {
        val brandSurveyId = "brandSurveyIdcpm"
        val brandSurveyName = "brandSurveyName"
        val newName = "new brandSurveyName"
        val brandSurvey = BrandSurvey()
            .withBrandSurveyId(brandSurveyId)
            .withName(brandSurveyName)
            .withClientId(clientInfo.clientId?.asLong())
            .withSegmentId(0L)
            .withExperimentId(0L)
            .withRetargetingConditionId(0L)
        uacCampaignInfo = uacCampaignSteps.createCpmCampaignWithBrandLift(clientInfo, brandSurvey = brandSurvey)
        val result = webYdbUacCampaignUpdateService.updateCampaign(
            uacCampaignInfo.uacCampaign, uacCampaignInfo.uacDirectCampaign.directCampaignId,
            emptyPatchInternalRequest().copy(
                displayName = "Brand new name",
                href = uacCampaignInfo.uacCampaign.storeUrl,
                contentIds = listOf(content.id),
                strategy = UacStrategy(
                    UacStrategyName.AUTOBUDGET_MAX_IMPRESSIONS,
                    UacStrategyData(
                        BigDecimal.ZERO,
                        true,
                        BigDecimal.TEN,
                        LocalDate.now().plusMonths(6),
                        LocalDate.now(),
                        BigDecimal.valueOf(2100),
                        BigDecimal.valueOf(100),
                        null
                    )
                ),
                videosAreNonSkippable = false,
                socdem = Socdem(
                    genders = listOf(Gender.MALE),
                    ageLower = AgePoint.AGE_25,
                    ageUpper = AgePoint.AGE_55,
                    incomeLower = Socdem.IncomeGrade.LOW,
                    incomeUpper = Socdem.IncomeGrade.MIDDLE
                ),
                brandSurveyId = brandSurveyId,
                brandSurveyName = newName
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())


        val brandSurveys = brandSurveyRepository.getBrandSurvey(clientInfo.shard, brandSurveyId)
        assertNotNull(brandSurveys)
        assertEquals(1, brandSurveys.size)
        assertEquals(newName, brandSurveys[0].name)
    }
}
