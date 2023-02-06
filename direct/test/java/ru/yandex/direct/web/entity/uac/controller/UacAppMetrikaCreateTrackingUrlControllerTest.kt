package ru.yandex.direct.web.entity.uac.controller

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.appmetrika.AppMetrikaClient
import ru.yandex.direct.appmetrika.model.response.Application
import ru.yandex.direct.appmetrika.model.response.BundleId
import ru.yandex.direct.appmetrika.model.response.UniversalCampaignInfo
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.Platform
import ru.yandex.direct.core.entity.uac.model.Store
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAppInfo
import ru.yandex.direct.core.entity.uac.samples.ANDROID_APP_INFO_DATA
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.repository.TestUacYdbAppInfoRepository
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacAppMetrikaCreateTrackingUrlControllerTest {

    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var appMetrikaClient: AppMetrikaClient

    @Autowired
    private lateinit var uacAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    private lateinit var testUacYdbAppInfoRepository: TestUacYdbAppInfoRepository

    private lateinit var userInfo: UserInfo
    private lateinit var appInfo: UacYdbAppInfo
    private lateinit var universalCampaignInfo: UniversalCampaignInfo

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )

        appInfo = defaultAppInfo(
            appId = "com.yandex.some", bundleId = "com.yandex.some",
            platform = Platform.ANDROID, source = Store.GOOGLE_PLAY,
            data = ANDROID_APP_INFO_DATA
        )
        uacAppInfoRepository.saveAppInfo(appInfo)

        val appMetrikaApplications: List<Application> = listOf(
            Application().apply {
                bundleIds = listOf(
                    BundleId().apply {
                        bundleId = "com.yandex.some"
                        platform = "android"
                    },
                )
                id = 123
            },
        )
        universalCampaignInfo = UniversalCampaignInfo().apply {
            campaignName = "Yandex.Direct 1180165037273145960"
            trackingId = "1180165037273145960"
            trackingUrl =
                "https://redirect.appmetrica.yandex.com/serve/1180165037273145960?click_id={LOGID}&google_aid={GOOGLEAID}&android_id={ANDROIDID}&android_id_sha1={ANDROID_ID_LC_SH1}&search_term={keyword}&google_aid_sha1={GOOGLE_AID_LC_SH1}&campaign_id={campaign_id}&ios_ifa={IOSIFA}&ios_ifa_sha1={IDFA_LC_SH1}&device_type={device_type}&region_name={region_name}&source_type={source_type}&source={source}&position_type={position_type}&phrase_id={phrase_id}"
        }
        doReturn(appMetrikaApplications).`when`(appMetrikaClient).getApplications(
            userInfo.uid, appInfo.bundleId, ru.yandex.direct.appmetrika.model.Platform.android, null, 1, null
        )
        doReturn(universalCampaignInfo).`when`(appMetrikaClient).getUniversalCampaign(
            any(), any(), any()
        )
    }

    @After
    fun after() {
        testUacYdbAppInfoRepository.clean()
    }

    @Test
    fun testCreateTrackingUrl() {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/appmetrika/tracking_url?app_info_id=${appInfo.id}&ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        Assertions.assertThat(JsonUtils.MAPPER.readTree(result)["result"]).isEqualTo(
            JsonUtils.MAPPER.readTree(
                JsonUtils.toJson(universalCampaignInfo)
            )
        )
    }
}
