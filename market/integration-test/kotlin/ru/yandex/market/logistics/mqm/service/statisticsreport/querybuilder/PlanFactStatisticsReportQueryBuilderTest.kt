package ru.yandex.market.logistics.mqm.service.statisticsreport.querybuilder

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.util.DateTimeUtils.MOSCOW_ZONE
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import java.time.LocalDateTime


class PlanFactStatisticsReportQueryBuilderTest : AbstractContextualTest() {

    @Autowired
    private lateinit var ffAssemblyQueryBuilder : AssemblyStatisticsReportQueryBuilder

    @Autowired
    private lateinit var ffShipmentQueryBuilder : ShipmentStatisticsReportQueryBuilder

    @Autowired
    private lateinit var ffDsIntakeQueryBuilder : IntakeStatisticsReportQueryBuilder

    @Autowired
    private lateinit var mcIntakeQueryBuilder : MarketCourierIntakeStatisticsReportQueryBuilder

    @Test
    fun buildIntakeQuery() {
        val fromPartnerType = PartnerType.FULFILLMENT
        val toPartnerType = PartnerType.DELIVERY

        assertSoftly {
            ffDsIntakeQueryBuilder.buildStatisticsForPeriodQuery(PERIOD_FROM, PERIOD_TO) shouldBe
                STATISTICS_QUERY_COMMON_PREFIX +
                "JOIN lom_waybill_segment lws ON pf.entity_id = lws.id AND pf.entity_type = 'LOM_WAYBILL_SEGMENT' " +
                "JOIN lom_waybill_segment previous_lws " +
                "    ON lws.order_id = previous_lws.order_id " +
                "    AND lws.waybill_segment_index = previous_lws.waybill_segment_index + 1 " +
                "WHERE pf.expected_status = 'IN' " +
                "    AND pf.expected_status_datetime <= '${PERIOD_TO}' " +
                "    AND pf.created <= '${PERIOD_TO}' " +
                "    AND (pf.fact_status_datetime IS NULL OR pf.fact_status_datetime > '${PERIOD_FROM}') " +
                "    AND (pf.end_of_processing_datetime IS NULL or pf.end_of_processing_datetime > '${PERIOD_FROM}') " +
                "    AND pf.plan_fact_status NOT IN (" +
                "        'IN_TIME','EXPIRED'" +
                "    ) " +
                "    AND previous_lws.partner_type = '$fromPartnerType' " +
                "AND (previous_lws.partner_subtype IS NULL OR previous_lws.partner_subtype <> 'MARKET_COURIER') " +
                "AND lws.partner_type = '$toPartnerType' " +
                "AND (lws.partner_subtype IS NULL OR lws.partner_subtype <> 'MARKET_COURIER') " +
                STATISTICS_QUERY_COMMON_POSTFIX

            ffDsIntakeQueryBuilder.buildPartnersWithMaxPlanFactsQuery(PERIOD_FROM) shouldBe
                MAX_PLAN_FACTS_QUERY_COMMON_PREFIX +
                "JOIN lom_waybill_segment previous_lws " +
                "    ON lws.order_id = previous_lws.order_id " +
                "    AND lws.waybill_segment_index = previous_lws.waybill_segment_index + 1 " +
                "WHERE pf.expected_status = 'IN' " +
                "    AND pf.expected_status_datetime <= '${PERIOD_FROM}' " +
                "    AND pf.created <= '${PERIOD_FROM}' " +
                "    AND (pf.fact_status_datetime IS NULL OR pf.fact_status_datetime > '${PERIOD_FROM}') " +
                "    AND (pf.end_of_processing_datetime IS NULL or pf.end_of_processing_datetime > '${PERIOD_FROM}') " +
                "    AND pf.plan_fact_status NOT IN (" +
                "        'IN_TIME','EXPIRED'" +
                "    ) " +
                "    AND previous_lws.partner_type = '$fromPartnerType' " +
                "AND (previous_lws.partner_subtype IS NULL OR previous_lws.partner_subtype <> 'MARKET_COURIER') " +
                "AND lws.partner_type = '$toPartnerType' " +
                "AND (lws.partner_subtype IS NULL OR lws.partner_subtype <> 'MARKET_COURIER') " +
                "GROUP BY partner ORDER BY amount desc"
        }
    }

    @Test
    fun buildShipmentQuery() {
        val partnerType = PartnerType.FULFILLMENT

        assertSoftly {
            ffShipmentQueryBuilder.buildStatisticsForPeriodQuery(PERIOD_FROM, PERIOD_TO) shouldBe
                STATISTICS_QUERY_COMMON_PREFIX +
                "JOIN lom_waybill_segment lws ON pf.entity_id = lws.id AND pf.entity_type = 'LOM_WAYBILL_SEGMENT' " +
                "WHERE pf.expected_status = 'OUT' " +
                "    AND pf.expected_status_datetime <= '${PERIOD_TO}' " +
                "    AND pf.created <= '${PERIOD_TO}' " +
                "    AND (pf.fact_status_datetime IS NULL OR pf.fact_status_datetime > '${PERIOD_FROM}') " +
                "    AND (pf.end_of_processing_datetime IS NULL or pf.end_of_processing_datetime > '${PERIOD_FROM}') " +
                "    AND pf.plan_fact_status NOT IN (" +
                "        'IN_TIME','EXPIRED'" +
                "    ) " +
                "    AND lws.partner_type = '$partnerType' " +
                STATISTICS_QUERY_COMMON_POSTFIX

            ffShipmentQueryBuilder.buildPartnersWithMaxPlanFactsQuery(PERIOD_FROM) shouldBe
                MAX_PLAN_FACTS_QUERY_COMMON_PREFIX +
                "WHERE pf.expected_status = 'OUT' " +
                "    AND pf.expected_status_datetime <= '${PERIOD_FROM}' " +
                "    AND pf.created <= '${PERIOD_FROM}' " +
                "    AND (pf.fact_status_datetime IS NULL OR pf.fact_status_datetime > '${PERIOD_FROM}') " +
                "    AND (pf.end_of_processing_datetime IS NULL or pf.end_of_processing_datetime > '${PERIOD_FROM}') " +
                "    AND pf.plan_fact_status NOT IN (" +
                "        'IN_TIME','EXPIRED'" +
                "    ) " +
                "    AND lws.partner_type = '$partnerType' " +
                "GROUP BY partner ORDER BY amount desc"
        }
    }

    @Test
    fun buildAssemblyQuery() {
        val segmentType = SegmentType.FULFILLMENT

        assertSoftly {
            ffAssemblyQueryBuilder.buildStatisticsForPeriodQuery(PERIOD_FROM, PERIOD_TO) shouldBe
                STATISTICS_QUERY_COMMON_PREFIX +
                " " +
                "WHERE pf.expected_status = 'TRANSIT_PREPARED' " +
                "    AND pf.expected_status_datetime <= '${PERIOD_TO}' " +
                "    AND pf.created <= '${PERIOD_TO}' " +
                "    AND (pf.fact_status_datetime IS NULL OR pf.fact_status_datetime > '${PERIOD_FROM}') " +
                "    AND (pf.end_of_processing_datetime IS NULL or pf.end_of_processing_datetime > '${PERIOD_FROM}') " +
                "    AND pf.plan_fact_status NOT IN (" +
                "        'IN_TIME','EXPIRED'" +
                "    ) " +
                "    AND pf.waybill_segment_type = '$segmentType' " +
                STATISTICS_QUERY_COMMON_POSTFIX

            ffAssemblyQueryBuilder.buildPartnersWithMaxPlanFactsQuery(PERIOD_FROM) shouldBe
                MAX_PLAN_FACTS_QUERY_COMMON_PREFIX +
                "WHERE pf.expected_status = 'TRANSIT_PREPARED' " +
                "    AND pf.expected_status_datetime <= '${PERIOD_FROM}' " +
                "    AND pf.created <= '${PERIOD_FROM}' " +
                "    AND (pf.fact_status_datetime IS NULL OR pf.fact_status_datetime > '${PERIOD_FROM}') " +
                "    AND (pf.end_of_processing_datetime IS NULL or pf.end_of_processing_datetime > '${PERIOD_FROM}') " +
                "    AND pf.plan_fact_status NOT IN (" +
                "        'IN_TIME','EXPIRED'" +
                "    ) " +
                "    AND pf.waybill_segment_type = '$segmentType' " +
                "GROUP BY partner ORDER BY amount desc"
        }
    }

    @Test
    fun buildMarketCourierQuery() {
        assertSoftly {
            mcIntakeQueryBuilder.buildStatisticsForPeriodQuery(PERIOD_FROM, PERIOD_TO) shouldBe
                STATISTICS_QUERY_COMMON_PREFIX +
                "JOIN lom_waybill_segment lws ON pf.entity_id = lws.id AND pf.entity_type = 'LOM_WAYBILL_SEGMENT' " +
                "WHERE pf.expected_status = 'IN' " +
                "    AND pf.expected_status_datetime <= '${PERIOD_TO}' " +
                "    AND pf.created <= '${PERIOD_TO}' " +
                "    AND (pf.fact_status_datetime IS NULL OR pf.fact_status_datetime > '${PERIOD_FROM}') " +
                "    AND (pf.end_of_processing_datetime IS NULL or pf.end_of_processing_datetime > '${PERIOD_FROM}') " +
                "    AND pf.plan_fact_status NOT IN (" +
                "        'IN_TIME','EXPIRED'" +
                "    ) " +
                "    AND lws.partner_subtype = 'MARKET_COURIER' " +
                STATISTICS_QUERY_COMMON_POSTFIX

            mcIntakeQueryBuilder.buildPartnersWithMaxPlanFactsQuery(PERIOD_FROM) shouldBe
                MAX_PLAN_FACTS_QUERY_COMMON_PREFIX +
                "WHERE pf.expected_status = 'IN' " +
                "    AND pf.expected_status_datetime <= '${PERIOD_FROM}' " +
                "    AND pf.created <= '${PERIOD_FROM}' " +
                "    AND (pf.fact_status_datetime IS NULL OR pf.fact_status_datetime > '${PERIOD_FROM}') " +
                "    AND (pf.end_of_processing_datetime IS NULL or pf.end_of_processing_datetime > '${PERIOD_FROM}') " +
                "    AND pf.plan_fact_status NOT IN (" +
                "        'IN_TIME','EXPIRED'" +
                "    ) " +
                "    AND lws.partner_subtype = 'MARKET_COURIER' " +
                "GROUP BY partner ORDER BY amount desc"
        }
    }

    companion object {
        private val PERIOD_FROM = LocalDateTime.of(2021, 8, 17, 10, 10, 10).atZone(MOSCOW_ZONE).toInstant()
        private val PERIOD_TO = LocalDateTime.of(2021, 8, 18, 12, 12, 12).atZone(MOSCOW_ZONE).toInstant()

        private const val STATISTICS_QUERY_COMMON_PREFIX = "WITH overdue_plan_facts AS ( " +
            "SELECT " +
            "    (CASE " +
            "       WHEN pf.expected_status_datetime < pf.created " +
            "       THEN pf.created " +
            "       ELSE pf.expected_status_datetime " +
            "    END) AS overdue_start, " +
            "    COALESCE(pf.end_of_processing_datetime, pf.fact_status_datetime) AS overdue_end " +
            "FROM plan_fact pf "

        private const val STATISTICS_QUERY_COMMON_POSTFIX = ") " +
            "SELECT overdue_start AS event_time, 1 AS delta " +
            "FROM overdue_plan_facts " +
            "UNION ALL " +
            "( " +
            "    SELECT overdue_end AS event_time, -1 AS delta " +
            "    FROM overdue_plan_facts " +
            ") " +
            "ORDER BY event_time"

        private const val MAX_PLAN_FACTS_QUERY_COMMON_PREFIX = "SELECT " +
            "    CONCAT('[id=', lws.partner_id, '] ', lws.partner_name) AS partner, " +
            "    CAST(SUM(1) AS INTEGER) AS amount " +
            "FROM plan_fact pf " +
            "JOIN lom_waybill_segment lws ON pf.entity_id = lws.id AND pf.entity_type = 'LOM_WAYBILL_SEGMENT' "
    }
}
