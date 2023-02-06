package ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler

import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.StringList
import ru.yandex.adv.direct.campaign.Campaign
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign
import ru.yandex.direct.ess.logicobjects.bsexport.campaing.BsExportCampaignObject
import ru.yandex.direct.ess.logicobjects.bsexport.campaing.CampaignResourceType
import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler.CampaignHandlerAssertions.assertCampaignHandledCorrectly
import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler.CampaignHandlerAssertions.assertProtoFilledCorrectly

internal class CampaignMinusPhrasesHandlerTest {
    private val handler = CampaignMinusPhrasesHandler()

    @Test
    fun `minus phrases are mapped to proto correctly`() = assertProtoFilledCorrectly(
        handler,
        resource = MINUS_PHRASES,
        expectedProto = Campaign.newBuilder()
            .setMinusPhrases(StringList.newBuilder().addAllValues(MINUS_PHRASES))
            .buildPartial()
    )

    @Test
    fun `empty minus phrases are mapped to proto correctly`() = assertProtoFilledCorrectly(
        handler,
        resource = listOf(),
        expectedProto = Campaign.newBuilder()
            .setMinusPhrases(StringList.getDefaultInstance())
            .buildPartial()
    )

    @Test
    fun `campaign with minus phrases is handled correctly`() = assertCampaignHandledCorrectly(
        handler,
        campaign = DynamicCampaign()
            .withId(1)
            .withName("")
            .withStatusArchived(false)
            .withStatusShow(true)
            .withClientId(15L)
            .withType(CampaignType.DYNAMIC)
            .withMinusKeywords(MINUS_PHRASES),
        expectedResource = MINUS_PHRASES,
    )

    companion object {
        private val MINUS_PHRASES = listOf("!своими", "асклепий", "бесплатно", "диплом")
        private val LOGIC_OBJECT = BsExportCampaignObject.Builder()
            .setCampaignResourceType(CampaignResourceType.CAMPAIGN_MINUS_PHRASES)
            .setCid(DynamicCampaign()
                .withId(1)
                .withName("")
                .withStatusArchived(false)
                .withStatusShow(true)
                .withClientId(15L)
                .withType(CampaignType.DYNAMIC)
                .withMinusKeywords(MINUS_PHRASES).id)
            .build()
    }
}
