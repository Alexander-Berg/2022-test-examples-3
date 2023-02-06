package ru.yandex.market.logistics.yard.repository.mapper

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.domain.entity.CapacityUnitEntity
import ru.yandex.market.logistics.yard_v2.repository.mapper.CapacityUnitMapper

class CapacityUnitMapperTest(@Autowired val mapper: CapacityUnitMapper) : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/capacity_unit/before.xml"])
    fun getById() {
        val capacityUnit = mapper.getById(1)

        assertions().assertThat(capacityUnit?.id).isEqualTo(1)
        assertions().assertThat(capacityUnit?.capacityId).isEqualTo(1)
        assertions().assertThat(capacityUnit?.readableName).isEqualTo("test_capacity_unit")
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/capacity_unit/before.xml"])
    fun getByCapacityId() {
        val byCapacityId = mapper.getByCapacityId(2)

        assertions().assertThat(byCapacityId).isNotEmpty
        val capacityUnit = byCapacityId[0]
        assertions().assertThat(capacityUnit.id).isEqualTo(2)
        assertions().assertThat(capacityUnit.capacityId).isEqualTo(2)
        assertions().assertThat(capacityUnit.readableName).isEqualTo("test_capacity_unit_2")
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/capacity_unit/persist/before.xml"])
    @ExpectedDatabase(value = "classpath:fixtures/repository/capacity_unit/persist/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun persist() {
        mapper.insert(CapacityUnitEntity(null, null, 1, "test_capacity_unit", true))
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/capacity_unit/persist/before.xml"])
    @ExpectedDatabase("classpath:fixtures/repository/capacity_unit/persist/after_batch.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun persistBatch() {
        val persisted = mapper.persistBatch(
            listOf(
                CapacityUnitEntity(null, null, 1, "test_capacity_unit_1", true),
                CapacityUnitEntity(null, null, 1, "test_capacity_unit_2", true),
                CapacityUnitEntity(null, null, 1, "test_capacity_unit_3", false)
            )
        )
        assertions().assertThat(persisted).hasSize(3)
    }
}
