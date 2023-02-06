package ru.yandex.direct.core.entity.hrefparams.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.direct.core.entity.hrefparams.service.HrefWithParamsBuildingService.applyTrackingParams
import ru.yandex.direct.utils.model.UrlParts

class HrefWithParamsBuildingServiceApplyTrackingParamsTest {
    companion object {
        @JvmStatic
        fun params() = arrayOf(
            arrayOf<Any?>(
                "Href with no params does not change",
                TestParams(
                    href = "https://yandex.ru/",
                    params = null,
                    expected = "https://yandex.ru/"
                )
            ),
            arrayOf<Any?>(
                "Href with params on it does not change",
                TestParams(
                    href = "https://yandex.ru/?some_param=val",
                    params = null,
                    expected = "https://yandex.ru/?some_param=val"
                )
            ),
            arrayOf<Any?>(
                "Params are added",
                TestParams(
                    href = "https://yandex.ru/",
                    params = "utm_mark=some_value&utm_mark=some_value2",
                    expected = "https://yandex.ru/?utm_mark=some_value&utm_mark=some_value2"
                )
            ),
            arrayOf<Any?>(
                "Params are united with params on href",
                TestParams(
                    href = "https://yandex.ru/?utm_mark2=some_value2",
                    params = "utm_mark=some_value",
                    expected = "https://yandex.ru/?utm_mark2=some_value2&utm_mark=some_value"
                )
            ),
            arrayOf<Any?>(
                "Same key values of params overwrite those on href, but different keys remain",
                TestParams(
                    href = "https://yandex.ru/?utm_key=old_val&utm_camp={campaign_name}",
                    params = "utm_key=new_val&other_key=val",
                    expected = "https://yandex.ru/?utm_camp={campaign_name}&utm_key=new_val&other_key=val"
                )
            ),
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("params")
    internal fun `test applyTrackingParams`(description: String, testParams: TestParams) {
        val (href, params, expected) = testParams

        val actual = applyTrackingParams(UrlParts.fromUrl(href), UrlParts.parseParameters(params))
        assertThat(actual.toUrl())
            .isEqualTo(expected)
    }
}

data class TestParams(
    val href: String,
    val params: String?,
    val expected: String,
)
