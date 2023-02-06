package ru.yandex.direct.web.entity.uac.service

import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.Mockito
import org.mockito.Mockito.anyString
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.entity.banner.type.href.BannerUrlCheckService
import ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes
import ru.yandex.direct.core.entity.uac.STORE_URL_FOR_APP_ID
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.service.RmpCampaignService
import ru.yandex.direct.core.entity.uac.service.appinfo.GooglePlayAppInfoGetter
import ru.yandex.direct.core.service.urlchecker.RedirectCheckResult
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.web.entity.uac.model.PatchCampaignInternalRequest
import ru.yandex.direct.web.entity.uac.model.PatchCampaignRequest
import kotlin.properties.Delegates

abstract class UacCampaignUpdateServiceTestBase {

    abstract val uacCampaignUpdateServiceWeb: WebBaseUacCampaignUpdateService

    @Autowired
    protected lateinit var steps: Steps

    @Autowired
    protected lateinit var bannerUrlCheckService: BannerUrlCheckService

    @Autowired
    protected lateinit var rmpCampaignService: RmpCampaignService

    @Autowired
    protected lateinit var uacYdbAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    protected lateinit var googlePlayAppInfoGetter: GooglePlayAppInfoGetter

    protected lateinit var uacCampaign: UacYdbCampaign
    protected var directCampaignId by Delegates.notNull<Long>()
    protected lateinit var clientInfo: ClientInfo

    abstract fun getUacCampaign(id: String): UacYdbCampaign?

    abstract fun createUacCampaign(
        clientInfo: ClientInfo,
        draft: Boolean,
        impressionUrl: String? = null,
        briefSynced: Boolean? = null,
    )

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        steps.dbQueueSteps().registerJobType(DbQueueJobTypes.UAC_UPDATE_ADS)
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.IN_APP_EVENTS_IN_RMP_ENABLED, true)
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.CAMPAIGN_ALLOWED_ON_ADULT_CONTENT, true)

        Mockito.doReturn(RedirectCheckResult.createSuccessResult(STORE_URL_FOR_APP_ID, ""))
            .`when`(bannerUrlCheckService).getRedirect(anyString(), anyString(), anyBoolean())
    }

    @Test
    fun updateCampaignNameTest() {
        val newCampaignName = "Brand new name"

        val result = uacCampaignUpdateServiceWeb.updateCampaign(
            uacCampaign, directCampaignId,
            emptyPatchInternalRequest().copy(
                displayName = newCampaignName
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())
        assertThat((result.result as UacYdbCampaign).name, `is`(newCampaignName))

        getUacCampaign(uacCampaign.id)?.name
            .checkEquals(newCampaignName)
        rmpCampaignService.getMobileContentCampaign(
            clientInfo.clientId!!,
            directCampaignId
        )?.name
            .checkEquals(newCampaignName)
    }

    @Test
    fun updateStartedCampaign_BriefSyncedIsFalse() {
        createUacCampaign(clientInfo, draft = false, briefSynced = true)

        val result = uacCampaignUpdateServiceWeb.updateCampaign(
            uacCampaign, directCampaignId,
            emptyPatchInternalRequest().copy(
                displayName = "Brand new name"
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())

        getUacCampaign(uacCampaign.id)!!.briefSynced
            .checkEquals(false)
    }

    @Test
    fun updateStartedOldCampaign_BriefSyncedIsFalse() {
        createUacCampaign(clientInfo, draft = false, briefSynced = null)

        val result = uacCampaignUpdateServiceWeb.updateCampaign(
            uacCampaign, directCampaignId,
            emptyPatchInternalRequest().copy(
                displayName = "Brand new name"
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())

        getUacCampaign(uacCampaign.id)!!.briefSynced
            .checkEquals(false)
    }

    @Test
    fun updateStartedNotSyncedCampaign_BriefSyncedIsFalse() {
        createUacCampaign(clientInfo, draft = false, briefSynced = false)

        val result = uacCampaignUpdateServiceWeb.updateCampaign(
            uacCampaign, directCampaignId,
            emptyPatchInternalRequest().copy(
                displayName = "Brand new name"
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())

        getUacCampaign(uacCampaign.id)!!.briefSynced
            .checkEquals(false)
    }

    @Test
    fun updateDraftCampaign_BriefSyncedIsTrue() {
        createUacCampaign(clientInfo, draft = true, briefSynced = true)

        val result = uacCampaignUpdateServiceWeb.updateCampaign(
            uacCampaign, directCampaignId,
            emptyPatchInternalRequest().copy(
                displayName = "Brand new name"
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())

        getUacCampaign(uacCampaign.id)!!.briefSynced
            .checkEquals(true)
    }

    @Test
    fun updateDraftOldCampaign_BriefSyncedIsTrue() {
        createUacCampaign(clientInfo, draft = true, briefSynced = null)

        val result = uacCampaignUpdateServiceWeb.updateCampaign(
            uacCampaign, directCampaignId,
            emptyPatchInternalRequest().copy(
                displayName = "Brand new name"
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())

        getUacCampaign(uacCampaign.id)!!.briefSynced
            .checkEquals(true)
    }

    @Test
    fun updateRegionsTest() {
        val newGeo = listOf(213L)

        val result = uacCampaignUpdateServiceWeb.updateCampaign(
            uacCampaign, directCampaignId,
            emptyPatchInternalRequest().copy(
                regions = newGeo
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())
        assertThat((result.result as UacYdbCampaign).regions, `is`(newGeo))

        getUacCampaign(uacCampaign.id)?.regions
            .checkEquals(newGeo)
    }

    @Test
    fun updateEmptyRegionsTest() {
        createUacCampaign(clientInfo = clientInfo, draft = true)
        val result = uacCampaignUpdateServiceWeb.updateCampaign(
            uacCampaign, directCampaignId,
            emptyPatchInternalRequest().copy(
                regions = listOf()
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())
        assertThat((result.result as UacYdbCampaign).regions, `is`(listOf()))

        getUacCampaign(uacCampaign.id)?.regions
            .checkEquals(listOf())
    }

    @Test
    fun updateEmptyTrackingUrlTest() {
        createUacCampaign(clientInfo = clientInfo, draft = true)
        val result = uacCampaignUpdateServiceWeb.updateCampaign(
            uacCampaign, directCampaignId,
            emptyPatchInternalRequest().copy(
                trackingUrl = ""
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())
        assertThat((result.result as UacYdbCampaign).trackingUrl, nullValue())

        getUacCampaign(uacCampaign.id)?.trackingUrl
            .checkEquals(null)
    }

    @Test
    fun updateEmptyImpressionUrlTest() {
        val impressionUrl = "https://app.appsflyer.com/com.im30.ROE.gp?pid=yandexdirect_int&c=" +
            "{campaign_name_lat}&af_c_id={campaign_id}&af_adset_id={gbid}&af_ad_id={ad_id}&af_sub1={phrase_id}" +
            "&af_sub2={retargeting_id}&af_sub3={keyword}&af_sub4={adtarget_name}&af_click_lookback=7d&clickid={logid}" +
            "&google_aid={googleaid}&advertising_id={google_aid}&ya_click_id={logid}&idfa={ios_ifa}"
        createUacCampaign(clientInfo = clientInfo, draft = true, impressionUrl = impressionUrl)
        getUacCampaign(uacCampaign.id)?.impressionUrl
            .checkEquals(impressionUrl)
        val result = uacCampaignUpdateServiceWeb.updateCampaign(
            uacCampaign, directCampaignId,
            emptyPatchInternalRequest().copy(
                impressionUrl = ""
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())
        assertThat((result.result as UacYdbCampaign).impressionUrl, nullValue())

        getUacCampaign(uacCampaign.id)?.impressionUrl
            .checkEquals(null)
    }

    @Test
    fun updateTrackingUrlTest() {
        val newTrackingUrl = "https://control.kochava.com/v1/cpi/click?campaign_id=koyandex-music-" +
            "m3svtsg41de342a2d66d47&ko_exchange=true&site_id=apmetrix&network_id=1517&device_id_type=adid" +
            "&adid={google_aid}&device_id={google_aid}&android_id={android_id}&click_id={logid}"

        val result = uacCampaignUpdateServiceWeb.updateCampaign(
            uacCampaign, directCampaignId,
            emptyPatchInternalRequest().copy(
                trackingUrl = newTrackingUrl
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())
        assertThat((result.result as UacYdbCampaign).trackingUrl, `is`(newTrackingUrl))

        getUacCampaign(uacCampaign.id)?.trackingUrl
            .checkEquals(newTrackingUrl)
    }

    @Test
    fun updateAdultContentEnabledTest() {
        val result = uacCampaignUpdateServiceWeb.updateCampaign(
            uacCampaign, directCampaignId,
            emptyPatchInternalRequest().copy(
                adultContentEnabled = true
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())
        assertThat((result.result as UacYdbCampaign).adultContentEnabled, `is`(true))

        getUacCampaign(uacCampaign.id)?.adultContentEnabled
            .checkEquals(true)
    }

    @Test
    fun updateImpressionUrlTest() {
        val newImpressionUrl = "https://impression.appsflyer.com/ru.yandex.music?af_viewthrough_lookback=1d" +
            "&google_aid={GOOGLE_AID_LC}&android_id={ANDROID_ID_LC}&c=MSCAMP_4__MU_460__MA_And_N_RU_2_goal_MU_For" +
            "_Work___auto___&af_adset_id={GBID}&af_ad_id=10723447474&is_retargeting=true&advertising_id={google_aid}" +
            "&oaid={oaid}&pid=yandexdirect_int&clickid={logid}&af_c_id={campaign_id}" +
            "&af_ip={client_ip}&af_ua={user_agent}&af_lang={device_lang}"


        val result = uacCampaignUpdateServiceWeb.updateCampaign(
            uacCampaign, directCampaignId,
            emptyPatchInternalRequest().copy(
                impressionUrl = newImpressionUrl
            ),
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
        )

        assertThat(result.isSuccessful, `is`(true))
        assertThat(result.validationResult, nullValue())
        assertThat((result.result as UacYdbCampaign).impressionUrl, `is`(newImpressionUrl))

        getUacCampaign(uacCampaign.id)?.impressionUrl
            .checkEquals(newImpressionUrl)
    }
}

fun emptyPatchInternalRequest(): PatchCampaignInternalRequest {
    return PatchCampaignInternalRequest(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
    )
}

fun emptyPatchRequest(): PatchCampaignRequest {
    return PatchCampaignRequest(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
    )
}
