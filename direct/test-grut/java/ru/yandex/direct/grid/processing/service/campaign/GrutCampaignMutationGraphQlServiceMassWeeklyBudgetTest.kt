package ru.yandex.direct.grid.processing.service.campaign

import org.assertj.core.api.Assertions
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignService
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AverageBidStrategy
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.DayBudget
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.DayBudgetShowMode
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.grid.processing.configuration.GrutGridProcessingTest
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayloadItem
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignsWeeklyBudget
import ru.yandex.direct.grid.processing.util.KtGraphQLTestExecutor
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.grid.processing.util.UserHelper
import ru.yandex.direct.test.utils.assertj.Conditions
import java.math.BigDecimal

@GrutGridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class GrutCampaignMutationGraphQlServiceMassWeeklyBudgetTest {

    private val MAX_WEEKLY_BUDGET = Currencies.getCurrency(CurrencyCode.RUB).maxAutobudget

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var grutUacCampaignService: GrutUacCampaignService

    @Autowired
    private lateinit var ktGraphQLTestExecutor: KtGraphQLTestExecutor

    private lateinit var operator: User

    private lateinit var clientInfo: ClientInfo

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        grutSteps.createClient(clientInfo)
        operator = UserHelper.getUser(clientInfo.client!!)
        TestAuthHelper.setDirectAuthentication(operator)
    }

    @Test
    fun testMassSetWeeklyBudget_UC_successSingle() {
        val expectedStrategy = createAverageBidStrategyWithoutDayBudget()
        val cid = grutSteps.createTextCampaign(clientInfo = clientInfo, strategy = expectedStrategy)
        val input = createRequest(MAX_WEEKLY_BUDGET, listOf(cid))
        val payload = ktGraphQLTestExecutor.withDefaultGraphQLContext(operator).updateWeeklyBudgetRequest(input)
        val expectedPayloadItem = GdUpdateCampaignPayloadItem().withId(cid)
        val expectedPayload = GdUpdateCampaignPayload().withUpdatedCampaigns(listOf(expectedPayloadItem))
        val actualCampaign = campaignTypedRepository.getTypedCampaigns(clientInfo.shard, listOf(cid))[0] as TextCampaign
        val actualGrutCampaign = grutUacCampaignService.getCampaignById(cid.toIdString())

        SoftAssertions.assertSoftly {
            it.assertThat(payload).`is`(Conditions.matchedBy(BeanDifferMatcher.beanDiffer(expectedPayload)))
            //mysql
            it.assertThat(actualCampaign.strategy.strategyData.sum).isEqualByComparingTo(MAX_WEEKLY_BUDGET)
            //grut
            it.assertThat(actualGrutCampaign!!.weekLimit).isEqualByComparingTo(MAX_WEEKLY_BUDGET)
        }
    }

    @Test
    fun testMassSetWeeklyBudget_UAC_successSingle() {
        val expectedStrategy = createAverageBidStrategyWithoutDayBudget()
        val cid = grutSteps.createMobileAppCampaign(clientInfo = clientInfo, strategy = expectedStrategy,
            createInDirect = true)
        val input = createRequest(MAX_WEEKLY_BUDGET, listOf(cid))
        val payload = ktGraphQLTestExecutor.withDefaultGraphQLContext(operator).updateWeeklyBudgetRequest(input)
        val expectedPayloadItem = GdUpdateCampaignPayloadItem().withId(cid)
        val expectedPayload = GdUpdateCampaignPayload().withUpdatedCampaigns(listOf(expectedPayloadItem))
        val actualCampaign = campaignTypedRepository.getTypedCampaigns(clientInfo.shard,
            listOf(cid))[0] as MobileContentCampaign
        val actualGrutCampaign = grutUacCampaignService.getCampaignById(cid.toIdString())

        SoftAssertions.assertSoftly {
            it.assertThat(payload).`is`(Conditions.matchedBy(BeanDifferMatcher.beanDiffer(expectedPayload)))
            //mysql
            it.assertThat(actualCampaign.strategy.strategyData.sum).isEqualByComparingTo(MAX_WEEKLY_BUDGET)
            //grut
            it.assertThat(actualGrutCampaign!!.weekLimit).isEqualByComparingTo(MAX_WEEKLY_BUDGET)
        }
    }

    @Test
    fun testMassSetWeeklyBudget_UAC_successMulti() {
        val expectedStrategy = createAverageBidStrategyWithoutDayBudget()
        val textCampaignCid = grutSteps.createTextCampaign(clientInfo = clientInfo, strategy = expectedStrategy)
        val mobileAppCampaignCid = grutSteps.createMobileAppCampaign(clientInfo = clientInfo,
            strategy = expectedStrategy, createInDirect = true)
        val input = createRequest(MAX_WEEKLY_BUDGET, listOf(textCampaignCid, mobileAppCampaignCid))
        val payload = ktGraphQLTestExecutor.withDefaultGraphQLContext(operator).updateWeeklyBudgetRequest(input)

        val textCampaignPayloadItem = GdUpdateCampaignPayloadItem().withId(textCampaignCid)
        val mobileAppCampaignPayloadItem = GdUpdateCampaignPayloadItem().withId(mobileAppCampaignCid)
        val expectedPayload = GdUpdateCampaignPayload().withUpdatedCampaigns(listOf(textCampaignPayloadItem,
            mobileAppCampaignPayloadItem))

        val actualTextCampaign = campaignTypedRepository.getTypedCampaigns(clientInfo.shard, listOf(textCampaignCid))[0] as TextCampaign
        val actualTextGrutCampaign = grutUacCampaignService.getCampaignById(textCampaignCid.toIdString())
        val actualMobileAppCampaign = campaignTypedRepository.getTypedCampaigns(clientInfo.shard,
            listOf(mobileAppCampaignCid))[0] as MobileContentCampaign
        val actualMobileAppGrutCampaign = grutUacCampaignService.getCampaignById(mobileAppCampaignCid.toIdString())

        SoftAssertions.assertSoftly {
            it.assertThat(payload).`is`(Conditions.matchedBy(BeanDifferMatcher.beanDiffer(expectedPayload)))
            //mysql
            it.assertThat(actualTextCampaign.strategy.strategyData.sum).isEqualByComparingTo(MAX_WEEKLY_BUDGET)
            it.assertThat(actualMobileAppCampaign.strategy.strategyData.sum).isEqualByComparingTo(MAX_WEEKLY_BUDGET)
            //grut
            it.assertThat(actualTextGrutCampaign!!.weekLimit).isEqualByComparingTo(MAX_WEEKLY_BUDGET)
            it.assertThat(actualMobileAppGrutCampaign!!.weekLimit).isEqualByComparingTo(MAX_WEEKLY_BUDGET)
        }
    }

    @Test
    fun testMassDisableWeeklyBudget_UC_validationError() {
        val cid = grutSteps.createTextCampaign(clientInfo = clientInfo,
            strategy = createAverageBidStrategyWithoutDayBudget())
        val input = createRequest(weeklyBudget = null, cids = listOf(cid))
        val payload = ktGraphQLTestExecutor.withDefaultGraphQLContext(operator).updateWeeklyBudgetRequest(input)
        Assertions.assertThat(payload.validationResult.errors).hasSize(1)
    }

    private fun createRequest(weeklyBudget: BigDecimal?, cids: List<Long>): GdUpdateCampaignsWeeklyBudget {
        return GdUpdateCampaignsWeeklyBudget()
            .withCampaignIds(cids)
            .withWeeklyBudget(weeklyBudget)
    }

    private fun createAverageBidStrategyWithoutDayBudget(): AverageBidStrategy {
        return AverageBidStrategy()
            .withAverageBid(BigDecimal(15))
            .withMaxWeekSum(BigDecimal(2795))
            .withDayBudget(
                DayBudget()
                .withDailyChangeCount(0L)
                .withDayBudget(BigDecimal.ZERO)
                .withShowMode(DayBudgetShowMode.DEFAULT))
    }
}
