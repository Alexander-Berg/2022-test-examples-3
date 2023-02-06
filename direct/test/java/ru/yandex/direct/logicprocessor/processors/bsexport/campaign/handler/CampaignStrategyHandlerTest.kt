package ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.UInt32List
import ru.yandex.adv.direct.campaign.Campaign
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign
import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.container.CampaignStrategy
import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler.CampaignHandlerAssertions.assertCampaignHandledCorrectly
import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler.CampaignHandlerAssertions.assertProtoFilledCorrectly
import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.utils.DisallowedTargetTypesCalculator

internal class CampaignStrategyHandlerTest {
    private val disallowedTargetTypesCalculator = mock<DisallowedTargetTypesCalculator> {
        on(it.calculate(any())) doReturn DISALLOWED_TARGET_TYPES
    }
    private val STRATEGY_ID = 1L;
    private val handler = CampaignStrategyHandler(disallowedTargetTypesCalculator)

    @Test
    fun `resource is mapped to proto correctly`() = assertProtoFilledCorrectly(
        handler,
        resource = CampaignStrategy(null, DISALLOWED_TARGET_TYPES),
        expectedProto = Campaign.newBuilder()
            .setDisallowedTargetTypes(
                UInt32List.newBuilder().addAllValues(DISALLOWED_TARGET_TYPES)
            )
            .buildPartial()
    )

    @Test
    fun `resourse with strategy_id is mapped to proto correctly`() = assertProtoFilledCorrectly(
        handler,
        resource = CampaignStrategy(STRATEGY_ID, DISALLOWED_TARGET_TYPES),
        expectedProto = Campaign.newBuilder()
            .setDisallowedTargetTypes(
                UInt32List.newBuilder().addAllValues(DISALLOWED_TARGET_TYPES)
            )
            .setStrategyId(STRATEGY_ID)
            .buildPartial()
    )

    @Test
    fun `campaign is processed correctly`() = assertCampaignHandledCorrectly(
        handler,
        campaign = DynamicCampaign()
            .withId(1L)
            .withClientId(15L)
            .withType(CampaignType.DYNAMIC),
        expectedResource = CampaignStrategy(null, DISALLOWED_TARGET_TYPES)
    )

    @Test
    fun `campaign with strategy_id is processed correctly`() = assertCampaignHandledCorrectly(
        handler,
        campaign = DynamicCampaign()
            .withId(1L)
            .withClientId(15L)
            .withStrategyId(STRATEGY_ID)
            .withType(CampaignType.DYNAMIC),
        expectedResource = CampaignStrategy(STRATEGY_ID, DISALLOWED_TARGET_TYPES)
    )

    companion object {
        private val DISALLOWED_TARGET_TYPES = listOf(1, 2, 3)
    }
}
