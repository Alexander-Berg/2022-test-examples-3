package ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler

import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.campaign.Campaign
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler.CampaignHandlerAssertions.assertCampaignHandledCorrectly
import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler.CampaignHandlerAssertions.assertProtoFilledCorrectly

class CampaignAllowedOnAdultContentHandlerTest {
    private val handler = CampaignAllowedOnAdultContentHandler()

    @Test
    fun `resource is mapped to proto correctly`() {
        assertProtoFilledCorrectly(
            handler,
            resource = true,
            expectedProto = Campaign
                .newBuilder()
                .setAllowedOnAdultContent(true)
                .buildPartial()
        )
    }

    @Test
    fun `campaign is processed correctly`() {
        val campaign = TextCampaign()
            .withId(42L)
            .withName("")
            .withStatusArchived(false)
            .withStatusShow(true)
            .withClientId(15L)
            .withType(CampaignType.TEXT)
            .withIsAllowedOnAdultContent(true)

        assertCampaignHandledCorrectly(
            handler,
            campaign = campaign,
            expectedProto = Campaign.newBuilder().setAllowedOnAdultContent(true).buildPartial(),
        )
    }
}
