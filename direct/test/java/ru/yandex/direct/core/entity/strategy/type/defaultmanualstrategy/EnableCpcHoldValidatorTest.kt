package ru.yandex.direct.core.entity.strategy.type.defaultmanualstrategy

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.hamcrest.Matcher
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects.notNull
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.ValidationResult

@RunWith(JUnitParamsRunner::class)
internal class EnableCpcHoldValidatorTest {

    fun testData(): List<List<Any?>> = listOf(
        listOf(CampaignType.TEXT, false, null),
        listOf(CampaignType.TEXT, true, null),
        listOf(CampaignType.TEXT, null, notNull()),
        listOf(CampaignType.CPM_BANNER, null, null),
        listOf(CampaignType.CPM_BANNER, false, null),
        listOf(CampaignType.CPM_BANNER, true, null),
    )

    @Test
    @Parameters(method = "testData")
    @TestCaseName("{0}")
    fun `validation is correct`(campaignType: CampaignType?, isCpcHoldEnabled: Boolean?, defect: Defect<*>?) {
        val validator = EnableCpcHoldValidator(campaignType)

        val validationResult = validator.apply(isCpcHoldEnabled)

        val matcher: Matcher<ValidationResult<Boolean?, Defect<*>>> = if (defect != null) {
            Matchers.hasDefectDefinitionWith<Boolean?>(
                Matchers.validationError(
                    PathHelper.emptyPath(),
                    defect
                )
            )
        } else {
            Matchers.hasNoDefectsDefinitions()
        }

        validationResult.check(matcher)
    }
}
