package ru.yandex.direct.grid.processing.service.campaign

import one.util.streamex.StreamEx
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.direct.core.entity.campaign.converter.CampaignConverter
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService
import ru.yandex.direct.core.entity.metrika.utils.MetrikaGoalsUtils.ecommerceGoalId
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.info.campaign.CampaignInfo
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.dbschema.ppc.Tables
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.api.GdDefect
import ru.yandex.direct.grid.processing.model.api.GdValidationResult
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdMeaningfulGoalRequest
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayloadItem
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateMeaningfulGoals
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.TemplateMutation
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import java.math.BigDecimal

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class CampaignGoalsMassUpdateMeaningfulGoalsGraphQLServiceTest {
    @Autowired
    private lateinit var processor: GraphQlTestExecutor

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var metrikaGoalsService: MetrikaGoalsService

    private lateinit var operator: User
    private lateinit var defaultUser: UserInfo
    private val counterId = RandomNumberUtils.nextPositiveInteger().toLong()
    private val goalId = RandomNumberUtils.nextPositiveInteger().toLong()
    private val counterId2 = RandomNumberUtils.nextPositiveInteger().toLong()
    private val goalId2 = RandomNumberUtils.nextPositiveInteger().toLong()
    private val unavailableCounterId = RandomNumberUtils.nextPositiveInteger().toLong()
    private val unavailableGoalId = RandomNumberUtils.nextPositiveInteger().toLong()
    private val unavailableEcommerceCounterId = RandomNumberUtils.nextPositiveInteger().toLong()

    @Before
    fun before() {
        defaultUser = steps.userSteps().createDefaultUser()
        operator = defaultUser.user!!
        TestAuthHelper.setDirectAuthentication(operator)
        metrikaClientStub.addCounterGoal(counterId.toInt(), goalId.toInt())
        metrikaClientStub.addUserCounter(defaultUser.uid, counterId.toInt())

        metrikaClientStub.addCounterGoal(counterId2.toInt(), goalId2.toInt())
        metrikaClientStub.addUserCounter(defaultUser.uid, counterId2.toInt())

        metrikaClientStub.addCounterGoal(unavailableCounterId.toInt(), unavailableGoalId.toInt())
        metrikaClientStub.addUnavailableCounter(unavailableCounterId)

        metrikaClientStub.addUnavailableEcommerceCounter(unavailableEcommerceCounterId)
    }

    @After
    fun after() {
        metrikaClientStub.clearUnavailableCounters()
    }

    @Test
    fun testSuccess() {
        val validCampaignInfo = createTextCampaign()

        val input = createRequest(validCampaignInfo, goalId)
        val payload = processor.doMutationAndGetPayload(UPDATE_GOALS_MUTATION, input, operator)

        assertCampaignUpdated(payload, validCampaignInfo)
    }

    @Test
    fun testNoBindCounters_Success() {
        val validCampaignInfo = createTextCampaign(counterId)

        val input = createRequest(validCampaignInfo, goalId, bindCounters = false)
        val payload = processor.doMutationAndGetPayload(UPDATE_GOALS_MUTATION, input, operator)

        assertCampaignUpdated(payload, validCampaignInfo)
    }

    @Test
    fun testUnavailableGoal_Success() {
        steps.featureSteps().addClientFeature(defaultUser.clientId, FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true)
        val validCampaignInfo = createTextCampaign(unavailableCounterId)

        val input = createRequest(validCampaignInfo, unavailableGoalId)
        val payload = processor.doMutationAndGetPayload(UPDATE_GOALS_MUTATION, input, operator)

        assertCampaignUpdated(payload, validCampaignInfo)
    }

    @Test
    fun testUnavailableEcommerceGoal_Success() {
        steps.featureSteps().addClientFeature(defaultUser.clientId, FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true)
        steps.featureSteps().addClientFeature(defaultUser.clientId, FeatureName.COLD_START_FOR_ECOMMERCE_GOALS, true)

        val validCampaignInfo = createTextCampaign(unavailableEcommerceCounterId)

        val input = createRequest(validCampaignInfo, ecommerceGoalId(unavailableEcommerceCounterId))
        val payload = processor.doMutationAndGetPayload(UPDATE_GOALS_MUTATION, input, operator)

        assertCampaignUpdated(payload, validCampaignInfo)
    }

    @Test
    fun testUnavailableGoal_SuccessMulti() {
        steps.featureSteps().addClientFeature(defaultUser.clientId, FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true)
        val validCampaignInfo1 = createTextCampaign(counterId)
        val validCampaignInfo2 = createTextCampaign(unavailableCounterId)

        val campaignInfos = listOf(validCampaignInfo1, validCampaignInfo2)
        val input = createRequest(campaignInfos, unavailableGoalId)
        val payload = processor.doMutationAndGetPayload(UPDATE_GOALS_MUTATION, input, operator)

        assertCampaignsUpdated(payload, campaignInfos)
    }

    @Test
    fun testUnavailableGoal_ValidationError() {
        val validCampaignInfo = createTextCampaign()

        val input = createRequest(validCampaignInfo, unavailableGoalId)
        val payload = processor.doMutationAndGetPayload(UPDATE_GOALS_MUTATION, input, operator)

        assertGoalNotFound(payload)
    }

    @Test
    fun testSuccessRemoveMeaningfulGoal() {
        val validCampaignInfo = createTextCampaign(counterId, goalId)

        val deleteInput = createRequest(validCampaignInfo, goalId)
            .withMeaningfulGoals(listOf())

        processor.doMutationAndGetPayload(UPDATE_GOALS_MUTATION, deleteInput, operator)
        val savedMeaningfulGoals = getMeaningfulGoals(validCampaignInfo)
        assertThat(savedMeaningfulGoals).hasSize(0)
    }

    @Test
    fun testSuccessChangeMeaningfulGoal() {
        val validCampaignInfo = createTextCampaign(counterId, goalId)

        val input = createRequest(validCampaignInfo, goalId2)
        val payload = processor.doMutationAndGetPayload(UPDATE_GOALS_MUTATION, input, operator)

        assertCampaignUpdated(payload, validCampaignInfo)
    }

    @Test
    fun testWrongCampaignType() {
        val invalidTypeCampaignInfo = steps.contentPromotionCampaignSteps()
            .createDefaultCampaign(defaultUser.clientInfo!!)
        val input = createRequest(invalidTypeCampaignInfo, goalId)
        val payload = processor.doMutationAndGetPayload(UPDATE_GOALS_MUTATION, input, operator)
        assertThat(payload.validationResult.errors).hasSize(1)
    }

    private fun getMeaningfulGoals(info: CampaignInfo<*>): List<MeaningfulGoal> {
        val result = dslContextProvider.ppc(info.shard)
            .select(Tables.CAMP_OPTIONS.MEANINGFUL_GOALS)
            .from(Tables.CAMP_OPTIONS)
            .where(Tables.CAMP_OPTIONS.CID.eq(info.id))
            .fetch(Tables.CAMP_OPTIONS.MEANINGFUL_GOALS)
        return StreamEx.of(result)
            .nonNull()
            .map { CampaignConverter.meaningfulGoalsFromDb(it) }
            .findFirst()
            .orElse(listOf())
    }

    private fun createRequest(
        info: CampaignInfo<*>,
        goalId: Long,
        bindCounters: Boolean = true,
    ) = createRequest(listOf(info), goalId, bindCounters)

    private fun createRequest(
        infos: List<CampaignInfo<*>>,
        goalId: Long,
        bindCounters: Boolean = true,
    ): GdUpdateMeaningfulGoals {
        val goal = GdMeaningfulGoalRequest()
            .withGoalId(goalId)
            .withConversionValue(BigDecimal.TEN)
        return GdUpdateMeaningfulGoals()
            .withCampaignIds(infos.map { it.id })
            .withMeaningfulGoals(listOf(goal))
            .withBindCounters(bindCounters)
    }

    private fun createTextCampaign(counterId: Long? = null, goalId: Long? = null): TextCampaignInfo {
        val textCampaign = TestCampaigns.defaultTextCampaignWithSystemFields(defaultUser.clientInfo)
        textCampaign.apply {
            counterId?.let { metrikaCounters = listOf(it) }
            goalId?.let { meaningfulGoals = listOf(MeaningfulGoal()
                .withGoalId(goalId)
                .withConversionValue(BigDecimal.TEN))
            }
        }
        return steps.textCampaignSteps().createCampaign(defaultUser.clientInfo!!, textCampaign)
    }

    private fun assertCampaignUpdated(payload: GdUpdateCampaignPayload, campaignInfo: TextCampaignInfo) =
        assertCampaignsUpdated(payload, listOf(campaignInfo))

    private fun assertCampaignsUpdated(payload: GdUpdateCampaignPayload, campaignInfos: List<TextCampaignInfo>) {
        val expectedPayloadItems = campaignInfos.map { GdUpdateCampaignPayloadItem().withId(it.id) }
        val expectedPayload = GdUpdateCampaignPayload().withUpdatedCampaigns(expectedPayloadItems)
        assertThat(payload).`is`(matchedBy(beanDiffer(expectedPayload)))

        val softAssertions = SoftAssertions()
        campaignInfos.forEach {
            val savedMeaningfulGoals = getMeaningfulGoals(it)
            softAssertions.assertThat(savedMeaningfulGoals).hasSize(1)
        }
        softAssertions.assertAll()
    }

    private fun assertGoalNotFound(payload: GdUpdateCampaignPayload) {
        val expectedPayload = GdUpdateCampaignPayload().withUpdatedCampaigns(listOf(null))
            .withValidationResult(GdValidationResult()
                .withErrors(listOf(GdDefect()
                    .withCode("CollectionDefectIds.Gen.MUST_BE_IN_COLLECTION")
                    .withParams(null)
                    .withPath("campaignUpdateItems[0].meaningfulGoals[0].goalId"))))
        assertThat(payload).`is`(matchedBy(beanDiffer(expectedPayload)))
    }

    companion object {
        private const val MUTATION_NAME = "updateMeaningfulGoals"
        private const val MUTATION_TEMPLATE = """
            mutation {
              %s (input: %s) {
                validationResult {
                  errors {
                    code
                    path
                    params
                  }
                }
                updatedCampaigns {
                  id
                }
              }
            }
            """

        private val UPDATE_GOALS_MUTATION = TemplateMutation(MUTATION_NAME, MUTATION_TEMPLATE,
            GdUpdateMeaningfulGoals::class.java, GdUpdateCampaignPayload::class.java)
    }
}
