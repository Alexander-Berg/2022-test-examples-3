package ru.yandex.direct.core.entity.strategy.type.withcustomperiodbudgetandcustombid

import java.time.LocalDate
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.DateDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper

@RunWith(JUnitParamsRunner::class)
class FinishPreValidatorTest {

    fun testsData(): List<Array<Any>> = listOf(
        arrayOf(
            "invalid finish: before now",
            TestData(
                finish = NOW.minusDays(1),
                now = NOW,
                defect = DateDefects.greaterThanOrEqualTo(NOW)
            )
        ),
        arrayOf("valid finish: greater than or equal to now", TestData(finish = NOW, now = NOW)),
        arrayOf("valid finish: null", TestData(finish = null))
    )

    @Test
    @Parameters(method = "testsData")
    @TestCaseName("{0}")
    fun test(testCaseName: String, testData: TestData) {
        val validator = FinishPreValidator(testData.now)
        val validationResult = validator.apply(testData.finish)

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
            val finish: LocalDate?,
            val now: LocalDate = NOW,
            val defect: Defect<*>? = null
        )
    }
}
