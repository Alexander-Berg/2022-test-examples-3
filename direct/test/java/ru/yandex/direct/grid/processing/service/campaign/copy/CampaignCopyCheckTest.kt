package ru.yandex.direct.grid.processing.service.campaign.copy

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.testing.data.TestClients.defaultClient
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.campaign.GdCheckCopyCampaigns
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.rbac.RbacRole

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class CampaignCopyCheckTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var copyCampaignCopyMutationService: CampaignCopyMutationService

    private lateinit var operator: ClientInfo
    private lateinit var context: GridGraphQLContext

    @Before
    fun before() {
        operator = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER)
        context = ContextHelper.buildContext(operator.chiefUserInfo!!.user)
    }

    @Test
    fun `clients with same currency returns null currencyChangeTo`() {
        val clientFrom = steps.clientSteps().createDefaultClient()
        val clientTo = steps.clientSteps().createDefaultClient()

        val input = GdCheckCopyCampaigns(loginFrom = clientFrom.login, loginTo = clientTo.login)
        val result = copyCampaignCopyMutationService.checkCopyCampaigns(context, input)

        assertThat(result.currencyChangeTo).isNull()
    }

    @Test
    fun `clients with different currency returns currencyChangeTo`() {
        val clientFrom = steps.clientSteps().createClient(defaultClient().withWorkCurrency(CurrencyCode.RUB))
        val clientTo = steps.clientSteps().createClient(defaultClient().withWorkCurrency(CurrencyCode.USD))

        val input = GdCheckCopyCampaigns(loginFrom = clientFrom.login, loginTo = clientTo.login)
        val result = copyCampaignCopyMutationService.checkCopyCampaigns(context, input)

        assertThat(result.currencyChangeTo).isEqualTo(CurrencyCode.USD)
    }
}
