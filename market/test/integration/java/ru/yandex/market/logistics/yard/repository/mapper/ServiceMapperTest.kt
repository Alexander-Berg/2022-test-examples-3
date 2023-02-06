package ru.yandex.market.logistics.yard.repository.mapper

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.domain.entity.ServiceEntity
import ru.yandex.market.logistics.yard_v2.repository.mapper.CapacityUnitMapper
import ru.yandex.market.logistics.yard_v2.repository.mapper.ServiceMapper
import java.util.*

class ServiceMapperTest(@Autowired private val mapper: ServiceMapper,
                        @Autowired private val capacityUnitMapper: CapacityUnitMapper) : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/service/before.xml"])
    @ExpectedDatabase("classpath:fixtures/repository/service/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun persistOptionalWhenSuccessfulInsert() {
        val id = mapper.persistOptional(ServiceEntity(1, null, "name", null))
        assertions().assertThat(id).isEqualTo(1)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/service/before.xml"])
    @ExpectedDatabase("classpath:fixtures/repository/service/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun persistWhenUnsuccessfulInsert() {
        val id = mapper.persistOptional(ServiceEntity(2, null, "name", null))
        assertions().assertThat(id).isNull()
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/service/before.xml"])
    @ExpectedDatabase("classpath:fixtures/repository/service/after-update-initial-state-id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun updateInitialStateId() {
        mapper.updateInitialStateId(2, 22)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/service/before.xml"])
    fun findBySiteId() {
        val serviceIdBySiteId = mapper.getServiceIdBySiteId(1)
        assertions().assertThat(serviceIdBySiteId[0]).isEqualTo(2L)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/service/before.xml"])
    fun findByUUID() {
        val serviceIdByUUID = mapper.getByUUID(UUID.fromString("f1a82833-bb39-46df-b9cc-a2ec2d257a39"))
        assertions().assertThat(serviceIdByUUID!!.id).isEqualTo(2L)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/service/full_info_before.xml"])
    fun getFullInfo() {
        val services = mapper.getAllServicesWithCapacityUnits()

        assertions().assertThat(services.size).isEqualTo(2)

        val serviceEntity = services[0]
        assertions().assertThat(serviceEntity.id).isNotNull
        assertions().assertThat(serviceEntity.name).isNotNull

        val capacities = serviceEntity.capacities
        assertions().assertThat(capacities.size).isEqualTo(2)
        assertions().assertThat(capacities[0].capacityUnits).isNotEmpty
        assertions().assertThat(capacities[1].capacityUnits).isNotEmpty
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/service/full_info_before.xml"])
    fun getFullById() {
        val service = mapper.getFullById(2L)

        assertions().assertThat(service).isNotNull

        assertions().assertThat(service!!.id).isEqualTo(2L)
        assertions().assertThat(service.name).isEqualTo("service_with_capacity_units")

        val capacities = service.capacities
        assertions().assertThat(capacities.size).isEqualTo(2)
        assertions().assertThat(capacities[0].capacityUnits).isNotEmpty
        assertions().assertThat(capacities[1].capacityUnits).isNotEmpty
    }
}
