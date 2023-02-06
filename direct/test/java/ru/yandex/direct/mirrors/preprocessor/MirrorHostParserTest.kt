package ru.yandex.direct.mirrors.preprocessor

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.direct.mirrors.preprocessor.MirrorHost.Companion.parse

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MirrorHostParserTest {
    @ParameterizedTest
    @MethodSource("params")
    fun testParser(param: Param) {
        assertThat(parse(param.src)).isEqualTo(param.expected)
    }

    fun params(): List<Param> = listOf(
        Param("yandex.ru", MirrorHost("yandex.ru", false)),
        Param("http://yanDex.ru", MirrorHost("yandex.ru", false)),
        Param("https://yandex.ru", MirrorHost("yandex.ru", true)),
        Param("https://yandex.ru:443", MirrorHost("yandex.ru", true)),
        Param("http://yandex.ru:8080", MirrorHost("yandex.ru", false)),
        Param("yandex.rU:8080", MirrorHost("yandex.ru", false))
    )

    data class Param(val src: String, val expected: MirrorHost)
}
