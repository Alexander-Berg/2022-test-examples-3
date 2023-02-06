package ru.yandex.market.logistics.yard.repository.mapper

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.domain.entity.PriorityFunctionEntity
import ru.yandex.market.logistics.yard.client.dto.configurator.types.PriorityFunctionType
import ru.yandex.market.logistics.yard_v2.repository.mapper.PriorityFunctionMapper

class PriorityFunctionMapperTest(@Autowired private val mapper: PriorityFunctionMapper, @Autowired val jdbcTemplate: JdbcTemplate) :
    AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/priority_function/before.xml"])
    fun getById() {
        val priorityFunction = mapper.getById(1)

        assertions().assertThat(priorityFunction?.id).isEqualTo(1L)
        assertions().assertThat(priorityFunction?.type).isEqualTo(PriorityFunctionType.DEFAULT)
        assertions().assertThat(priorityFunction?.params?.size).isEqualTo(2)
        assertions().assertThat(priorityFunction?.params?.map{it.name}?.sorted()?.toList()).isEqualTo(listOf("param1", "param2"))
        assertions().assertThat(priorityFunction?.params?.map{it.value}?.sorted()?.toList()).isEqualTo(listOf("100", "200"))
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/priority_function/before-persist.xml"])
    @ExpectedDatabase("classpath:fixtures/repository/priority_function/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun persistBatchWorks() {
        val persisted = mapper.persistBatch(
            listOf(
                PriorityFunctionEntity(null, PriorityFunctionType.ARRIVAL_TIME, emptyList()),
                PriorityFunctionEntity(null, PriorityFunctionType.ARRIVAL_TIME, emptyList()),
                PriorityFunctionEntity(null, PriorityFunctionType.DEFAULT, emptyList())
            )
        )
        assertions().assertThat(persisted).hasSize(3)
        assertions().assertThat(persisted.map { it.type })
            .containsExactlyInAnyOrder(
                PriorityFunctionType.ARRIVAL_TIME, PriorityFunctionType.ARRIVAL_TIME,
                PriorityFunctionType.DEFAULT)
    }
}
