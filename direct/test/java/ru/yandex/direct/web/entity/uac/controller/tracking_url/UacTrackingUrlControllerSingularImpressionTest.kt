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
class UacTrackingUrlControllerSingularImpressionTest {
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
            "system" to "SINGULAR",
            "parameters" to SINGULAR_PARAMS,
            "has_impression" to false,
            "tracker_id" to null
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
                URL_STR to "https://i.sng.link/Dstn5/dxeu?cp=NA&cl={TRACKID}&idfa={IDFA_UC}&aifa={GOOGLE_AID_UC}&oaid={OAID_UC}",
                VALIDATION_TYPE_STR to IMPRESSION_URL_STR
            ),
            baseExpected + mapOf(
                "url" to "https://i.sng.link/Dstn5/dxeu?cp=NA&idfa={IDFA_UC}&aifa={GOOGLE_AID_UC}&oaid={OAID_UC}&cl={logid}",
            )
        )
    }

    @Test
    fun testWithAndroidAppId() {
        checkCorrectRequest(
            mockMvc,
            mapOf(
                URL_STR to "https://i.sng.link/Dstn5/dxeu?cp=NA&cl={TRACKID}&idfa={IDFA_UC}&aifa={GOOGLE_AID_UC}&oaid={OAID_UC}",
                VALIDATION_TYPE_STR to IMPRESSION_URL_STR,
                APP_ID_STR to "com.yandex.some"
            ),
            baseExpected + mapOf(
                "url" to "https://i.sng.link/Dstn5/dxeu?cp=NA&idfa={IDFA_UC}&aifa={google_aid}&oaid={oaid}&cl={logid}"
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
                URL_STR to "https://i.sng.link/Dstn5/dxeu?cp=NA&cl={TRACKID}&idfa={IDFA_UC}&aifa={GOOGLE_AID_UC}&oaid={OAID_UC}",
                VALIDATION_TYPE_STR to IMPRESSION_URL_STR,
                APP_INFO_ID_STR to appInfo.id
            ),
            baseExpected + mapOf(
                "url" to "https://i.sng.link/Dstn5/dxeu?cp=NA&idfa={IDFA_UC}&aifa={google_aid}&oaid={oaid}&cl={logid}"
            )
        )
        testUacYdbAppInfoRepository.clean()
    }

    @Test
    fun testWithIosAppId() {
        checkCorrectRequest(
            mockMvc,
            mapOf(
                URL_STR to "https://i.sng.link/Dstn5/dxeu?cp=NA&cl={TRACKID}&idfa={IDFA_UC}&aifa={GOOGLE_AID_UC}&oaid={OAID_UC}",
                VALIDATION_TYPE_STR to IMPRESSION_URL_STR,
                APP_ID_STR to "id123"
            ),
            baseExpected + mapOf(
                "url" to "https://i.sng.link/Dstn5/dxeu?cp=NA&aifa={GOOGLE_AID_UC}&oaid={OAID_UC}&idfa={ios_ifa}&cl={logid}"
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
                URL_STR to "https://i.sng.link/Dstn5/dxeu?cp=NA&cl={TRACKID}&idfa={IDFA_UC}&aifa={GOOGLE_AID_UC}&oaid={OAID_UC}",
                VALIDATION_TYPE_STR to IMPRESSION_URL_STR,
                APP_INFO_ID_STR to appInfo.id
            ),
            baseExpected + mapOf(
                "url" to "https://i.sng.link/Dstn5/dxeu?cp=NA&aifa={GOOGLE_AID_UC}&oaid={OAID_UC}&idfa={ios_ifa}&cl={logid}"
            )
        )
        testUacYdbAppInfoRepository.clean()
    }
}
