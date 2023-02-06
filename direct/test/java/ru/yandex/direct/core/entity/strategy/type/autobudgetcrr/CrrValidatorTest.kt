package ru.yandex.direct.core.entity.strategy.type.autobudgetcrr

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.CRR_MAX
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.CRR_MIN
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects.notNull
import ru.yandex.direct.validation.defect.NumberDefects.inInterval
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper

@RunWith(JUnitParamsRunner::class)
internal class CrrValidatorTest {

    fun testData(): Array<Array<Any?>> = arrayOf(
        arrayOf(null, notNull()),
        arrayOf(CRR_MIN - 1, inInterval(CRR_MIN, CRR_MAX)),
        arrayOf(CRR_MAX + 1, inInterval(CRR_MIN, CRR_MAX)),
        arrayOf(CRR_MIN, null),
        arrayOf(CRR_MAX, null),
        arrayOf(CRR_MIN + 1, null),
        arrayOf(CRR_MAX - 1, null),
    )

    @Test
    @Parameters(method = "testData")
    @TestCaseName("crr=[{0}]")
    fun `crr validation is correct`(crr: Long?, defect: Defect<Long?>?) {
        val validationResult = CrrValidator.apply(crr)
        if (defect != null) {
            val matcher = Matchers.hasDefectDefinitionWith<Long?>(
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
