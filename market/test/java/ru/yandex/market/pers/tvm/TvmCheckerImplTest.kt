package ru.yandex.market.pers.tvm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import ru.yandex.market.pers.tvm.model.ServiceTicketWrapper
import ru.yandex.passport.tvmauth.TicketStatus
import java.util.Optional

/**
 * @author Ilya Kislitsyn / ilyakis@ / 18.03.2021
 */
class TvmCheckerImplTest {
    companion object {
        private const val TICKET = "ticket"
        private const val TVM_ID = 123144
        private const val TVM_ID_ISSUE = 3234135
        private const val TVM_ID_BAD = 524524
        const val ISSUER_UID = 3341
    }

    private val ticketFetcher = mock(TvmTicketFetcher::class.java)
    private val tvmChecker = TvmCheckerImpl(
        ticketFetcher,
        mapOf<String, Set<Int>>(
            Pair("one", setOf(TVM_ID)),
            Pair("two", setOf(TVM_ID_ISSUE))
        ),
        setOf(TVM_ID_ISSUE)
    )

    private fun mockTicketCheck(ticket: String, status: TicketStatus, sourceTvmId: Int, issuerUid: Long = 0L) {
        val ticketMock = mock(ServiceTicketWrapper::class.java)
        `when`(ticketFetcher.fetchServiceTicket(ticket)).thenReturn(ticketMock)
        `when`(ticketMock.status).thenReturn(status)
        `when`(ticketMock.src).thenReturn(sourceTvmId)
        `when`(ticketMock.issuerUid).thenReturn(issuerUid)
    }

    @Test
    fun testPositiveCheck() {
        mockTicketCheck(TICKET, TicketStatus.OK, TVM_ID)
        mockTicketCheck(TICKET + 1, TicketStatus.OK, TVM_ID_ISSUE, ISSUER_UID.toLong())
        assertEquals(Optional.empty<Any>(), tvmChecker.checkTvmGetError(TICKET))
        assertEquals(Optional.empty<Any>(), tvmChecker.checkTvmGetError(TICKET, listOf("one")))
        assertEquals(Optional.empty<Any>(), tvmChecker.checkTvmGetError(TICKET + 1))
    }

    @Test
    fun testNegativeCheck() {
        mockTicketCheck(TICKET, TicketStatus.EXPIRED, TVM_ID)
        mockTicketCheck(TICKET + 2, TicketStatus.OK, TVM_ID_BAD)
        mockTicketCheck(TICKET + 3, TicketStatus.OK, TVM_ID, ISSUER_UID.toLong())
        mockTicketCheck(TICKET + 4, TicketStatus.OK, TVM_ID)
        assertEquals(
            Optional.of("Tvm header is required: " + TvmUtils.SERVICE_TICKET_HEADER),
            tvmChecker.checkTvmGetError(null)
        )
        assertEquals(
            Optional.of("Tvm ticket is not valid"),
            tvmChecker.checkTvmGetError(TICKET + 1)
        )
        assertEquals(
            Optional.of("Tvm ticket is not valid: " + TicketStatus.EXPIRED),
            tvmChecker.checkTvmGetError(TICKET)
        )
        assertEquals(
            Optional.of("Access denied to tvmId = " + TVM_ID_BAD),
            tvmChecker.checkTvmGetError(TICKET + 2)
        )
        assertEquals(
            Optional.of("User-issued tvm tickets are not allowed to tvmId = " + TVM_ID),
            tvmChecker.checkTvmGetError(TICKET + 3)
        )

        // check trusted names
        assertEquals(
            Optional.of("Access denied to tvmId = " + TVM_ID),
            tvmChecker.checkTvmGetError(TICKET + 4, listOf("two"))
        )
        assertEquals(
            Optional.empty<Any>(),
            tvmChecker.checkTvmGetError(TICKET + 4, listOf())
        )
    }

    @Test
    fun testNegativeCheckException() {
        mockTicketCheck(TICKET, TicketStatus.EXPIRED, TVM_ID)
        mockTicketCheck(TICKET + 1, TicketStatus.OK, TVM_ID)

        // fail - expired
        assertThrows(IllegalArgumentException::class.java) { tvmChecker.checkTvm(TICKET) }

        // ok - mocked well
        tvmChecker.checkTvm(TICKET + 1)
    }

    @Test
    fun testMandatoryCheckOnOpt() {
        val tvmCheckerOpt = TvmCheckerImpl(
            ticketFetcher,
            mapOf<String, Set<Int>>(
                Pair("one", setOf(TVM_ID)),
                Pair("two", setOf(TVM_ID_ISSUE))
            ),
            setOf(TVM_ID_ISSUE)
        )
        tvmCheckerOpt.setTvmMandatory(false)
        assertEquals(Optional.empty<Any>(), tvmCheckerOpt.checkTvmGetError(null))
        assertEquals(
            Optional.of("Tvm header is required: " + TvmUtils.SERVICE_TICKET_HEADER),
            tvmChecker.checkTvmGetError(null)
        )
    }
}
