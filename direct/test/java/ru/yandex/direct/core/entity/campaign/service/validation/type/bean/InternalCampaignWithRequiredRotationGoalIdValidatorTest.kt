package ru.yandex.direct.core.entity.campaign.service.validation.type.bean

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.model.InternalCampaignWithRotationGoalId
import ru.yandex.direct.core.entity.campaign.model.InternalDistribCampaign
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MIN_INTERNAL_CAMPAIGN_ROTATION_GOAL_ID
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.defect.NumberDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.path


@RunWith(JUnitParamsRunner::class)
class InternalCampaignWithRequiredRotationGoalIdValidatorTest {

    private val validator = InternalCampaignWithRequiredRotationGoalIdValidator()

    fun parametrizedTestData(): List<List<Any?>> = listOf(
            listOf(3L, true, null),
            listOf(4L, false, null),
            listOf(4L, true, null),

            listOf(3L, false, CommonDefects.inconsistentState()),
            listOf(MIN_INTERNAL_CAMPAIGN_ROTATION_GOAL_ID - 1, false,
                    NumberDefects.greaterThanOrEqualTo(MIN_INTERNAL_CAMPAIGN_ROTATION_GOAL_ID)),
            listOf(null, false, CommonDefects.notNull()),
    )

    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("rotationGoalId = {0}, isMobile = {1}, expectedDefect = {2}")
    fun checkInternalCampaignWithRotationGoalIdValidator(rotationGoalId: Long?,
                                                         isMobile: Boolean,
                                                         expectedDefect: Defect<Any>?) {
        val campaign = InternalDistribCampaign()
                .withRotationGoalId(rotationGoalId)
                .withIsMobile(isMobile)

        val validationResult = validator.apply(campaign)
        if (expectedDefect == null) {
            assertThat(validationResult)
                    .`is`(matchedBy(hasNoDefectsDefinitions<Defect<Any>>()))
        } else {
            val expectedError = validationError(path(field(InternalCampaignWithRotationGoalId.ROTATION_GOAL_ID)), expectedDefect)
            assertThat(validationResult)
                    .`is`(matchedBy(Matchers.hasDefectWithDefinition<Any>(expectedError)))
        }
    }

}
