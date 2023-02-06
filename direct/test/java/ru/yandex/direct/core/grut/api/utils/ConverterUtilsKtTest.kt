package ru.yandex.direct.core.grut.api.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ConverterUtilsKtTest {
    @Test
    fun testColorStringToUint64() {
        assertThat(colorStringToUint64("#FFAABB")).isEqualTo(0xFFAABBL)
        assertThat(colorStringToUint64("#00aabb")).isEqualTo(0x00AABBL)
    }

    @Test(expected = IllegalStateException::class)
    fun testColorStringToUint64Invalid() {
        colorStringToUint64("FFFFFF")
    }

    @Test
    fun testCrc64StringToLong() {
        assertThat(crc64StringToLong("1C4C9A3F7832AE18")).isEqualTo(0x1C4C9A3F7832AE18L)
        assertThat(crc64StringToLong("BC4C9A3F7832AE18")).isEqualTo(-4878354698958885352L)
    }
}
