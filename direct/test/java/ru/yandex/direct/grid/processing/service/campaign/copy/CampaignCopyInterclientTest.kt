package ru.yandex.direct.grid.processing.service.campaign.copy

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.copyentity.model.CampaignCopyJobParams
import ru.yandex.direct.core.copyentity.model.CampaignCopyJobResult
import ru.yandex.direct.core.entity.campaign.service.validation.CopyCampaignDefects.yndFixedCurrency
import ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns.fullTextCampaign
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.dbqueue.LimitOffset
import ru.yandex.direct.dbqueue.model.DbQueueJob
import ru.yandex.direct.dbqueue.repository.DbQueueRepository
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.campaign.GdCopyCampaignsFilter
import ru.yandex.direct.grid.processing.model.campaign.GdCopyCampaignsInterclient
import ru.yandex.direct.grid.processing.model.campaign.GdCopyCampaignsSelectorType
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.validation.hasErrorsWith
import ru.yandex.direct.rbac.RbacRole
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class CampaignCopyInterclientTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var dbQueueRepository: DbQueueRepository

    @Autowired
    private lateinit var campaignCopyMutationService: CampaignCopyMutationService

    private lateinit var context: GridGraphQLContext

    private lateinit var operator: ClientInfo
    private lateinit var clientFrom: ClientInfo
    private lateinit var clientTo: ClientInfo

    @Before
    fun before() {
        operator = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER)
        clientFrom = steps.clientSteps().createDefaultClient()
        clientTo = steps.clientSteps().createDefaultClient()
        context = ContextHelper.buildContext(operator.chiefUserInfo!!.user)

        steps.dbQueueSteps().registerJobType(DbQueueJobTypes.CAMPAIGNS_COPY)
    }

    @Test
    fun `single campaign by id success`() {
        val campaign = steps.textCampaignSteps().createDefaultCampaign(clientFrom)
        val input = GdCopyCampaignsInterclient(
            loginFrom = clientFrom.login, loginTo = clientTo.login,
            filter = GdCopyCampaignsFilter(campaignIds = listOf(campaign.campaignId)),
        )

        val payload = campaignCopyMutationService.copyCampaignsInterclient(context, input)

        val jobs = findCampaignCopyJobs()
        softly {
            assertThat(payload.campaignIdsToCopy).containsExactly(campaign.campaignId)
            assertThat(payload.skippedCampaignIds).isEmpty()
            assertThat(payload.copiedCampaignId).isNull()

            assertThat(jobs).singleElement()
                .extracting { it!!.args as CampaignCopyJobParams }
                .extracting { it.campaignId }
                .isEqualTo(campaign.campaignId)
        }
    }

    @Test
    fun `single campaign by campaignType success`() {
        val campaign = steps.textCampaignSteps().createDefaultCampaign(clientFrom)
        val input = GdCopyCampaignsInterclient(
            loginFrom = clientFrom.login, loginTo = clientTo.login,
            filter = GdCopyCampaignsFilter(campaignsType = GdCopyCampaignsSelectorType.ACTIVE),
        )

        val payload = campaignCopyMutationService.copyCampaignsInterclient(context, input)

        val jobs = findCampaignCopyJobs()
        softly {
            assertThat(payload.campaignIdsToCopy).containsExactly(campaign.campaignId)
            assertThat(payload.skippedCampaignIds).isEmpty()
            assertThat(payload.copiedCampaignId).isNull()

            assertThat(jobs).singleElement()
                .extracting { it!!.args as CampaignCopyJobParams }
                .extracting { it.campaignId }
                .isEqualTo(campaign.campaignId)
        }
    }

    private fun findCampaignCopyJobs(): List<DbQueueJob<CampaignCopyJobParams, CampaignCopyJobResult>> =
        dbQueueRepository.getJobsByJobTypeAndClientIds(
            clientTo.shard,
            DbQueueJobTypes.CAMPAIGNS_COPY,
            listOf(clientFrom.clientId!!.asLong()),
            LimitOffset.maxLimited()
        )

    @Test
    fun `invalid campaign returns validation result`() {
        val campaign = steps.textCampaignSteps().createCampaign(
            clientFrom,
            fullTextCampaign()
                .withCurrency(CurrencyCode.YND_FIXED)
        )

        val input = GdCopyCampaignsInterclient(
            loginFrom = clientFrom.login, loginTo = clientTo.login,
            filter = GdCopyCampaignsFilter(campaignIds = listOf(campaign.campaignId)),
        )

        val payload = campaignCopyMutationService.copyCampaignsInterclient(context, input)

        softly {
            assertThat(payload.campaignIdsToCopy).isEmpty()
            assertThat(payload.skippedCampaignIds).containsExactly(campaign.campaignId)

            assertThat(payload)
                .extracting { it.validationResult }
                .hasErrorsWith(
                    path(
                        field(GdCopyCampaignsInterclient::filter),
                        field(GdCopyCampaignsFilter::campaignIds),
                        index(0),
                    ),
                    yndFixedCurrency(),
                )
        }
    }

    @Test
    fun `campaigns are copied partially`() {
        val invalidCampaign = steps.textCampaignSteps()
            .createCampaign(clientFrom, fullTextCampaign().withCurrency(CurrencyCode.YND_FIXED))
        val validCampaign = steps.textCampaignSteps().createDefaultCampaign(clientFrom)

        val input = GdCopyCampaignsInterclient(
            loginFrom = clientFrom.login, loginTo = clientTo.login,
            filter = GdCopyCampaignsFilter(campaignIds = listOf(
                validCampaign.campaignId,
                invalidCampaign.campaignId,
            )),
        )

        val payload = campaignCopyMutationService.copyCampaignsInterclient(context, input)

        softly {
            assertThat(payload.campaignIdsToCopy).containsExactly(validCampaign.campaignId)
            assertThat(payload.skippedCampaignIds).containsExactly(invalidCampaign.campaignId)

            assertThat(payload)
                .extracting { it.validationResult }
                .hasErrorsWith(
                    path(
                        field(GdCopyCampaignsInterclient::filter),
                        field(GdCopyCampaignsFilter::campaignIds),
                        index(1), // index of invalid campaign
                    ),
                    yndFixedCurrency(),
                )
        }
    }
}
