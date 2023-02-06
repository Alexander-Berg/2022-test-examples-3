package ru.yandex.direct.core.entity.uac.service.appinfo

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import ru.yandex.direct.core.entity.uac.service.UacAvatarsService

@RunWith(JUnitParamsRunner::class)
class AppInfoInputProcessorTest {
    @Mock
    lateinit var uacAvatarsService: UacAvatarsService

    private lateinit var appInfoInputProcessor: AppInfoInputProcessor

    @Before
    fun init() {
        MockitoAnnotations.initMocks(this)
        appInfoInputProcessor = AppInfoInputProcessor(uacAvatarsService)
    }

    @Suppress("HttpUrlsUsage")
    fun provideAvatarsUrls() = arrayOf(
        arrayOf(
            "https://google.com",
            "https://google.com",
        ),
        arrayOf(
            "http://google.com",
            "http://google.com",
        ),
        arrayOf(
            "http://avatars.mds.yandex.net/get-itunes-screens/1986935/5ee3031fff5a5b644206af6f0f2d0449/orig",
            "https://avatars.mds.yandex.net/get-itunes-screens/1986935/5ee3031fff5a5b644206af6f0f2d0449/orig",
        ),
        arrayOf(
            "https://avatars.mds.yandex.net/get-itunes-screens/1986935/5ee3031fff5a5b644206af6f0f2d0449/orig",
            "https://avatars.mds.yandex.net/get-itunes-screens/1986935/5ee3031fff5a5b644206af6f0f2d0449/orig",
        ),
        arrayOf(
            "https://avatars.mdst.yandex.net/get-itunes-screens/1986935/5ee3031fff5a5b644206af6f0f2d0449/orig",
            "https://avatars.mdst.yandex.net/get-itunes-screens/1986935/5ee3031fff5a5b644206af6f0f2d0449/orig",
        ),
        arrayOf(
            "http://avatars.mdst.yandex.net/get-itunes-screens/1986935/5ee3031fff5a5b644206af6f0f2d0449/orig",
            "https://avatars.mdst.yandex.net/get-itunes-screens/1986935/5ee3031fff5a5b644206af6f0f2d0449/orig",
        ),
    )

    @Test
    @Parameters(method = "provideAvatarsUrls")
    fun testFixAvatarsUrl(iconUrl: String?, expected: String?) {
        val actualUrl = appInfoInputProcessor.fixAvatarsUrl(iconUrl)
        assertThat(actualUrl).isEqualTo(expected)
    }

    fun provideAgeLimits(): Array<Array<Any?>> = arrayOf(
        arrayOf(
            null,
            null,
        ),
        arrayOf(
            "12+",
            12,
        ),
        arrayOf(
            "19",
            19,
        ),
        arrayOf(
            "",
            null,
        ),
        arrayOf(
            "18++",
            null,
        ),
        arrayOf(
            "18plus+",
            null,
        ),
    )

    @Test
    @Parameters(method = "provideAgeLimits")
    fun testFixAgeLimit(ageLimit: String?, expected: Int?) {
        val actualAgeLimit = appInfoInputProcessor.fixAgeLimit(ageLimit)
        assertThat(actualAgeLimit).isEqualTo(expected)
    }

    fun provideTexts() = arrayOf(
        arrayOf("abcdefghdfhskdfhserke,rhkeshsefsdhf", "abcdefghdfhskdfhserke, rhkeshsefsdhf"),
        arrayOf("2×2=4", "22=4"),
        arrayOf("×××××××", ""),

        arrayOf("correct, innit?", "correct, innit?"),
        arrayOf("incorrect ,innit?", "incorrect, innit?"),
        arrayOf("too much space      , lol", "too much space, lol"),
        arrayOf("nospace,atall", "nospace, atall"),

        arrayOf("i－love rmp", "i - love rmp"),
        arrayOf("i love rmp", "i love rmp"),
        arrayOf("", ""),
        arrayOf("i-love rmp", "i-love rmp"),

        arrayOf("i－love  , r×××mp", "i - love, rmp"),
    )

    @Test
    @Parameters(method = "provideTexts")
    fun testProcessText(text: String?, expected: String?) {
        val actualText = appInfoInputProcessor.processText(text)
        assertThat(actualText).isEqualTo(expected)
    }

}
