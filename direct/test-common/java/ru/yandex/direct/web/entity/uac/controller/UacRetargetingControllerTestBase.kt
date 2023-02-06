package ru.yandex.direct.web.entity.uac.controller

import org.junit.After
import one.util.streamex.StreamEx
import org.jooq.DSLContext
import org.junit.Before
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes
import ru.yandex.direct.core.entity.metrika.repository.LalSegmentRepository
import ru.yandex.direct.core.entity.metrika.service.MetrikaSegmentService
import ru.yandex.direct.core.entity.mobileapp.model.ExternalTrackerEventName
import ru.yandex.direct.core.entity.mobileapp.model.MobileApp
import ru.yandex.direct.core.entity.mobileapp.model.MobileExternalTrackerEvent
import ru.yandex.direct.core.entity.mobilegoals.MobileAppGoalsExternalTrackerRepository
import ru.yandex.direct.core.entity.mobilegoals.MobileAppGoalsService
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.GoalType
import ru.yandex.direct.core.entity.retargeting.model.RawMetrikaSegmentPreset
import ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionService
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.AppInfo
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoal
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoalType
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAppInfo
import ru.yandex.direct.core.entity.uac.service.UacAppInfoService
import ru.yandex.direct.core.testing.data.TestFullGoals
import ru.yandex.direct.core.testing.data.TestFullGoals.defaultAudience
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.metrika.client.model.response.Counter
import ru.yandex.direct.metrika.client.model.response.Segment
import ru.yandex.direct.utils.FunctionalUtils
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import java.util.stream.Collectors.toList

private const val COUNTER_ID = 123
private const val GOAL_ID = 1234L
private const val SITE_VISIT_GOAL_ID = Goal.METRIKA_COUNTER_LOWER_BOUND + COUNTER_ID

private val COUNTER = Counter()
    .withId(COUNTER_ID)
    .withOwnerLogin("owner")

private val GOAL = TestFullGoals.defaultGoal(GOAL_ID)
private val SITE_VISIT_GOAL = TestFullGoals.defaultGoal(SITE_VISIT_GOAL_ID)

private val NOT_BOUNCE_SEGMENT_PRESET = RawMetrikaSegmentPreset().apply {
    presetId = 7
    name = "Неотказы"
    expression = MetrikaSegmentService.NOT_BOUNCE_EXPRESSION
    tankerNameKey = "notBounce"
}

abstract class UacRetargetingControllerTestBase {
    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    protected lateinit var steps: Steps

    @Autowired
    private lateinit var uacAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    private lateinit var uacAppInfoService: UacAppInfoService

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var mobileAppGoalsService: MobileAppGoalsService

    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub

    @Autowired
    private lateinit var mobileAppGoalsExternalTrackerRepository: MobileAppGoalsExternalTrackerRepository

    @Autowired
    protected lateinit var lalSegmentRepository: LalSegmentRepository

    @Autowired
    protected lateinit var retargetingConditionService: RetargetingConditionService

    @Autowired
    protected lateinit var mockMvc: MockMvc

    protected lateinit var userInfo: UserInfo
    protected lateinit var mobileApp: MobileApp
    protected lateinit var mobileAppGoalIds: List<Long>
    protected lateinit var mobileAppGoalNameById: Map<Long, String>
    protected lateinit var ydbAppInfo: UacYdbAppInfo
    protected lateinit var audienceGoals: List<Goal>
    protected lateinit var appInfo: AppInfo
    protected lateinit var segment: Segment

    @Before
    fun before() {
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.IN_APP_MOBILE_TARGETING, true)
        ydbAppInfo = defaultAppInfo()
        uacAppInfoRepository.saveAppInfo(ydbAppInfo)
        appInfo = uacAppInfoService.getAppInfo(ydbAppInfo)

        mobileApp = steps.mobileAppSteps().createMobileApp(userInfo.clientInfo!!, appInfo.url).mobileApp
        val secondMobileApp = steps.mobileAppSteps().createMobileApp(userInfo.clientInfo!!, appInfo.url).mobileApp
        val externalTrackerEventNamesForFirstApp = ExternalTrackerEventName.values().take(10).map { it.name }
        val externalTrackerEventNamesForSecondApp = ExternalTrackerEventName.values()
            .map { it.name }
            .filter { !externalTrackerEventNamesForFirstApp.contains(it) }
        createMobileAppGoalsForExternalTracker(
            dslContextProvider.ppc(userInfo.shard),
            mobileApp,
            externalTrackerEventNamesForFirstApp,
        )
        createMobileAppGoalsForExternalTracker(
            dslContextProvider.ppc(userInfo.shard),
            secondMobileApp,
            externalTrackerEventNamesForSecondApp,
        )
        mobileAppGoalsService.updateMobileAppGoalsForExternalTracker(
            dslContextProvider.ppc(userInfo.shard), userInfo.clientId, listOf(mobileApp, secondMobileApp)
        )
        mobileAppGoalNameById =
            mobileAppGoalsService.getGoalsByApps(userInfo.clientId, listOf(mobileApp, secondMobileApp)).associate { it.id to it.name }
        mobileAppGoalIds = mobileAppGoalNameById.keys.toList()
        audienceGoals = listOf(defaultAudience(), defaultAudience())
        steps.retargetingGoalsSteps().createMetrikaGoalsInPpcDict(audienceGoals)

        metrikaClientStub.addGoals(userInfo.uid, audienceGoals.toSet())

        steps.retargetingGoalsSteps().createMetrikaSegmentPreset(NOT_BOUNCE_SEGMENT_PRESET)
        metrikaClientStub.addUserCounter(userInfo.uid, COUNTER_ID)
        metrikaClientStub.addEditableCounter(COUNTER)
        segment = metrikaClientStub
            .createSegment(COUNTER_ID, NOT_BOUNCE_SEGMENT_PRESET.name, NOT_BOUNCE_SEGMENT_PRESET.expression, null)
        val segmentGoal = TestFullGoals.defaultGoalWithId(segment.id.toLong(), GoalType.SEGMENT)
            .withCounterId(COUNTER_ID) as Goal
        metrikaClientStub.addGoals(userInfo.uid, setOf(GOAL, SITE_VISIT_GOAL, segmentGoal))
        steps.retargetingGoalsSteps()
            .createMetrikaGoalsInPpcDict(listOf(segmentGoal))
    }

    @After
    fun after() {
        steps.retargetingGoalsSteps().clearMetrikaSegmentPresets()
        metrikaClientStub.clearEditableCounters()
        metrikaClientStub.clearSegments()
    }

    protected fun createUacRetargetingConditionRuleGoal(
        id: Long,
        time: Int? = 540,
        type: UacRetargetingConditionRuleGoalType? = UacRetargetingConditionRuleGoalType.MOBILE,
        name: String? = null,
    ): UacRetargetingConditionRuleGoal {
        return UacRetargetingConditionRuleGoal(
            id = id,
            time = time,
            type = type,
            name = name
        )
    }

    private fun createMobileAppGoalsForExternalTracker(
        dslContext: DSLContext,
        mobileApp: MobileApp,
        externalTrackerEventNames: List<String>
    ) {
        val mapIds = listOf(mobileApp.id)
        val mapIdsWithGoals = FunctionalUtils.listToSet(
            mobileAppGoalsExternalTrackerRepository.getEventsByAppIds(dslContext, mapIds, false)
        ) { obj: MobileExternalTrackerEvent -> obj.mobileAppId }

        val mobileGoals = StreamEx.of(mapIds)
            .filter { id: Long -> !mapIdsWithGoals.contains(id) }
            .cross(ExternalTrackerEventName.values().filter { it.name in externalTrackerEventNames })
            .mapKeyValue { id: Long?, en: ExternalTrackerEventName? ->
                MobileExternalTrackerEvent()
                    .withMobileAppId(id)
                    .withEventName(en)
                    .withCustomName("")
                    .withIsDeleted(false)
            }
            .collect(toList())
        mobileAppGoalsExternalTrackerRepository.addEvents(dslContext, mobileGoals)
    }
}
