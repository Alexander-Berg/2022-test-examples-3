package ru.yandex.market.abo.core.rating.partner.stat

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.jupiter.api.AfterEach
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.EmptyTest
import ru.yandex.market.abo.api.entity.rating.operational.PartnerRatingCalculationPeriodType
import ru.yandex.market.abo.api.entity.rating.operational.PartnerRatingCalculationPeriodType.LAST_ORDERS
import ru.yandex.market.abo.core.rating.partner.ORDERS_FOR_WEIGHTED_ORDERS_RATING_CALCULATION
import ru.yandex.market.abo.cpa.order.model.PartnerModel
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBB
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBS
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 05.07.2022
 */
abstract class PartnerRatingBaseTest(
    private val jdbcTemplate: JdbcTemplate
) : EmptyTest() {

    @AfterEach
    fun tearDown() {
        jdbcTemplate.execute("""
           TRUNCATE TABLE cpa_order_stat, cpa_order_delivery,
                          lom_order_checkpoint, cpa_order_param, cpa_order_limit_count
        """
        )
    }

    /**
     * Наполняет данными таблицы, которые используются в расчете рейтинга fbs.
     */
    protected fun initDsbbRatingSourceData(partnerId: Long, testDateTime: LocalDateTime) {
        jdbcTemplate.update(
            """
            insert into cpa_order_stat(order_id, shop_id, user_id, creation_date, processing, rgb)
            values (?, ?, ?, ?, ?, 1),
                   (?, ?, ?, ?, ?, 1),
                   (?, ?, ?, ?, ?, 1),
                   (?, ?, ?, ?, ?, 1)
        """.trimIndent(),
            DSBB_ORDER_ID_1, partnerId, -17L, testDateTime.minusDays(4), testDateTime.minusDays(4),
            DSBB_ORDER_ID_2, partnerId, -17L, testDateTime.minusDays(4), testDateTime.minusDays(4),
            DSBB_ORDER_ID_3, partnerId, -17L, testDateTime.minusDays(4), testDateTime.minusDays(4),
            DSBB_ORDER_ID_4, partnerId, -17L, testDateTime.minusDays(4), testDateTime.minusDays(4)
        )
        val esd = testDateTime.toLocalDate().minusDays(3)
        jdbcTemplate.update(
            """
            insert into cpa_order_delivery(order_id, by_shipment, delivery_partner_type)
            values (?, ?, 'YANDEX_MARKET'),
                   (?, ?, 'YANDEX_MARKET'),
                   (?, ?, 'YANDEX_MARKET'),
                   (?, ?, 'YANDEX_MARKET')
        """.trimIndent(),
            DSBB_ORDER_ID_1, esd,
            DSBB_ORDER_ID_2, esd,
            DSBB_ORDER_ID_3, esd,
            DSBB_ORDER_ID_4, esd
        )
        jdbcTemplate.update(
            """
            insert into lom_order_checkpoint(id, order_id, checkpoint_type_id, checkpoint_time, checkpoint_real_time)
            values (?, ?, 10, ?, ?)
        """.trimIndent(),
            1L, DSBB_ORDER_ID_1, esd.atStartOfDay().plusHours(12), esd.atStartOfDay().plusHours(15),
        )
        jdbcTemplate.update(
            """
            insert into cpa_order_param(order_id, param_type, items_update_time, items_update_reason_id)
            values (?, 'ITEMS_UPDATED_BY_PARTNER_FAULT', ?, ?)
        """.trimIndent(),
            DSBB_ORDER_ID_3, testDateTime.minusDays(2), HistoryEventReason.ITEMS_NOT_FOUND.id,
        )
        initOrderCount(
            partnerId, DSBB, testDateTime.minusDays(3).toLocalDate(),
            count = ORDERS_FOR_WEIGHTED_ORDERS_RATING_CALCULATION - 5)
    }

    fun getOrdersForDsbbLateShipRate(): Set<Long> =
        hashSetOf(DSBB_ORDER_ID_1, DSBB_ORDER_ID_2, DSBB_ORDER_ID_3, DSBB_ORDER_ID_4)

    fun getLateOrdersForDsbbLateShipRate(): Set<Long> = hashSetOf(DSBB_ORDER_ID_2, DSBB_ORDER_ID_3, DSBB_ORDER_ID_4)

    fun getCancelledOrUpdatedOrdersForDsbbCancellationRate(): Set<Long> = hashSetOf(DSBB_ORDER_ID_3)

    fun getOrdersForDsbbCancellationRate(): Set<Long> =
        hashSetOf(DSBB_ORDER_ID_1, DSBB_ORDER_ID_3)

    /**
     * Наполняет данными таблицы, которые используются в расчете рейтинга dbs.
     */
    fun initDsbsRatingSourceData(partnerId: Long, testDateTime: LocalDateTime) {
        initDsbsRatingSourceData(partnerId, testDateTime, LAST_ORDERS)
    }

    fun initDsbsRatingSourceData(
        partnerId: Long, testDateTime: LocalDateTime, periodType: PartnerRatingCalculationPeriodType
    ) {
        initDsbsOrder(partnerId, DSBS_ORDER_ID_1, false, testDateTime.minusDays(2))
        initDsbsOrder(partnerId, DSBS_ORDER_ID_2, false, testDateTime.minusDays(2))
        initDsbsOrder(partnerId, DSBS_ORDER_ID_3, false, testDateTime.minusDays(2))
        initDsbsOrder(partnerId, DSBS_ORDER_ID_4, true, testDateTime.minusDays(2))
        jdbcTemplate.update(
            """
            insert into cpa_order_param(order_id, param_type, items_update_time, items_update_reason_id)
            values (?, 'ITEMS_UPDATED_BY_PARTNER_FAULT', ?, ?)
        """.trimIndent(),
            DSBS_ORDER_ID_3, testDateTime.minusDays(2), HistoryEventReason.ITEMS_NOT_FOUND.id,
        )
        initOrderCount(
            partnerId, DSBS, testDateTime.minusDays(3).toLocalDate(),
            if (periodType == LAST_ORDERS) ORDERS_FOR_WEIGHTED_ORDERS_RATING_CALCULATION - 5
            else ORDERS_FOR_WEIGHTED_ORDERS_RATING_CALCULATION + 5
        )
    }

    fun getOrdersForDsbsLateDeliveryRate(): Set<Long> =
        hashSetOf(DSBS_ORDER_ID_1, DSBS_ORDER_ID_2, DSBS_ORDER_ID_3, DSBS_ORDER_ID_4)

    fun getLateOrdersForDsbsLateDeliveryRate(): Set<Long> =
        hashSetOf(DSBS_ORDER_ID_4)

    fun getCancelledOrUpdatedOrdersForDsbsCancellationRate(): Set<Long> = hashSetOf(DSBS_ORDER_ID_3)

    fun getOrdersForDsbsCancellationRate(): Set<Long> =
        hashSetOf(DSBS_ORDER_ID_1, DSBS_ORDER_ID_2, DSBS_ORDER_ID_3, DSBS_ORDER_ID_4)

    private fun initDsbsOrder(partnerId: Long, orderId: Long, isLate: Boolean, estimatedDateTime: LocalDateTime) {
        jdbcTemplate.update(
            """
            insert into cpa_order_stat(order_id, shop_id, user_id, creation_date, processing, delivery, delivered, rgb)
            values (?, ?, ?, ?, ?, ?, ?, 4)
        """.trimIndent(),
            orderId, partnerId, -17L,
            estimatedDateTime.minusDays(1),
            estimatedDateTime.minusDays(1),
            estimatedDateTime.minusDays(1),
            if (isLate) estimatedDateTime.plusDays(1) else estimatedDateTime
        )
        jdbcTemplate.update(
            """
            insert into cpa_order_delivery(order_id, by_order, delivery_partner_type)
            values (?, ?, 'SHOP')
        """.trimIndent(),
            orderId, estimatedDateTime.toLocalDate()
        )
    }

    private fun initOrderCount(partnerId: Long, partnerModel: PartnerModel, date: LocalDate, count: Int) {
        jdbcTemplate.update(
            """
            insert into cpa_order_limit_count(partner_id, partner_model, day, count)
            values (?, ?, ?, ?)
            """.trimIndent(),
            partnerId, partnerModel.id, date, count
        )
    }

    companion object {
        private const val DSBB_ORDER_ID_1 = 1111L
        private const val DSBB_ORDER_ID_2 = 1112L
        private const val DSBB_ORDER_ID_3 = 1113L
        private const val DSBB_ORDER_ID_4 = 1114L

        private const val DSBS_ORDER_ID_1 = 2111L
        private const val DSBS_ORDER_ID_2 = 2112L
        private const val DSBS_ORDER_ID_3 = 2113L
        private const val DSBS_ORDER_ID_4 = 2114L
    }
}
