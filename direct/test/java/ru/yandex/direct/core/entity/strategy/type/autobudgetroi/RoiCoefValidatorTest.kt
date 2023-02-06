package ru.yandex.direct.core.entity.strategy.type.autobudgetroi

import java.math.BigDecimal
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.ROI_COEF_MIN
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects.notNull
import ru.yandex.direct.validation.defect.NumberDefects.greaterThan
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper

@RunWith(JUnitParamsRunner::class)
internal class RoiCoefValidatorTest {

    fun testData(): Array<Array<Any?>> = arrayOf(
        arrayOf(null, notNull()),
        arrayOf(ROI_COEF_MIN.minus(1L.toBigDecimal()), greaterThan(ROI_COEF_MIN)),
        arrayOf(ROI_COEF_MIN, greaterThan(ROI_COEF_MIN)),
        arrayOf(ROI_COEF_MIN.plus(1L.toBigDecimal()), null),
        arrayOf(ROI_COEF_MIN.plus(10L.toBigDecimal()), null)
    )

    @Test
    @Parameters(method = "testData")
    @TestCaseName("roiCoef=[{0}]")
    fun `roi coefficient validation is correct`(roiCoef: BigDecimal?, defect: Defect<*>?) {
        val validationResult = RoiCoefValidator.apply(roiCoef)
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
