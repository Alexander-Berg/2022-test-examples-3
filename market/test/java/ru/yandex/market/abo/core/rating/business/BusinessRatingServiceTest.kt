package ru.yandex.market.abo.core.rating.business

import java.time.LocalDate
import java.util.Date
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.EmptyTest
import ru.yandex.market.abo.api.entity.order.limit.PublicOrderLimitReason
import ru.yandex.market.abo.api.entity.rating.business.PartnerLimitType
import ru.yandex.market.abo.api.entity.rating.business.RatingValueGrade
import ru.yandex.market.abo.api.entity.rating.operational.RatingPartnerType
import ru.yandex.market.abo.core.rating.partner.dynamic.PartnerRatingDynamic
import ru.yandex.market.abo.core.rating.partner.dynamic.PartnerRatingDynamicRepo
import ru.yandex.market.abo.core.rating.partner.dynamic.RatingComponentDiff
import ru.yandex.market.abo.core.rating.partner.dynamic.RatingComponentDiffsWrapper
import ru.yandex.market.abo.cpa.order.limit.CpaOrderLimitService
import ru.yandex.market.abo.cpa.order.limit.model.CpaOrderLimit
import ru.yandex.market.abo.cpa.order.limit.model.CpaOrderLimitReason.MANUAL
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBB
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramType.DROPSHIP

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 29.09.2021
 */
class BusinessRatingServiceTest @Autowired constructor(
    private val businessRatingService: BusinessRatingService,
    private val cpaOrderLimitService: CpaOrderLimitService,
    private val partnerRatingDynamicRepo: PartnerRatingDynamicRepo,
    private val jdbcTemplate: JdbcTemplate
) : EmptyTest() {

    @BeforeEach
    fun init() {
        jdbcTemplate.update("""
            insert into ext_organization_info(datasource_id, business_id, client_id)
            values (${PARTNER_ID}, ${BUSINESS_ID}, 2)
        """.trimIndent())

        jdbcTemplate.update("""
            insert into supplier(id, name)
            values (${PARTNER_ID}, '${PARTNER_NAME}')
        """.trimIndent())

        jdbcTemplate.update("""
            insert into ext_partner_placement_program(partner_id, program, status)
            values (${PARTNER_ID}, 'DROPSHIP', 'SUCCESS')
        """.trimIndent())

        partnerRatingDynamicRepo.saveAll(arrayListOf(buildDynamic()))

        cpaOrderLimitService.addIfNotExistsOrDeleted(
            CpaOrderLimit(PARTNER_ID, DSBB, MANUAL, 30, Date(), null), 1
        )

        flushAndClear()
    }

    @Test
    fun `get business ratings test`() {
        val ratings = businessRatingService.getRating(BUSINESS_ID, null, 3, null, null)
        assertEquals(1, ratings.partnerTypeRatings.size)
        ratings.partnerTypeRatings.forEach { page ->
            assertEquals(RatingPartnerType.DROPSHIP, page.partnerType)
            assertEquals(BusinessRatingService.COMPONENTS_BY_PLACEMENT_TYPE[DROPSHIP]!!, page.componentTypes)
            page.ratings.forEach { rating ->
                assertEquals(PARTNER_ID, rating.partnerId)
                assertEquals(PARTNER_NAME, rating.partnerName)
                assertEquals(PartnerLimitType.ORDERS_LIMIT, rating.limitType)
                assertEquals(RatingValueGrade.LOW, rating.ratingGrade)
                assertEquals(PublicOrderLimitReason.MANUAL, rating.limitReason)
                assertEquals(PARTNER_TOTAL_RATING, rating.value)
                rating.components?.forEach { assertEquals(COMPONENTS_VALUE, it.value) }
            }
        }
    }

    private fun buildDynamic() = PartnerRatingDynamic(
        LocalDate.now(), PARTNER_ID, DSBB,
        PARTNER_TOTAL_RATING, PARTNER_TOTAL_RATING_DIFF,
        RatingComponentDiffsWrapper(
            BusinessRatingService.COMPONENTS_BY_PLACEMENT_TYPE[DROPSHIP]!!
                .map { RatingComponentDiff(it, COMPONENTS_VALUE, COMPONENTS_VALUE_DIFF) }
        )
    )

    companion object {
        private const val PARTNER_ID = 123L
        private const val PARTNER_NAME = "name"

        private const val PARTNER_TOTAL_RATING = 35.7
        private const val PARTNER_TOTAL_RATING_DIFF = 1.0

        private const val COMPONENTS_VALUE = 8.5
        private const val COMPONENTS_VALUE_DIFF = 0.05

        private const val BUSINESS_ID = 124235L
    }
}
