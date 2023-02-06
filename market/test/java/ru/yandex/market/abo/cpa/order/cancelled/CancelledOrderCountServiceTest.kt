package ru.yandex.market.abo.cpa.order.cancelled

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.EmptyTest

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 16.11.2021
 */
class CancelledOrderCountServiceTest @Autowired constructor(
    private val cancelledOrderCountService: CancelledOrderCountService,
    private val jdbcTemplate: JdbcTemplate
) : EmptyTest() {
    @Test
    fun `cancelled order count test`() {
        cancelledOrderCountService.saveCancelledOrders(listOf(OrderCancellationCount(1L, 1)))
        flushAndClear()
        cancelledOrderCountService.saveCancelledOrders(listOf(OrderCancellationCount(1L, 1)))
        flushAndClear()
        val count = jdbcTemplate.queryForObject(
            "SELECT count FROM cpa_order_cancelled_count_temp WHERE order_id = 1", Long::class.java
        )
        assertEquals(2, count)
    }
}
