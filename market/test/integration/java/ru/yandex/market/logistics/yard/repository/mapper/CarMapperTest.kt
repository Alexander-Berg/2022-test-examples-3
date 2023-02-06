package ru.yandex.market.logistics.yard.repository.mapper

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.domain.entity.CarEntity
import ru.yandex.market.logistics.yard_v2.repository.mapper.CarMapper

class CarMapperTest(@Autowired val mapper: CarMapper) : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/car/before.xml"])
    fun getById() {
        val car = mapper.getById(1)

        assertions().assertThat(car?.id).isEqualTo(1)
        assertions().assertThat(car?.licencePlate).isEqualTo("E105TM53")
        assertions().assertThat(car?.requiredGateType).isEqualTo("big test car")
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/car/persist/before.xml"])
    @ExpectedDatabase(value = "classpath:fixtures/repository/car/persist/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun persist() {
        mapper.persist(CarEntity(null, "E911HK198", "small test car"))
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/car/before.xml"])
    fun update() {
        val car = mapper.getById(1)
        car!!.licencePlate = "N000EW05"

        val createdAt = car.createdAt
        val updatedAt = car.updatedAt

        mapper.update(car)

        val updatedCar = mapper.getById(1)
        assertions().assertThat(updatedCar!!.licencePlate).isEqualTo("N000EW05")
        assertions().assertThat(updatedCar.createdAt).isEqualTo(createdAt)
        assertions().assertThat(updatedCar.updatedAt).isAfter(updatedAt)
    }
}
