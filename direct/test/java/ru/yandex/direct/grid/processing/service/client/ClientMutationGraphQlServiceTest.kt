package ru.yandex.direct.grid.processing.service.client

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.client.GdRequestMetrikaCountersAccessPayload
import ru.yandex.direct.grid.processing.util.KtGraphQLTestExecutor
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.metrika.client.model.request.RequestGrantsObjectType
import ru.yandex.direct.metrika.client.model.request.RequestGrantsPermissionType
import ru.yandex.direct.metrika.client.model.request.RequestGrantsRequest
import ru.yandex.direct.metrika.client.model.request.RequestGrantsRequestItem
import ru.yandex.direct.metrika.client.model.response.CounterInfoDirect
import ru.yandex.direct.test.utils.checkEquals

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class ClientMutationGraphQlServiceTest {

    @Autowired
    private lateinit var ktGraphQLTestExecutor: KtGraphQLTestExecutor

    @Autowired
    private lateinit var steps: Steps

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub

    private lateinit var userInfo: UserInfo
    private lateinit var existentCounter: CounterInfoDirect

    companion object {
        private const val EXISTENT_COUNTER_ID = 1L
        private const val ACCESS_REQUESTED_COUNTER_ID = 3L
    }

    @Before
    fun setUp() {
        userInfo = steps.userSteps().createDefaultUser()
        val anotherUserInfo = steps.userSteps().createDefaultUser()

        existentCounter = MetrikaClientStub.buildCounter(EXISTENT_COUNTER_ID.toInt())
        metrikaClientStub.addUserCounters(userInfo.uid, listOf(existentCounter))
        metrikaClientStub.addUserCounter(anotherUserInfo.uid, ACCESS_REQUESTED_COUNTER_ID.toInt())
        metrikaClientStub.requestAccess(userInfo.uid, ACCESS_REQUESTED_COUNTER_ID)

        TestAuthHelper.setDirectAuthentication(userInfo.user!!)
        ktGraphQLTestExecutor.withDefaultGraphQLContext(userInfo.user!!)
    }

    @Test
    fun requestMetrikaCountersAccess_OneCounter() {
        ktGraphQLTestExecutor.requestMetrikaCountersAccess(setOf(EXISTENT_COUNTER_ID))
            .checkEquals(GdRequestMetrikaCountersAccessPayload().apply {
                isMetrikaAvailable = true
                success = true
            })

        Mockito.verify(metrikaClientStub).requestCountersGrants(RequestGrantsRequest().apply {
            requestItems = listOf(RequestGrantsRequestItem().apply {
                objectType = RequestGrantsObjectType.COUNTER
                objectId = EXISTENT_COUNTER_ID.toString()
                permission = RequestGrantsPermissionType.RO
                serviceName = RequestGrantsRequestItem.DIRECT_SERVICE_NAME
                requesterLogin = userInfo.login
            })
        })
    }

    @Test
    fun requestMetrikaCountersAccess_TwoDifferentCounters() {
        ktGraphQLTestExecutor.requestMetrikaCountersAccess(setOf(EXISTENT_COUNTER_ID, ACCESS_REQUESTED_COUNTER_ID))
            .checkEquals(GdRequestMetrikaCountersAccessPayload().apply {
                isMetrikaAvailable = true
                success = true
            })

        Mockito.verify(metrikaClientStub).requestCountersGrants(RequestGrantsRequest().apply {
            requestItems = listOf(
                RequestGrantsRequestItem().apply {
                    objectType = RequestGrantsObjectType.COUNTER
                    objectId = EXISTENT_COUNTER_ID.toString()
                    permission = RequestGrantsPermissionType.RO
                    serviceName = RequestGrantsRequestItem.DIRECT_SERVICE_NAME
                    requesterLogin = userInfo.login
                },
                RequestGrantsRequestItem().apply {
                    objectType = RequestGrantsObjectType.COUNTER
                    objectId = ACCESS_REQUESTED_COUNTER_ID.toString()
                    permission = RequestGrantsPermissionType.RO
                    serviceName = RequestGrantsRequestItem.DIRECT_SERVICE_NAME
                    requesterLogin = userInfo.login
                })
        })
    }
}
