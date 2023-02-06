package ru.yandex.direct.internaltools.tools.newinterface

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.internaltools.tools.newinterface.NewInterfaceAlertTool.sanitizeLongListString

@RunWith(JUnitParamsRunner::class)
class NewInterfaceAlertToolSanitizeLongListStringTest {
    companion object {
        @JvmStatic
        fun params() = arrayOf(
            " " to "",

            "1" to "1",
            " 1 " to "1",

            "1,2" to "1,2",
            "1 2" to "1,2",
            "1, 2" to "1,2",
            "1\n2" to "1,2",

            "-" to "",
            "-1" to "1",
            "-1-" to "1",
            "-1,-2" to "1,2",

            "0,1" to "0,1",
            "01" to "1",
        )

        @JvmStatic
        fun incorrectParams() = arrayOf(
            "a",
            "0x1",
            "101b",
            Long.MAX_VALUE.toULong().plus(1U).toString(),
        )
    }

    @Test
    @Parameters(method = "params")
    fun sanitizeLongListString_success(p: Pair<String, String>) {
        val (input, expected) = p
        val actual = sanitizeLongListString(input)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    @Parameters(method = "incorrectParams")
    fun sanitizeLongListString_throwException(input: String) {
        assertThatThrownBy { sanitizeLongListString(input) }
            .isInstanceOf(NumberFormatException::class.java)
    }
}
