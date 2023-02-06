package ru.yandex.direct.core.entity.uac.service.appinfo

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbRecommendedCostRepository
import ru.yandex.direct.core.entity.uac.service.UacAvatarsService

@RunWith(JUnitParamsRunner::class)
class ITunesAppInfoGetterTest {
    private lateinit var appInfoInput: AppInfoInputProcessor

    @Mock
    lateinit var appInfoInputProcessor: AppInfoInputProcessor

    @Mock
    lateinit var uacYdbRecommendedCostRepository: UacYdbRecommendedCostRepository

    @Mock
    lateinit var uacAvatarsService: UacAvatarsService

    @InjectMocks
    private lateinit var iTunesAppInfoGetter: ITunesAppInfoGetter

    @Before
    fun init() {
        MockitoAnnotations.initMocks(this)
        appInfoInput = AppInfoInputProcessor(uacAvatarsService)
    }

    fun provideITunesIcons() = arrayOf(
        arrayOf(
            mapOf(
                "artworkUrl83" to "icon_url1",
            ),
            "icon_url1",
        ),
        arrayOf(
            mapOf(
                "artworkUrl87" to "icon_url1",
                "artworkUrl88" to "icon_url2",
                "artworkUrl89" to "icon_url3",
            ),
            "icon_url2",
        ),
        arrayOf(
            mapOf(
                "artworkUrl10" to "icon_url1",
                "artworkUrl129" to "icon_url2",
                "artworkUrl80" to "icon_url3",
            ),
            "icon_url3",
        ),
    )

    @Test
    @Parameters(method = "provideITunesIcons")
    fun testExtractIcon(data: Map<String, Any>, expected: String) {
        val actualUrl = iTunesAppInfoGetter.extractIcon(data)
        assertThat(actualUrl).isEqualTo(expected)
    }

    fun provideAppPageUrl() = arrayOf(
        arrayOf("id1222623347", "be", "by", "https://apps.apple.com/be/app/id1222623347?l=by"),
        arrayOf("id1164853370", "ru", "by", "https://apps.apple.com/ru/app/id1164853370?l=by"),
    )

    @Test
    @Parameters(method = "provideAppPageUrl")
    fun testAppPageUrl(appId: String, region: String, language: String, expected: String) {
        val actualUrl = iTunesAppInfoGetter.appPageUrl(appId, region, language)
        assertThat(actualUrl).isEqualTo(expected)
    }
}
