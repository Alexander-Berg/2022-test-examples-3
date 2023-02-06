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
class FixNullTrackerIdImpressionUrlOneshotTest : BaseFixBrokenImpressionUrlOneshotTest() {

    private lateinit var fixNullTrackerIdInImpressionURLOneshot: FixNullTrackerIdInImpressionUrlOneshot

    companion object {
        private const val URL = "https://play.google.com/store/apps/details?hl=ru&gl=ru&id=com.yandex.mobile.drive"
        private const val IMPERSSION_URL_WITH_ERROR = "https://view.adjust.com/impression/null?t=2pwbbsj" +
            "&campaign=rmp" +
            "&gps_adid={google_aid}" +
            "&oaid={oaid}" +
            "&ya_click_id={logid}" +
            "&user_agent={user_agent}" +
            "&ip_address={client_ip}" +
            "&language={device_lang}"
        private const val IMPRESSION_URL = "https://view.adjust.com/impression/2pwbbsj&adj?t=2pwbbsj" +
            "&campaign=rmp" +
            "&gps_adid={google_aid}" +
            "&oaid={oaid}" +
            "&ya_click_id={logid}" +
            "&user_agent={user_agent}" +
            "&ip_address={client_ip}" +
            "&language={device_lang}"
        private const val TRACKING_URL = "https://6lg6.adj.st?adj_t=2pwbbsj" +
            "&adj_campaign=rmp" +
            "&gps_adid={google_aid}" +
            "&oaid={oaid}" +
            "&ya_click_id={logid}" +
            "&adj_gps_adid={google_aid}" +
            "&adj_oaid={oaid}" +
            "&adj_ya_click_id={logid}"
    }


    @BeforeEach
    fun init() {
        fixNullTrackerIdInImpressionURLOneshot = FixNullTrackerIdInImpressionUrlOneshot(
            trackingUrlParseService,
            ytProvider,
            parseAppStoreUrlService,
            grutApiService,
            ydbGrutConverterYtRepository,
            uacBannerService,
            shardHelper,
            clientRepository,
            grutTransactionProvider,
        )
    }

    /**
     * Проверка полного флоу исправления битой ссылки с атрибуцией к показу
     */
    @Test
    fun fixBrokenVTA_successful() {
        val campaignId = createCampaign(URL, IMPERSSION_URL_WITH_ERROR, TRACKING_URL)

        whenever(ydbGrutConverterYtRepository.getCampaignIdsFromYtTable(any(), any(), any(), any()))
            .thenReturn(listOf(campaignId))

        val campaignBefore = grutUacCampaignService.getCampaignById(campaignId.toIdString())
        val fixParam = FixImpressionInputData(YtCluster.HAHN, "tablePath", 300, 0)
        fixNullTrackerIdInImpressionURLOneshot.execute(fixParam, null)

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
                .isEqualTo(IMPERSSION_URL_WITH_ERROR)

            assertThat(campaignAfter)
                .`as`("Check VTA link changed")
                .isNotNull
                .extracting("impressionUrl")
                .isNotEqualTo(IMPERSSION_URL_WITH_ERROR)
        }.assertAll()

    }

    /**
     * Проверка полного флоу исправления битой ссылки с атрибуцией к показу, кейс с изначально целой ссылкой
     */
    @Test
    fun fixBrokenVTA_noErrorInUrl_successful() {
        val campaignId = createCampaign(URL, IMPRESSION_URL, TRACKING_URL)

        whenever(ydbGrutConverterYtRepository.getCampaignIdsFromYtTable(any(), any(), any(), any()))
            .thenReturn(listOf(campaignId))

        val campaignBefore = grutUacCampaignService.getCampaignById(campaignId.toIdString())
        val fixParam = FixImpressionInputData(YtCluster.HAHN, "tablePath", 300, 0)
        fixNullTrackerIdInImpressionURLOneshot.execute(fixParam, null)

        val campaignAfter = grutUacCampaignService.getCampaignById(campaignId.toIdString())

        SoftAssertions().apply {
            assertThat(campaignBefore!!.trackingUrl)
                .`as`("Check tracking url intact")
                .isEqualTo(TRACKING_URL)
                .isEqualTo(campaignAfter!!.trackingUrl)

            assertThat(campaignBefore)
                .`as`("Check VTA link intact (before)")
                .isNotNull
                .extracting("impressionUrl")
                .isEqualTo(IMPRESSION_URL)

            assertThat(campaignAfter)
                .`as`("Check VTA link intact (after)")
                .isNotNull
                .extracting("impressionUrl")
                .isEqualTo(IMPRESSION_URL)
        }.assertAll()
    }
}
