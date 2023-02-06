package ru.yandex.direct.core.entity.strategy.type.common

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects
import ru.yandex.direct.test.utils.assertj.Conditions
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper

@RunWith(JUnitParamsRunner::class)
internal class IsPublicValidatorTest {

    fun testDataForAdding(): Array<Array<Any?>> = arrayOf(
        arrayOf(true, listOf(1), null),
        arrayOf(true, listOf(1, 2), null),
        arrayOf(false, listOf(1), null),
        arrayOf(
            false,
            listOf(1, 2),
            StrategyDefects.linkingNonPublicStrategyToSeveralCampaigns()
        ),
        arrayOf(true, null, null),
        arrayOf(false, null, null),
    )

    @Test
    @Parameters(method = "testDataForAdding")
    @TestCaseName("isPublic=[{0}], linkingCampaigns=[{1}]")
    fun `isPublic validation is correct on creating`(
        isPublic: Boolean,
        linkingCampaignsIds: List<Long>?,
        expectedDefect: Defect<Boolean>?
    ) {
        testValidation(isPublic, linkingCampaignsIds, null, expectedDefect)
    }

    fun testDataForUpdating(): Array<Array<Any?>> = arrayOf(
        arrayOf(true, listOf(1), true, null),
        arrayOf(true, listOf(1), false, null),
        arrayOf(false, listOf(1), true, DefectConstants.PUBLIC_TO_PRIVATE),
        arrayOf(false, listOf(1), false, null),
        arrayOf(true, listOf(1, 2), true, null),
        arrayOf(true, listOf(1, 2), false, null),
        arrayOf(false, listOf(1), true, DefectConstants.PUBLIC_TO_PRIVATE),
        arrayOf(false, listOf(1), false, null),
        arrayOf(
            false,
            listOf(1, 2),
            true,
            DefectConstants.FEW_LINKS_FOR_PRIVATE_COMPANY
        ),
        arrayOf(
            false,
            listOf(1, 2),
            false,
            DefectConstants.FEW_LINKS_FOR_PRIVATE_COMPANY
        ),
        arrayOf(true, null, true, null),
        arrayOf(true, null, false, null),
        arrayOf(false, null, true, DefectConstants.PUBLIC_TO_PRIVATE),
        arrayOf(false, null, false, null),
    )

    @Test
    @Parameters(method = "testDataForUpdating")
    @TestCaseName("isPublic=[{0}], linkingCampaigns=[{1}], currentIsPublic=[{0}]")
    fun `isPublic validation is correct on updating`(
        isPublic: Boolean,
        linkingCampaignsIds: List<Long>?,
        currentIsPublic: Boolean,
        expectedDefect: Defect<Boolean>?
    ) {
        testValidation(isPublic, linkingCampaignsIds, currentIsPublic, expectedDefect)
    }

    private fun testValidation(
        isPublic: Boolean,
        linkingCampaignsIds: List<Long>?,
        currentIsPublic: Boolean?,
        expectedDefect: Defect<Boolean>?
    ) {
        val validator = IsPublicValidator(
            (linkingCampaignsIds ?: emptyList())
                .toList(),
            currentIsPublic,
        )
        val validationResult = validator.apply(isPublic)
        var matcher = Matchers.hasNoDefectsDefinitions<Boolean?>()
        if (expectedDefect != null) {
            matcher = Matchers.hasDefectDefinitionWith(
                Matchers.validationError(
                    PathHelper.emptyPath(),
                    expectedDefect
                )
            )
        }
        Assertions.assertThat(validationResult).`is`(Conditions.matchedBy(matcher))
    }

    object DefectConstants {
        val FEW_LINKS_FOR_PRIVATE_COMPANY: Defect<Void> = StrategyDefects.linkingNonPublicStrategyToSeveralCampaigns()
        val PUBLIC_TO_PRIVATE: Defect<Void> = StrategyDefects.changePublicStrategyToPrivate()
    }
}

