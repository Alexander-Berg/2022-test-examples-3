package ru.yandex.direct.core.entity.retargeting.service.uc

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyString
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.eq
import org.mockito.Mockito.reset
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.common.net.NetAcl
import ru.yandex.direct.core.entity.metrika.repository.LalSegmentRepository
import ru.yandex.direct.core.entity.metrika.service.MetrikaSegmentService.NOT_BOUNCE_EXPRESSION
import ru.yandex.direct.core.entity.retargeting.model.ConditionType
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.GoalType
import ru.yandex.direct.core.entity.retargeting.model.RawMetrikaSegmentPreset
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoal
import ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByTypeAndId
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.metrika.client.model.response.Counter
import ru.yandex.direct.metrika.client.model.response.Segment
import java.net.InetAddress

private const val COUNTER_ID = 123
private const val GOAL_ID = 1234L
private const val UNAVAILABLE_GOAL_ID = 1235L
private const val SITE_VISIT_GOAL_ID = Goal.METRIKA_COUNTER_LOWER_BOUND + COUNTER_ID

private val COUNTER = Counter()
    .withId(COUNTER_ID)
    .withOwnerLogin("owner")

private val GOAL = defaultGoal(GOAL_ID)
private val SITE_VISIT_GOAL = defaultGoal(SITE_VISIT_GOAL_ID)

private val NOT_BOUNCE_SEGMENT_PRESET = RawMetrikaSegmentPreset().apply {
    presetId = 7
    name = "Неотказы"
    expression = NOT_BOUNCE_EXPRESSION
    tankerNameKey = "notBounce"
}

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class UcRetargetingConditionServiceTest {
    @Autowired
    private lateinit var steps: Steps
    @Autowired
    private lateinit var netAcl: NetAcl
    @Autowired
    private lateinit var lalSegmentRepository: LalSegmentRepository
    @Autowired
    private lateinit var ucRetargetingConditionService: UcRetargetingConditionService
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub

    private lateinit var clientInfo: ClientInfo

    @Before
    fun before() {
        doReturn(false).`when`(netAcl).isInternalIp(ArgumentMatchers.any(InetAddress::class.java))
        clientInfo = steps.clientSteps().createDefaultClient()
        steps.retargetingGoalsSteps().createMetrikaSegmentPreset(NOT_BOUNCE_SEGMENT_PRESET)

        metrikaClientStub.addUserCounter(clientInfo.uid, COUNTER_ID)
        metrikaClientStub.addEditableCounter(COUNTER)
        metrikaClientStub.addGoals(clientInfo.uid, setOf(GOAL, SITE_VISIT_GOAL))

        // когда создается сегмент в Метрике - это значит создается цель с типом SEGMENT, а значит
        // эту цель надо добавить в доступные пользователю цели
        `when`(metrikaClientStub
            .createSegment(eq(COUNTER_ID), anyString(), anyString(), anyString()))
            .thenAnswer {
                val segment = it.callRealMethod() as Segment
                metrikaClientStub.addGoals(clientInfo.uid,
                    setOf(GOAL, SITE_VISIT_GOAL, defaultGoalByTypeAndId(segment.id.toLong(), GoalType.SEGMENT)))
                segment
            }
    }

    @After
    fun after() {
        steps.retargetingGoalsSteps().clearMetrikaSegmentPresets()
        metrikaClientStub.clearEditableCounters()
        metrikaClientStub.clearSegments()
        reset(netAcl)
    }

    @Test
    fun testFeaturesDisabled() {
        val autoRetargetingCondition = ucRetargetingConditionService.getAutoRetargetingCondition(
            clientInfo.clientId!!, listOf(COUNTER_ID), listOf(), null)
        assertThat(autoRetargetingCondition).isNull()
    }

    @Test
    fun testNoCountersAndGoals() {
        steps.featureSteps().addClientFeature(clientInfo.clientId,
            FeatureName.UNIVERSAL_CAMPAIGNS_AUTORETARGETING_ENABLED, true)
        val autoRetargetingCondition = ucRetargetingConditionService.getAutoRetargetingCondition(
            clientInfo.clientId!!, listOf(), listOf(), null)
        assertThat(autoRetargetingCondition).isNull()
    }

    @Test
    fun testBaseAndLal_OnSiteVisit() {
        steps.featureSteps().addClientFeature(clientInfo.clientId,
            FeatureName.UNIVERSAL_CAMPAIGNS_AUTORETARGETING_ENABLED, true)
        val autoRetargetingCondition = ucRetargetingConditionService.getAutoRetargetingCondition(
            clientInfo.clientId!!, listOf(COUNTER_ID), listOf(), null)
        assertAutoRetargetingCondition(autoRetargetingCondition)

        val rule = autoRetargetingCondition!!.rules[0]
        assertThat(rule.goals).hasSize(2)
        assertBaseRetargetingGoal(rule.goals, mapOf(SITE_VISIT_GOAL_ID to GoalType.GOAL))

        val lalSegmentIds =
            lalSegmentRepository.getLalSegmentsByParentIds(setOf(SITE_VISIT_GOAL_ID)).map { it.id }.toSet()
        assertThat(lalSegmentIds).hasSize(1)
        assertLalRetargetingGoal(rule.goals, lalSegmentIds, setOf(SITE_VISIT_GOAL_ID))
    }

    @Test
    fun testBaseAndLal_OnSiteVisitAndCampaignGoal() {
        steps.featureSteps().addClientFeature(clientInfo.clientId,
            FeatureName.UNIVERSAL_CAMPAIGNS_AUTORETARGETING_ENABLED, true)
        val autoRetargetingCondition = ucRetargetingConditionService.getAutoRetargetingCondition(
            clientInfo.clientId!!, listOf(COUNTER_ID), listOf(GOAL_ID), null)
        assertAutoRetargetingCondition(autoRetargetingCondition)

        val rule = autoRetargetingCondition!!.rules[0]
        assertThat(rule.goals).hasSize(4)
        assertBaseRetargetingGoal(rule.goals,
            mapOf(SITE_VISIT_GOAL_ID to GoalType.GOAL, GOAL_ID to GoalType.GOAL))

        val lalSegmentIds =
            lalSegmentRepository.getLalSegmentsByParentIds(setOf(SITE_VISIT_GOAL_ID, GOAL_ID)).map { it.id }.toSet()
        assertThat(lalSegmentIds).hasSize(2)
        assertLalRetargetingGoal(rule.goals, lalSegmentIds, setOf(SITE_VISIT_GOAL_ID, GOAL_ID))
    }

    @Test
    fun testBase_OnNotBounce() {
        steps.featureSteps().addClientFeature(clientInfo.clientId,
            FeatureName.UNIVERSAL_CAMPAIGNS_BASE_AUTORETARGETING_ENABLED, true)
        val autoRetargetingCondition = ucRetargetingConditionService.getAutoRetargetingCondition(
            clientInfo.clientId!!, listOf(COUNTER_ID), listOf(), null)
        assertAutoRetargetingCondition(autoRetargetingCondition)

        val rule = autoRetargetingCondition!!.rules[0]
        assertThat(rule.goals).hasSize(1)
        assertBaseRetargetingGoal(rule.goals, mapOf(getNotBounceGoalId() to GoalType.SEGMENT))
    }

    @Test
    fun testBase_OnNotBounceAndCampaignGoal() {
        steps.featureSteps().addClientFeature(clientInfo.clientId,
            FeatureName.UNIVERSAL_CAMPAIGNS_BASE_AUTORETARGETING_ENABLED, true)
        val autoRetargetingCondition = ucRetargetingConditionService.getAutoRetargetingCondition(
            clientInfo.clientId!!, listOf(COUNTER_ID), listOf(GOAL_ID), null)
        assertAutoRetargetingCondition(autoRetargetingCondition)

        val rule = autoRetargetingCondition!!.rules[0]
        assertThat(rule.goals).hasSize(2)
        assertBaseRetargetingGoal(rule.goals,
            mapOf(getNotBounceGoalId() to GoalType.SEGMENT, GOAL_ID to GoalType.GOAL))
    }

    @Test
    fun testBase_OnNotBounceAndUnavailableCampaignGoal() {
        steps.featureSteps().addClientFeature(clientInfo.clientId,
                FeatureName.UNIVERSAL_CAMPAIGNS_BASE_AUTORETARGETING_ENABLED, true)
        val autoRetargetingCondition = ucRetargetingConditionService.getAutoRetargetingCondition(
                clientInfo.clientId!!, listOf(COUNTER_ID), listOf(GOAL_ID, UNAVAILABLE_GOAL_ID), null)
        assertAutoRetargetingCondition(autoRetargetingCondition)

        val rule = autoRetargetingCondition!!.rules[0]
        assertThat(rule.goals).hasSize(2)
        assertBaseRetargetingGoal(rule.goals,
                mapOf(getNotBounceGoalId() to GoalType.SEGMENT, GOAL_ID to GoalType.GOAL))
    }

    @Test
    fun testLal_OnNotBounce() {
        steps.featureSteps().addClientFeature(clientInfo.clientId,
            FeatureName.UNIVERSAL_CAMPAIGNS_LAL_AUTORETARGETING_ENABLED, true)
        val autoRetargetingCondition = ucRetargetingConditionService.getAutoRetargetingCondition(
            clientInfo.clientId!!, listOf(COUNTER_ID), listOf(), null)
        assertAutoRetargetingCondition(autoRetargetingCondition)

        val rule = autoRetargetingCondition!!.rules[0]
        assertThat(rule.goals).hasSize(1)
        val lalSegmentIds =
            lalSegmentRepository.getLalSegmentsByParentIds(setOf(getNotBounceGoalId())).map { it.id }.toSet()
        assertThat(lalSegmentIds).hasSize(1)
        assertLalRetargetingGoal(rule.goals, lalSegmentIds, setOf())
    }

    @Test
    fun testLal_OnNotBounceAndCampaignGoal() {
        steps.featureSteps().addClientFeature(clientInfo.clientId,
            FeatureName.UNIVERSAL_CAMPAIGNS_LAL_AUTORETARGETING_ENABLED, true)
        val autoRetargetingCondition = ucRetargetingConditionService.getAutoRetargetingCondition(
            clientInfo.clientId!!, listOf(COUNTER_ID), listOf(GOAL_ID), null)
        assertAutoRetargetingCondition(autoRetargetingCondition)

        val rule = autoRetargetingCondition!!.rules[0]
        assertThat(rule.goals).hasSize(2)
        val lalSegmentIds =
            lalSegmentRepository.getLalSegmentsByParentIds(setOf(getNotBounceGoalId(), GOAL_ID)).map { it.id }.toSet()
        assertThat(lalSegmentIds).hasSize(2)
        assertLalRetargetingGoal(rule.goals, lalSegmentIds, setOf())
    }

    @Test
    fun testLal_OnNotBounceAndUnavailableCampaignGoal() {
        steps.featureSteps().addClientFeature(clientInfo.clientId,
                FeatureName.UNIVERSAL_CAMPAIGNS_LAL_AUTORETARGETING_ENABLED, true)
        val autoRetargetingCondition = ucRetargetingConditionService.getAutoRetargetingCondition(
                clientInfo.clientId!!, listOf(COUNTER_ID), listOf(GOAL_ID, UNAVAILABLE_GOAL_ID), null)
        assertAutoRetargetingCondition(autoRetargetingCondition)

        val rule = autoRetargetingCondition!!.rules[0]
        assertThat(rule.goals).hasSize(2)
        val lalSegmentIds =
                lalSegmentRepository.getLalSegmentsByParentIds(setOf(getNotBounceGoalId(), GOAL_ID)).map { it.id }.toSet()
        assertThat(lalSegmentIds).hasSize(2)
        assertLalRetargetingGoal(rule.goals, lalSegmentIds, setOf())
    }

    private fun getNotBounceGoalId(): Long {
        return metrikaClientStub.getSegments(COUNTER_ID, null).first {
            NOT_BOUNCE_EXPRESSION == it.expression
        }.id.toLong()
    }

    private fun assertAutoRetargetingCondition(autoRetargetingCondition: RetargetingCondition?) {
        assertThat(autoRetargetingCondition).isNotNull
        SoftAssertions.assertSoftly {
            it.assertThat(autoRetargetingCondition!!.clientId).isEqualTo(clientInfo.clientId!!.asLong())
            it.assertThat(autoRetargetingCondition.type).isEqualTo(ConditionType.metrika_goals)
            it.assertThat(autoRetargetingCondition.autoRetargeting).isTrue
            it.assertThat(autoRetargetingCondition.rules).hasSize(1)
        }
    }

    private fun assertBaseRetargetingGoal(goals: List<Goal>, baseGoalTypeByIds: Map<Long, GoalType>) {
        val goalById = goals.associateBy { it.id }
        for (baseGoalId in baseGoalTypeByIds.keys) {
            val baseGoal = goalById[baseGoalId]
            assertThat(baseGoal)
                .`as`("No goal with id $baseGoalId found")
                .isNotNull
            val goalType = baseGoalTypeByIds[baseGoalId]
            assertThat(baseGoal!!.type)
                .`as`("Base goal $baseGoalId has type ${baseGoal.type} but expected $goalType")
                .isEqualTo(goalType)
            assertThat(baseGoal.unionWithId == null)
                .`as`("Base goal $baseGoalId has parent goal ${baseGoal.unionWithId}")
                .isTrue
        }
    }

    private fun assertLalRetargetingGoal(goals: List<Goal>, lalSegmentIds: Set<Long>, parentGoalIds: Set<Long>) {
        val goalById = goals.associateBy { it.id }
        for (lalSegmentId in lalSegmentIds) {
            val lalGoal = goalById[lalSegmentId]
            assertThat(lalGoal)
                .`as`("No goal with id $lalSegmentId found")
                .isNotNull
            if (parentGoalIds.isEmpty()) {
                assertThat(lalGoal!!.unionWithId == null)
                    .`as`("LAL goal $lalSegmentId has parent goal ${lalGoal.unionWithId}")
                    .isTrue
            } else {
                assertThat(parentGoalIds.contains(lalGoal!!.unionWithId))
                    .`as`("LAL goal $lalSegmentId has wrong parent goal ${lalGoal.unionWithId}," +
                        "expected one of $parentGoalIds")
                    .isTrue
            }
        }
    }
}
