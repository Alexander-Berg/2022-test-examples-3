package ru.yandex.market.logistics.yard.repository.mapper

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.domain.entity.ActionEntity
import ru.yandex.market.logistics.yard.client.dto.configurator.types.ActionType
import ru.yandex.market.logistics.yard_v2.repository.mapper.ActionMapper

class ActionMapperTest(@Autowired private val mapper: ActionMapper): AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/action/before.xml"])
    @ExpectedDatabase("classpath:fixtures/repository/action/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun persistBatchWorks() {
        val persisted = mapper.persistBatch(
            listOf(
                ActionEntity(null, 1, ActionType.SEND_NOTIFICATION, emptyList()),
                ActionEntity(null, 2, ActionType.SEND_NOTIFICATION, emptyList())
            )
        )
        assertions().assertThat(persisted).hasSize(2)
        assertions().assertThat(persisted.map { it.type })
            .containsExactlyInAnyOrder(ActionType.SEND_NOTIFICATION, ActionType.SEND_NOTIFICATION)
    }
}
