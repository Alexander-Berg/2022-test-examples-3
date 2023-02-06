package ru.yandex.market.logistics.yard.repository.mapper

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.domain.entity.YardClientEventEntity
import ru.yandex.market.logistics.yard_v2.repository.mapper.YardClientEventMapper
import java.time.LocalDateTime

class YardClientEventMapperTest(
    @Autowired val mapper: YardClientEventMapper
) : AbstractSecurityMockedContextualTest() {

    private val event = YardClientEventEntity(
        null,
        2,
        "ARRIVED",
        LocalDateTime.of(2021, 5, 1, 10, 0, 0),
        LocalDateTime.of(2021, 6, 1, 10, 0, 0)
    )

    @Test
    @DatabaseSetup("classpath:fixtures/repository/client_event/get-by-id/before.xml")
    fun getById() {
        val event = mapper.getById(1)
        assertions().assertThat(event?.yardClientId).isEqualTo(this.event.yardClientId)
        assertions().assertThat(event?.createdAt).isEqualTo(this.event.createdAt)
        assertions().assertThat(event?.eventDate).isEqualTo(this.event.eventDate)
        assertions().assertThat(event?.id).isEqualTo(1)
        assertions().assertThat(event?.type).isEqualTo(this.event.type)
    }

    @Test
    @DatabaseSetup("classpath:fixtures/repository/client_event/get-by-yard-client-id/before.xml")
    fun getByYardClientId() {
        val events = mapper.getByYardClientId(3)
        assertions().assertThat(events).hasSize(2)
        assertions().assertThat(events.map { it.id }).containsExactlyInAnyOrder(2, 3)
        assertions().assertThat(events.map { it.type }).containsExactlyInAnyOrder("ARRIVED", "HAS_EVENT")
    }

    @Test
    @DatabaseSetup("classpath:fixtures/repository/client_event/persist/before.xml")
    @ExpectedDatabase(
        "classpath:fixtures/repository/client_event/persist/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun persist() {
        mapper.persist(event)
    }

    @Test
    @DatabaseSetup("classpath:fixtures/repository/client_event/persist-all/before.xml")
    @ExpectedDatabase(
        "classpath:fixtures/repository/client_event/persist-all/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun persistAll() {
        val list = mapper.persistAll(listOf(event, event))

        assertions().assertThat(list).isEqualTo(listOf(1L, 2L))
    }
}
