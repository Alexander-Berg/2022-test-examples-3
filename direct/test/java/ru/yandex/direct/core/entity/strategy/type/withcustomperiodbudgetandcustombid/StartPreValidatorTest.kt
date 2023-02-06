package ru.yandex.direct.core.entity.strategy.type.withcustomperiodbudgetandcustombid

import java.time.LocalDate
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.defect.DateDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper

@RunWith(JUnitParamsRunner::class)
internal class StartPreValidatorTest {

    fun testsData(): List<Array<Any>> = listOf(
        arrayOf(
            "invalid start: null",
            TestData(start = null, defect = CommonDefects.notNull())
        ),
        arrayOf(
            "invalid start: before now and isStrategyChanged is true",
            TestData(
                start = NOW.minusDays(1),
                now = NOW,
                isStrategyChanged = true,
                defect = DateDefects.greaterThanOrEqualTo(NOW)
            )
        ),
        arrayOf(
            "valid start: not check start when start is not null and isStrategyChanged is false",
            TestData(start = NOW.minusDays(1), now = NOW, isStrategyChanged = false)
        ),
        arrayOf(
            "valid start: greater than or equal to now and isStrategyChanged is true",
            TestData(start = NOW, now = NOW, isStrategyChanged = true)
        )
    )

    @Test
    @Parameters(method = "testsData")
    @TestCaseName("{0}")
    fun test(testCaseName: String, testData: TestData) {
        val validator = StartPreValidator(testData.now, testData.isStrategyChanged)
        val validationResult = validator.apply(testData.start)

        testData.defect?.let {
            Assert.assertThat(
                validationResult,
                Matchers.hasDefectDefinitionWith(
                    Matchers.validationError(PathHelper.emptyPath(), it)
                )
            )
        } ?: Assert.assertThat(validationResult, Matchers.hasNoDefectsDefinitions())
    }

    companion object {
        private val NOW = LocalDate.now()

        data class TestData(
            val start: LocalDate?,
            val now: LocalDate = NOW,
            val isStrategyChanged: Boolean = false,
            val defect: Defect<*>? = null
        )
    }
}
