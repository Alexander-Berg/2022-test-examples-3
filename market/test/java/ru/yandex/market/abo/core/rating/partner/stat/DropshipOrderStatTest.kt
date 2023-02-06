package ru.yandex.market.abo.core.rating.partner.stat

import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.cpa.order.model.PartnerModel

/**
 *@author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 12.07.2021
 */
class DropshipOrderStatTest @Autowired constructor(
    private val dropshipOrderStatService: DropshipOrderStatService,
    private val dropshipOrderStatServiceTesting: DropshipOrderStatServiceTesting
) : EmptyTest() {

    @Test
    fun `query correctness test`() {
        val dateFrom = LocalDate.now().minusDays(7)
        val dateTo = LocalDate.now()
        val stats = dropshipOrderStatService.calculateOrderStat(dateFrom, dateTo, PartnerModel.DSBB)
        val testingStats = dropshipOrderStatServiceTesting.calculateOrderStat(dateFrom, dateTo, PartnerModel.DSBB)

        assertNotNull(stats)
        assertNotNull(testingStats)
    }
}
