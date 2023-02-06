package ru.yandex.direct.core.testing.steps.uac

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import ru.yandex.direct.core.entity.brandSurvey.BrandSurvey
import ru.yandex.direct.core.entity.campaign.model.CampaignSource
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppAlternativeStore
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppDeviceTypeTargeting.PHONE
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppDeviceTypeTargeting.TABLET
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppNetworkTargeting.CELLULAR
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppNetworkTargeting.WI_FI
import ru.yandex.direct.core.entity.uac.createDefaultImageContent
import ru.yandex.direct.core.entity.uac.createDirectCampaign
import ru.yandex.direct.core.entity.uac.createImageCampaignContent
import ru.yandex.direct.core.entity.uac.createTextCampaignContent
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.AdvType
import ru.yandex.direct.core.entity.uac.model.CampaignContentStatus
import ru.yandex.direct.core.entity.uac.model.DirectCampaignStatus
import ru.yandex.direct.core.entity.uac.model.MediaType.TEXT
import ru.yandex.direct.core.entity.uac.model.MediaType.TITLE
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.core.entity.uac.model.UacRetargetingCondition
import ru.yandex.direct.core.entity.uac.model.UacStrategy
import ru.yandex.direct.core.entity.uac.model.UacStrategyData
import ru.yandex.direct.core.entity.uac.model.UacStrategyName
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatch
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.generateUniqueRandomId
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacCampaignMeasurer
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacCampaignMeasurerSystem
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAppInfo
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectCampaign
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign
import ru.yandex.direct.core.testing.data.TestCampaigns.activeMobileAppCampaign
import ru.yandex.direct.core.testing.data.TestCampaigns.averageCpaPerCampStrategy
import ru.yandex.direct.core.testing.data.TestCampaigns.averageCpaStrategy
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.repository.TestCampaignRepository
import ru.yandex.direct.core.testing.steps.CampaignSteps
import ru.yandex.direct.core.testing.steps.CampaignsMobileContentSteps
import ru.yandex.direct.core.testing.steps.MobileAppSteps
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign
import ru.yandex.direct.currency.CurrencyCode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime.now
import java.time.temporal.ChronoUnit

@Lazy
@Component
class UacCampaignSteps {

    @Autowired
    private lateinit var campaignSteps: CampaignSteps

    @Autowired
    private lateinit var campaignsMobileContentSteps: CampaignsMobileContentSteps

    @Autowired
    private lateinit var mobileAppSteps: MobileAppSteps

    @Autowired
    private lateinit var uacYdbCampaignRepository: UacYdbCampaignRepository

    @Autowired
    private lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    private lateinit var uacYdbDirectCampaignRepository: UacYdbDirectCampaignRepository

    @Autowired
    private lateinit var uacYdbAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    private lateinit var testCampaignRepository: TestCampaignRepository

    @Autowired
    private lateinit var uacYdbContentRepository: UacYdbContentRepository

    data class UacCampaignInfo(
        val uacCampaign: UacYdbCampaign,
        val uacDirectCampaign: UacYdbDirectCampaign,
        val contents: List<UacYdbCampaignContent>,
        val appInfo: UacYdbAppInfo?,
        val campaign: CampaignInfo,
    )

    fun createCpmCampaign(
        clientInfo: ClientInfo,
        campaign: Campaign? = null,
        impressionUrl: String? = null,
    ): UacCampaignInfo {
        val directCampaign = campaign ?: activeCpmBannerCampaign(null, null)
            .withOrderId(0L)
            .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB))
            .withStrategy(TestCampaigns.autobudgetMaxImpressionsCustomPeriodStrategy())

        val ydbAppInfo = defaultAppInfo()
        val ydbCampaign = createYdbCampaign(
            appId = ydbAppInfo.id,
            advType = AdvType.CPM_BANNER,
            impressionUrl = impressionUrl,
            startedAt = now().truncatedTo(ChronoUnit.SECONDS),
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
            campaignMeasurers = listOf(UacCampaignMeasurer(UacCampaignMeasurerSystem.MOAT, "{}"))
        )

        val campaignInfo = campaignSteps.createCampaign(directCampaign, clientInfo)
        return createDataInYdb(ydbCampaign, campaignInfo, ydbAppInfo)
    }

    fun createCpmCampaignWithBrandLift(
        clientInfo: ClientInfo,
        campaign: Campaign? = null,
        impressionUrl: String? = null,
        brandSurvey: BrandSurvey? = null,
    ): UacCampaignInfo {
        val ydbAppInfo = defaultAppInfo()
        val ydbCampaign = createYdbCampaign(
            appId = ydbAppInfo.id,
            advType = AdvType.CPM_BANNER,
            impressionUrl = impressionUrl,
            startedAt = now().truncatedTo(ChronoUnit.SECONDS),
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
            campaignMeasurers = listOf(UacCampaignMeasurer(UacCampaignMeasurerSystem.MOAT, "{}"))
        )
        val brandLift = brandSurvey ?: BrandSurvey()
            .withBrandSurveyId("brandSurveyId")
            .withName("brandSurveyName")
            .withClientId(clientInfo.clientId?.asLong())
            .withSegmentId(0L)
            .withExperimentId(0L)
            .withRetargetingConditionId(0L)
        val campaignInfo = campaignSteps.createActiveCpmBannerCampaignWithBrandLift(clientInfo, brandLift)
        return createDataInYdb(ydbCampaign, campaignInfo, ydbAppInfo)
    }

    fun createMobileAppCampaign(
        clientInfo: ClientInfo,
        campaign: Campaign? = null,
        draft: Boolean = false,
        impressionUrl: String? = null,
        briefSynced: Boolean? = null,
        accountId: String = generateUniqueRandomId(),
        appId: String? = null,
        retargetingCondition: UacRetargetingCondition? = null,
        cpa: BigDecimal? = BigDecimal.valueOf(100000000L, 6),
        altAppStores: Set<MobileAppAlternativeStore>? = null
    ): UacCampaignInfo {
        val directCampaign = campaign ?: activeMobileAppCampaign(null, null)
            .withOrderId(0L)
            .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB))

        var ydbAppInfo: UacYdbAppInfo? = null
        if (appId == null) {
            ydbAppInfo = defaultAppInfo()
        }
        val ydbCampaign = createYdbCampaign(
            appId = ydbAppInfo?.id ?: appId!!,
            impressionUrl = impressionUrl,
            startedAt = if (!draft) now().truncatedTo(ChronoUnit.SECONDS) else null,
            briefSynced = briefSynced,
            accountId = accountId,
            retargetingCondition = retargetingCondition,
            cpa = cpa
        )

        val mobileAppInfo = mobileAppSteps.createMobileApp(clientInfo, ydbCampaign.storeUrl)
        val campaignInfo = campaignSteps.createCampaign(directCampaign, clientInfo)
        campaignsMobileContentSteps.createCampaignsMobileContent(
            campaignInfo.shard, campaignInfo.campaignId,
            mobileAppInfo.mobileAppId, setOf(PHONE, TABLET), setOf(WI_FI, CELLULAR), altAppStores
        )

        return createDataInYdb(ydbCampaign, campaignInfo, ydbAppInfo)
    }

    fun createTextCampaign(
        clientInfo: ClientInfo,
        campaign: Campaign? = null,
        status: Status = Status.STARTED,
        accountId: String = generateUniqueRandomId(),
        relevanceMatch: UacRelevanceMatch? = null,
    ): UacCampaignInfo {
        return createCampaign(clientInfo, campaign, status, accountId, relevanceMatch).first
    }

    fun createCampaign(
        clientInfo: ClientInfo,
        campaign: Campaign? = null,
        status: Status = Status.STARTED,
        accountId: String = generateUniqueRandomId(),
        relevanceMatch: UacRelevanceMatch? = null,
        isEcom: Boolean = false
    ): Pair<UacCampaignInfo, CampaignInfo> {
        val directCampaign = campaign ?: TestCampaigns.activeTextCampaign(clientInfo.clientId, clientInfo.uid)
            .withOrderId(0L)
            .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB))
            .withStrategy(averageCpaStrategy())

        val ydbCampaign = createYdbCampaign(
            advType = AdvType.TEXT,
            startedAt = now().truncatedTo(ChronoUnit.SECONDS),
            accountId = accountId,
            appId = null,
            createdAt = now().truncatedTo(ChronoUnit.SECONDS),
            relevanceMatch = relevanceMatch,
            isEcom = isEcom
        )

        val campaignInfo = campaignSteps.createCampaign(directCampaign, clientInfo)
        testCampaignRepository.setSource(clientInfo.shard, campaignInfo.campaignId, CampaignSource.UAC)

        return createDataInYdb(ydbCampaign, campaignInfo) to campaignInfo
    }

    fun createEcomUcCampaigns(
        clientInfo: ClientInfo,
        campaign: Campaign? = null,
        status: Status = Status.STARTED,
        useNewBackend: Boolean = false
    ): Pair<UacCampaignInfo, Map<CampaignType, CampaignInfo>> {
        val (masterUacCampaignInfo, campaignInfo) = createCampaign(clientInfo, campaign, status)
        val campaignInfosByType = if (useNewBackend) {
            mapOf(CampaignType.TEXT to campaignInfo)
        } else {
            val subCampaignsInfo = createEcomUcSubcampaigns(clientInfo, masterUacCampaignInfo.campaign.campaignId)
            mapOf(
                CampaignType.TEXT to campaignInfo,
                CampaignType.DYNAMIC to subCampaignsInfo.dynamicInfo,
                CampaignType.PERFORMANCE to subCampaignsInfo.smartInfo
            )
        }
        return masterUacCampaignInfo to campaignInfosByType
    }

    fun createEcomUcSubcampaigns(clientInfo: ClientInfo, masterCampaignId: Long): EcomUcSubcampaignsInfo {
        val smartCampaign = TestCampaigns.activePerformanceCampaign(clientInfo.clientId, clientInfo.uid)
            .withMasterCid(masterCampaignId)
            .withStrategy(averageCpaPerCampStrategy())
        val dynamicCampaign = TestCampaigns.activeDynamicCampaign(clientInfo.clientId, clientInfo.uid)
            .withMasterCid(masterCampaignId)
            .withStrategy(averageCpaStrategy())
        val smartInfo = campaignSteps.createCampaign(smartCampaign, clientInfo)
        val dynamicInfo = campaignSteps.createCampaign(dynamicCampaign, clientInfo)

        testCampaignRepository.setSource(clientInfo.shard, smartInfo.campaignId, CampaignSource.UAC)
        testCampaignRepository.setSource(clientInfo.shard, dynamicInfo.campaignId, CampaignSource.UAC)

        return EcomUcSubcampaignsInfo(smartInfo, dynamicInfo)
    }

    fun createEcomUcCampaign(
        clientInfo: ClientInfo,
        campaign: Campaign? = null,
        status: Status = Status.STARTED,
        useNewBackend: Boolean = false
    ): UacCampaignInfo {
        return createEcomUcCampaigns(clientInfo, campaign, status, useNewBackend).first
    }

    private fun createDataInYdb(
        ydbCampaign: UacYdbCampaign,
        campaignInfo: CampaignInfo,
        ydbAppInfo: UacYdbAppInfo? = null,
    ): UacCampaignInfo {
        val ydbDirectCampaign = createDirectCampaign(
            id = ydbCampaign.id,
            directCampaignId = campaignInfo.campaignId,
            status = DirectCampaignStatus.DRAFT,
        )

        var uacCampaignContents = mutableListOf<UacYdbCampaignContent>()

        if (ydbCampaign.advType == AdvType.MOBILE_CONTENT) {
            val campaignTitle1 = createTextCampaignContent(campaignId = ydbCampaign.id, order = 0, mediaType = TITLE)
            val campaignTitle2 = createTextCampaignContent(campaignId = ydbCampaign.id, order = 1, mediaType = TITLE)
            val campaignTitle3 = createTextCampaignContent(
                campaignId = ydbCampaign.id,
                order = 1, mediaType = TITLE,
                status = CampaignContentStatus.DELETED,
                removedAt = now()
            )
            val campaignText1 = createTextCampaignContent(campaignId = ydbCampaign.id, order = 0, mediaType = TEXT)
            val campaignText2 = createTextCampaignContent(campaignId = ydbCampaign.id, order = 1, mediaType = TEXT)

            val content = createDefaultImageContent()
            uacYdbContentRepository.saveContents(listOf(content))
            val campaignImage1 = createImageCampaignContent(campaignId = ydbCampaign.id, contentId = content.id)

            ydbAppInfo?.let { uacYdbAppInfoRepository.saveAppInfo(it) }
            uacYdbCampaignContentRepository.addCampaignContents(
                listOf(
                    campaignTitle1, campaignTitle2, campaignTitle3,
                    campaignText1, campaignText2, campaignImage1
                )
            )

            uacCampaignContents =
                mutableListOf(campaignTitle1, campaignTitle2, campaignText1, campaignText2, campaignImage1)
        } else if (ydbCampaign.advType == AdvType.TEXT) {
            val campaignTitle = createTextCampaignContent(campaignId = ydbCampaign.id, order = 0, mediaType = TITLE)
            val campaignText = createTextCampaignContent(campaignId = ydbCampaign.id, order = 0, mediaType = TEXT)
            val campaignTextDeleted = createTextCampaignContent(
                campaignId = ydbCampaign.id,
                order = 0, mediaType = TEXT,
                status = CampaignContentStatus.DELETED,
                removedAt = now()
            )

            uacYdbCampaignContentRepository.addCampaignContents(
                listOf(
                    campaignTitle,
                    campaignText,
                    campaignTextDeleted
                )
            )

            uacCampaignContents = mutableListOf(campaignTitle, campaignText)
        }

        uacYdbCampaignRepository.addCampaign(ydbCampaign)
        uacYdbDirectCampaignRepository.saveDirectCampaign(ydbDirectCampaign)

        return UacCampaignInfo(
            ydbCampaign,
            ydbDirectCampaign,
            uacCampaignContents,
            ydbAppInfo,
            campaignInfo,
        )
    }
}

data class EcomUcSubcampaignsInfo(
    val smartInfo: CampaignInfo,
    val dynamicInfo: CampaignInfo,
)
