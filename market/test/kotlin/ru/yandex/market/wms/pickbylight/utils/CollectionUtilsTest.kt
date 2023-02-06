package ru.yandex.market.wms.pickbylight.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CollectionUtilsTest {

    @Test
    fun startsWith() {
        val prefix = byteArrayOf(1, 2, 3)

        assertThat(byteArrayOf(1, 2, 3).startsWith(prefix)).isTrue
        assertThat(byteArrayOf(1, 2, 3, 4, 5).startsWith(prefix)).isTrue

        assertThat(byteArrayOf(1, 1, 3, 4, 5).startsWith(prefix)).isFalse
        assertThat(byteArrayOf(1, 2).startsWith(prefix)).isFalse
    }

    @Test
    fun bytesToReadableStringAndBack() {
        val arrayString = "01AaZz![\\]<1><f><12><1f>Ok"
        val array = byteArrayOf(48, 49, 65, 97, 90, 122, 33, 91, 92, 93, 1, 15, 18, 31, 79, 107)

        assertThat(byteArrayFromReadableString(arrayString)).isEqualTo(array)
        assertThat(array.toReadableString()).isEqualTo(arrayString)

        val array2 = byteArrayOf(1, 15, 15, 15, 15)
        assertThat(byteArrayFromReadableString("<01><f><F><0f><0F><>")).isEqualTo(array2)
        assertThat(array2.toReadableString()).isEqualTo("<1><f><f><f><f>")
    }
}
