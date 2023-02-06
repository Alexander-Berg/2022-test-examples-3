package ru.yandex.direct.core.entity.strategy.type.autobudgetroi

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.RESERVE_RETURN_MAX
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.RESERVE_RETURN_MIN
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.RESERVE_RETURN_STEP
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.incorrectReserveReturn
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects.notNull
import ru.yandex.direct.validation.defect.NumberDefects.inInterval
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper

@RunWith(JUnitParamsRunner::class)
internal class ReserveReturnValidatorTest {

    private val outOfRangeDefect = inInterval(RESERVE_RETURN_MIN, RESERVE_RETURN_MAX)

    fun testData(): Array<Array<Any?>> = arrayOf(
        //null
        arrayOf(null, notNull()),
        //out of range
        arrayOf(RESERVE_RETURN_MIN - 1, outOfRangeDefect),
        arrayOf(RESERVE_RETURN_MAX + 1, outOfRangeDefect),
        //not multiple of RESERVE_RETURN_STEP
        arrayOf(RESERVE_RETURN_MAX - 1, incorrectReserveReturn()),
        arrayOf(RESERVE_RETURN_MIN + 1, incorrectReserveReturn()),
        //validation success
        arrayOf(RESERVE_RETURN_MIN, null),
        arrayOf(RESERVE_RETURN_MAX, null),
        arrayOf(RESERVE_RETURN_MAX - RESERVE_RETURN_STEP, null),
        arrayOf(RESERVE_RETURN_MIN + RESERVE_RETURN_STEP, null)
    )

    @Test
    @Parameters(method = "testData")
    @TestCaseName("reserveReturn=[{0}]")
    fun `reserve return validation is correct`(reserveReturn: Long?, defect: Defect<Long>?) {
        val validationResult = ReserveReturnValidator.apply(reserveReturn)
        if (defect != null) {
            val matcher = Matchers.hasDefectDefinitionWith<Long>(
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
