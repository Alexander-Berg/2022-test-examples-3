package ru.yandex.market.logistics.yard.facade

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.facade.TicketNumberGenerator

class TicketNumberGeneratorTest(
    @Autowired private val ticketNumberGenerator: TicketNumberGenerator
) : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/ticket_number_generator/before.xml"])
    @ExpectedDatabase("classpath:fixtures/facade/ticket_number_generator/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun ticketNumberGeneratorIfPreviousExist() {
        val ticket = ticketNumberGenerator.generateNewTicketNumber(100, 1, "SIGNING_DOCUMENTS")
        assertions().assertThat(ticket).isNotNull
        assertions().assertThat(ticket).isEqualTo("3043")
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/ticket_number_generator/before.xml"])
    @ExpectedDatabase("classpath:fixtures/facade/ticket_number_generator/after_creation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun ticketNumberGeneratorIfPreviousNotExist() {
        val ticket = ticketNumberGenerator.generateNewTicketNumber(100, 1, "LOADING")
        assertions().assertThat(ticket).isNotNull
        assertions().assertThat(ticket).isEqualTo("2000")
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/ticket_number_generator/before_with_param.xml"])
    @ExpectedDatabase("classpath:fixtures/facade/ticket_number_generator/after_creation_with_param.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun ticketNumberGeneratorIfPreviousNotExistWithServiceParam() {
        val ticket = ticketNumberGenerator.generateNewTicketNumber(100, 1, "LOADING")
        assertions().assertThat(ticket).isNotNull
        assertions().assertThat(ticket).isEqualTo("5000")
    }
}
