package ru.yandex.direct.web.entity.uac.controller

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.anyOrNull
import java.util.Locale
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.test.util.ReflectionTestUtils
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.AppInfo
import ru.yandex.direct.core.entity.uac.model.Platform
import ru.yandex.direct.core.entity.uac.model.Store
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.samples.ANDROID_APP_INFO_DATA
import ru.yandex.direct.core.entity.uac.samples.IOS_APP_INFO_DATA
import ru.yandex.direct.core.entity.uac.service.UacAppInfoService
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.repository.TestUacYdbAppInfoRepository
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.uac.model.Suggest
import ru.yandex.direct.web.entity.uac.model.UacSiteSuggest
import ru.yandex.direct.web.entity.uac.service.UacSuggestService

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacSuggestControllerTest {

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
    private lateinit var uacAppInfoService: UacAppInfoService

    @Autowired
    private lateinit var testUacYdbAppInfoRepository: TestUacYdbAppInfoRepository

    @Autowired
    private lateinit var uacSuggestController: UacSuggestController

    @Autowired
    private lateinit var uacSuggestService: UacSuggestService

    private lateinit var userInfo: UserInfo
    private lateinit var appMetrikaApplications: List<AppInfo>
    private lateinit var expectedAppMetrikaApplications: List<AppInfo>
    private lateinit var libraryApplications: List<AppInfo>
    private lateinit var saasApplications: List<AppInfo>
    private lateinit var webUrls: List<UacSiteSuggest>

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
        // нужно для проверки регионов, так как из ручки возвращается регион в локали en
        LocaleContextHolder.setLocale(Locale.ENGLISH)

        val mockedUacSuggestService = spy(uacSuggestService)

        val appMetrikaApplicationsYdb = listOf(
            defaultAppInfo(
                appId = "com.yandex.some", bundleId = "com.yandex.some",
                platform = Platform.ANDROID, source = Store.GOOGLE_PLAY,
                data = ANDROID_APP_INFO_DATA
            ),
            defaultAppInfo(
                appId = "id123", bundleId = "com.yandex.awesome",
                platform = Platform.IOS, source = Store.ITUNES,
                data = IOS_APP_INFO_DATA
            ),
            defaultAppInfo(
                appId = "id12345", bundleId = "com.yandex.some",
                platform = Platform.IOS, source = Store.ITUNES,
                data = IOS_APP_INFO_DATA
            ),
            defaultAppInfo(
                appId = "com.yandex.awesome", bundleId = "com.yandex.awesome",
                platform = Platform.ANDROID, source = Store.GOOGLE_PLAY,
                data = ANDROID_APP_INFO_DATA
            ),
        )

        val libraryApplicationsYdb = listOf(
            defaultAppInfo(
                appId = "id123", bundleId = "com.yandex.awesome",
                platform = Platform.IOS, source = Store.ITUNES,
                data = IOS_APP_INFO_DATA
            ),
            defaultAppInfo(
                appId = "com.yandex.some", bundleId = "com.yandex.some",
                platform = Platform.ANDROID, source = Store.GOOGLE_PLAY,
                data = ANDROID_APP_INFO_DATA
            ),
        )

        val saasApplicationsYdb = listOf(
            defaultAppInfo(
                appId = "com.yandex.some", bundleId = "com.yandex.some",
                platform = Platform.ANDROID, source = Store.GOOGLE_PLAY,
                data = ANDROID_APP_INFO_DATA
            ),
            defaultAppInfo(
                appId = "id123", bundleId = "com.yandex.some",
                platform = Platform.IOS, source = Store.ITUNES,
                data = IOS_APP_INFO_DATA
            ),
        )

        webUrls = listOf(
            UacSiteSuggest(
                title = "Yandex by",
                url = "https://yandex.by",
                icon = "https://favicon.yandex.net/favicon/v2/https://yandex.by/?size=32&stub=1",
            ),
            UacSiteSuggest(
                title = "Yandex",
                url = "https://yandex.ru",
                icon = "https://yandex.ru/icon",
            ),
        )

        appMetrikaApplications = appMetrikaApplicationsYdb.map { uacAppInfoService.getAppInfo(it) }
        libraryApplications = libraryApplicationsYdb.map { uacAppInfoService.getAppInfo(it) }
        saasApplications = saasApplicationsYdb.map { uacAppInfoService.getAppInfo(it) }

        // если в ответе по метрике, и в ответе по библиотеке,
        // у нас возникают одинаковые bundleId + storeId
        // то из ответа по метрике мы выкидываем дубли
        expectedAppMetrikaApplications = listOf(
            appMetrikaApplicationsYdb[2],
            appMetrikaApplicationsYdb[3],
        ).map { uacAppInfoService.getAppInfo(it) }

        listOf(appMetrikaApplicationsYdb, libraryApplicationsYdb, saasApplicationsYdb)
            .flatten()
            .forEach { uacAppInfoRepository.saveAppInfo(it) }

        doReturn(appMetrikaApplications).`when`(mockedUacSuggestService).suggestAppMetrikaApps(
            anyOrNull(), anyOrNull(),
        )
        doReturn(libraryApplications).`when`(mockedUacSuggestService).suggestLibraryApps(
            anyOrNull(), anyOrNull(),
        )
        doReturn(saasApplications).`when`(mockedUacSuggestService).suggestSaasApps(
            anyOrNull(), ArgumentMatchers.anyInt()
        )
        doReturn(webUrls).`when`(mockedUacSuggestService).suggestWebUrls(
            anyOrNull(), anyOrNull(),
        )
        ReflectionTestUtils.setField(
            uacSuggestController, "uacSuggestService", mockedUacSuggestService)
    }

    @After
    fun after() {
        testUacYdbAppInfoRepository.clean()
    }

    @Test
    fun getSuggestText() {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/suggest?ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString
        val expected = Suggest(
            appMetrika = expectedAppMetrikaApplications,
            library = libraryApplications,
            saas = saasApplications,
            webUrls = webUrls,
        )
        Assertions.assertThat(JsonUtils.MAPPER.readTree(result)["result"]).isEqualTo(
            JsonUtils.MAPPER.readTree(
                JsonUtils.toJson(expected)
            )
        )
    }

    @Test
    fun getSuggestWithLimit() {
        val limit = 1
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/suggest?ulogin=" + userInfo.clientInfo!!.login + "&limit=$limit")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString
        val expected = Suggest(
            appMetrika = expectedAppMetrikaApplications.take(limit),
            library = libraryApplications.take(limit),
            saas = saasApplications.take(limit),
            webUrls = webUrls.take(limit),
        )
        Assertions.assertThat(JsonUtils.MAPPER.readTree(result)["result"]).isEqualTo(
            JsonUtils.MAPPER.readTree(
                JsonUtils.toJson(expected)
            )
        )
    }

    private fun getSeparateSuggest(path: String, expected: Any) {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get(path)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString
        Assertions.assertThat(JsonUtils.MAPPER.readTree(result)["result"]).isEqualTo(
            JsonUtils.MAPPER.readTree(
                JsonUtils.toJson(expected)
            )
        )
    }

    @Test
    fun getLibrarySuggests() {
        getSeparateSuggest(
            "/uac/suggest/library?ulogin="  + userInfo.clientInfo!!.login,
            libraryApplications
        )
    }

    @Test
    fun getSaasSuggests() {
        getSeparateSuggest(
            "/uac/suggest/saas?ulogin="  + userInfo.clientInfo!!.login,
            saasApplications
        )
    }

    @Test
    fun getWebUrlsSuggests() {
        getSeparateSuggest(
            "/uac/suggest/urls?ulogin="  + userInfo.clientInfo!!.login,
            webUrls
        )
    }
}
