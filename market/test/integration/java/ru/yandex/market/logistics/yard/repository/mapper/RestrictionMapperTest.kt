package ru.yandex.market.logistics.yard.repository.mapper

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.domain.entity.RestrictionEntity
import ru.yandex.market.logistics.yard.client.dto.configurator.types.RestrictionType
import ru.yandex.market.logistics.yard_v2.repository.mapper.RestrictionMapper

class RestrictionMapperTest(@Autowired private val mapper: RestrictionMapper): AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/restriction/before.xml"])
    @ExpectedDatabase("classpath:fixtures/repository/restriction/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun persistBatchWorks() {
        val persisted = mapper.persistBatch(
            listOf(
                RestrictionEntity(null, 1, RestrictionType.EVENT_REQUIRED, emptyList()),
                RestrictionEntity(null, 2, RestrictionType.ARRIVAL_TIME_NEAR_SLOT, emptyList())
            )
        )
        assertions().assertThat(persisted).hasSize(2)
        assertions().assertThat(persisted.map { it.type })
            .containsExactlyInAnyOrder(RestrictionType.EVENT_REQUIRED, RestrictionType.ARRIVAL_TIME_NEAR_SLOT)
    }
}
