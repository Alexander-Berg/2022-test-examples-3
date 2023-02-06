package ru.yandex.market.logistics.yard.facade

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.client.dto.registration.ClientDataDto
import ru.yandex.market.logistics.yard.client.dto.registration.TicketRequestForYardClientDto
import ru.yandex.market.logistics.yard_v2.facade.TicketRegistrationFacade

class TicketRegistrationFacadeTest(
    @Autowired private val ticketFacade: TicketRegistrationFacade
) : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/ticket-facade/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/facade/ticket-facade/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getTicketError() {
        try {
            ticketFacade.getTicket(getTicketRequest("WRONG_TYPE"), 100)
        } catch (e: Exception) {
            assertions().assertThat(e.message)
                .contains("Wrong request type = WRONG_TYPE")
        }
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/ticket-facade/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/facade/ticket-facade/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getTicketSuccess() {
        val createClientAndGenerateNewTicket =
            ticketFacade.getTicket(getTicketRequest("SHIPMENT"), 100)

        assertions().assertThat(createClientAndGenerateNewTicket.ticketNumber).isEqualTo("1001")
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/ticket-facade/before_with_waiting_time.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/facade/ticket-facade/after_with_waiting_time.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getTicketWithApproximateWaitingTimeSuccess() {
        val createClientAndGenerateNewTicket =
            ticketFacade.getTicket(getTicketRequest("SHIPMENT"), 100)

        assertions().assertThat(createClientAndGenerateNewTicket.ticketNumber).isEqualTo("1001")
        assertions().assertThat(createClientAndGenerateNewTicket.approximateWaitingTimeSeconds).isEqualTo(150)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/ticket-facade/signing/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/facade/ticket-facade/signing/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getTicketForSignSuccess() {
        val createClientAndGenerateNewTicket =
            ticketFacade.getTicket(getTicketRequestForSigning("Р042"), 100)

        assertions().assertThat(createClientAndGenerateNewTicket.ticketNumber).isEqualTo("Р042")
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/ticket-facade/signing/before.xml"])
    fun getTicketForSignWrongNumber() {
        try {
            ticketFacade.getTicket(getTicketRequestForSigning("Д111"), 100)
        } catch (e: Exception) {
            assertions().assertThat(e.message)
                .contains("Ticket with ticketNumber Д111 not found")
        }
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/ticket-facade/signing/before.xml"])
    fun getTicketForSignNoData() {
        try {
            ticketFacade.getTicket(getTicketRequestForSigning(null), 100)
        } catch (e: Exception) {
            assertions().assertThat(e.message)
                .contains("Field 'driverPhoneNumber' is empty")
        }
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/ticket-facade/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/facade/ticket-facade/after_with_recalc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getTicketForSCWithRecalcSuccess() {
        val createClientAndGenerateNewTicket =
            ticketFacade.getTicket(TicketRequestForYardClientDto("SHIPMENT", createClientDataForSC(true)), 100)

        assertions().assertThat(createClientAndGenerateNewTicket.ticketNumber).isEqualTo("1001")
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/ticket-facade/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/facade/ticket-facade/after_without_recalc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getTicketForSCWithoutRecalcSuccess() {
        val createClientAndGenerateNewTicket =
            ticketFacade.getTicket(TicketRequestForYardClientDto("SHIPMENT", createClientDataForSC(false)), 100)

        assertions().assertThat(createClientAndGenerateNewTicket.ticketNumber).isEqualTo("1001")
    }

    private fun getTicketRequestForSigning(ticketNumber: String?): TicketRequestForYardClientDto {
        return TicketRequestForYardClientDto(
            "SIGNING_DOCUMENTS",
            ClientDataDto(
                ticketNumber,
                null,
                null,
                null,
                null,
                takeAwayReturns = null,
                takeAwayPallets = null
            )
        )
    }

    private fun getTicketRequest(type: String): TicketRequestForYardClientDto {
        return TicketRequestForYardClientDto(type, createClientData())
    }

    private fun createClientData(): ClientDataDto {
        return ClientDataDto(
            null,
            "test_document",
            "E105TM53",
            "8-800-555-35-35",
            "BIG",
            takeAwayReturns = false,
            takeAwayPallets = false
        )
    }

    private fun createClientDataForSC(needRecalc: Boolean): ClientDataDto {
        return ClientDataDto(
            null,
            null,
            "E105TM53",
            "8-800-555-35-35",
            null,
            takeAwayReturns = null,
            takeAwayPallets = null,
            needRecalc = needRecalc
        )
    }
}
