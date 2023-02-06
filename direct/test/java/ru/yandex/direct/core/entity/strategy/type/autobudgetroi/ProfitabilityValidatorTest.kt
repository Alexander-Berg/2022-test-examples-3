package ru.yandex.direct.core.entity.strategy.type.autobudgetroi

import java.math.BigDecimal
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.PROFITABILITY_MAX
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.PROFITABILITY_MIN
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects.notNull
import ru.yandex.direct.validation.defect.NumberDefects.inInterval
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper

@RunWith(JUnitParamsRunner::class)
internal class ProfitabilityValidatorTest {

    fun testData(): Array<Array<Any?>> = arrayOf(
        arrayOf(null, notNull()),
        arrayOf(PROFITABILITY_MIN.minus(1L.toBigDecimal()), inInterval(PROFITABILITY_MIN, PROFITABILITY_MAX)),
        arrayOf(PROFITABILITY_MAX.plus(1L.toBigDecimal()), inInterval(PROFITABILITY_MIN, PROFITABILITY_MAX)),
        arrayOf(PROFITABILITY_MAX, null),
        arrayOf(PROFITABILITY_MIN, null),
        arrayOf(PROFITABILITY_MIN.plus(1.toBigDecimal()), null),
        arrayOf(PROFITABILITY_MAX.minus(1.toBigDecimal()), null)
    )

    @Test
    @Parameters(method = "testData")
    @TestCaseName("Profitability=[{0}]")
    fun `profitability validation is correct`(profitability: BigDecimal?, defect: Defect<*>?) {
        val validationResult = ProfitabilityValidator.apply(profitability)
        if (defect != null) {
            val matcher = Matchers.hasDefectDefinitionWith<BigDecimal?>(
                Matchers.validationError(
                    PathHelper.emptyPath(),
                    defect
                )
            )
            validationResult.check(matcher)
        } else {
            validationResult.check(Matchers.hasNoDefectsDefinitions())
        }

    }
}
