package ru.yandex.market.logistics.yard.repository.mapper

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.repository.mapper.CapacityParamMapper

class CapacityParamMapperTest(@Autowired private val capacityParamMapper: CapacityParamMapper) :
    AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/capacity/param/before.xml"])
    fun findByCapacityId() {
        val list = capacityParamMapper.getAllByCapacityId(1)
        assertions().assertThat(list).isNotEmpty
        assertions().assertThat(list[0].capacityId).isEqualTo(1)
        assertions().assertThat(list[0].name).isEqualTo("first")
        assertions().assertThat(list[1].capacityId).isEqualTo(1)
        assertions().assertThat(list[1].name).isEqualTo("second")
    }
}
