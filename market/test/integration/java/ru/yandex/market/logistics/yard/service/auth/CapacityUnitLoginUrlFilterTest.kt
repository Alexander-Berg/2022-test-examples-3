package ru.yandex.market.logistics.yard.service.auth

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest

class CapacityUnitLoginUrlFilterTest(@Autowired val capacityUnitLoginUrlFilter: CapacityUnitLoginUrlFilter) :
    AbstractSecurityMockedContextualTest() {

    @Test
    fun getCapacityUnitIdTest() {

        val capacityUnitId =
            capacityUnitLoginUrlFilter.getCapacityUnitId("/window/22")

        assertions().assertThat(capacityUnitId).isEqualTo(22L)

    }


    @Test
    @DatabaseSetup("classpath:fixtures/service/auth/capacity_unit.xml")
    fun getCapacityUnitIdTestWhenUUID() {

        val capacityUnitId =
            capacityUnitLoginUrlFilter.getCapacityUnitId("/window/uuid/0673bb5f-043e-42ee-8e6e-b4a28191bd68")

        assertions().assertThat(capacityUnitId).isEqualTo(1L)

    }


}
