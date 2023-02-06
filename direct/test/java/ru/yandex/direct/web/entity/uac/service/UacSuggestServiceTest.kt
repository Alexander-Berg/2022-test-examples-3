package ru.yandex.direct.web.entity.uac.service

import com.nhaarman.mockitokotlin2.doReturn
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.util.ReflectionTestUtils
import ru.yandex.altay.model.language.LanguageOuterClass
import ru.yandex.direct.appmetrika.AppMetrikaClient
import ru.yandex.direct.appmetrika.model.response.Application
import ru.yandex.direct.appmetrika.model.response.BundleId
import ru.yandex.direct.core.entity.campaign.service.CampMetrikaCountersService
import ru.yandex.direct.core.entity.metrikacounter.model.MetrikaCounterWithAdditionalInformation
import ru.yandex.direct.core.entity.organizations.service.OrganizationService
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.Platform
import ru.yandex.direct.core.entity.uac.model.Store
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAppInfo
import ru.yandex.direct.core.entity.uac.samples.ANDROID_APP_INFO_DATA
import ru.yandex.direct.core.entity.uac.samples.APPGALLERY_APP_INFO_DATA
import ru.yandex.direct.core.entity.uac.samples.IOS_APP_INFO_DATA
import ru.yandex.direct.core.entity.uac.service.UacAppInfoService
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.repository.TestUacYdbAppInfoRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.organizations.swagger.OrganizationApiInfo
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.core.entity.mobilecontent.model.WebMobileApp
import ru.yandex.direct.web.core.entity.mobilecontent.model.WebMobileContent
import ru.yandex.direct.web.core.entity.mobilecontent.model.WebOsType
import ru.yandex.direct.web.core.entity.mobilecontent.service.WebCoreMobileAppService
import ru.yandex.direct.web.entity.uac.createUacSaasResponse
import ru.yandex.direct.web.entity.uac.model.SaasRequest
import ru.yandex.direct.web.entity.uac.model.UacSiteSuggest
import java.util.Locale
import ru.yandex.direct.web.core.entity.mobilecontent.model.Store as MobileContentStore

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacSuggestServiceTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var appMetrikaClient: AppMetrikaClient

    @Autowired
    private lateinit var uacSuggestService: UacSuggestService

    @Autowired
    private lateinit var uacYdbAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    private lateinit var testUacYdbAppInfoRepository: TestUacYdbAppInfoRepository

    @Autowired
    private lateinit var uacAppInfoService: UacAppInfoService

    private lateinit var userInfo: UserInfo
    private lateinit var androidAppInfo: UacYdbAppInfo
    private lateinit var iosAppInfo: UacYdbAppInfo
    private lateinit var appgalleryAppInfo: UacYdbAppInfo
    private lateinit var unmatchedAppInfo: UacYdbAppInfo

    @Before
    fun before() {
        userInfo = steps.userSteps().createDefaultUser()
        androidAppInfo = defaultAppInfo(
            appId = "com.yandex.android",
            bundleId = "com.yandex.android",
            platform = Platform.ANDROID,
            source = Store.GOOGLE_PLAY,
            data = ANDROID_APP_INFO_DATA,
        )
        iosAppInfo = defaultAppInfo(
            appId = "app123",
            bundleId = "com.yandex.ios",
            platform = Platform.IOS,
            source = Store.ITUNES,
            data = IOS_APP_INFO_DATA,
        )
        appgalleryAppInfo = defaultAppInfo(
            appId = "C123",
            bundleId = "com.yandex.appgallery",
            platform = Platform.ANDROID,
            source = Store.APPGALLERY,
            data = APPGALLERY_APP_INFO_DATA,
        )
        unmatchedAppInfo = defaultAppInfo(
            appId = "app123",
            bundleId = "com.yandex.unmatched",
            platform = Platform.IOS,
            source = Store.ITUNES,
            data = IOS_APP_INFO_DATA,
        )
        listOf(androidAppInfo, iosAppInfo, appgalleryAppInfo, unmatchedAppInfo).forEach { uacYdbAppInfoRepository.saveAppInfo(it) }
    }

    @After
    fun after() {
        testUacYdbAppInfoRepository.clean()
    }

    @Test
    fun suggestSaasApps() {
        val uacSaasClient = mock(UacSaasClient::class.java)
        doReturn(listOf(
            createUacSaasResponse(listOf(androidAppInfo.id, androidAppInfo.id)),
            createUacSaasResponse(listOf(iosAppInfo.id)),
        )).`when`(uacSaasClient).suggest(
            listOf(
                SaasRequest(null, "google_play"),
                SaasRequest(null, "itunes"),
            ), 10
        )
        ReflectionTestUtils.setField(uacSuggestService, "uacSaasClient", uacSaasClient)

        // порядок между сторами задается релевантностью, в этом
        // тесте приложение из google play релевантнее
        uacSuggestService.suggestSaasApps(null, 10)
            .checkEquals(
                listOf(androidAppInfo, iosAppInfo).map { uacAppInfoService.getAppInfo(it, false) })
    }

    @Test
    fun suggestSaasAppsWithLimit() {
        val uacSaasClient = mock(UacSaasClient::class.java)
        doReturn(listOf(
            createUacSaasResponse(listOf(androidAppInfo.id)),
            createUacSaasResponse(listOf(iosAppInfo.id, iosAppInfo.id)),
        )).`when`(uacSaasClient).suggest(
            listOf(
                SaasRequest(null, "google_play"),
                SaasRequest(null, "itunes"),
            ), 1
        )
        ReflectionTestUtils.setField(uacSuggestService, "uacSaasClient", uacSaasClient)

        // приложение из itunes имеет большую релевантность,
        // при лимите в 1 приложение будет именно оттуда
        uacSuggestService.suggestSaasApps(null, 1)
            .checkEquals(
                listOf(uacAppInfoService.getAppInfo(iosAppInfo, false)))
    }

    @Test
    fun suggestAppMetrikaSameBundleId() {
        val application = Application().apply {
            bundleIds = listOf(
                BundleId().apply {
                    bundleId = "com.yandex.android"
                    platform = "ios"
                },
            )
        }
        doReturn(
            listOf(
                application,
                application
            )
        ).`when`(appMetrikaClient).getApplications(
            userInfo.uid, null, null, null, 100, null
        )
        // с bundleId, аналогичным androidAppInfo из before
        val sameIosAppInfo = defaultAppInfo(
            appId = "app123", bundleId = "com.yandex.android",
            platform = Platform.IOS, source = Store.ITUNES,
            data = IOS_APP_INFO_DATA
        )
        uacYdbAppInfoRepository.saveAppInfo(sameIosAppInfo)

        // полный дубль должен отсечься
        uacSuggestService.suggestAppMetrikaApps(userInfo.user!!, null)
            .checkEquals(listOf(uacAppInfoService.getAppInfo(sameIosAppInfo)))
    }

    @Test
    fun suggestLibraryApps() {
        val webCoreMobileAppService = mock(WebCoreMobileAppService::class.java)
        val ios = WebMobileApp()
            .withMobileContent(
                WebMobileContent()
                    .withBundleId("com.yandex.ios")
                    .withOsType(WebOsType.IOS)
            )
            .withStore(MobileContentStore.APP_STORE)
        val android = WebMobileApp()
            .withMobileContent(
                WebMobileContent()
                    .withStoreContentId("com.yandex.android")
                    .withOsType(WebOsType.ANDROID)
            )
            .withStore(MobileContentStore.GOOGLE_PLAY)
        doReturn(
            listOf(
                ios,
                android,
                android,
                ios
            )
        ).`when`(webCoreMobileAppService).getAppList(
            ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()
        )
        // мне видится опасным подменять сервис на уровне контекста для всех тестов
        ReflectionTestUtils.setField(uacSuggestService, "webCoreMobileAppService", webCoreMobileAppService)

        // полные дубли должны отсечься
        uacSuggestService.suggestLibraryApps(userInfo.user!!, "par")
            .checkEquals(listOf(uacAppInfoService.getAppInfo(iosAppInfo)))
        uacSuggestService.suggestLibraryApps(userInfo.user!!, "cor")
            .checkEquals(listOf(uacAppInfoService.getAppInfo(androidAppInfo)))
    }

    @Test
    fun suggestWebUrls() {
        LocaleContextHolder.setLocale(Locale.ENGLISH)
        val campMetrikaCountersService = mock(CampMetrikaCountersService::class.java)
        val organizationService = mock(OrganizationService::class.java)
        doReturn(
            setOf(
                MetrikaCounterWithAdditionalInformation()
                    .withDomain("yandex.by")
                    .withName("Yandex by"),
                MetrikaCounterWithAdditionalInformation()
                    .withDomain("google.com")
                    .withName("Google")
            )
        ).`when`(campMetrikaCountersService).getAvailableCountersByClientId(
            ArgumentMatchers.eq(userInfo.clientId),
            ArgumentMatchers.any(CampMetrikaCountersService.CounterWithAdditionalInformationFilter::class.java)
        )
        doReturn(
            listOf(
                OrganizationApiInfo()
                    .withCompanyName("Yandex")
                    .withPreviewHref("https://yandex.ru/icon")
                    .withUrls(mutableListOf("https://yandex.ru")),
                OrganizationApiInfo()
                    .withCompanyName("Google")
                    .withPreviewHref("https://google.com/icon")
                    .withUrls(mutableListOf("https://google.com"))
            )
        ).`when`(organizationService).getApiClientOrganizations(
            userInfo.clientId, userInfo.chiefUid, LanguageOuterClass.Language.EN, null
        )
        ReflectionTestUtils.setField(
            uacSuggestService, "campMetrikaCountersService", campMetrikaCountersService)
        ReflectionTestUtils.setField(
            uacSuggestService, "organizationService", organizationService)
        uacSuggestService.suggestWebUrls(userInfo.user!!, "and")
            .checkEquals(listOf(
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
            ))
    }
}
