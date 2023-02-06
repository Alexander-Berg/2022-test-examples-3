package ru.yandex.direct.grid.processing.service.campaign.copy

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.campaign.model.CampaignSource
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns.fullDraftTextCampaign
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns.fullTextCampaign
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.campaign.GdCopyCampaignsFilter
import ru.yandex.direct.grid.processing.model.campaign.GdCopyCampaignsSelectorType

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class CampaignCopySelectCampaignsTest {

    @Autowired
    private lateinit var campaignCopyMutationService: CampaignCopyMutationService

    @Autowired
    private lateinit var steps: Steps

    private lateinit var client: ClientInfo
    private lateinit var otherClient: ClientInfo

    private lateinit var activeCampaign: CampaignInfo
    private lateinit var stoppedCampaign: CampaignInfo
    private lateinit var draftCampaign: CampaignInfo
    private lateinit var archivedCampaign: CampaignInfo
    private lateinit var uacCampaign: CampaignInfo
    private lateinit var deletedCampaign: CampaignInfo
    private lateinit var otherClientCampaign: CampaignInfo

    @Before
    fun beforeAll() {
        client = steps.clientSteps().createDefaultClient()
        otherClient = steps.clientSteps().createDefaultClient()

        activeCampaign = steps.textCampaignSteps().createDefaultCampaign(client)
        stoppedCampaign = steps.textCampaignSteps().createCampaign(
            client, fullTextCampaign()
                .withStatusShow(false)
        )
        draftCampaign = steps.textCampaignSteps().createCampaign(
            client, fullDraftTextCampaign()
                .withStatusShow(true)
        )
        archivedCampaign = steps.textCampaignSteps().createCampaign(
            client, fullTextCampaign()
                .withStatusArchived(true)
        )
        uacCampaign = steps.textCampaignSteps().createCampaign(
            client, fullTextCampaign()
                .withSource(CampaignSource.UAC)
        )
        deletedCampaign = steps.textCampaignSteps().createCampaign(
            client, fullTextCampaign()
                .withStatusEmpty(true)
        )
        otherClientCampaign = steps.textCampaignSteps().createDefaultCampaign(otherClient)
    }

    @Test
    fun `test campaignTypes=ACTIVE`() {
        val filter = GdCopyCampaignsFilter(campaignsType = GdCopyCampaignsSelectorType.ACTIVE)
        val result = campaignCopyMutationService.selectCampaignsForInterclientCopy(client.clientId!!, filter)
        assertThat(result)
            .containsExactlyInAnyOrder(
                activeCampaign.campaignId,
                stoppedCampaign.campaignId,
            )
    }

    @Test
    fun `test campaignTypes=STOPPED`() {
        val filter = GdCopyCampaignsFilter(campaignsType = GdCopyCampaignsSelectorType.STOPPED)
        val result = campaignCopyMutationService.selectCampaignsForInterclientCopy(client.clientId!!, filter)
        assertThat(result)
            .containsExactlyInAnyOrder(
                stoppedCampaign.campaignId,
            )
    }

    @Test
    fun `test campaignTypes=DRAFT`() {
        val filter = GdCopyCampaignsFilter(campaignsType = GdCopyCampaignsSelectorType.DRAFT)
        val result = campaignCopyMutationService.selectCampaignsForInterclientCopy(client.clientId!!, filter)
        assertThat(result)
            .containsExactlyInAnyOrder(
                draftCampaign.campaignId,
            )
    }

    @Test
    fun `test campaignTypes=ARCHIVED`() {
        listOf<String>().sorted()
        val filter = GdCopyCampaignsFilter(campaignsType = GdCopyCampaignsSelectorType.ARCHIVED)
        val result = campaignCopyMutationService.selectCampaignsForInterclientCopy(client.clientId!!, filter)
        assertThat(result)
            .containsExactlyInAnyOrder(
                archivedCampaign.campaignId,
            )
    }

    @Test
    fun `test campaignTypes=ALL`() {
        val filter = GdCopyCampaignsFilter(campaignsType = GdCopyCampaignsSelectorType.ALL)
        val result = campaignCopyMutationService.selectCampaignsForInterclientCopy(client.clientId!!, filter)
        assertThat(result)
            .containsExactlyInAnyOrder(
                activeCampaign.campaignId,
                stoppedCampaign.campaignId,
                draftCampaign.campaignId,
                archivedCampaign.campaignId,
            )
    }

    @Test
    fun `test campaignIds`() {
        val filter = GdCopyCampaignsFilter(campaignIds = listOf(activeCampaign.campaignId, draftCampaign.campaignId))
        val result = campaignCopyMutationService.selectCampaignsForInterclientCopy(client.clientId!!, filter)
        assertThat(result)
            .containsExactlyInAnyOrder(
                activeCampaign.campaignId,
                draftCampaign.campaignId,
            )
    }

    @Test
    fun `test campaignIds filtered`() {
        val filter = GdCopyCampaignsFilter(campaignIds = listOf(
            activeCampaign.campaignId,
            draftCampaign.campaignId,
            uacCampaign.campaignId,
            deletedCampaign.campaignId,
            otherClientCampaign.campaignId,
        ))
        val result = campaignCopyMutationService.selectCampaignsForInterclientCopy(client.clientId!!, filter)
        assertThat(result)
            .containsExactlyInAnyOrder(
                activeCampaign.campaignId,
                draftCampaign.campaignId,
            )
    }

}
