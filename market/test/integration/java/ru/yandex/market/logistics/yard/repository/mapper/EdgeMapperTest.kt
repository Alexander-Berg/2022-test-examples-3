package ru.yandex.market.logistics.yard.repository.mapper

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.domain.entity.EdgeEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.EntityParam
import ru.yandex.market.logistics.yard.client.dto.configurator.types.ActionType
import ru.yandex.market.logistics.yard.client.dto.configurator.types.RestrictionType
import ru.yandex.market.logistics.yard_v2.repository.mapper.EdgeMapper

class EdgeMapperTest(@Autowired private val edgeMapper: EdgeMapper) : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/edge/get_by_id/before.xml"])
    fun getById() {
        val edge = edgeMapper.getById(1)
        assertions().assertThat(edge?.id).isEqualTo(1)
        assertions().assertThat(edge?.stateFromId).isEqualTo(666L)
        assertions().assertThat(edge?.stateToId).isEqualTo(999L)
        assertions().assertThat(edge?.priority).isEqualTo(1L)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/edge/get_full_by_id/before.xml"])
    fun fullEdgeById() {
        val edge = edgeMapper.getFullById(1)
        assertions().assertThat(edge?.id).isEqualTo(1)
        assertions().assertThat(edge?.stateFromId).isEqualTo(666)
        assertions().assertThat(edge?.stateToId).isEqualTo(999)
        assertions().assertThat(edge?.priority).isEqualTo(1)

        assertions().assertThat(edge?.actions?.get(0)?.id).isEqualTo(2)
        assertions().assertThat(edge?.actions?.get(0)?.type).isEqualTo(ActionType.SEND_NOTIFICATION)
        assertions().assertThat(edge?.actions?.get(0)?.params?.get(0)).isEqualTo(EntityParam("aparam", "avalue"))

        assertions().assertThat(edge?.restrictions?.get(0)?.id).isEqualTo(3)
        assertions().assertThat(edge?.restrictions?.get(0)?.type).isEqualTo(RestrictionType.EVENT_REQUIRED)
        assertions().assertThat(edge?.restrictions?.get(0)?.params?.get(0))
            .isEqualTo(EntityParam("rparam", "rvalue"))
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/edge/persist/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/edge/persist/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun persist() {
        val persist = edgeMapper.persist(EdgeEntity(null, 666, 999, 1, listOf(), listOf()))
        assertions().assertThat(persist).isEqualTo(1)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/edge/get_by_client_id/before.xml"])
    fun findAllByClientId() {
        val edges = edgeMapper.findAllByClientId(0)

        assertions().assertThat(edges.size).isEqualTo(2)

        assertions().assertThat(edges[0].id).isEqualTo(1)
        assertions().assertThat(edges[0].stateFromId).isEqualTo(666)
        assertions().assertThat(edges[0].stateToId).isEqualTo(999)
        assertions().assertThat(edges[0].priority).isEqualTo(1)

        assertions().assertThat(edges[1].id).isEqualTo(2)
        assertions().assertThat(edges[1].stateFromId).isEqualTo(666)
        assertions().assertThat(edges[1].stateToId).isEqualTo(1000)
        assertions().assertThat(edges[1].priority).isEqualTo(2)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/edge/persist_batch/before.xml"])
    @ExpectedDatabase("classpath:fixtures/repository/edge/persist_batch/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun persistBatchWorks() {
        val persisted = edgeMapper.persistBatch(
            listOf(
                EdgeEntity(null, 1, 2, 0, emptyList(), emptyList()),
                EdgeEntity(null, 2, 3, 1, emptyList(), emptyList())
            )
        )
        assertions().assertThat(persisted).hasSize(2)
        assertions().assertThat(persisted.map { it.stateToId }).containsExactlyInAnyOrder(2, 3)
    }
}
