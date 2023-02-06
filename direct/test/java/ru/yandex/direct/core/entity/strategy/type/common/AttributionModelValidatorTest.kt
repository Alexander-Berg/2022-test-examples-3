package ru.yandex.direct.core.entity.strategy.type.common

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper

@RunWith(JUnitParamsRunner::class)
internal class AttributionModelValidatorTest {

    fun testData(): Array<Array<Any?>> = arrayOf(
        arrayOf(null, CommonDefects.notNull()),
        arrayOf(StrategyAttributionModel.FIRST_CLICK, null),
        arrayOf(StrategyAttributionModel.LAST_CLICK, null),
        arrayOf(StrategyAttributionModel.LAST_SIGNIFICANT_CLICK, null),
        arrayOf(StrategyAttributionModel.LAST_YANDEX_DIRECT_CLICK, null),
        arrayOf(StrategyAttributionModel.FIRST_CLICK_CROSS_DEVICE, null),
        arrayOf(StrategyAttributionModel.LAST_SIGNIFICANT_CLICK_CROSS_DEVICE, null),
        arrayOf(StrategyAttributionModel.LAST_YANDEX_DIRECT_CLICK_CROSS_DEVICE, null),
    )

    @Test
    @Parameters(method = "testData")
    @TestCaseName("attributionModel=[{0}]")
    fun `attributionModel validation is correct`(
        attributionModel: StrategyAttributionModel?,
        defect: Defect<StrategyAttributionModel?>?
    ) {
        val validationResult = AttributionModelValidator.apply(attributionModel)
        if (defect != null) {
            val matcher = Matchers.hasDefectDefinitionWith<StrategyAttributionModel?>(
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
