package ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler

import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.campaign.Campaign

class CampaignWithBrandLiftHandlerTest {
    private val handler = CampaignWithBrandLiftHandler()

    @Test
    fun `resource is mapped to proto correctly`() = CampaignHandlerAssertions.assertProtoFilledCorrectly(
        handler,
        resource = true,
        expectedProto = Campaign.newBuilder()
            .setIsCpmGlobalAbSegment(true)
            .buildPartial()
    )
}
