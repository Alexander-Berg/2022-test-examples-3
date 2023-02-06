package ru.yandex.market.abo.cpa.order.limit.startrek

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.EmptyTest
import ru.yandex.market.abo.cpa.order.limit.CpaOrderLimitService
import ru.yandex.market.abo.cpa.order.limit.model.CpaOrderLimit
import ru.yandex.market.abo.cpa.order.limit.model.CpaOrderLimitReason
import ru.yandex.market.abo.cpa.order.model.PartnerModel
import java.util.Date

class OrderLimitStartrekTicketServiceTest @Autowired constructor(
    private val orderLimitStartrekTicketService: OrderLimitStartrekTicketService,
    private val cpaOrderLimitService: CpaOrderLimitService,
    private val orderLimitStartrekTicketRepo: OrderLimitStartrekTicketRepo,
    private val pgJdbcTemplate: JdbcTemplate,
) : EmptyTest() {

    @BeforeEach
    fun init() {
        orderLimitStartrekTicketRepo.deleteAll()
        flushAndClear()
    }

    @Test
    fun `save and load new order limits`() {
        insertShop(2L)
        flushAndClear()
        val orderLimit = CpaOrderLimit(
            2L,
            PartnerModel.DSBS,
            CpaOrderLimitReason.OPERATIONAL_RATING,
            50,
            Date(),
            10,
        )
        val userId = -1L
        val savedOrderLimit = cpaOrderLimitService.addOrderLimitIfNotExistsOrDeleted(orderLimit, userId)
        orderLimitStartrekTicketRepo.save(
            OrderLimitStartrekTicket(
                orderLimitId = savedOrderLimit.id,
                shopId = orderLimit.shopId,
                userId = userId,
            )
        )
        flushAndClear()

        assertThat(orderLimitStartrekTicketService.getNewOrderLimits())
            .singleElement()
            .matches { it.orderLimitId == savedOrderLimit.id }
            .matches { it.shopId == savedOrderLimit.shopId }
            .matches { !it.created }
    }

    @Test
    fun `set created true`() {
        val orderLimitStartrekTicket = OrderLimitStartrekTicket(
            orderLimitId = 5L,
            shopId = 5L,
        )
        val saved = orderLimitStartrekTicketService.save(orderLimitStartrekTicket)
        orderLimitStartrekTicketService.setCreatedTrue(saved.id)
        flushAndClear()

        val result = orderLimitStartrekTicketService.findAll()
        assertThat(result.first().created).isTrue
        assertThat(orderLimitStartrekTicketService.getNewOrderLimits())
            .isEmpty()
    }

    private fun insertShop(shopId: Long) {
        pgJdbcTemplate.update(
            """
            INSERT INTO shop(id, is_enabled, ping_enabled, in_prd_base, is_offline, manager_id)
                 VALUES (?, TRUE, TRUE, TRUE, FALSE, 1234)
            """.trimIndent(),
            shopId
        )
        pgJdbcTemplate.update(
            """
            INSERT INTO ext_campaign_info(campaign_id, shop_id, start_date, first_start_date)
                 VALUES (?, ?, NOW(), NOW())
        """.trimIndent(),
            shopId, shopId
        )
    }
}
