package ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.campaign.Campaign
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository
import ru.yandex.direct.currency.CurrencyCode
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
class CampaignWidgetPartnerIdHandlerTest {
    private val CID_1 = 1L
    private val CID_2 = 123L
    private val WIDGET_PARTNER_ID_1 = 10L
    private val WIDGET_PARTNER_ID_2 = 1230L
    private val campaignRepository = mock<CampaignRepository> {
        on(it.getWidgetPartnerIdsByCids(any(), any())) doReturn mapOf(
            CID_1 to WIDGET_PARTNER_ID_1,
            CID_2 to WIDGET_PARTNER_ID_2,
        )
    }
    private val handler = CampaignWidgetPartnerIdHandler(campaignRepository)

    @Test
    fun `text campaign`() {
        val expectedProto = Campaign.newBuilder()
            .setWidgetPartnerId(WIDGET_PARTNER_ID_1)
            .buildPartial()

        CampaignHandlerAssertions.assertCampaignHandledCorrectly(
            handler,
            campaign = TextCampaign()
                .withId(1)
                .withCurrency(CurrencyCode.RUB)
                .withName("")
                .withStatusArchived(false)
                .withStatusShow(true)
                .withClientId(15L)
                .withType(CampaignType.TEXT)
                .withUid(CID_1),
            expectedProto = expectedProto,
        )
    }
}
