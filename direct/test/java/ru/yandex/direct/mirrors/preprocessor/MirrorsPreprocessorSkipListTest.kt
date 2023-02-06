package ru.yandex.direct.mirrors.preprocessor

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MirrorsPreprocessorSkipListTest {
    @Test
    fun noBadHttps() {
        val preprocessor = MirrorsPreprocessor(
            listOf(
                listOf(
                    MirrorHost("yandex.ru", true),
                    MirrorHost("yandex.ru", false),
                    MirrorHost("ya.ru", false)
                ),
                listOf(
                    MirrorHost("google.com", false),
                    MirrorHost("google.com", true)
                )
            )
        )
        assertThat(preprocessor.makeHttpsSkipList()).isEmpty()
    }

    @Test
    fun badHttpsMirror() {
        val preprocessor = MirrorsPreprocessor(
            listOf(
                listOf(
                    MirrorHost("yandex.ru", false),
                    MirrorHost("ya.ru", false)
                ),
                listOf(
                    MirrorHost("google.com", false),
                    MirrorHost("yandex.ru", true),
                    MirrorHost("google.com", true)
                )
            )
        )
        assertThat(preprocessor.makeHttpsSkipList()).containsExactly("yandex.ru")
    }
}
