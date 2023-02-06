package ru.yandex.market.logistics.yard.repository.mapper

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.domain.service.ServiceParamType
import ru.yandex.market.logistics.yard_v2.repository.mapper.ServiceParamMapper

class ServiceParamMapperTest(@Autowired private val mapper: ServiceParamMapper): AbstractSecurityMockedContextualTest() {
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/service_param/before.xml"])
    fun getByServiceIdAndName() {
        val paramEntity = mapper.getByServiceIdAndName(1, "LOADING_PREFIX")

        assertions().assertThat(paramEntity!!.id).isEqualTo(1)
        assertions().assertThat(paramEntity.serviceId).isEqualTo(1)
        assertions().assertThat(paramEntity.name).isEqualTo(ServiceParamType.LOADING_PREFIX)
        assertions().assertThat(paramEntity.value).isEqualTo("5")
    }
}
