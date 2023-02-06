package ru.yandex.market.abo.cpa.quality.recheck.listener

import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.abo.core.prepay.PrepayRequestManager
import ru.yandex.market.abo.core.spark.risks.SparkRisksManager
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicket
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketType
import ru.yandex.market.api.cpa.yam.dto.PrepayRequestDTO
import ru.yandex.market.api.cpa.yam.entity.RequestType

class SparkRisksRecheckListenerTest {

    val sparkRiskManager: SparkRisksManager = mock()
    val prepayRequestManager: PrepayRequestManager = mock()

    @Test
    fun `test create with broken spark risks`() {
        val requestId = 2L
        val request: PrepayRequestDTO = mock()
        whenever(request.requestType).thenReturn(RequestType.MARKETPLACE)
        whenever(prepayRequestManager.getPrepayRequest(requestId, null)).thenReturn(request)

        whenever(sparkRiskManager.loadRisks(anyString()))
            .thenThrow(RuntimeException("Spark-API is down"))

        val listener = SparkRisksRecheckListener(sparkRiskManager, prepayRequestManager)
        val ticket = RecheckTicket(-1L, RecheckTicketType.BLUE_PREMODERATION, "synopsis").apply {
            this.sourceId = requestId
        }
        listener.notify(ticket)
    }
}
