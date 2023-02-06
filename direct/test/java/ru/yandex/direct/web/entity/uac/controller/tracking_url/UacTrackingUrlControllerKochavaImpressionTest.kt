package ru.yandex.direct.web.entity.uac.controller.tracking_url

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.Platform
import ru.yandex.direct.core.entity.uac.model.Store
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.samples.ANDROID_APP_INFO_DATA
import ru.yandex.direct.core.entity.uac.samples.IOS_APP_INFO_DATA
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.repository.TestUacYdbAppInfoRepository
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacTrackingUrlControllerKochavaImpressionTest {
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var uacAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    private lateinit var testUacYdbAppInfoRepository: TestUacYdbAppInfoRepository

    private lateinit var userInfo: UserInfo

    companion object {

        private val baseExpected = mapOf(
            "skad_network_integrated" to false,
            "system" to "KOCHAVA",
            "parameters" to KOCHAVA_IMPRESSION_PARAMS,
            "has_impression" to false,
            "tracker_id" to "\${tracker_id}"
        )
    }

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
    }

    @Test
    fun testWithoutApp() {
        checkCorrectRequest(
            mockMvc,
            mapOf(
                URL_STR to "https://imp.control.kochava.com/track/impression?event=view&campaign_id=${'$'}{tracker_id}&impression_id={TRACKID}",
                VALIDATION_TYPE_STR to IMPRESSION_URL_STR
            ),
            baseExpected + mapOf(
                "url" to "https://imp.control.kochava.com/track/impression?event=view&campaign_id=${'$'}{tracker_id}&impression_id={logid}",
            )
        )
    }

    @Test
    fun testWithAndroidAppId() {
        checkCorrectRequest(
            mockMvc,
            mapOf(
                URL_STR to "https://imp.control.kochava.com/track/impression?event=view&campaign_id=${'$'}{tracker_id}&impression_id={TRACKID}&adid={GOOGLE_AID_LC}",
                VALIDATION_TYPE_STR to IMPRESSION_URL_STR,
                APP_ID_STR to "com.yandex.some"
            ),
            baseExpected + mapOf(
                "url" to "https://imp.control.kochava.com/track/impression?event=view&campaign_id=${'$'}{tracker_id}&network_id=1517&device_id_type=adid&adid={google_aid}&device_id={google_aid}&android_id={android_id}&impression_id={logid}"
            )
        )
    }

    @Test
    fun testWithAndroidAppInfoId() {
        val appInfo = defaultAppInfo(
            appId = "com.yandex.any", bundleId = "com.yandex.any",
            platform = Platform.ANDROID, source = Store.GOOGLE_PLAY,
            data = ANDROID_APP_INFO_DATA
        )
        uacAppInfoRepository.saveAppInfo(appInfo)
        checkCorrectRequest(
            mockMvc,
            mapOf(
                URL_STR to "https://imp.control.kochava.com/track/impression?event=view&campaign_id=${'$'}{tracker_id}&impression_id={TRACKID}&adid={GOOGLE_AID_LC}",
                VALIDATION_TYPE_STR to IMPRESSION_URL_STR,
                APP_INFO_ID_STR to appInfo.id
            ),
            baseExpected + mapOf(
                "url" to "https://imp.control.kochava.com/track/impression?event=view&campaign_id=${'$'}{tracker_id}&network_id=1517&device_id_type=adid&adid={google_aid}&device_id={google_aid}&android_id={android_id}&impression_id={logid}"
            )
        )
        testUacYdbAppInfoRepository.clean()
    }

    @Test
    fun testWithIosAppId() {
        checkCorrectRequest(
            mockMvc,
            mapOf(
                URL_STR to "https://imp.control.kochava.com/track/impression?event=view&campaign_id=${'$'}{tracker_id}&impression_id={TRACKID}&idfa={IDFA_UC}",
                VALIDATION_TYPE_STR to IMPRESSION_URL_STR,
                APP_ID_STR to "id123"
            ),
            baseExpected + mapOf(
                "url" to "https://imp.control.kochava.com/track/impression?event=view&campaign_id=${'$'}{tracker_id}&network_id=1516&device_id_type=idfa&idfa={ios_ifa}&device_id={ios_ifa}&ios_idfa={ios_ifa}&impression_id={logid}"
            )
        )
    }

    @Test
    fun testWithIosAppInfoId() {
        val appInfo = defaultAppInfo(
            appId = "id1234", bundleId = "com.yandex.yetanother",
            platform = Platform.IOS, source = Store.ITUNES,
            data = IOS_APP_INFO_DATA
        )
        uacAppInfoRepository.saveAppInfo(appInfo)
        checkCorrectRequest(
            mockMvc,
            mapOf(
                URL_STR to "https://imp.control.kochava.com/track/impression?event=view&campaign_id=${'$'}{tracker_id}&impression_id={TRACKID}&idfa={IDFA_UC}",
                VALIDATION_TYPE_STR to IMPRESSION_URL_STR,
                APP_INFO_ID_STR to appInfo.id
            ),
            baseExpected + mapOf(
                "url" to "https://imp.control.kochava.com/track/impression?event=view&campaign_id=${'$'}{tracker_id}&network_id=1516&device_id_type=idfa&idfa={ios_ifa}&device_id={ios_ifa}&ios_idfa={ios_ifa}&impression_id={logid}"
            )
        )
        testUacYdbAppInfoRepository.clean()
    }
}
