package ru.yandex.market.logistics.yard_v2.domain.service.priority_function

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.client.dto.configurator.types.PriorityFunctionType
import ru.yandex.market.logistics.yard_v2.domain.entity.CapacityEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.CapacityUnitEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.ClientQueueEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.PriorityFunctionEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.StateEntity
import ru.yandex.market.logistics.yard_v2.facade.ClientQueueFacade

class TimelinessPriorityFunctionTest(
    @Autowired private val clientQueueFacade: ClientQueueFacade,
    @Autowired private val timelinessPriorityFunction: TimelinessPriorityFunction
) : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/service/priority_function/timelinesspriorityfunction/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/service/priority_function/timelinesspriorityfunction/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testGetPriority() {
        val otherPriorityGroupClientId1: Long = 1
        val otherPriorityGroupClientId4: Long = 4
        val otherPriorityGroupClientId5: Long = 5

        val lineHaulPriorityGroupClientId: Long = 3

        val stateToId: Long = 2
        val assignedState = StateEntity(
            id = stateToId,
            name = "ASSIGNED",
            serviceId = 1,
            capacity = CapacityEntity(
                id = 1, name = "test capacity", value = 10, capacityUnits = mutableListOf(
                    CapacityUnitEntity(id = 1, capacityId = 1, readableName = "№1", isActive = true)
                )
            ),
            priorityFunction = PriorityFunctionEntity(id = 1, type = PriorityFunctionType.TIMELINESS),
        )

        val client1Priority = timelinessPriorityFunction.getPriority(assignedState, otherPriorityGroupClientId1)
        clientQueueFacade.push(
            ClientQueueEntity(
                stateToId = stateToId,
                clientId = otherPriorityGroupClientId1,
                priority = client1Priority,
                currentEdgeId = 1
            )
        )

        val client4Priority = timelinessPriorityFunction.getPriority(assignedState, otherPriorityGroupClientId4)
        clientQueueFacade.push(
            ClientQueueEntity(
                stateToId = stateToId,
                clientId = otherPriorityGroupClientId4,
                priority = client4Priority,
                currentEdgeId = 1
            )
        )

        val client5Priority = timelinessPriorityFunction.getPriority(assignedState, otherPriorityGroupClientId5)
        clientQueueFacade.push(
            ClientQueueEntity(
                stateToId = stateToId,
                clientId = otherPriorityGroupClientId5,
                priority = client5Priority,
                currentEdgeId = 1
            )
        )

        assertions().assertThat(client1Priority).isEqualTo(401)
        assertions().assertThat(client4Priority).isEqualTo(404)
        assertions().assertThat(client5Priority).isEqualTo(405)

        assertions().assertThat(timelinessPriorityFunction.getPriority(assignedState, lineHaulPriorityGroupClientId))
            .isEqualTo(101)

    }
}
