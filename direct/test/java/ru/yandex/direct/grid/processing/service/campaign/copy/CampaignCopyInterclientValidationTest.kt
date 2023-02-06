package ru.yandex.direct.grid.processing.service.campaign.copy

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.campaign.service.validation.CopyCampaignDefects.selectorNotAllowed
import ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.campaign.GdCopyCampaignsFilter
import ru.yandex.direct.grid.processing.model.campaign.GdCopyCampaignsInterclient
import ru.yandex.direct.grid.processing.model.campaign.GdCopyCampaignsSelectorType
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.validation.asGridValidationException
import ru.yandex.direct.grid.processing.util.validation.extractingValidationResult
import ru.yandex.direct.grid.processing.util.validation.hasErrorsWith
import ru.yandex.direct.rbac.RbacRole
import ru.yandex.direct.rbac.RbacRole.PLACER
import ru.yandex.direct.test.utils.randomPositiveLong
import ru.yandex.direct.validation.defect.CommonDefects.invalidValue
import ru.yandex.direct.validation.defect.CommonDefects.objectNotFound
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class CampaignCopyInterclientValidationTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var campaignCopyMutationService: CampaignCopyMutationService

    private lateinit var context: GridGraphQLContext

    private lateinit var operator: ClientInfo
    private lateinit var clientFrom: ClientInfo
    private lateinit var clientTo: ClientInfo

    private lateinit var campaign: CampaignInfo

    @Before
    fun before() {
        operator = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER)
        clientFrom = steps.clientSteps().createDefaultClient()
        clientTo = steps.clientSteps().createDefaultClient()
        context = ContextHelper.buildContext(operator.chiefUserInfo!!.user)

        campaign = steps.textCampaignSteps().createDefaultCampaign(clientFrom)

        steps.dbQueueSteps().registerJobType(DbQueueJobTypes.CAMPAIGNS_COPY)
    }

    @Test
    fun `filter with both campaignIds and campaignsType throws validation error`() {
        val input = GdCopyCampaignsInterclient(
            loginFrom = clientFrom.login, loginTo = clientTo.login,
            filter = GdCopyCampaignsFilter(
                campaignIds = listOf(campaign.campaignId),
                campaignsType = GdCopyCampaignsSelectorType.ALL,
            ),
        )

        assertThatThrownBy { campaignCopyMutationService.copyCampaignsInterclient(context, input) }
            .asGridValidationException().extractingValidationResult()
            .hasErrorsWith(path(field(GdCopyCampaignsInterclient::filter)), invalidValue())
    }

    @Test
    fun `placer using campaignsType throws validation error`() {
        val operator = steps.clientSteps().createDefaultClientWithRole(PLACER)
        val context = ContextHelper.buildContext(operator.chiefUserInfo!!.user)

        val input = GdCopyCampaignsInterclient(
            loginFrom = clientFrom.login, loginTo = clientTo.login,
            filter = GdCopyCampaignsFilter(campaignsType = GdCopyCampaignsSelectorType.ALL),
        )

        assertThatThrownBy { campaignCopyMutationService.copyCampaignsInterclient(context, input) }
            .asGridValidationException().extractingValidationResult()
            .hasErrorsWith(path(field(GdCopyCampaignsInterclient::filter)), selectorNotAllowed())
    }

    @Test
    fun `non-existent campaign id throws validation error`() {
        val input = GdCopyCampaignsInterclient(
            loginFrom = clientFrom.login, loginTo = clientTo.login,
            filter = GdCopyCampaignsFilter(campaignIds = listOf(randomPositiveLong())),
        )

        assertThatThrownBy { campaignCopyMutationService.copyCampaignsInterclient(context, input) }
            .asGridValidationException().extractingValidationResult()
            .hasErrorsWith(
                path(
                    field(GdCopyCampaignsInterclient::filter),
                    field(GdCopyCampaignsFilter::campaignIds),
                    index(0),
                ),
                objectNotFound(),
            )
    }
}
