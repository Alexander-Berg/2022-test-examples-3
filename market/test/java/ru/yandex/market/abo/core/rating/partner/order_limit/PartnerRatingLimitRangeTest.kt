package ru.yandex.market.abo.core.rating.partner.order_limit

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBB

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 21.07.2021
 */
class PartnerRatingLimitRangeTest @Autowired constructor(
    private val partnerRatingLimitRangeService: PartnerRatingLimitRangeService
) : EmptyTest() {

    @Test
    fun `limit resolution test`() {
        val limitRanges = partnerRatingLimitRangeService.getModelRanges(DSBB)
        val range = getRatingRange(RATING_VALUE, limitRanges)

        assertNotNull(range)
        assertTrue(range.shouldSwitchOffPartner())
    }

    companion object {
        private const val RATING_VALUE = 35.0
    }
}
