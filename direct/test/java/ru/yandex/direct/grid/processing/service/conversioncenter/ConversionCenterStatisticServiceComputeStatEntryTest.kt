package ru.yandex.direct.grid.processing.service.conversioncenter

import com.nhaarman.mockitokotlin2.mock
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(JUnitParamsRunner::class)
class ConversionCenterStatisticServiceComputeStatEntryTest {

    lateinit var conversionCenterStatisticService: ConversionCenterStatisticService

    @Before
    fun before() {
        conversionCenterStatisticService = ConversionCenterStatisticService(mock(), mock())
    }

    fun testData(): List<TestData> {
        return listOf(
            TestData(null, null, ConversionsStat(0, 1f)),
            TestData(0, null, ConversionsStat(0, 1f)),
            TestData(null, 1, ConversionsStat(1, 0f)),
            TestData(1, null, ConversionsStat(0, 1f)),
            TestData(0, 1, ConversionsStat(1, 0f)),
            TestData(1, 1, ConversionsStat(1, 1f)),
            TestData(1, 10, ConversionsStat(10, 0.1f)),
            TestData(2, 1, ConversionsStat(1, 1f)),
        )
    }

    @Test
    @Parameters(method = "testData")
    @TestCaseName("{0}")
    fun computeStatEntry(testData: TestData) {
        val actual = conversionCenterStatisticService.computeStatEntry(
            testData.clientDirectConversions,
            testData.allMetrikaConversions,
        )

        assertThat(actual).isEqualTo(testData.expected)
    }

    data class TestData(
        val clientDirectConversions: Long?,
        val allMetrikaConversions: Long?,
        val expected: ConversionsStat,
    )
}
