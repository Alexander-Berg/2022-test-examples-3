package ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler

import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.campaign.Campaign

class CampaignWithWwManagedOrderFlagHandlerTest {

    private val handler = CampaignWithWwManagedOrderFlagHandler()

    @Test
    fun `resource is mapped to proto correctly`() = CampaignHandlerAssertions.assertProtoFilledCorrectly(
        handler,
        resource = true,
        expectedProto = Campaign.newBuilder()
            .setIsWwManagedOrder(true)
            .buildPartial()
    )
}
