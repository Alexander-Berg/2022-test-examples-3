package ru.yandex.market.mdm.lib.util

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.Test
import kotlin.test.assertFailsWith

class AlgorithmTest {

    @Test
    fun testEmpty() {
        val table = listOf<List<Char>>()
        val result = cartesian(table)
        val expected = listOf<List<Char>>()

        result shouldBe expected
    }

    @Test
    fun testTrivial1() {
        val table = listOf(
            listOf('A', 'B', 'C')
        )
        val result = cartesian(table)
        val expected = listOf(
            listOf('A'),
            listOf('B'),
            listOf('C')
        )

        result shouldBe expected
    }

    @Test
    fun testTrivial2() {
        val table = listOf(
            listOf('A'),
            listOf('B'),
            listOf('C')
        )
        val result = cartesian(table)
        val expected = listOf(
            listOf('A', 'B', 'C')
        )

        result shouldBe expected
    }

    @Test
    fun testComplex2d() {
        val table = listOf(
            listOf('A', 'B', 'C'),
            listOf('X', 'Y', 'Z'),
        )
        val result = cartesian(table)
        val expected = listOf(
            listOf('A', 'X'),
            listOf('A', 'Y'),
            listOf('A', 'Z'),
            listOf('B', 'X'),
            listOf('B', 'Y'),
            listOf('B', 'Z'),
            listOf('C', 'X'),
            listOf('C', 'Y'),
            listOf('C', 'Z'),
        )

        result shouldBe expected
    }

    @Test
    fun testComplex2dArbitraryLengths() {
        val table = listOf(
            listOf('A', 'B', 'C'),
            listOf('X', 'Y'),
            listOf('λ')
        )
        val result = cartesian(table)
        val expected = listOf(
            listOf('A', 'X', 'λ'),
            listOf('A', 'Y', 'λ'),
            listOf('B', 'X', 'λ'),
            listOf('B', 'Y', 'λ'),
            listOf('C', 'X', 'λ'),
            listOf('C', 'Y', 'λ'),
        )

        result shouldBe expected
    }

    @Test
    fun testOneBigSetAndOtherTrivial() {
        // Сценарий близок к продакшену, где у нас много service_id + один shop_sku
        val table = listOf(
            listOf('A', 'B', 'C', 'D'),
            listOf('X'),
            listOf('Y'),
            listOf('Z'),
        )
        val result = cartesian(table)
        val expected = listOf(
            listOf('A', 'X', 'Y', 'Z'),
            listOf('B', 'X', 'Y', 'Z'),
            listOf('C', 'X', 'Y', 'Z'),
            listOf('D', 'X', 'Y', 'Z')
        )

        result shouldBe expected
    }

    @Test
    fun `when at least one is empty result also should be empty`() {
        // given
        val table = listOf(
            listOf('A', 'B', 'C', 'D'),
            listOf('E', 'F', 'G', 'H'),
            listOf('I', 'J', 'K', 'L'),
            listOf(),
        )

        // when
        val result = cartesian(table)

        // then
        result shouldHaveSize 0
    }

    @Test
    fun `when result size overflows signed int32 should throw exception`() {
        // given
        val table = listOf(
            (1..1000).toList(),
            (1..1000).toList(),
            (1..1000).toList(),
            (1..1000).toList(),
        )

        // when
        val exception = assertFailsWith<ArithmeticException> { cartesian(table) }

        // then
        exception.message shouldBe "integer overflow"
    }
}
