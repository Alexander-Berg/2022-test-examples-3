package ru.yandex.direct.grid.processing.service.campaign

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.PromoExtensionInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.exception.GridValidationException
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayloadItem
import ru.yandex.direct.grid.processing.model.promoextension.GdUpdateCampaignsPromoExtension
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.test.utils.checkNotNull
import ru.yandex.direct.test.utils.checkNull
import ru.yandex.direct.test.utils.checkSize
import ru.yandex.direct.validation.result.DefectIds


@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class CampaignMutationServiceUpdatePromoExtensionChangesTest {
    @Autowired
    lateinit var steps: Steps

    @Autowired
    private lateinit var campaignMutationService: CampaignMutationService

    private lateinit var client: ClientInfo
    private lateinit var textCampaignInfo: CampaignInfo
    private lateinit var promoExtensionInfo: PromoExtensionInfo
    private lateinit var context: GridGraphQLContext

    @Before
    fun setUp() {
        client = steps.clientSteps().createDefaultClient()
        promoExtensionInfo = steps.promoExtensionSteps().createDefaultPromoExtension(client)
        textCampaignInfo = steps.campaignSteps().createActiveTextCampaign(client)
        context = GridGraphQLContext(client.chiefUserInfo!!.user)
    }

    @Test
    fun updateTextCampaignPromoExtensionSuccess() {
        val input = GdUpdateCampaignsPromoExtension(listOf(textCampaignInfo.campaignId), promoExtensionInfo.promoExtensionId)
        val result = campaignMutationService.updateCampaignPromoExtensionChanges(context, input)
        result.validationResult.checkNull()
        result.updatedCampaigns.checkSize(1)
        result.updatedCampaigns[0].checkEquals(GdUpdateCampaignPayloadItem().withId(textCampaignInfo.campaignId))
    }


    @Test(expected = GridValidationException::class)
    fun updateTextAndCpmBannerCampaignPromoExtensionNonUpdated() {
        val cpmCampaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign(client)
        val input = GdUpdateCampaignsPromoExtension(
            listOf(textCampaignInfo.campaignId, cpmCampaignInfo.campaignId),
            promoExtensionInfo.promoExtensionId)
        campaignMutationService.updateCampaignPromoExtensionChanges(context, input)
    }

    @Test
    fun updateCampaignWithAnotherClientPromoExtensionValidationError() {
        val anotherClient = steps.clientSteps().createDefaultClient()
        val anotherPromoactionInfo = steps.promoExtensionSteps().createDefaultPromoExtension(anotherClient)
        val input = GdUpdateCampaignsPromoExtension(
            listOf(textCampaignInfo.campaignId),
            anotherPromoactionInfo.promoExtensionId)
        val result = campaignMutationService.updateCampaignPromoExtensionChanges(context, input)
        result.validationResult.checkNotNull()
        result.validationResult.errors.checkSize(1)
        result.validationResult.errors[0].code.checkEquals(DefectIds.OBJECT_NOT_FOUND.code)
        result.updatedCampaigns.checkSize(1)
        result.updatedCampaigns[0].checkNull()
    }
}
