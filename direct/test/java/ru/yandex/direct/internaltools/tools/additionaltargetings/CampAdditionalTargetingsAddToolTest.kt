package ru.yandex.direct.internaltools.tools.additionaltargetings

import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.internaltools.configuration.InternalToolsTest
import ru.yandex.direct.internaltools.tools.additionaltargetings.model.CampAdditionalTargetingsAddParameters
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.result.DefectIds

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner::class)
class CampAdditionalTargetingsAddToolTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var tool: CampAdditionalTargetingsAddTool

    fun CampAdditionalTargetingsAddTool.validate(paramBuilder: CampAdditionalTargetingsAddParameters.() -> Unit) =
        this.validate(CampAdditionalTargetingsAddParameters().apply(paramBuilder))

    var campaignId: Long = 0L

    @Before
    fun before() {
        val defaultUser = steps.userSteps().createDefaultUser()
        campaignId = steps.campaignSteps().createActiveTextCampaign(defaultUser.clientInfo).getCampaignId()
    }

    @Test
    fun checkValidation_campaignIdsCorrect() {
        assertThat(
            tool.validate { campaignIds = "$campaignId" },
            Matchers.hasNoErrors()
        )
    }

    @Test
    fun checkValidation_campaignIdsNotFound() {
        assertThat(
            tool.validate { campaignIds = "${campaignId + 1L}" },
            Matchers.hasDefectDefinitionWith(Matchers.validationError(DefectIds.OBJECT_NOT_FOUND))
        )
    }

    @Test
    fun checkValidation_targetingNotJson() {
        assertThat(
            tool.validate { campaignIds = "$campaignId"; targeting = "qwerty" },
            Matchers.hasDefectDefinitionWith(Matchers.validationError(DefectIds.INVALID_FORMAT))
        )
    }

    @Test
    fun checkValidation_targetingIsEmptyJson() {
        assertThat(
            tool.validate { campaignIds = "$campaignId"; targeting = "{}" },
            Matchers.hasDefectDefinitionWith(Matchers.validationError(DefectIds.INVALID_FORMAT))
        )
    }

    @Test
    fun checkValidation_targetingIsEmptyAndJson() {
        assertThat(
            tool.validate { campaignIds = "$campaignId"; targeting = """{"and": []}""" },
            Matchers.hasDefectDefinitionWith(Matchers.validationError(DefectIds.INVALID_FORMAT))
        )
    }

    @Test
    fun checkValidation_targetingIsEmptyOrJson() {
        assertThat(
            tool.validate { campaignIds = "$campaignId"; targeting = """{"and":[{"or":[{}]}]}""" },
            Matchers.hasDefectDefinitionWith(Matchers.validationError(DefectIds.INVALID_FORMAT))
        )
    }

    @Test
    fun checkValidation_targetingIncorrectJson() {
        assertThat(
            tool.validate { campaignIds = "$campaignId"; targeting = "{\"and\":123}" },
            Matchers.hasDefectDefinitionWith(Matchers.validationError(DefectIds.INVALID_FORMAT))
        )
    }

    @Test
    fun checkValidation_targetingCorrectJson() {
        assertThat(
            tool.validate {
                campaignIds = "$campaignId"; targeting =
                """{"and": [{"or": [{"keyword": 1,"operation": 2,"value": "val"}] }] }"""
            },
            Matchers.hasNoErrors()
        )
    }
}
