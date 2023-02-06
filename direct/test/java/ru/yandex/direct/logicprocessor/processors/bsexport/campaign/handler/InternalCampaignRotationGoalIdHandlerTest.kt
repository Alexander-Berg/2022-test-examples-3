package ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.adv.direct.campaign.Campaign
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.InternalAutobudgetCampaign
import ru.yandex.direct.core.entity.campaign.model.InternalCampaignWithRotationGoalId
import ru.yandex.direct.core.entity.campaign.model.InternalDistribCampaign
import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler.CampaignHandlerAssertions.assertCampaignHandledCorrectly
import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler.CampaignHandlerAssertions.assertProtoFilledCorrectly
import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler.InternalCampaignRotationGoalIdHandler.Companion.DEFAULT_ROTATION_GOAL_ID

class InternalCampaignRotationGoalIdHandlerTest {
    private val handler = InternalCampaignRotationGoalIdHandler()

    @Test
    fun `rotationGoalId is mapped to proto correctly`() = assertProtoFilledCorrectly(
        handler,
        resource = 42,
        expectedProto = Campaign.newBuilder()
            .setRotationGoalId(42)
            .buildPartial(),
    )

    @Test
    fun `default rotationGoalId is mapped to proto correctly`() = assertProtoFilledCorrectly(
        handler,
        resource = DEFAULT_ROTATION_GOAL_ID,
        expectedProto = Campaign.newBuilder()
            .setRotationGoalId(DEFAULT_ROTATION_GOAL_ID)
            .buildPartial()
    )

    @MethodSource
    @ParameterizedTest(name = "{0} is handled correctly")
    fun `campaign is handled correctly`(
        @Suppress("unused") campaignName: String,
        campaign: InternalCampaignWithRotationGoalId,
        expectedRotationGoalId: Long,
    ) = assertCampaignHandledCorrectly(
        handler,
        campaign = campaign,
        expectedResource = expectedRotationGoalId,
    )

    companion object {
        private const val CID = 42L
        private const val ROTATION_GOAL_ID = 43L

        fun campaignTestArguments(
            campaignName: String,
            campaign: InternalCampaignWithRotationGoalId,
            expectedRotationGoalId: Long,
        ) = Arguments.of(campaignName, campaign, expectedRotationGoalId)

        @JvmStatic
        fun `campaign is handled correctly`() = listOf(
            campaignTestArguments(
                campaignName = "InternalDistribCampaign",
                campaign = InternalDistribCampaign()
                    .withId(CID)
                    .withName("")
                    .withStatusArchived(false)
                    .withStatusShow(true)
                    .withClientId(15L)
                    .withType(CampaignType.INTERNAL_DISTRIB)
                    .withRotationGoalId(ROTATION_GOAL_ID),
                expectedRotationGoalId = ROTATION_GOAL_ID,
            ),
            campaignTestArguments(
                campaignName = "InternalAutobudgetCampaign",
                campaign = InternalAutobudgetCampaign()
                    .withId(CID)
                    .withName("")
                    .withStatusArchived(false)
                    .withStatusShow(true)
                    .withClientId(15L)
                    .withType(CampaignType.INTERNAL_AUTOBUDGET)
                    .withRotationGoalId(ROTATION_GOAL_ID),
                expectedRotationGoalId = ROTATION_GOAL_ID,
            ),
            campaignTestArguments(
                campaignName = "InternalAutobudgetCampaign with null rotationGoalId",
                campaign = InternalAutobudgetCampaign()
                    .withId(CID)
                    .withName("")
                    .withStatusArchived(false)
                    .withStatusShow(true)
                    .withClientId(15L)
                    .withType(CampaignType.INTERNAL_AUTOBUDGET)
                    .withRotationGoalId(null),
                expectedRotationGoalId = DEFAULT_ROTATION_GOAL_ID,
            )
        )
    }
}
