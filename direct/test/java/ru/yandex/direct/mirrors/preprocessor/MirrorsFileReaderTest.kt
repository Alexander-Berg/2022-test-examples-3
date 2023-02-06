package ru.yandex.direct.mirrors.preprocessor

import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.ArrayList

class MirrorsFileReaderTest {
    @Test
    fun readerReadsAllData(@TempDir tmpDir: Path) {
        val tmpFile = tmpDir.resolve("data")
        fillFile(tmpFile)
        val reader = MirrorsFileReader(tmpFile.toString())

        val data: MutableList<List<MirrorHost>> = ArrayList()
        for (it in reader) {
            data.add(it)
        }

        println(data)
        val softly = SoftAssertions()
        softly.assertThat(data.size).isEqualTo(3)
        softly.assertThat(data[0]).containsExactly(
            MirrorHost("yandex.ru", true),
            MirrorHost("yandex.ru", false),
            MirrorHost("ya.ru", false)
        )
        softly.assertThat(data[1]).containsExactly(
            MirrorHost("google.com", false),
            MirrorHost("google.com", true)
        )
        softly.assertThat(data[2]).containsExactly(
            MirrorHost("0--0--7.tumblr.com", false),
            MirrorHost("0--0--7.tumblr.com", true),
            MirrorHost("www.0--0--7.tumblr.com", false)
        )
        softly.assertAll()
    }

    fun fillFile(tmpFile: Path) {
        Files.write(
            tmpFile, listOf(
                "12341234\t1324423524\thttps://yandex.ru",
                "12341234\t1324423524\tyandex.ru",
                "12341234\t1324423524\tya.ru",
                "",
                "324234\t341234\tgoogle.com",
                "324234\t341234\thttps://google.com:444",
                "",
                "805306368\t0\thttp://0--0--7.tumblr.com",
                "268435456\t0\thttps://0--0--7.tumblr.com",
                "268435712\t0\thttp://www.0--0--7.tumblr.com"
            )
        )
    }
}
