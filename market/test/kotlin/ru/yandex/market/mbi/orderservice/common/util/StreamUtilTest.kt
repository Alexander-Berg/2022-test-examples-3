package ru.yandex.market.mbi.orderservice.common.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StreamUtilTest {
    @Test
    fun `takeWhileInclusive test`() {
        assertThat(sequenceOf(1, 2, 3).takeWhileInclusive { it < 2 }.toList()).containsExactly(1, 2)
    }

    @Test
    fun `fetchAsSequence test`() {
        assertThat(fetchAsSequence(2, 1, this::batchOffsetSelector).toList()).containsExactly(2, 3, 4)
    }

    @Test
    fun `fetchUsingLastAsSequence test`() {
        assertThat(
            fetchUsingLastAsSequence(2, 1, this::batchLastSelector) { it }.map {
                print(it)
                it
            }.toList()
        ).containsExactly(2L, 3L, 4L)
    }

    @Test
    fun `fetchUsingLastAsSequence test empty fetch`() {
        assertThat(
            fetchUsingLastAsSequence(2, 2, this::batchLastSelector) { it }.map {
                print(it)
                it
            }.toList()
        ).containsExactly(3L, 4L)
    }

    fun batchOffsetSelector(limit: Long, offset: Long) =
        generateSequence(1) { it + 1 }.takeWhile { it < 5 }.dropWhile { it <= offset }.take(limit.toInt()).toList()

    fun batchLastSelector(limit: Long, last: Long): List<Long> {
        val res =
            generateSequence(1L) { it + 1 }.dropWhile { it <= last }.takeWhile { it < 5 }.take(limit.toInt()).toList()
        return res
    }
}
