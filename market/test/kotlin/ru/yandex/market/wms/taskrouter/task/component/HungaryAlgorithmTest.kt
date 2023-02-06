package ru.yandex.market.wms.taskrouter.task.component

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class HungaryAlgorithmTest {

    private val hungary = HungaryAlgorithm()

    @Test
    fun test1() {
        val costs = arrayOf(
            intArrayOf(1),
        )
        val expected = intArrayOf(0)
        val actual = hungary.solve(costs)
        assertArrayEquals(expected, actual)
    }

    @Test
    fun test2() {
        val costs = arrayOf(
            intArrayOf(3, 2),
            intArrayOf(2, 2),
        )
        val expected = intArrayOf(1, 0)
        val actual = hungary.solve(costs)
        assertArrayEquals(expected, actual)
    }

    @Test
    fun test3() {
        val costs = arrayOf(
            intArrayOf(1, 2, 3),
            intArrayOf(3, 2, 1),
            intArrayOf(2, 1, 3),
        )
        val expected = intArrayOf(0, 2, 1)
        val actual = hungary.solve(costs)
        assertArrayEquals(expected, actual)
    }

    @Test
    fun test4() {
        val costs = arrayOf(
            intArrayOf(3, 3, 3),
            intArrayOf(3, 3, 3),
            intArrayOf(3, 3, 3),
        )
        val expected = intArrayOf(0, 1, 2)
        val actual = hungary.solve(costs)
        assertArrayEquals(expected, actual)
    }

    @Test
    fun test5() {
        val costs = arrayOf(
            intArrayOf(32, 28, 4, 26, 4),
            intArrayOf(17, 19, 4, 17, 4),
            intArrayOf(4, 4, 5, 4, 4),
            intArrayOf(17, 14, 4, 14, 4),
            intArrayOf(21, 16, 4, 13, 4),
        )
        val expected = intArrayOf(2, 4, 0, 1, 3)
        val actual = hungary.solve(costs)
        assertArrayEquals(expected, actual)
    }

    @Test
    fun test6() {
        val costs = Array(100) { IntArray(100) { (1000..2000).random() } }
        val expected = (0..99).shuffled().toIntArray()
        for (i in 0..99) {
            costs[i][expected[i]] = expected[i] + 100
        }
        val actual = hungary.solve(costs)
        assertArrayEquals(expected, actual)
    }

    @Test
    fun test7() {
        val costs = Array(100) { IntArray(200) { (1000..2000).random() } }
        val expected = (0..199).shuffled().subList(0, 100).toIntArray()
        for (i in 0..99) {
            costs[i][expected[i]] = expected[i] + 100
        }
        val actual = hungary.solve(costs)
        assertArrayEquals(expected, actual)
    }

    @Test
    fun testException1() {
        val costs = arrayOf<IntArray>()
        assertThrows<IllegalArgumentException> {
            hungary.solve(costs)
        }
    }

    @Test
    fun testException2() {
        val costs = arrayOf(
            intArrayOf(1),
            intArrayOf(2),
        )
        assertThrows<IllegalArgumentException> {
            hungary.solve(costs)
        }
    }

    @Test
    fun testException3() {
        val costs = arrayOf(
            intArrayOf(1, 2, 3),
            intArrayOf(2, 3),
        )
        assertThrows<IllegalArgumentException> {
            hungary.solve(costs)
        }
    }

}
