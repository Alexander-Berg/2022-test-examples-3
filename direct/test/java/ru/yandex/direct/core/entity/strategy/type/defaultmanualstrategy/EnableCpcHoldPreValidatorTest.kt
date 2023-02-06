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
import ru.yandex.direct.validation.defect.CommonDefects.isNull
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.ValidationResult

@RunWith(JUnitParamsRunner::class)
internal class EnableCpcHoldPreValidatorTest {

    fun testData(): List<List<Any?>> = listOf(
        listOf(CampaignType.CPM_BANNER, false, true, isNull()),
        listOf(CampaignType.CPM_BANNER, false, false, isNull()),
        listOf(CampaignType.CPM_BANNER, false, null, null),
        listOf(CampaignType.CPM_BANNER, true, false, null),
        listOf(CampaignType.CPM_BANNER, true, true, null),
        listOf(CampaignType.CPM_BANNER, false, null, null),
        listOf(CampaignType.TEXT, false, false, null),
        listOf(CampaignType.TEXT, false, true, null),
        listOf(CampaignType.TEXT, false, null, null),
        listOf(CampaignType.TEXT, true, true, null),
        listOf(CampaignType.TEXT, true, false, null),
        listOf(CampaignType.TEXT, true, null, null),
    )

    @Test
    @Parameters(method = "testData")
    @TestCaseName("campaignType={0},isCopy={1},isCpcHoldEnabled={2}")
    fun `validation is correct`(
        campaignType: CampaignType?,
        isCopy: Boolean,
        isCpcHoldEnabled: Boolean?,
        defect: Defect<*>?
    ) {
        val validator = EnableCpcHoldPreValidator(campaignType, isCopy)

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
