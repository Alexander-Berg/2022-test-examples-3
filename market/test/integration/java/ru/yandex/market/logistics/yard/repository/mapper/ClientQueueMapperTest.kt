package ru.yandex.market.logistics.yard.repository.mapper

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.domain.entity.ClientQueueEntity
import ru.yandex.market.logistics.yard_v2.repository.mapper.ClientQueueMapper

class ClientQueueMapperTest(@Autowired val clientQueueMapper: ClientQueueMapper) :
    AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup("classpath:fixtures/repository/client_queue/persist/before.xml")
    @ExpectedDatabase(
        "classpath:fixtures/repository/client_queue/persist/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun persist() {
        clientQueueMapper.persist(
            ClientQueueEntity(
                stateToId = 666,
                clientId = 0,
                priority = 100,
                currentEdgeId = 1
            )
        )
    }

    @Test
    @DatabaseSetup("classpath:fixtures/repository/client_queue/get_last_priority_number/before.xml")
    fun getLastPriorityNumber() {
        val lastPriorityNumber = clientQueueMapper.getLastPriorityNumber(666, 100)

        assertions().assertThat(lastPriorityNumber).isEqualTo(130)
    }

    @Test
    @DatabaseSetup("classpath:fixtures/repository/client_queue/get_last_priority_number/before.xml")
    fun getLastPriorityNumberOnEmptyQueue() {
        val lastPriorityNumber = clientQueueMapper.getLastPriorityNumber(777, 100)

        assertions().assertThat(lastPriorityNumber).isEqualTo(0)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/client_queue/get_number_of_clients_in_queue/before.xml"])
    fun getNumberOfClientsInQueueToStates() {
        val clientsInStates = clientQueueMapper.getNumberOfClientsInQueueToStates(
            listOf("state", "state1"),
            1,
            1
        )
        assertions().assertThat(clientsInStates).isEqualTo(1)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/client_queue/find_all/before.xml"])
    fun findAllByStateToId() {
        val clientsInStates = clientQueueMapper.findAllByStateToId(666)
        assertions().assertThat(clientsInStates.size).isEqualTo(1)

        assertions().assertThat(clientsInStates[0].id).isEqualTo(1)
        assertions().assertThat(clientsInStates[0].clientId).isEqualTo(0)
        assertions().assertThat(clientsInStates[0].currentEdgeId).isEqualTo(1)
        assertions().assertThat(clientsInStates[0].priority).isEqualTo(100)
        assertions().assertThat(clientsInStates[0].stateToId).isEqualTo(666)
        assertions().assertThat(clientsInStates[0].clientsPassedBefore).isEqualTo(35)

    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/client_queue/find_all/before.xml"])
    fun findByClientId() {
        val client = clientQueueMapper.findByClientId(0)
        assertions().assertThat(client).isNotNull
        val notClient = clientQueueMapper.findByClientId(1)
        assertions().assertThat(notClient).isNull()
    }
}
