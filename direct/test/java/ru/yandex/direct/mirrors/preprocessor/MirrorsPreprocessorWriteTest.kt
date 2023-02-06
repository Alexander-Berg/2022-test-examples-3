package ru.yandex.direct.mirrors.preprocessor

import com.google.common.collect.ImmutableSet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class MirrorsPreprocessorWriteTest {
    @Test
    fun resultFileCreatedCorrectly(@TempDir folder: Path) {
        val tmpFile = folder.resolve("ddd")

        val preprocesser = MirrorsPreprocessor(
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
        val httpsSkiplist = ImmutableSet.of("yandex.ru")
        preprocesser.writeResultFile(tmpFile.toString(), httpsSkiplist)

        val lines = Files.readAllLines(tmpFile)
        assertThat(lines).containsExactly(
            "yandex.ru ya.ru",
            "google.com google.com"
        )
    }
}
