package ru.yandex.market.logistics.yard.repository.mapper

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.repository.mapper.EntityParamMapper

class EntityParamMapperTest(@Autowired val mapper: EntityParamMapper) : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/entity_param/before.xml"])
    fun getById() {
        val param = mapper.getById(1)

        assertions().assertThat(param?.name).isEqualTo("param")
        assertions().assertThat(param?.value).isEqualTo("100")
    }
}
