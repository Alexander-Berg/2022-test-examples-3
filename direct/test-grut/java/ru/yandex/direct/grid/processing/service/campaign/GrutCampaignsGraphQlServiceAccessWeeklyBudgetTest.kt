package ru.yandex.direct.grid.processing.service.campaign

import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.grid.model.campaign.GdCampaignAction
import ru.yandex.direct.grid.processing.configuration.GrutGridProcessingTest
import ru.yandex.direct.grid.processing.util.KtGraphQLTestExecutor

@GrutGridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class GrutCampaignsGraphQlServiceAccessWeeklyBudgetTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var ktGraphQLTestExecutor: KtGraphQLTestExecutor

    private lateinit var clientInfo: ClientInfo

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        grutSteps.createClient(clientInfo)
        val user = clientInfo.chiefUserInfo!!.user!!
        ktGraphQLTestExecutor.withDefaultGraphQLContext(user)
    }

    @Test
    fun testWeeklyBudgetActions_whenUCCampaign_success() {
        val cid = grutSteps.createTextCampaign(clientInfo = clientInfo)

        val campaignsContext = ktGraphQLTestExecutor.getTextCampaignsWithStatsAndAccess(uid = clientInfo.uid,
            campaignIds = setOf(cid));
        val actions = campaignsContext.rowset[0].access.actions
        Assertions.assertThat(actions).contains(GdCampaignAction.EDIT_WEEKLY_BUDGET)
        Assertions.assertThat(actions).doesNotContain(GdCampaignAction.DISABLE_WEEKLY_BUDGET)
    }
}
