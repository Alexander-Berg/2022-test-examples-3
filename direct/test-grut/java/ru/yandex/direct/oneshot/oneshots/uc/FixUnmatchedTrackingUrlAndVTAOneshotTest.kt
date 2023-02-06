package ru.yandex.direct.oneshot.oneshots.uc

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.oneshot.configuration.GrutOneshotTest
import ru.yandex.direct.ytwrapper.model.YtCluster

@GrutOneshotTest
@ExtendWith(SpringExtension::class)
class FixUnmatchedTrackingUrlAndVTAOneshotTest : BaseFixBrokenImpressionUrlOneshotTest() {

    companion object {
        private const val URL = "https://play.google.com/store/apps/details?hl=ru&gl=ru&id=com.yandex.mobile.drive"
        private const val IMPRESSION_WITH_ERROR = "https://impression.appsflyer.com/ru.holodilnik.android?" +
            "c={campaign_name}" +
            "&af_siteid={source_type}_{source}" +
            "&af_adset_id={gbid}" +
            "&af_ad_id={ad_id}" +
            "&af_sub1={keyword}" +
            "&af_sub2={source}" +
            "&af_sub3={region_name}" +
            "&af_viewthrough_lookback=24h" +
            "&google_aid={google_aid}" +
            "&android_id={android_id}" +
            "&af_prt=realweb" +
            "&advertising_id={google_aid}" +
            "&oaid={oaid}" +
            "&pid=yandexdirect_int" +
            "&clickid={logid}" +
            "&af_c_id={campaign_id}" +
            "&af_ip={client_ip}" +
            "&af_ua={user_agent}" +
            "&af_lang={device_lang}"
        private const val TRACKING_URL = "https://redirect.appmetrica.yandex.com/serve/748275949350471243" +
            "?google_aid_sha1={GOOGLE_AID_LC_SH1}" +
            "&android_id_sha1={ANDROID_ID_LC_SH1}" +
            "&device_type={device_type}" +
            "&source_type={source_type}" +
            "&source={source}" +
            "&search_term={keyword}" +
            "&region_name={region_name}" +
            "&phrase_id={phrase_id}" +
            "&android_id={ANDROIDID}" +
            "&position_type={position_type}" +
            "&campaign_id={campaign_id}" +
            "&google_aid={google_aid}" +
            "&click_id={logid}"
    }

    private lateinit var fixUnmatchedTrackingUrlAndVTAOneshot: FixUnmatchedTrackingUrlAndVTAOneshot


    @BeforeEach
    fun init() {
        fixUnmatchedTrackingUrlAndVTAOneshot = FixUnmatchedTrackingUrlAndVTAOneshot(
            trackingUrlParseService,
            ytProvider,
            parseAppStoreUrlService,
            grutApiService,
            ydbGrutConverterYtRepository,
            uacBannerService,
            shardHelper,
            clientRepository,
            grutTransactionProvider
        )
    }

    /**
     * Проверка полного флоу исправления битой ссылки с атрибуцией к показу
     */
    @Test
    fun fixBrokenVTA_successful() {
        val campaignId = createCampaign(URL, IMPRESSION_WITH_ERROR, TRACKING_URL)

        whenever(ydbGrutConverterYtRepository.getCampaignIdsFromYtTable(any(), any(), any(), any()))
            .thenReturn(listOf(campaignId))

        val campaignBefore = grutUacCampaignService.getCampaignById(campaignId.toIdString())
        val fixParam = FixImpressionInputData(YtCluster.HAHN, "tablePath", 300, 0)
        fixUnmatchedTrackingUrlAndVTAOneshot.execute(fixParam, null)

        val campaignAfter = grutUacCampaignService.getCampaignById(campaignId.toIdString())

        SoftAssertions().apply {

            assertThat(campaignBefore!!.trackingUrl)
                .`as`("Check tracking url intact")
                .isEqualTo(TRACKING_URL)
                .isEqualTo(campaignAfter!!.trackingUrl)

            assertThat(campaignBefore)
                .`as`("Check VTA link(before)")
                .isNotNull
                .extracting("impressionUrl")
                .isEqualTo(IMPRESSION_WITH_ERROR)

            assertThat(campaignAfter)
                .`as`("Check VTA link is fixed")
                .isNotNull
                .extracting("impressionUrl")
                .isNull()

        }.assertAll()

    }

    /**
     * Проверка полного флоу исправления битой ссылки с атрибуцией к показу, кейс с изначально пустой ссылкой
     */
    @Test
    fun fixBrokenVTA_noErrorInUrl_successful() {
        val campaignId = createCampaign(URL, null, TRACKING_URL)

        whenever(ydbGrutConverterYtRepository.getCampaignIdsFromYtTable(any(), any(), any(), any()))
            .thenReturn(listOf(campaignId))

        val campaignBefore = grutUacCampaignService.getCampaignById(campaignId.toIdString())
        val fixParam = FixImpressionInputData(YtCluster.HAHN, "tablePath", 300, 0)
        fixUnmatchedTrackingUrlAndVTAOneshot.execute(fixParam, null)

        val campaignAfter = grutUacCampaignService.getCampaignById(campaignId.toIdString())

        SoftAssertions().apply {
            assertThat(campaignBefore!!.trackingUrl)
                .`as`("Check tracking url intact")
                .isEqualTo(TRACKING_URL)
                .isEqualTo(campaignAfter!!.trackingUrl)
            assertThat(campaignBefore)
                .`as`("Check VTA link is null (before)")
                .isNotNull
                .extracting("impressionUrl")
                .isNull()
            assertThat(campaignAfter)
                .`as`("Check VTA link is null (after)")
                .isNotNull
                .extracting("impressionUrl")
                .isNull()
        }.assertAll()
    }
}
