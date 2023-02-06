package ru.yandex.direct.web.entity.uac.repository

import junitparams.JUnitParamsRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.model.AdvType
import ru.yandex.direct.core.entity.uac.model.AgePoint
import ru.yandex.direct.core.entity.uac.model.DeviceType
import ru.yandex.direct.core.entity.uac.model.Gender
import ru.yandex.direct.core.entity.uac.model.InventoryType
import ru.yandex.direct.core.entity.uac.model.LimitPeriodType
import ru.yandex.direct.core.entity.uac.model.Socdem
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.core.entity.uac.model.TargetStatus
import ru.yandex.direct.core.entity.uac.model.TargetType
import ru.yandex.direct.core.entity.uac.model.UacCampaignOptions
import ru.yandex.direct.core.entity.uac.model.UacGoal
import ru.yandex.direct.core.entity.uac.model.UacRetargetingCondition
import ru.yandex.direct.core.entity.uac.model.UacStrategy
import ru.yandex.direct.core.entity.uac.model.UacStrategyData
import ru.yandex.direct.core.entity.uac.model.UacStrategyName
import ru.yandex.direct.core.entity.uac.model.UacStrategyPlatform
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.web.configuration.DirectWebTest
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacYdbCampaignRepositoryTest : AbstractUacRepositoryTest() {
    @Autowired
    private lateinit var uacYdbCampaignRepository: UacYdbCampaignRepository

    @Test
    fun getCampaignTest() {
        val campaign = getFullCampaign()
        uacYdbCampaignRepository.addCampaign(campaign)
        val gotCampaign = uacYdbCampaignRepository.getCampaign(campaign.id)
        assertThat(gotCampaign).isEqualTo(campaign)
    }

    @Test
    fun deleteTest() {
        val campaign = createYdbCampaign()
        uacYdbCampaignRepository.addCampaign(campaign)
        uacYdbCampaignRepository.delete(campaign.id)
        val gotCampaign = uacYdbCampaignRepository.getCampaign(campaign.id)
        assertThat(gotCampaign).isNull()
    }

    private fun getFullCampaign() =
        UacYdbCampaign(
            id = UacYdbUtils.generateUniqueRandomId(),
            name = "Яндекс.Музыка и Подкасты – скачивайте и слушайте (Android)",
            advType = AdvType.MOBILE_CONTENT,
            cpa = BigDecimal.valueOf(100000000L, 6),
            weekLimit = BigDecimal.valueOf(10000000000L, 6),
            regions = listOf(157),
            minusRegions = listOf(111),
            storeUrl = "https://play.google.com/store/apps/details?hl=ru&gl=ru&id=ru.yandex.music",
            appId = "4503159017934857465",
            targetId = TargetType.ORDER,
            trackingUrl = "https://redirect.appmetrica.yandex.com/serve/1179849830915165578?c=ya_direct&c=ya_direct&google_aid_sha1={GOOGLE_AID_LC_SH1_HEX}&android_id_sha1={ANDROID_ID_LC_SH1_HEX}&device_type={DEVICE_TYPE}&source_type={STYPE}&source={SRC}&google_aid={google_aid}&click_id={logid}&search_term={PHRASE}&region_name={REGN_BS}&phrase_id={PHRASE_EXPORT_ID}&android_id={ANDROID_ID_LC}&position_type={PTYPE}&campaign_id=54494649",
            account = "10342753804164253308",
            impressionUrl = "https://app.appsflyer.com/com.im30.ROE.gp?pid=yandexdirect_int&c={campaign_name_lat}&af_c_id={campaign_id}&af_adset_id={gbid}&af_ad_id={ad_id}&af_sub1={phrase_id}&af_sub2={retargeting_id}&af_sub3={keyword}&af_sub4={adtarget_name}&af_click_lookback=7d&clickid={logid}&google_aid={googleaid}&advertising_id={google_aid}&ya_click_id={logid}&idfa={ios_ifa}",
            createdAt = LocalDateTime.of(2021, 5, 24, 10, 12, 34),
            updatedAt = LocalDateTime.of(2021, 5, 24, 15, 12, 34),
            startedAt = LocalDateTime.of(2021, 5, 24, 20, 12, 34),
            targetStatus = TargetStatus.STARTED,
            contentFlags = mapOf("age" to "0"),
            options = UacCampaignOptions(LimitPeriodType.WEEK),
            skadNetworkEnabled = true,
            adultContentEnabled = true,
            hyperGeoId = 5,
            keywords = listOf("сиба ину купить щенка", "сиба ину купить щенка москва"),
            minusKeywords = listOf("кот", "котенок"),
            socdem = Socdem(genders = listOf(Gender.FEMALE), ageLower = AgePoint.AGE_18, ageUpper = AgePoint.AGE_INF, Socdem.IncomeGrade.HIGH, Socdem.IncomeGrade.PREMIUM),
            deviceTypes = setOf(DeviceType.ALL),
            inventoryTypes = null,
            goals = listOf(UacGoal(goalId = 12, conversionValue = BigDecimal.valueOf(123))),
            counters = listOf(50603470),
            permalinkId = 987561L,
            phoneId = 123459L,
            calltrackingSettingsId = 1, //произвольный ID
            timeTarget = null,
            strategy = UacStrategy(UacStrategyName.AUTOBUDGET_MAX_IMPRESSIONS,
                UacStrategyData(
                    BigDecimal.valueOf(10L, 6),
                    false, BigDecimal.TEN,
                    LocalDate.now(),
                    LocalDate.now(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    0),
            ),
            retargetingCondition = UacRetargetingCondition(listOf(), "Test condition"),
            videosAreNonSkippable = null,
            brandSurveyId = null,
            briefSynced = true,
            showsFrequencyLimit = null,
            strategyPlatform = UacStrategyPlatform.BOTH,
            isEcom = null,
            crr = null,
            feedId = null,
            feedFilters = null,
            trackingParams = null,
            cpmAssets = null,
            campaignMeasurers = null,
            uacBrandsafety = null,
            uacDisabledPlaces = null,
            recommendationsManagementEnabled = null,
            priceRecommendationsManagementEnabled = null,
        )
}
