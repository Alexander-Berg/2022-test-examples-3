package ru.yandex.market.mbi.orderservice.common.service.yt.dynamic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class YtIndexTablesServiceTest {

    @Test
    fun `verify asQueryPart method`() {
        val emptyRange = YtIndexTablesService.OrderIdRange(null, null)
        assertThat(emptyRange.asQueryPart())
            .isEqualTo(" and  orderId < 1000000000000")

        val hasStart = YtIndexTablesService.OrderIdRange(123, null)
        assertThat(hasStart.asQueryPart("o.orderId")).isEqualTo(
            " and  o.orderId >= 123 and  o.orderId < 1000000000000"
        )

        val hasEnd = YtIndexTablesService.OrderIdRange(null, 456)
        assertThat(hasEnd.asQueryPart()).isEqualTo(
            " and  orderId < 456"
        )

        val hasBoth = YtIndexTablesService.OrderIdRange(456, 777)
        assertThat(hasBoth.asQueryPart("orders.orderId")).isEqualTo(
            " and  orders.orderId >= 456 and  orders.orderId < 777"
        )
    }
}
