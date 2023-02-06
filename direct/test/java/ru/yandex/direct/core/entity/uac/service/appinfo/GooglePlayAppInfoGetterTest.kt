package ru.yandex.direct.core.entity.uac.service.appinfo

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbRecommendedCostRepository
import ru.yandex.direct.core.entity.uac.service.UacAvatarsService

@RunWith(JUnitParamsRunner::class)
class GooglePlayAppInfoGetterTest {
    private lateinit var appInfoInput: AppInfoInputProcessor

    @Mock
    lateinit var appInfoInputProcessor: AppInfoInputProcessor

    @Mock
    lateinit var uacYdbRecommendedCostRepository: UacYdbRecommendedCostRepository

    @Mock
    lateinit var uacAvatarsService: UacAvatarsService

    @InjectMocks
    private lateinit var googlePlayAppInfoGetter: GooglePlayAppInfoGetter

    @Before
    fun init() {
        MockitoAnnotations.initMocks(this)
        appInfoInput = AppInfoInputProcessor(uacAvatarsService)
    }

    fun provideDescriptions(): Array<Array<Any?>> = arrayOf(
        arrayOf(
            null,
            null,
            null,
        ),
        arrayOf(
            "👍🏿Google play app description.🤔",
            "👍🏿Google play app description.🤔",
            "Google play app description.",
        ),
        arrayOf(
            "Google Play App Description. And some other stuff.",
            "Google Play App Description.",
            "Google Play App Description.",
        ),
        arrayOf(
            "1. Google Play App Description. 2. And some other stuff.",
            "Google Play App Description.",
            "Google Play App Description.",
        ),
        arrayOf(
            "1.Google Play App Description. 2.And some other stuff.",
            "Google Play App Description.",
            "Google Play App Description.",
        ),
        arrayOf(
            "1.\n Google Play App Description? 2.\n And some other stuff.",
            "Google Play App Description?",
            "Google Play App Description?",
        ),
        arrayOf(
            "The part of! 1.\n Google Play App Description. 2.\n And some other stuff.",
            "The part of!",
            "The part of!",
        ),
        arrayOf(
            "",
            "",
            null,
        ),
    )

    @Test
    @Parameters(method = "provideDescriptions")
    fun testExtractSubtitle(description: String?, descriptionAfterFix: String?, expected: String?) {
        `when`(appInfoInputProcessor.fixDescription(description)).thenReturn(appInfoInput.fixDescription(description))
        `when`(appInfoInputProcessor.processText(descriptionAfterFix)).thenReturn(
            appInfoInput.processText(
                descriptionAfterFix
            )
        )
        val actualSubtitle = googlePlayAppInfoGetter.subtitle(description)
        assertThat(actualSubtitle).isEqualTo(expected)
    }

    fun provideAppPageUrl() = arrayOf(
        arrayOf("app.id", "be", "by", "https://play.google.com/store/apps/details?hl=by&gl=be&id=app.id"),
        arrayOf(
            "in.vuhams.messenger",
            "ru",
            "by",
            "https://play.google.com/store/apps/details?hl=by&gl=ru&id=in.vuhams.messenger"
        ),
    )

    @Test
    @Parameters(method = "provideAppPageUrl")
    fun testAppPageUrl(appId: String, region: String, language: String, expected: String) {
        val actualUrl = googlePlayAppInfoGetter.appPageUrl(appId, region, language)
        assertThat(actualUrl).isEqualTo(expected)
    }
}
