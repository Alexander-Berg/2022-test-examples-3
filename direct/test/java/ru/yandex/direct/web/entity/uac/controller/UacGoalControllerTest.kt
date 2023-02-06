package ru.yandex.direct.web.entity.uac.controller

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.metrika.client.model.response.CounterGoal
import ru.yandex.direct.metrika.client.model.response.CounterInfoDirect
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.web.configuration.DirectWebTest

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacGoalControllerTest : BaseMvcTest() {
    @Autowired
    private lateinit var steps: Steps

    private lateinit var clientInfo: ClientInfo

    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        clientInfo = steps.clientSteps().createDefaultClient()

        testAuthHelper.setOperatorAndSubjectUser(clientInfo.uid)
        testAuthHelper.setSecurityContext()

        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.COLD_START_FOR_ECOMMERCE_GOALS, true)
    }

    @After
    fun after() {
        metrikaClientStub.clearEditableCounters()
        metrikaClientStub.clearUserCounters(clientInfo.uid)
    }

    @Test
    fun testGoals_singleCounterWithGoal() {
        val goalId = RandomNumberUtils.nextPositiveInteger()
        val counterId = RandomNumberUtils.nextPositiveInteger()
        val counterGoal = CounterGoal().withId(goalId).withType(CounterGoal.Type.EMAIL)

        metrikaClientStub.addUserCounter(clientInfo.uid, counterId)
        metrikaClientStub.addCounterGoal(counterId, counterGoal)

        doRequest("/uac/goals?counter_ids=${counterId}", HttpMethod.GET,200)
            .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.result.size()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.result[0].counter_id").value(counterId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.result[0].goal_id").value(goalId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.result[0].goal_type").value("OTHER"))
    }

    @Test
    fun testGoals_singleCounterWithEcommerce() {
        val counterId = RandomNumberUtils.nextPositiveInteger()
        var counter = CounterInfoDirect().withId(counterId).withEcommerce(true)
        val ecomGoalId = Goal.METRIKA_ECOMMERCE_BASE + counterId.toLong()

        metrikaClientStub.addUserCounter(clientInfo.uid, counter)

        doRequest("/uac/goals?counter_ids=${counterId}", HttpMethod.GET, 200)
            .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.result.size()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.result[0].counter_id").value(counterId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.result[0].goal_id").value(ecomGoalId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.result[0].goal_type").value("ECOMMERCE"))
    }
}
