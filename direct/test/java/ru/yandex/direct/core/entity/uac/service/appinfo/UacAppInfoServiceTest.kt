package ru.yandex.direct.core.entity.uac.service.appinfo

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.AppInfo
import ru.yandex.direct.core.entity.uac.model.Platform
import ru.yandex.direct.core.entity.uac.model.RecommendedCostType
import ru.yandex.direct.core.entity.uac.model.Store
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbRecommendedCostRepository
import ru.yandex.direct.core.entity.uac.samples.ANDROID_APP_INFO_DATA
import ru.yandex.direct.core.entity.uac.samples.ANDROID_APP_INFO_WITHOUT_CATEGORIES_DATA
import ru.yandex.direct.core.entity.uac.samples.IOS_APP_INFO_DATA
import ru.yandex.direct.core.entity.uac.samples.IOS_APP_INFO_DATA_2
import ru.yandex.direct.core.entity.uac.samples.IOS_APP_INFO_DATA_3
import ru.yandex.direct.core.entity.uac.service.UacAppInfoService
import ru.yandex.direct.core.entity.uac.service.UacAvatarsService

class UacAppInfoServiceTest {
    @Mock
    lateinit var appInfoInputProcessor: AppInfoInputProcessor

    @Mock
    lateinit var uacYdbRecommendedCostRepository: UacYdbRecommendedCostRepository

    @Mock
    lateinit var uacAvatarsService: UacAvatarsService

    @InjectMocks
    private lateinit var iTunesAppInfoGetter: ITunesAppInfoGetter

    @InjectMocks
    private lateinit var googlePlayAppInfoGetter: GooglePlayAppInfoGetter

    private lateinit var uacAppInfoService: UacAppInfoService
    private lateinit var appInfoInput: AppInfoInputProcessor

    @Before
    fun init() {
        MockitoAnnotations.initMocks(this)
        val parsers = listOf(iTunesAppInfoGetter, googlePlayAppInfoGetter)
        uacAppInfoService = UacAppInfoService(parsers)
        appInfoInput = AppInfoInputProcessor(uacAvatarsService)
    }

    @Test
    fun testGetAppInfoAndroid() {
        val uacYdbAppInfo = defaultAppInfo(
            appId = "com.stark.cornerstonround",
            bundleId = "com.stark.cornerstonround",
            platform = Platform.ANDROID,
            source = Store.GOOGLE_PLAY,
            data = ANDROID_APP_INFO_DATA,
            language = "kk",
            region = "kz",
        )
        val icon =
            "http://avatars.mds.yandex.net/get-google-play-app-icon/1961900/76ad1ebdfb38bcc079af8962193477c1/orig"
        val title = "Cornerstone Round Icon Pack"
        val subTitle = "<b>Attention:</b> You require"
        val fixedSubTitle = appInfoInput.fixDescription(subTitle)
        val adult = "3+"
        `when`(appInfoInputProcessor.processText(title)).thenReturn(appInfoInput.processText(title))
        `when`(appInfoInputProcessor.fixDescription(subTitle)).thenReturn(fixedSubTitle)
        `when`(appInfoInputProcessor.processText(fixedSubTitle)).thenReturn(appInfoInput.processText(fixedSubTitle))
        `when`(appInfoInputProcessor.fixAvatarsUrl(icon)).thenReturn(appInfoInput.fixAvatarsUrl(icon))
        `when`(appInfoInputProcessor.fixAgeLimit(adult)).thenReturn(appInfoInput.fixAgeLimit(adult))
        `when`(appInfoInputProcessor.fixIconUrl(icon)).thenReturn(appInfoInput.fixAvatarsUrl(icon))
        val appInfo = uacAppInfoService.getAppInfo(uacYdbAppInfo)

        assertThat(appInfo).isEqualTo(
            AppInfo(
                id = uacYdbAppInfo.id,
                appId = "com.stark.cornerstonround",
                bundleId = "com.stark.cornerstonround",
                title = "Cornerstone Round Icon Pack",
                subtitle = "Attention: You require",
                description = "Attention: You require custom launcher like Nova",
                iconUrl = "https://avatars.mds.yandex.net/get-google-play-app-icon/1961900/76ad1ebdfb38bcc079af8962193477c1/orig",
                language = "kk",
                platform = Platform.ANDROID,
                region = "kz",
                vendor = "Stark Designs",
                rating = 4.8,
                reviews = 21,
                ageLimit = 3,
                minOsVersion = null,
                url = "https://play.google.com/store/apps/details?hl=kk&gl=kz&id=com.stark.cornerstonround",
                currency = "KZT",
                price = 300.0,
                interests = setOf(21672505, 21672500),
                recommendedCpi = null,
                recommendedCpa = null,
            )
        )
    }

    @Test
    fun testGetAppInfoAndroidWithoutCategories() {
        val uacYdbAppInfo = defaultAppInfo(
            appId = "com.stark.cornerstonround",
            bundleId = "com.stark.cornerstonround",
            platform = Platform.ANDROID,
            source = Store.GOOGLE_PLAY,
            data = ANDROID_APP_INFO_WITHOUT_CATEGORIES_DATA,
            language = "kk",
            region = "kz",
        )
        val icon =
            "http://avatars.mds.yandex.net/get-google-play-app-icon/1961900/76ad1ebdfb38bcc079af8962193477c1/orig"
        val title = "Cornerstone Round Icon Pack"
        val subTitle = "<b>Attention:</b> You require"
        val fixedSubTitle = appInfoInput.fixDescription(subTitle)
        val adult = "3+"
        `when`(appInfoInputProcessor.processText(title)).thenReturn(appInfoInput.processText(title))
        `when`(appInfoInputProcessor.fixDescription(subTitle)).thenReturn(fixedSubTitle)
        `when`(appInfoInputProcessor.processText(fixedSubTitle)).thenReturn(appInfoInput.processText(fixedSubTitle))
        `when`(appInfoInputProcessor.fixAvatarsUrl(icon)).thenReturn(appInfoInput.fixAvatarsUrl(icon))
        `when`(appInfoInputProcessor.fixAgeLimit(adult)).thenReturn(appInfoInput.fixAgeLimit(adult))
        `when`(appInfoInputProcessor.fixIconUrl(icon)).thenReturn(appInfoInput.fixAvatarsUrl(icon))
        val appInfo = uacAppInfoService.getAppInfo(uacYdbAppInfo)

        assertThat(appInfo).isEqualTo(
            AppInfo(
                id = uacYdbAppInfo.id,
                appId = "com.stark.cornerstonround",
                bundleId = "com.stark.cornerstonround",
                title = "Cornerstone Round Icon Pack",
                subtitle = "Attention: You require",
                description = "Attention: You require custom launcher like Nova",
                iconUrl = "https://avatars.mds.yandex.net/get-google-play-app-icon/1961900/76ad1ebdfb38bcc079af8962193477c1/orig",
                language = "kk",
                platform = Platform.ANDROID,
                region = "kz",
                vendor = "Stark Designs",
                rating = 4.8,
                reviews = 21,
                ageLimit = 3,
                minOsVersion = null,
                url = "https://play.google.com/store/apps/details?hl=kk&gl=kz&id=com.stark.cornerstonround",
                currency = "KZT",
                price = 300.0,
                interests = setOf(),
                recommendedCpi = null,
                recommendedCpa = null,
            )
        )
    }

    @Test
    fun testGetAppInfoIos() {
        val uacYdbAppInfo = defaultAppInfo(
            id = "3066243178174468096",
            appId = "id1164853370",
            bundleId = "com.parkingtelecom.parkingtelecom",
            language = "ru",
            region = "tr",
            platform = Platform.IOS,
            source = Store.ITUNES,
            data = IOS_APP_INFO_DATA,
        )
        val icon = "http://avatars.mds.yandex.net/get-itunes-icon/40548/5fa2e7f717c4874c3cb61187a2ca04df/orig"
        val title = "Parking Telecom"
        val adult = "4+"
        `when`(appInfoInputProcessor.processText(title)).thenReturn(appInfoInput.processText(title))
        `when`(appInfoInputProcessor.fixAvatarsUrl(icon)).thenReturn(appInfoInput.fixAvatarsUrl(icon))
        `when`(appInfoInputProcessor.fixAgeLimit(adult)).thenReturn(appInfoInput.fixAgeLimit(adult))
        `when`(appInfoInputProcessor.fixIconUrl(icon)).thenReturn(appInfoInput.fixAvatarsUrl(icon))
        val appInfo = uacAppInfoService.getAppInfo(uacYdbAppInfo)
        assertThat(appInfo).isEqualTo(
            AppInfo(
                id = uacYdbAppInfo.id,
                appId = uacYdbAppInfo.appId,
                bundleId = uacYdbAppInfo.bundleId,
                title = "Parking Telecom",
                subtitle = null,
                description = "Park mobile",
                iconUrl = "https://avatars.mds.yandex.net/get-itunes-icon/40548/5fa2e7f717c4874c3cb61187a2ca04df/orig",
                language = "ru",
                platform = Platform.IOS,
                region = "tr",
                vendor = "IngeniQue Corp.",
                rating = 0.0,
                reviews = 0,
                ageLimit = 4,
                minOsVersion = "9.0",
                url = "https://apps.apple.com/tr/app/id1164853370?l=ru",
                currency = "USD",
                price = 0.0,
                interests = setOf(21672180, 21672165),
                recommendedCpi = null,
                recommendedCpa = null,
            )
        )
    }

    @Test
    fun testGetAppInfoIos2() {
        val uacYdbAppInfo = defaultAppInfo(
            id = "8209186795570",
            appId = "id1174489793",
            bundleId = "com.pixeljam.snowballMobile",
            language = "ru",
            region = "us",
            platform = Platform.IOS,
            source = Store.ITUNES,
            data = IOS_APP_INFO_DATA_2,
        )
        val icon = "https://avatars.mds.yandex.net/get-itunes-icon/24055/346b40331f0f8b963e18dade58d899a2/orig"
        val title = "Snowball!! - GameClub"
        val adult = "4+"
        `when`(appInfoInputProcessor.processText(title)).thenReturn(appInfoInput.processText(title))
        `when`(appInfoInputProcessor.fixAvatarsUrl(icon)).thenReturn(appInfoInput.fixAvatarsUrl(icon))
        `when`(appInfoInputProcessor.fixAgeLimit(adult)).thenReturn(appInfoInput.fixAgeLimit(adult))
        `when`(appInfoInputProcessor.fixIconUrl(icon)).thenReturn(appInfoInput.fixAvatarsUrl(icon))
        val appInfo = uacAppInfoService.getAppInfo(uacYdbAppInfo)
        assertThat(appInfo).isEqualTo(
            AppInfo(
                id = uacYdbAppInfo.id,
                appId = uacYdbAppInfo.appId,
                bundleId = uacYdbAppInfo.bundleId,
                title = "Snowball!! - GameClub",
                subtitle = null,
                description = "An avalanche",
                iconUrl = "https://avatars.mds.yandex.net/get-itunes-icon/24055/346b40331f0f8b963e18dade58d899a2/orig",
                language = "ru",
                platform = Platform.IOS,
                region = "us",
                vendor = "GameClub",
                rating = 4.57692,
                reviews = 26,
                ageLimit = 4,
                minOsVersion = "10.0",
                url = "https://apps.apple.com/us/app/id1174489793?l=ru",
                currency = "USD",
                price = 0.0,
                interests = setOf(),
                recommendedCpi = null,
                recommendedCpa = null,
            )
        )
    }

    @Test
    fun testGetAppInfoWithDuplicatedInterests() {
        val uacYdbAppInfo = defaultAppInfo(
            id = "8209186795570",
            appId = "id1174489793",
            bundleId = "com.pixeljam.snowballMobile",
            language = "ru",
            region = "us",
            platform = Platform.IOS,
            source = Store.ITUNES,
            data = IOS_APP_INFO_DATA_3,
        )
        val icon = "https://avatars.mds.yandex.net/get-itunes-icon/24055/346b40331f0f8b963e18dade58d899a2/orig"
        val title = "Snowball!! - GameClub"
        val adult = "4+"
        `when`(appInfoInputProcessor.processText(title)).thenReturn(appInfoInput.processText(title))
        `when`(appInfoInputProcessor.fixAvatarsUrl(icon)).thenReturn(appInfoInput.fixAvatarsUrl(icon))
        `when`(appInfoInputProcessor.fixAgeLimit(adult)).thenReturn(appInfoInput.fixAgeLimit(adult))
        `when`(appInfoInputProcessor.fixIconUrl(icon)).thenReturn(appInfoInput.fixAvatarsUrl(icon))
        val appInfo = uacAppInfoService.getAppInfo(uacYdbAppInfo)
        assertThat(appInfo).isEqualTo(
            AppInfo(
                id = uacYdbAppInfo.id,
                appId = uacYdbAppInfo.appId,
                bundleId = uacYdbAppInfo.bundleId,
                title = "Snowball!! - GameClub",
                subtitle = null,
                description = "An avalanche",
                iconUrl = "https://avatars.mds.yandex.net/get-itunes-icon/24055/346b40331f0f8b963e18dade58d899a2/orig",
                language = "ru",
                platform = Platform.IOS,
                region = "us",
                vendor = "GameClub",
                rating = 4.57692,
                reviews = 26,
                ageLimit = 4,
                minOsVersion = "10.0",
                url = "https://apps.apple.com/us/app/id1174489793?l=ru",
                currency = "USD",
                price = 0.0,
                interests = setOf(21672490, 21672495),
                recommendedCpi = null,
                recommendedCpa = null,
            )
        )
    }

    @Test
    fun testAppInfoRecommendationsAndroid() {
        val uacYdbAppInfo = defaultAppInfo(
            appId = "com.stark.cornerstonround",
            bundleId = "com.stark.cornerstonround",
            platform = Platform.ANDROID,
            source = Store.GOOGLE_PLAY,
            data = ANDROID_APP_INFO_DATA,
            language = "kk",
            region = "kz",
        )
        val recommendations = mapOf(
            RecommendedCostType.CPA to 100.toLong(),
            RecommendedCostType.CPI to 120.toLong(),
        )
        `when`(
            uacYdbRecommendedCostRepository.getRecommendedCostByCategoryAndPlatform(
                "application", Platform.ANDROID
            )
        ).thenReturn(recommendations)

        val appInfo = uacAppInfoService.getAppInfo(uacYdbAppInfo)
        assertThat(appInfo.recommendedCpa).isEqualTo(100)
        assertThat(appInfo.recommendedCpi).isEqualTo(120)

        val appInfoWithoutRecommendations = uacAppInfoService.getAppInfo(uacYdbAppInfo, addRecommendations = false)
        assertThat(appInfoWithoutRecommendations.recommendedCpa).isNull()
        assertThat(appInfoWithoutRecommendations.recommendedCpi).isNull()
    }

    @Test
    fun testAppInfoRecommendationsIos() {
        val uacYdbAppInfo = defaultAppInfo(
            id = "3066243178174468096",
            appId = "id1164853370",
            bundleId = "com.parkingtelecom.parkingtelecom",
            language = "ru",
            region = "tr",
            platform = Platform.IOS,
            source = Store.ITUNES,
            data = IOS_APP_INFO_DATA,
        )
        val recommendations = mapOf(
            RecommendedCostType.CPA to 100.toLong(),
            RecommendedCostType.CPI to 120.toLong(),
        )
        `when`(
            uacYdbRecommendedCostRepository.getRecommendedCostByCategoryAndPlatform(
                "", Platform.IOS
            )
        ).thenReturn(recommendations)

        val appInfo = uacAppInfoService.getAppInfo(uacYdbAppInfo)
        assertThat(appInfo.recommendedCpa).isEqualTo(100)
        assertThat(appInfo.recommendedCpi).isEqualTo(120)

        val appInfoWithoutRecommendations = uacAppInfoService.getAppInfo(uacYdbAppInfo, addRecommendations = false)
        assertThat(appInfoWithoutRecommendations.recommendedCpa).isNull()
        assertThat(appInfoWithoutRecommendations.recommendedCpi).isNull()
    }
}
