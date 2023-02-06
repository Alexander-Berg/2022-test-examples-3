package ru.yandex.market.abo.core.rating.partner

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.CANCELLATION_RATE
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.LATE_SHIP_RATE
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.RETURN_RATE
import ru.yandex.market.abo.core.rating.partner.details.ComponentDetails
import ru.yandex.market.abo.core.rating.partner.details.PartnerRatingDetails
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBB
import java.time.LocalDateTime

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 09.03.2022
 */
class PartnerRatingServiceTest @Autowired constructor(
    private val partnerRatingService: PartnerRatingService,

    private val partnerRatingRepo: PartnerRatingRepo.RatingRepo
) : EmptyTest() {
    @Test
    fun `new rating save test`() {
        val rating = saveNewRating(mapOf(PARTNER_ID_1 to RATING_VALUE_1))[0]

        assertThat(partnerRatingRepo.findAll()[0])
            .usingRecursiveComparison()
            .isEqualTo(rating)
        assertThat(partnerRatingService.getActualRatings()[0])
            .usingRecursiveComparison()
            .isEqualTo(rating.toActual())
    }

    @Test
    fun `find actual rating test`() {
        val actualRating = saveNewRating(mapOf(PARTNER_ID_1 to RATING_VALUE_1))[0].toActual()

        assertThat(partnerRatingService.getActualRating(PARTNER_ID_1))
            .usingRecursiveComparison()
            .isEqualTo(actualRating)
        assertThat(partnerRatingService.getActualRating(PARTNER_ID_1))
            .usingRecursiveComparison()
            .isEqualTo(actualRating)
        assertTrue(partnerRatingService.getActualForMarket().isEmpty())
    }

    @Test
    fun `find previous rating test`() {
        val previousRating = saveNewRating(mapOf(PARTNER_ID_1 to RATING_PREVIOUS_VALUE_1))[0]
        saveNewRating(mapOf(PARTNER_ID_1 to RATING_VALUE_1))

        assertThat(partnerRatingService.getPreviousRating(setOf(PARTNER_ID_1), DSBB)[0])
            .usingRecursiveComparison(
                RecursiveComparisonConfiguration.builder().withIgnoredFields("actual").build()
            )
            .isEqualTo(previousRating)
    }

    @Test
    fun `find previous rating total by partner test`() {
        saveNewRating(mapOf(PARTNER_ID_1 to RATING_PREVIOUS_VALUE_1, PARTNER_ID_2 to RATING_PREVIOUS_VALUE_2))
        saveNewRating(mapOf(PARTNER_ID_1 to RATING_VALUE_1, PARTNER_ID_2 to RATING_VALUE_2))

        val a = partnerRatingService.getPreviousRatingTotalByPartner(setOf(PARTNER_ID_1, PARTNER_ID_2), DSBB)
        assertThat(a)
            .isEqualTo(mapOf(PARTNER_ID_1 to RATING_PREVIOUS_VALUE_1, PARTNER_ID_2 to RATING_PREVIOUS_VALUE_2))
    }

    private fun saveNewRating(ratingByPartner: Map<Long, Double>): List<PartnerRating> {
        val ratings = ratingByPartner.map { (partnerId, ratingValue) ->
            PartnerRating(
                LocalDateTime.now(), partnerId, DSBB,
                emptyMap(),
                PartnerRatingDetails(25, listOf(
                    ComponentDetails(LATE_SHIP_RATE, 1.0),
                    ComponentDetails(CANCELLATION_RATE, 1.0),
                    ComponentDetails(RETURN_RATE, 1.0)
                )),
                ratingValue
            )
        }
        partnerRatingService.saveNewRating(ratings, DSBB, LocalDateTime.now())
        flushAndClear()

        return ratings
    }

    companion object {
        private const val PARTNER_ID_1 = 123L
        private const val PARTNER_ID_2 = 321L

        private const val RATING_VALUE_1 = 87.1
        private const val RATING_VALUE_2 = 83.1
        private const val RATING_PREVIOUS_VALUE_1 = 91.3
        private const val RATING_PREVIOUS_VALUE_2 = 89.3
    }
}
