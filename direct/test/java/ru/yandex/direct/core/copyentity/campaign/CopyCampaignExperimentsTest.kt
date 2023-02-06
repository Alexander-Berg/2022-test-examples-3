package ru.yandex.direct.core.copyentity.campaign

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.campaign.model.CampaignExperiment
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.retargeting.model.ExperimentRetargetingConditions
import ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionService
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestFullGoals.defaultABSegmentGoal
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns.fullTextCampaign
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.metrika.client.model.request.GoalType

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyCampaignExperimentsTest : BaseCopyCampaignTest() {

    @Autowired
    private lateinit var retargetingConditionService: RetargetingConditionService

    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub

    private lateinit var experimentRetargetingConditions: ExperimentRetargetingConditions

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
        experimentRetargetingConditions = createExperiment(client)

        steps.featureSteps().addClientFeature(client.clientId!!, FeatureName.AB_SEGMENTS, true)
    }

    @Test
    @Parameters("true", "false")
    fun copyExperimentTest(experimentRetConditionsCreatingOnTextCampaignsModifyInJavaForDna: Boolean) {
        steps.featureSteps().addClientFeature(
            client.clientId!!,
            FeatureName.EXPERIMENT_RET_CONDITIONS_CREATING_ON_TEXT_CAMPAIGNS_MODIFY_IN_JAVA_FOR_DNA,
            experimentRetConditionsCreatingOnTextCampaignsModifyInJavaForDna
        )

        val campaign = steps.textCampaignSteps().createCampaign(client, fullTextCampaign()
            .withAbSegmentRetargetingConditionId(experimentRetargetingConditions.retargetingConditionId)
            .withAbSegmentStatisticRetargetingConditionId(experimentRetargetingConditions.statisticRetargetingConditionId)
        )

        val copiedCampaign: TextCampaign = copyValidCampaign(campaign)

        softly {
            assertThat(copiedCampaign.abSegmentRetargetingConditionId)
                .isEqualTo(experimentRetargetingConditions.retargetingConditionId)
            assertThat(copiedCampaign.abSegmentStatisticRetargetingConditionId)
                .isEqualTo(experimentRetargetingConditions.statisticRetargetingConditionId)
        }
    }

    private fun createExperiment(clientInfo: ClientInfo): ExperimentRetargetingConditions {
        val goal = defaultABSegmentGoal()
        metrikaClientStub.addGoals(client.uid, setOf(goal))

        val conditions = metrikaClientStub.getGoals(listOf(clientInfo.uid), GoalType.AB_SEGMENT).uidToConditions
        return retargetingConditionService.findOrCreateExperimentsRetargetingConditions(
            client.clientId!!,
            listOf(
                CampaignExperiment()
                    .withSectionIds(listOf(goal.sectionId))
                    .withAbSegmentGoalIds(listOf(goal.id))
            ),
            conditions,
            null
        ).first()
    }
}
