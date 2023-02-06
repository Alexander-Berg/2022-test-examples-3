package ru.yandex.market.logistics.yard.repository.mapper

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.domain.entity.RestrictionParamEntity
import ru.yandex.market.logistics.yard.client.dto.configurator.types.RestrictionParamType
import ru.yandex.market.logistics.yard_v2.repository.mapper.RestrictionParamMapper

class RestrictionParamMapperTest(@Autowired private val mapper: RestrictionParamMapper):
    AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/restriction_param/before.xml"])
    @ExpectedDatabase("classpath:fixtures/repository/restriction_param/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun persistBatchWorks() {
        val persisted = mapper.persistBatch(
            listOf(
                RestrictionParamEntity(null, 1, RestrictionParamType.RESTRICTION_TYPES, "123"),
                RestrictionParamEntity(null, 2, RestrictionParamType.MINUTES_TO_STAY_IN_STATE, "345"),
            )
        )
        assertions().assertThat(persisted).hasSize(2)
        assertions().assertThat(persisted.map { it.name })
            .containsExactlyInAnyOrder(
                RestrictionParamType.RESTRICTION_TYPES, RestrictionParamType.MINUTES_TO_STAY_IN_STATE)
    }
}
