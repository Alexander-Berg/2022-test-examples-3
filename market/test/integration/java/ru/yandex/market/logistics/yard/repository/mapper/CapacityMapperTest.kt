package ru.yandex.market.logistics.yard.repository.mapper

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.domain.entity.CapacityEntity
import ru.yandex.market.logistics.yard_v2.repository.mapper.CapacityMapper
import java.util.*

class CapacityMapperTest(@Autowired private val capacityMapper: CapacityMapper) :
    AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/capacity/before.xml"])
    @ExpectedDatabase("classpath:fixtures/repository/capacity/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun persistBatchWorks() {
        val persisted = capacityMapper.persistBatch(
            listOf(
                CapacityEntity(null, null, "NAME1", 5),
                CapacityEntity(null, null, "NAME2", 6)
            )
        )
        assertions().assertThat(persisted).hasSize(2)
        assertions().assertThat(persisted.map { it.name }).containsExactlyInAnyOrder("NAME1", "NAME2")
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/capacity/by_service/before.xml"])
    @ExpectedDatabase("classpath:fixtures/repository/capacity/by_service/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun findByServiceId() {
        val list = capacityMapper.getFullByServiceId(1)
        assertions().assertThat(list).isNotEmpty
        assertions().assertThat(list[0].capacityUnits).isNotEmpty
        assertions().assertThat(list[0].uuid).isEqualTo(UUID.fromString("1ae39a24-bd62-43d8-a3a0-fe0ebc9a1111"))
    }
}
