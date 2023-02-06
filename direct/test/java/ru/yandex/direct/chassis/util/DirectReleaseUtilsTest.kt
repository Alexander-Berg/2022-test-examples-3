package ru.yandex.direct.chassis.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DirectReleaseUtilsTest {

    private fun releaseVersionStringParams() = arrayOf(
        arrayOf("релиз: Сборка от 2022-03-28 - выложить 1.9281025.9292232-1", "1.9281025.9292232-1"),
        arrayOf("релиз java-api5: Сборка от 2022-01-27 - выложить 1.9078758-1", "1.9078758-1"),
        arrayOf("релиз java-jobs: Сборка от 2022-01-28 - выложить 1.9083436.9088651-1", "1.9083436.9088651-1"),

        arrayOf("релиз java-jobs: сборка из NewCI v37 - выложить 1.9302816-1", "37"),
        arrayOf("релиз java-web: сборка из NewCI v32.6 - выложить 1.9286265-1", "32.6"),

        arrayOf("НЕ ВЫКЛАДЫВАТЬ релиз java-jobs: сборка из NewCI v37 - выложить 1.9302816-1", null),
        arrayOf("релиз java-jobs: сборка из NewCI v37 - НЕ ВЫКЛАДЫВАТЬ выложить 1.9302816-1", null),
        arrayOf("релиз java-jobs: сборка из NewCI v37 - выложить 1.9302816-1 НЕ ВЫКЛАДЫВАТЬ", "37"),
    )

    @ParameterizedTest
    @MethodSource("releaseVersionStringParams")
    fun `test getReleaseVersionString`(summary: String, expectedVersion: String?) {
        val actualVersion = DirectReleaseUtils.getReleaseVersionString(summary)
        assertThat(actualVersion).isEqualTo(expectedVersion)
    }

    private fun getReleaseVersionFromDockerImageTagParams() = arrayOf(
        arrayOf("1-8ba580188eb0211a216d4c53a712764739886560", "1"),
        arrayOf("2.1-7854c21d1426286b424a91d441b6c09d4e6aeb99", "2.1"),
        arrayOf("2.4-1dd925871dc4ded5d1a0c15d7d8731852003eb29", "2.4"),
    )

    @ParameterizedTest
    @MethodSource("getReleaseVersionFromDockerImageTagParams")
    fun `test getReleaseVersionFromDockerImageTag`(tag: String, expectedVersion: String?) {
        val actualVersion = DirectReleaseUtils.getReleaseVersionFromDockerImageTag(tag)
        assertThat(actualVersion).isEqualTo(expectedVersion)
    }

    private fun releaseVersionParams() = arrayOf(
        arrayOf("1.9078758-1", ReleaseVersion(9078758, null)),
        arrayOf("1.9083436.9088651-1", ReleaseVersion(9083436, 9088651)),

        arrayOf("37", ReleaseVersion(37, null)),
        arrayOf("36.1", ReleaseVersion(36, 1)),
        arrayOf("32.6", ReleaseVersion(32, 6)),

        arrayOf("v32.6", null),
    )

    @ParameterizedTest
    @MethodSource("releaseVersionParams")
    fun `test ReleaseVersion parse`(version: String, expectedVersion: ReleaseVersion?) {
        val actualVersion = ReleaseVersion.parse(version)
        assertThat(actualVersion).isEqualTo(expectedVersion)
    }
}
