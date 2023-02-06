package ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler

import org.assertj.core.api.Assertions.assertThat
import ru.yandex.adv.direct.campaign.Campaign
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.container.CampaignWithBuilder

object CampaignHandlerAssertions {
    fun <T> assertProtoFilledCorrectly(
        handler: TypedCampaignResourceHandler<T, *>,
        resource: T,
        expectedProto: Campaign,
    ) {
        val actualProto = resourceToProto(handler, resource)
        assertThat(actualProto).isEqualTo(expectedProto)
    }

    fun <T> assertCampaignHandledCorrectly(
        handler: TypedCampaignResourceHandler<T, *>,
        campaign: BaseCampaign,
        expectedResource: T,
    ) {
        val expectedProto = resourceToProto(handler, expectedResource)
        assertCampaignHandledCorrectly(handler, campaign, expectedProto)
    }

    fun <T> assertCampaignHandledCorrectly(
        handler: ICampaignResourceHandler<T>,
        campaign: BaseCampaign,
        expectedProto: Campaign,
    ) {
        val campaignWithBuilder = CampaignWithBuilder(campaign, Campaign.newBuilder())
        handler.handle(1, mapOf(campaign.id to campaignWithBuilder))

        // ignore ContextId
        if (!expectedProto.hasContextId())
            campaignWithBuilder.builder.clearContextId()
        else campaignWithBuilder.builder.contextId = expectedProto.contextId

        val actualProto = campaignWithBuilder.builder.buildPartial()
        assertThat(actualProto).isEqualTo(expectedProto)
    }

    private fun <T> resourceToProto(handler: TypedCampaignResourceHandler<T, *>, resource: T) =
        Campaign.newBuilder().also { handler.fillExportObject(resource, it) }.buildPartial()
}
