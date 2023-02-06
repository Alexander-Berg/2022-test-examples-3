package ru.yandex.market.logistics.yard.repository.mapper

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.domain.entity.ActionParamEntity
import ru.yandex.market.logistics.yard.client.dto.configurator.types.ActionParamType
import ru.yandex.market.logistics.yard_v2.repository.mapper.ActionParamMapper

class ActionParamMapperTest(@Autowired private val mapper: ActionParamMapper): AbstractSecurityMockedContextualTest() {
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/action_param/before.xml"])
    @ExpectedDatabase("classpath:fixtures/repository/action_param/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun persistBatchWorks() {
        val persisted = mapper.persistBatch(
            listOf(
                ActionParamEntity(null, 1, ActionParamType.URL, "123"),
                ActionParamEntity(null, 2, ActionParamType.URL, "345"),
            )
        )
        assertions().assertThat(persisted).hasSize(2)
        assertions().assertThat(persisted.map { it.name })
            .containsExactlyInAnyOrder(ActionParamType.URL, ActionParamType.URL)
    }
}
