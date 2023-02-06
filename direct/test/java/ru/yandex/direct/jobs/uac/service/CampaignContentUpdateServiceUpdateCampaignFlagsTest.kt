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
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.createDirectAd
import ru.yandex.direct.core.entity.uac.createDirectAdGroup
import ru.yandex.direct.core.entity.uac.createTextCampaignContent
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectAdGroupRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectAdRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAppInfo
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectAd
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectAdGroup
import ru.yandex.direct.core.entity.uac.service.CampaignContentUpdateService
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestNewMobileAppBanners
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.NewMobileAppBannerInfo
import ru.yandex.direct.core.testing.info.TypedCampaignInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.TypedCampaignStepsUnstubbed
import ru.yandex.direct.jobs.configuration.JobsTest
import ru.yandex.direct.jobs.uac.repository.AbstractUacRepositoryJobTest

/**
 * Проверяем обновление флагов content_flags у кампании в ydb через вызов
 * функции updateCampaignContentsAndCampaignFlags в [CampaignContentUpdateService]
 */
@JobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CampaignContentUpdateServiceUpdateCampaignFlagsTest : AbstractUacRepositoryJobTest() {

    @Autowired
    private lateinit var campaignContentUpdateService: CampaignContentUpdateService

    @Autowired
    private lateinit var uacYdbDirectAdRepository: UacYdbDirectAdRepository

    @Autowired
    private lateinit var typedCampaignStepsUnstubbed: TypedCampaignStepsUnstubbed

    @Autowired
    private lateinit var uacYdbDirectAdGroupRepository: UacYdbDirectAdGroupRepository

    @Autowired
    private lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    private lateinit var uacYdbCampaignRepository: UacYdbCampaignRepository

    @Autowired
    private lateinit var uacYdbAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    private lateinit var steps: Steps

    private lateinit var clientInfo: ClientInfo
    private lateinit var userInfo: UserInfo
    private lateinit var campaignInfo: TypedCampaignInfo
    private lateinit var adGroupInfo: AdGroupInfo
    private lateinit var uacAppInfo: UacYdbAppInfo
    private lateinit var uacCampaign: UacYdbCampaign
    private lateinit var uacDirectAdGroup: UacYdbDirectAdGroup
    private lateinit var titleCampaignContent: UacYdbCampaignContent
    private lateinit var textCampaignContent: UacYdbCampaignContent

    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        userInfo = clientInfo.chiefUserInfo!!

        steps.trustedRedirectSteps().addValidCounters()

        val mobileAppInfo = steps.mobileAppSteps().createMobileApp(clientInfo, STORE_URL)
        campaignInfo = typedCampaignStepsUnstubbed.createMobileContentCampaign(userInfo, clientInfo,
            TestCampaigns.defaultMobileContentCampaignWithSystemFields(clientInfo)
                .withStrategy(TestCampaignsStrategy.defaultAutobudgetStrategy())
                .withMobileAppId(mobileAppInfo.mobileAppId))

        adGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(campaignInfo.toCampaignInfo())

        uacAppInfo = defaultAppInfo()
        uacYdbAppInfoRepository.saveAppInfo(uacAppInfo)

        uacCampaign = createYdbCampaign(appId = uacAppInfo.id)
        uacYdbCampaignRepository.addCampaign(uacCampaign)

        uacDirectAdGroup = createDirectAdGroup(
            directAdGroupId = adGroupInfo.adGroupId,
            directCampaignId = uacCampaign.id,
        )
        uacYdbDirectAdGroupRepository.saveDirectAdGroup(uacDirectAdGroup)

        titleCampaignContent = createCampaignContent(
            campaignId = uacCampaign.id,
            type = MediaType.TITLE,
        )
        textCampaignContent = createTextCampaignContent(
            campaignId = uacCampaign.id,
        )
        uacYdbCampaignContentRepository.addCampaignContents(listOf(titleCampaignContent, textCampaignContent))
    }

    /**
     * В группе только один баннер с флагом age=6 -> кампания в ydb обновляется с флагом age=6
     */
    @Test
    fun updateCampaignContentsAndCampaignFlags_BannerWithAge6_CampaignWithAge6() {
        val bannerId = createMobileContentBanner(BannerFlags()
            .with(BannerFlags.AGE, Age.AGE_6))

        createUacYdbDirectAd(bannerId)

        val expectContentFlags = mutableMapOf(
            "age" to "6",
        )

        checkUpdateFlagsAndCheck(expectContentFlags)
    }

    /**
     * В группе два баннера с разными флагами age=6/16 -> кампания в ydb обновляется с максимальным флагом age=16
     */
    @Test
    fun updateCampaignContentsAndCampaignFlags_BannersWithAge6And16_CampaignWithAge16() {
        val bannerId1 = createMobileContentBanner(BannerFlags()
            .with(BannerFlags.AGE, Age.AGE_6)
            .with(BannerFlags.GOODFACE, true))
        val bannerId2 = createMobileContentBanner(BannerFlags()
            .with(BannerFlags.AGE, Age.AGE_16)
            .with(BannerFlags.SOCIAL_ADVERTISING, true))

        createUacYdbDirectAd(bannerId1)
        createUacYdbDirectAd(bannerId2)

        val expectContentFlags = mutableMapOf(
            "age" to "16",
            "goodface" to null,
            "social_advertising" to null,
        )
        checkUpdateFlagsAndCheck(expectContentFlags)
    }

    /**
     * В группе баннер без флагов -> кампания в ydb обновляется без флагов
     */
    @Test
    fun updateCampaignContentsAndCampaignFlags_BannerWithoutAge_CampaignWithoutAge() {
        val bannerId = createMobileContentBanner(BannerFlags())

        createUacYdbDirectAd(bannerId)

        checkUpdateFlagsAndCheck(emptyMap())
    }

    /**
     * В группе баннер с разными флагами, не содержащими флаг age -> кампания в ydb обновляется с флагами баннера
     */
    @Test
    fun updateCampaignContentsAndCampaignFlags_BannerWithDifferentFlags_CampaignWithTheSameFlags() {
        val bannerId = createMobileContentBanner(BannerFlags()
            .with(BannerFlags.GOODFACE, true)
            .with(BannerFlags.SOCIAL_ADVERTISING, true)
            .with(BannerFlags.ALCOHOL, true))

        createUacYdbDirectAd(bannerId)

        val expectContentFlags = mutableMapOf(
            "goodface" to null,
            "social_advertising" to null,
            "alcohol" to null,
        )

        checkUpdateFlagsAndCheck(expectContentFlags)
    }

    private fun createMobileContentBanner(bannerFlags: BannerFlags): Long {
        val banner = TestNewMobileAppBanners
            .fullMobileAppBanner(campaignInfo.id, adGroupInfo.adGroupId)
            .withFlags(bannerFlags)

        return steps.mobileAppBannerSteps()
            .createMobileAppBanner(NewMobileAppBannerInfo()
                .withClientInfo(clientInfo)
                .withAdGroupInfo(adGroupInfo)
                .withBanner(banner)).bannerId
    }

    private fun createUacYdbDirectAd(bannerId: Long): UacYdbDirectAd {
        val uacDirectAd = createDirectAd(
            titleContentId = titleCampaignContent.id,
            textContentId = textCampaignContent.id,
            directAdGroupId = uacDirectAdGroup.id,
            directAdId = bannerId,
        )
        uacYdbDirectAdRepository.saveDirectAd(uacDirectAd)
        return uacDirectAd
    }

    private fun checkUpdateFlagsAndCheck(expectContentFlags: Map<String, String?>) {
        campaignContentUpdateService.updateCampaignContentsAndCampaignFlags(uacCampaign.id,
            userInfo.clientId, listOf(uacDirectAdGroup), listOf(titleCampaignContent, textCampaignContent))

        val actualUacCampaign = uacYdbCampaignRepository.getCampaign(uacCampaign.id)

        Assertions.assertThat(actualUacCampaign!!.contentFlags)
            .`as`("флаг возрастной категории")
            .isEqualTo(expectContentFlags)
    }
}
