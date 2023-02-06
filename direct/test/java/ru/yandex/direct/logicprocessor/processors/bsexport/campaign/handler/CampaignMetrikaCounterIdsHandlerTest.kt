package ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler

import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.UInt64List
import ru.yandex.adv.direct.campaign.Campaign
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler.CampaignHandlerAssertions.assertCampaignHandledCorrectly
import javax.annotation.ParametersAreNonnullByDefault

@ParametersAreNonnullByDefault
class CampaignMetrikaCounterIdsHandlerTest {
    private val handler = CampaignMetrikaCounterIdsHandler()

    @Test
    fun `campaign without metrika counters (null)`() = assertCampaignHandledCorrectly(
            handler,
            campaign = TextCampaign()
                .withMetrikaCounters(null),
            expectedProto = Campaign.newBuilder()
                .setMetrikaCounterIds(UInt64List.getDefaultInstance())
                .buildPartial(),
    )

    @Test
    fun `campaign without metrika counters (empty list)`() = assertCampaignHandledCorrectly(
            handler,
            campaign = TextCampaign()
                .withMetrikaCounters(listOf()),
            expectedProto = Campaign.newBuilder()
                .setMetrikaCounterIds(UInt64List.getDefaultInstance())
                .buildPartial(),
    )

    @Test
    fun `campaign with a single metrika counter`() = assertCampaignHandledCorrectly(
            handler,
            campaign = SmartCampaign()
                .withMetrikaCounters(listOf(12L)),
            expectedProto = Campaign.newBuilder()
                .setMetrikaCounterIds(UInt64List.newBuilder()
                    .addValues(12L)
                    .build())
                .buildPartial(),
    )

    @Test
    fun `campaign with multiple metrika counters`() = assertCampaignHandledCorrectly(
            handler,
            campaign = DynamicCampaign()
                .withMetrikaCounters(listOf(12L, 13L)),
            expectedProto = Campaign.newBuilder()
                .setMetrikaCounterIds(UInt64List.newBuilder()
                    .addValues(12L)
                    .addValues(13L)
                    .build())
                .buildPartial(),
    )
}
