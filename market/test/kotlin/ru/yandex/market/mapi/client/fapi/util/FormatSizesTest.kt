package ru.yandex.market.mapi.client.fapi.util

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FormatSizesTest {

    @Test
    fun pantsSizesWithoutExtraTest() {
        val sizes: List<Double> = listOf(13.0, 14.0, 15.0, 16.0, 19.0, 22.0, 27.0, 28.0, 29.0)
        val chartName = "pants_size"
        val maxSizesCount = 4

        val result = SizeFormatUtils.formatSizes(sizes, chartName, maxSizesCount)

        assertEquals("13-16,19,22,27-29", result)
    }

    @Test
    fun pantsSizesWithExtra3Test() {
        val sizes: List<Double> = listOf(13.0, 14.0, 15.0, 16.0, 19.0, 22.0, 27.0, 28.0, 29.0)
        val chartName = "pants_size"
        val maxSizesCount = 3

        val result = SizeFormatUtils.formatSizes(sizes, chartName, maxSizesCount)

        assertEquals("13-16,19,22 +3", result)
    }

    @Test
    fun emptySizesTest() {
        val sizes: List<Double> = emptyList()
        val chartName = "shoes_size"
        val maxSizesCount = 3

        val result = SizeFormatUtils.formatSizes(sizes, chartName, maxSizesCount)

        assertEquals("", result)
    }

    @Test
    fun pantsSizesWithRangesWithoutExtraForCount9() {
        val sizes: List<Double> = listOf(3.0, 14.0, 15.0, 16.0, 21.0, 22.0, 27.0, 28.0, 79.0)
        val chartName = "pants_size"
        val maxSizesCount = 9

        val result = SizeFormatUtils.formatSizes(sizes, chartName, maxSizesCount)

        assertEquals("3,14-16,21,22,27,28,79", result)
    }

    @Test
    fun pantsSizesWithRangesWithExtra4() {
        val sizes: List<Double> = listOf(3.0, 14.0, 15.0, 16.0, 21.0, 22.0, 27.0, 28.0, 79.0)
        val chartName = "pants_size"
        val maxSizesCount = 3

        val result = SizeFormatUtils.formatSizes(sizes, chartName, maxSizesCount)

        assertEquals("3,14-16,21 +4", result)
    }

    @Test
    fun pantsSizesWithRangesWithExtra1() {
        val sizes: List<Double> = listOf(13.0, 15.0, 19.0, 21.0, 23.0, 25.0, 29.0)
        val chartName = "pants_size"
        val maxSizesCount = 3

        val result = SizeFormatUtils.formatSizes(sizes, chartName, maxSizesCount)

        assertEquals("13,15,19-25 +1", result)
    }

    @Test
    fun shoesSizesWithRangesWithExtra2() {
        val sizes: List<Double> = listOf(41.0, 42.5, 43.0, 43.5, 44.5, 46.0, 46.5)
        val chartName = "shoes_size"
        val maxSizesCount = 3

        val result = SizeFormatUtils.formatSizes(sizes, chartName, maxSizesCount)

        assertEquals("41,42.5-43.5,44.5 +2", result)
    }

    @Test
    fun shoesSizesWithRangesWithExtra12() {
        val sizes: List<Double> = listOf(33.0, 34.0, 35.0, 36.0, 37.0, 38.0, 39.0, 40.0, 41.0, 42.5, 43.0, 43.5, 45.5, 46.0, 46.5)
        val chartName = "shoes_size"
        val maxSizesCount = 3

        val result = SizeFormatUtils.formatSizes(sizes, chartName, maxSizesCount)

        assertEquals("33,34,35 +12", result)
    }

    @Test
    fun shoesSizesOnlyOneSize() {
        val sizes: List<Double> = listOf(33.0)
        val chartName = "pants_size"
        val maxSizesCount = 1

        val result = SizeFormatUtils.formatSizes(sizes, chartName, maxSizesCount)

        assertEquals("33", result)
    }

    @Test
    fun shoesSizesWithRangesWithoutExtra() {
        val sizes: List<Double> = listOf(41.0, 41.5, 42.0, 42.5, 43.0, 43.5, 44.5, 45.0, 45.5, 47.0, 48.0)
        val chartName = "shoes_size"
        val maxSizesCount = 4

        val result = SizeFormatUtils.formatSizes(sizes, chartName, maxSizesCount)

        assertEquals("41-43.5,44.5-45.5,47,48", result)
    }

    @Test
    fun shoesSizesOnlyRangesWithExtra2() {
        val sizes: List<Double> = listOf(41.0, 41.5, 42.0, 42.5, 43.0, 43.5, 44.5, 45.0, 45.5, 47.0, 48.0)
        val chartName = "shoes_size"
        val maxSizesCount = 2

        val result = SizeFormatUtils.formatSizes(sizes, chartName, maxSizesCount)

        assertEquals("41-43.5,44.5-45.5 +2", result)
    }
}
