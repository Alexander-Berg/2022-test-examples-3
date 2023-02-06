package ru.yandex.direct.web.entity.uac.repository

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils

@RunWith(JUnitParamsRunner::class)
class UacYdbUtilsTest {

    fun cityHashParams() = listOf(
        listOf(
            arrayOf(
                "app.yandex.some",
                "ru",
                "ru",
                1,
                1
            ),
            "4791608187873723201"
        ),
        listOf(
            arrayOf(
                "com.sendmode.comunityalerts",
                "uk",
                "ua",
                1,
                1
            ),
            "5742401298855168881"
        ),
        listOf(
            arrayOf(
                "id1489340046",
                "ru",
                "ru",
                2,
                2
            ),
            "6678314111666544000"
        )
    )

    @Test
    @Parameters(method = "cityHashParams")
    fun cityHash64(args: Array<Any>, expectedHash: String) {
        assertThat(UacYdbUtils.cityHash64(*args)).isEqualTo(expectedHash)
    }
}
