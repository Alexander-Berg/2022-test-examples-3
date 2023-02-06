package ru.yandex.direct.jobs.uac.service

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.banner.model.Age
import ru.yandex.direct.core.entity.banner.model.BannerFlags
import ru.yandex.direct.core.entity.uac.STORE_URL
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAppInfo
import ru.yandex.direct.core.entity.uac.service.CampaignContentUpdateService
import ru.yandex.direct.core.entity.uac.service.GrutCampaignContentUpdateService
import ru.yandex.direct.jobs.configuration.GrutJobsTest
import ru.yandex.grut.objects.proto.AgePoint.EAgePoint.AP_AGE_16
import ru.yandex.grut.objects.proto.AgePoint.EAgePoint.AP_AGE_6
import ru.yandex.grut.objects.proto.ContentFlags
import ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_TEXT

/**
 * Проверяем обновление флагов content_flags у кампании в груте через вызов
 * функции updateCampaignContentsAndCampaignFlags в [CampaignContentUpdateService]
 */
@GrutJobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class GrutCampaignContentUpdateServiceUpdateCampaignFlagsTest : BaseGrutCampaignContentUpdateServiceTest() {

    @Autowired
    private lateinit var campaignContentUpdateService: GrutCampaignContentUpdateService

    private lateinit var uacAppInfo: UacYdbAppInfo

    open var adGroupBriefEnabled = false

    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        createGrutClient(clientInfo)
        userInfo = clientInfo.chiefUserInfo!!

        steps.trustedRedirectSteps().addValidCounters()

        mobileAppInfo = steps.mobileAppSteps().createMobileApp(clientInfo, STORE_URL)
        uacAppInfo = defaultAppInfo()
    }

    /**
     * В группе только один баннер с флагом age=6 -> кампания в ydb обновляется с флагом age=6
     */
    @Test
    fun updateCampaignContentsAndCampaignFlags_BannerWithAge6_CampaignWithAge6() {
        val testData = createTestData(
            setOf(MT_TEXT),
            adGroupBriefEnabled = adGroupBriefEnabled,
        )
        val bannerId = createMobileContentBanner(testData, BannerFlags()
            .with(BannerFlags.AGE, Age.AGE_6))

        createGrutBanner(testData, bannerId)

        val expectContentFlags =
            ContentFlags.TContentFlags.newBuilder()
                .setAge(AP_AGE_6)
                .build()

        checkUpdateFlagsAndCheck(testData, expectContentFlags)
    }

    /**
     * В группе два баннера с разными флагами age=6/16 -> кампания в ydb обновляется с максимальным флагом age=16
     */
    @Test
    fun updateCampaignContentsAndCampaignFlags_BannersWithAge6And16_CampaignWithAge16() {
        val testData = createTestData(
            setOf(MT_TEXT),
            adGroupBriefEnabled = adGroupBriefEnabled,
        )
        val bannerId1 = createMobileContentBanner(testData, BannerFlags()
            .with(BannerFlags.AGE, Age.AGE_6)
            .with(BannerFlags.GOODFACE, true))
        val bannerId2 = createMobileContentBanner(testData, BannerFlags()
            .with(BannerFlags.AGE, Age.AGE_16)
            .with(BannerFlags.SOCIAL_ADVERTISING, true))

        createGrutBanner(testData, bannerId1)
        createGrutBanner(testData, bannerId2)

        val expectContentFlags =
            ContentFlags.TContentFlags.newBuilder()
                .setAge(AP_AGE_16)
                .setGoodface(true)
                .setSocialAdvertising(true)
                .build()
        checkUpdateFlagsAndCheck(testData, expectContentFlags)
    }

    /**
     * В группе баннер без флагов -> кампания в ydb обновляется без флагов
     */
    @Test
    fun updateCampaignContentsAndCampaignFlags_BannerWithoutAge_CampaignWithoutAge() {
        val testData = createTestData(
            setOf(MT_TEXT),
            adGroupBriefEnabled = adGroupBriefEnabled,
        )
        val bannerId = createMobileContentBanner(testData, BannerFlags())

        createGrutBanner(testData, bannerId)

        checkUpdateFlagsAndCheck(testData, ContentFlags.TContentFlags.newBuilder().build())
    }

    /**
     * В группе баннер с разными флагами, не содержащими флаг age -> кампания в ydb обновляется с флагами баннера
     */
    @Test
    fun updateCampaignContentsAndCampaignFlags_BannerWithDifferentFlags_CampaignWithTheSameFlags() {
        val testData = createTestData(
            setOf(MT_TEXT),
            adGroupBriefEnabled = adGroupBriefEnabled,
        )
        val bannerId = createMobileContentBanner(testData, BannerFlags()
            .with(BannerFlags.GOODFACE, true)
            .with(BannerFlags.SOCIAL_ADVERTISING, true)
            .with(BannerFlags.ALCOHOL, true))

        createGrutBanner(testData, bannerId)

        val expectContentFlags = ContentFlags.TContentFlags.newBuilder()
            .setGoodface(true)
            .setSocialAdvertising(true)
            .setAlcohol(true)
            .build()

        checkUpdateFlagsAndCheck(testData, expectContentFlags)
    }


    /**
     * В группе активный/архивный баннеры с разными флагами -> кампания обновляется только с флагами от активного баннера
     */
    @Test
    fun updateCampaignContentsAndCampaignFlags_WithDeletedBanner_CampaignWithTheFlagsFromActiveBanner() {
        val testData = createTestData(
            setOf(MT_TEXT),
            adGroupBriefEnabled = adGroupBriefEnabled,
        )
        val activeBannerId = createMobileContentBanner(
            testData,
            BannerFlags().with(BannerFlags.GOODFACE, true)
        )
        val deletedBannerId = createMobileContentBanner(
            testData,
            BannerFlags().with(BannerFlags.MEDICINE, true),
            isDeleted = true
        )

        createGrutBanner(testData, activeBannerId)
        createGrutBanner(testData, deletedBannerId, true)

        val expectContentFlags = ContentFlags.TContentFlags.newBuilder()
            .setGoodface(true)
            .build()

        checkUpdateFlagsAndCheck(testData, expectContentFlags)
    }

    private fun checkUpdateFlagsAndCheck(testData: TestData, expectContentFlags: ContentFlags.TContentFlags) {
        campaignContentUpdateService.updateCampaignContentsAndCampaignFlags(testData.campaignInfo.shard, listOf(testData.campaignInfo.id))

        val gotCampaign = grutApiService.briefGrutApi.getBrief(testData.campaignId)!!

        Assertions.assertThat(gotCampaign.spec.campaignBrief.contentFlags)
            .`as`("флаг возрастной категории")
            .isEqualTo(expectContentFlags)
    }
}
