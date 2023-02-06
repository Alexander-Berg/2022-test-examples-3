package ru.yandex.market.logistics.yard.repository.mapper

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.domain.entity.CapacityEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.PriorityFunctionEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.StateEntity
import ru.yandex.market.logistics.yard.client.dto.configurator.types.PriorityFunctionType
import ru.yandex.market.logistics.yard_v2.repository.mapper.StateMapper

class StateMapperTest(@Autowired val mapper: StateMapper) : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/state/before.xml"])
    fun getById() {
        val state = mapper.getById(1000)

        assertions().assertThat(state?.id).isEqualTo(1000L)
        assertions().assertThat(state?.name).isEqualTo("state")
        assertions().assertThat(state?.serviceId).isEqualTo(1L)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/state/before.xml"])
    fun getByIds() {
        val states = mapper.getByIds(setOf(1000, 10001))

        assertions().assertThat(states).hasSize(1)
        val state = states[0]

        assertions().assertThat(state.id).isEqualTo(1000L)
        assertions().assertThat(state.name).isEqualTo("state")
        assertions().assertThat(state.serviceId).isEqualTo(1L)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/state/before.xml"])
    fun getFullStateById() {
        val state = mapper.getFullById(1000)

        assertions().assertThat(state?.id).isEqualTo(1000L)
        assertions().assertThat(state?.name).isEqualTo("state")
        assertions().assertThat(state?.serviceId).isEqualTo(1L)

        assertions().assertThat(state?.capacity?.id).isEqualTo(100)
        assertions().assertThat(state?.capacity?.name).isEqualTo("capacity")
        assertions().assertThat(state?.capacity?.value).isEqualTo(1)

        assertions().assertThat(state?.priorityFunction?.id).isEqualTo(10)
        assertions().assertThat(state?.priorityFunction?.type).isEqualTo(PriorityFunctionType.DEFAULT)
        assertions().assertThat(state?.priorityFunction?.params?.size).isEqualTo(2)

        assertions().assertThat(state?.priorityFunction?.params?.stream()
            ?.map {it.value}?.sorted()).isEqualTo(listOf("100", "200"))
        assertions().assertThat(state?.priorityFunction?.params?.stream()
            ?.map {it.name}?.sorted()).isEqualTo(listOf("param1", "param2"))

    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/state/before-clients-in-states.xml"])
    fun getNumberOfClientsInStates() {
        val clientsInStates = mapper.getNumberOfClientsInStates(listOf("state", "state1"), 1)
        assertions().assertThat(clientsInStates).isEqualTo(2)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/state/before-persist.xml"])
    @ExpectedDatabase("classpath:fixtures/repository/state/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun persistBatchWorks() {
        val persisted = mapper.persistBatch(
            listOf(
                StateEntity(null, "state1", 123, CapacityEntity(id = null), PriorityFunctionEntity(id = null)),
                StateEntity(null, "state2", 123, CapacityEntity(id = 1), PriorityFunctionEntity(id = 2)),
                StateEntity(null, "state3", 124, CapacityEntity(id = null), PriorityFunctionEntity(id = null))
            )
        )
        assertions().assertThat(persisted).hasSize(3)
        assertions().assertThat(persisted.map { it.name }).containsExactlyInAnyOrder("state1", "state2", "state3")
    }
}
