package ru.yandex.market.logistics.yard.repository.mapper

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.domain.entity.LastTicketNumberEntity
import ru.yandex.market.logistics.yard_v2.repository.mapper.LastTicketNumberMapper

class LastTicketNumberMapperTest(@Autowired private val mapper: LastTicketNumberMapper) : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/last_ticket_number/before.xml"])
    @ExpectedDatabase("classpath:fixtures/repository/last_ticket_number/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun persist() {
        val persisted = mapper.persist(
            LastTicketNumberEntity(capacityId = 100, requestType = "TEST_TYPE_2")
        )
        assertions().assertThat(persisted).isNotNull
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/last_ticket_number/before.xml"])
    @ExpectedDatabase("classpath:fixtures/repository/last_ticket_number/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun getById() {
        val persisted = mapper.getById(100)
        assertions().assertThat(persisted).isNotNull
        assertions().assertThat(persisted!!.requestType).isEqualTo("SIGNING_DOCUMENTS")
        assertions().assertThat(persisted.number).isEqualTo(42)

    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/last_ticket_number/before.xml"])
    @ExpectedDatabase("classpath:fixtures/repository/last_ticket_number/after_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun updateNumber() {
        val persisted = mapper.updateNumber(100, "SIGNING_DOCUMENTS")
        assertions().assertThat(persisted).isEqualTo(43)
    }
}
