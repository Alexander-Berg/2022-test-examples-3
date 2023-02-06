package ru.yandex.direct.core.entity.campaign.service.type.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

class BannerHrefParamsUtilsTest {

    companion object {
        @JvmStatic
        fun params() = arrayOf(
            "" to null,
            " " to null,
            "\t" to null,
            "\tparams " to "params",
            "?" to null,
            "&" to null,
            "?&" to null,
            "?&params&" to "params",
            "?&params&params&" to "params&params",
            "?&&params&params&&" to "params&params",
            "?&?params?&params??&" to "params?&params??",
            "?&кириллица&" to "кириллица",
        )
    }

    @ParameterizedTest
    @MethodSource("params")
    internal fun `check sanitizeHrefParams`(p: Pair<String, String>) {
        val (input, expected) = p
        val actual = BannerHrefParamsUtils.sanitizeHrefParams(input)
        assertThat(actual)
            .isEqualTo(expected)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "params",
            "params=value",
            "params&params",
            "params=value&params=value",
            // невалидный с точки зрения URL параметров, но мы его не меняем
            "%",
            // разные способы закодировать пробел
            "%20",
            "+",
        ]
    )
    internal fun `check sanitizeHrefParams do nothing`(input: String) {
        val actual = BannerHrefParamsUtils.sanitizeHrefParams(input)
        assertThat(actual)
            .isEqualTo(input)
    }
}
