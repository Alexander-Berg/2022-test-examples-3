package ru.yandex.market.abo.cpa.order.limit.cutoff

import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import ru.yandex.market.abo.cpa.order.limit.CpaOrderLimitPartner
import ru.yandex.market.abo.cpa.order.limit.count.CpaOrderCountService
import ru.yandex.market.abo.cpa.order.limit.model.CpaOrderLimit
import ru.yandex.market.abo.cpa.order.limit.model.CpaOrderLimitReason
import ru.yandex.market.abo.cpa.order.model.PartnerModel
import ru.yandex.market.abo.util.kotlin.toDate

/**
 * @author komarovns
 */
class CpaOrderLimitCutoffManagerUnitTest {
    private val cpaOrderCountService: CpaOrderCountService = mock()
    private val cpaOrderLimitCutoffManager = CpaOrderLimitCutoffManager(
        mock(), mock(), cpaOrderCountService, mock(), mock()
    )

    @ParameterizedTest
    @CsvSource(
        "true, -1",
        "true,  0",
        "false, 1")
    fun `expired limits by date`(expired: Boolean, expiryDateDiff: Long) {
        val limit = limit(expiryDate = LocalDate.now().plusDays(expiryDateDiff))
        cpaOrderCountService.stub {
            on { loadTotalCount(eq(PARTNER)) } doReturn 0
        }
        assertEquals(expired, cpaOrderLimitCutoffManager.isLimitExpired(limit))
    }

    @ParameterizedTest
    @CsvSource(
        "false,  9",
        "true,  10",
        "true,  11",
    )
    fun `expired limits by count`(expired: Boolean, ordersCount: Long) {
        val limit = limit(expiryCount = 10)
        cpaOrderCountService.stub {
            on { loadTotalCount(eq(PARTNER)) } doReturn ordersCount
            on { loadTotalFrom(eq(PARTNER), any()) } doReturn ordersCount
        }
        assertEquals(expired, cpaOrderLimitCutoffManager.isLimitExpired(limit))
    }

    private fun limit(expiryCount: Int? = null, expiryDate: LocalDate? = null) = CpaOrderLimit(
        PARTNER.partnerId, PARTNER.partnerModel, CpaOrderLimitReason.MANUAL, 0, expiryDate?.toDate(), expiryCount
    ).apply { creationTime = LocalDate.now().toDate() }

    private companion object {
        private val PARTNER = CpaOrderLimitPartner(0, PartnerModel.DSBS)
    }
}
