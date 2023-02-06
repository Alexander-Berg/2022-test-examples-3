package ru.yandex.direct.grid.processing.service.campaign.copy

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.copyentity.model.CampaignCopyJobParams
import ru.yandex.direct.core.copyentity.model.CampaignCopyJobResult
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.campaignNotFound
import ru.yandex.direct.core.entity.campaign.service.validation.CopyCampaignDefects
import ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.dbqueue.LimitOffset
import ru.yandex.direct.dbqueue.model.DbQueueJob
import ru.yandex.direct.dbqueue.repository.DbQueueRepository
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.campaign.GdCopyCampaigns
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.validation.asGridValidationException
import ru.yandex.direct.grid.processing.util.validation.extractingValidationResult
import ru.yandex.direct.grid.processing.util.validation.hasErrorsWith
import ru.yandex.direct.rbac.RbacRole
import ru.yandex.direct.validation.defect.CollectionDefects.notEmptyCollection
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class CampaignCopyTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var dbQueueRepository: DbQueueRepository

    @Autowired
    private lateinit var campaignCopyMutationService: CampaignCopyMutationService

    private lateinit var operator: ClientInfo
    private lateinit var client: ClientInfo
    private lateinit var context: GridGraphQLContext

    @Before
    fun before() {
        operator = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER)
        client = steps.clientSteps().createDefaultClient()
        context = ContextHelper.buildContext(operator.chiefUserInfo?.user!!, client.chiefUserInfo?.user!!)

        steps.dbQueueSteps().registerJobType(DbQueueJobTypes.CAMPAIGNS_COPY)
    }

    @Test
    fun `copy single campaign`() {
        val campaign = steps.textCampaignSteps().createDefaultCampaign(client)
        val input = GdCopyCampaigns(campaignIds = listOf(campaign.campaignId))

        val payload = campaignCopyMutationService.copyCampaigns(context, input)

        val jobs = findCampaignCopyJobs()
        softly {
            assertThat(payload.campaignIdsToCopy).containsExactly(campaign.campaignId)
            assertThat(payload.validationResult).isNull()

            assertThat(jobs).singleElement()
                .extracting { it.args.campaignId }
                .isEqualTo(campaign.campaignId)
        }
    }

    private fun findCampaignCopyJobs(): List<DbQueueJob<CampaignCopyJobParams, CampaignCopyJobResult>> =
        dbQueueRepository.getJobsByJobTypeAndClientIds(
            client.shard,
            DbQueueJobTypes.CAMPAIGNS_COPY,
            listOf(client.clientId!!.asLong()),
            LimitOffset.maxLimited()
        )

    @Test
    fun `copy multiple campaigns`() {
        val campaigns = (0 until 2).map {
            steps.textCampaignSteps().createDefaultCampaign(client)
        }
        val campaignIds = campaigns.map { it.id }
        val input = GdCopyCampaigns(campaignIds = campaignIds)

        val payload = campaignCopyMutationService.copyCampaigns(context, input)

        softly {
            assertThat(payload.campaignIdsToCopy).containsExactlyInAnyOrderElementsOf(campaignIds)
            assertThat(payload.validationResult).isNull()
        }
    }

    @Test
    fun `copy invalid input throws validation error`() {
        val input = GdCopyCampaigns(campaignIds = listOf())

        assertThatThrownBy { campaignCopyMutationService.copyCampaigns(context, input) }
            .asGridValidationException().extractingValidationResult()
            .hasErrorsWith(path(field(GdCopyCampaigns::campaignIds)), notEmptyCollection())
    }

    @Test
    fun `copy without access returns validation result`() {
        val otherClient = steps.clientSteps().createDefaultClient()
        val campaign = steps.textCampaignSteps().createDefaultCampaign(otherClient)
        val input = GdCopyCampaigns(campaignIds = listOf(campaign.campaignId))

        val payload = campaignCopyMutationService.copyCampaigns(context, input)

        softly {
            assertThat(payload.campaignIdsToCopy).isEmpty()
            assertThat(payload.validationResult)
                .hasErrorsWith(
                    path(field(GdCopyCampaigns::campaignIds), index(0)),
                    campaignNotFound(),
                )
        }
    }

    @Test
    fun `one valid and one invalid campaign produces partial copy`() {
        val invalidCampaign = steps.textCampaignSteps()
            .createCampaign(client, TestTextCampaigns.fullTextCampaign().withCurrency(CurrencyCode.YND_FIXED))
        val validCampaign = steps.textCampaignSteps().createDefaultCampaign(client)
        val input = GdCopyCampaigns(campaignIds = listOf(validCampaign.campaignId, invalidCampaign.campaignId))

        val payload = campaignCopyMutationService.copyCampaigns(context, input)

        softly {
            assertThat(payload.campaignIdsToCopy).containsExactly(validCampaign.campaignId)
            assertThat(payload.validationResult)
                .hasErrorsWith(
                    path(field(GdCopyCampaigns::campaignIds), index(1)),
                    CopyCampaignDefects.yndFixedCurrency(),
                )
        }
    }

    @Test
    fun `copy campaign two times - second copy attempt fails`() {
        val campaign = steps.textCampaignSteps().createDefaultCampaign(client)
        val input = GdCopyCampaigns(campaignIds = listOf(campaign.campaignId))

        val firstPayload = campaignCopyMutationService.copyCampaigns(context, input)
        assertThat(firstPayload.campaignIdsToCopy).containsExactly(campaign.campaignId)

        val secondPayload = campaignCopyMutationService.copyCampaigns(context, input)
        assertThat(secondPayload.validationResult)
            .hasErrorsWith(
                path(field(GdCopyCampaigns::campaignIds), index(0)),
                CopyCampaignDefects.alreadyInCopyQueue(),
            )
    }
}
