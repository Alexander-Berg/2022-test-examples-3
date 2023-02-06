package ru.yandex.market.abo.core.rating.partner.report

import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.CANCELLATION_RATE
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.LATE_SHIP_RATE
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.RETURN_RATE
import ru.yandex.market.abo.core.rating.partner.PartnerRatingActual
import ru.yandex.market.abo.core.rating.partner.PartnerRatingRepo.RatingActualRepo
import ru.yandex.market.abo.core.rating.partner.details.ComponentDetails
import ru.yandex.market.abo.core.rating.partner.details.PartnerRatingDetails
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBB

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 02.03.2022
 */
class PartnerRatingDataProviderTest @Autowired constructor(
    private val provider: PartnerRatingDataProvider,

    private val partnerRatingActualRepo: RatingActualRepo
): EmptyTest() {

    @Test
    fun `get rating test`() {
        val rating = PartnerRatingActual(
            PARTNER_ID, DSBB, LocalDateTime.now(), emptyMap(),
            PartnerRatingDetails(12, listOf(
                ComponentDetails(LATE_SHIP_RATE, 1.0),
                ComponentDetails(CANCELLATION_RATE, 1.0),
                ComponentDetails(RETURN_RATE, 1.0),
            )), 87.1, 1
        )

        partnerRatingActualRepo.save(rating)
        flushAndClear()

        assertThat(provider.getRating(PARTNER_ID, DSBB))
            .usingRecursiveComparison()
            .withStrictTypeChecking()
            .isEqualTo(rating)
    }

    companion object {
        private const val PARTNER_ID = 123L
    }
}
