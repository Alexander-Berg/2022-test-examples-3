package ru.yandex.market.abo.core.dictionary

import java.time.LocalDateTime
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.CANCELLATION_RATE
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.CROSSDOCK_LATE_SHIP_RATE
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.CROSSDOCK_PLAN_FACT_RATE
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.CROSSDOCK_RETURN_RATE
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.DSBS_CANCELLATION_RATE
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.DSBS_LATE_DELIVERY_RATE
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.DSBS_RETURN_RATE
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.FF_LATE_SHIP_RATE
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.FF_PLANFACT_RATE
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.FF_RETURN_RATE
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.LATE_SHIP_RATE
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.RETURN_RATE
import ru.yandex.market.abo.core.rating.partner.PartnerRating
import ru.yandex.market.abo.core.rating.partner.PartnerRatingRepo
import ru.yandex.market.abo.core.rating.partner.details.ComponentDetails
import ru.yandex.market.abo.core.rating.partner.details.PartnerRatingDetails
import ru.yandex.market.abo.cpa.order.model.PartnerModel
import ru.yandex.market.abo.cpa.order.model.PartnerModel.CLICK_AND_COLLECT
import ru.yandex.market.abo.cpa.order.model.PartnerModel.CROSSDOCK
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBB
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBS
import ru.yandex.market.abo.cpa.order.model.PartnerModel.FULFILLMENT
import ru.yandex.market.abo.cpa.order.model.PartnerModel.valueOf

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 06.09.2021
 */
class PartnerRatingDictionaryTest @Autowired constructor(
    private val jdbcTemplate: JdbcTemplate,
    private val ratingRepo: PartnerRatingRepo.RatingRepo
) : MstatDictionaryTest() {

    override fun getDictionaryConfig(): MstatDictionaryConfig {
        return MstatDictionaryConfig(
            ENTITY_NAME,
            setOf("calc_time", "partner_id", "partner_model", "total_rating", "details", "actual")
        )
    }

    override fun initDictionarySourceData() {
        val ratings = listOf(
            buildPartnerRating(DSBB, hashSetOf(LATE_SHIP_RATE, CANCELLATION_RATE, RETURN_RATE)),
            buildPartnerRating(DSBS, hashSetOf(DSBS_LATE_DELIVERY_RATE, DSBS_CANCELLATION_RATE, DSBS_RETURN_RATE)),
            buildPartnerRating(CLICK_AND_COLLECT, hashSetOf(LATE_SHIP_RATE, CANCELLATION_RATE, RETURN_RATE)),
            buildPartnerRating(CROSSDOCK, hashSetOf(CROSSDOCK_LATE_SHIP_RATE, CROSSDOCK_PLAN_FACT_RATE, CROSSDOCK_RETURN_RATE)),
            buildPartnerRating(FULFILLMENT, hashSetOf(FF_LATE_SHIP_RATE, FF_PLANFACT_RATE, FF_RETURN_RATE))
        )
        ratingRepo.saveAll(ratings)
    }

    override fun validateResult() {
        val ratingDetailsByModel = HashMap<PartnerModel, JsonNode>()
        jdbcTemplate.query("""
            SELECT partner_model, details
            FROM $ENTITY_NAME
        """.trimIndent()) { rs, _ ->
            ratingDetailsByModel.put(
                valueOf(rs.getString("partner_model")),
                OBJECT_MAPPER.readTree(rs.getString("details"))
            )
        }

        assertEquals(5, ratingDetailsByModel.size)
        assertTrue(ratingDetailsByModel.keys.containsAll(hashSetOf(DSBB, DSBS, CLICK_AND_COLLECT, CROSSDOCK, FULFILLMENT)))
        ratingDetailsByModel.forEach(this::validateDetailsForModel)
    }

    private fun buildPartnerRating(partnerModel: PartnerModel, componentTypes: Set<RatingMetric>) = PartnerRating(
        LocalDateTime.now(),
        PARTNER_ID,
        partnerModel,
        emptyMap(),
        PartnerRatingDetails(ORDERS_COUNT, componentTypes.map { ComponentDetails(it, COMPONENT_VALUE) }),
        TOTAL_RATING_VALUE
    )

    private fun validateDetailsForModel(partnerModel: PartnerModel, details: JsonNode) {
        val ordersCount = details.get("ordersCount")
        assertTrue(ordersCount.isInt)
        assertEquals(ORDERS_COUNT, ordersCount.asInt())

        val ratesMap = details.get("ratesMap")

        when (partnerModel) {
            DSBB, DSBS, CLICK_AND_COLLECT -> {
                validateComponent("LATE_SHIP_RATE", ratesMap)
                validateComponent("CANCELLATION_RATE", ratesMap)
                validateComponent("RETURN_RATE", ratesMap)
            }
            CROSSDOCK, FULFILLMENT -> {
                validateComponent("FF_LATE_SHIP_RATE", ratesMap)
                validateComponent("FF_PLAN_FACT_RATE", ratesMap)
                validateComponent("FF_RETURN_RATE", ratesMap)
            }
            else -> throw IllegalStateException("Unsupported partner model")
        }
    }

    private fun validateComponent(dictionaryComponentType: String, ratesMap: JsonNode) {
        assertTrue(ratesMap.has(dictionaryComponentType))

        val componentValue = ratesMap.get(dictionaryComponentType)
        assertTrue(componentValue.isDouble)
        assertEquals(COMPONENT_VALUE, componentValue.asDouble())
    }

    companion object {
        private val OBJECT_MAPPER = ObjectMapper()

        private const val ENTITY_NAME = "v_yt_exp_partner_rating"

        private const val PARTNER_ID = 12345L
        private const val ORDERS_COUNT = 123
        private const val TOTAL_RATING_VALUE = 43.0
        private const val COMPONENT_VALUE = 4.5
    }
}
