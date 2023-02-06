package ru.yandex.market.logistics.yard.repository.mapper

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.domain.entity.PriorityFunctionParamEntity
import ru.yandex.market.logistics.yard.client.dto.configurator.types.PriorityFunctionParamType
import ru.yandex.market.logistics.yard_v2.repository.mapper.PriorityFunctionParamMapper

class PriorityFunctionParamMapperTest(@Autowired private val mapper: PriorityFunctionParamMapper):
AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/priority_function_param/before.xml"])
    @ExpectedDatabase("classpath:fixtures/repository/priority_function_param/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun persistBatchWorks() {
        val persisted = mapper.persistBatch(
            listOf(
                PriorityFunctionParamEntity(null, 10, PriorityFunctionParamType.PRIORITY_STEP, "100"),
                PriorityFunctionParamEntity(null, 10, PriorityFunctionParamType.SKIP_N_CLIENTS, "200"),
                PriorityFunctionParamEntity(null, 11, PriorityFunctionParamType.PRIORITY_STEP, "400"),
            )
        )
        assertions().assertThat(persisted).hasSize(3)
        assertions().assertThat(persisted.map { it.name })
            .containsExactlyInAnyOrder(
                PriorityFunctionParamType.PRIORITY_STEP,
                PriorityFunctionParamType.SKIP_N_CLIENTS, PriorityFunctionParamType.PRIORITY_STEP)
    }
}
