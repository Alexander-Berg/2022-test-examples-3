package ru.yandex.market.partner.status.status.regular

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.partner.status.AbstractFunctionalTest

/**
 * Тесты для [RegularRecalculationService].
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class RegularRecalculationServiceTest : AbstractFunctionalTest() {

    @Autowired
    private lateinit var regularRecalculationService: RegularRecalculationService

    @Test
    @DbUnitDataSet(after = ["RegularRecalculationServiceTest/empty.after.csv"])
    fun `empty data`() {
        regularRecalculationService.scheduleCalculation()
    }

    @Test
    @DbUnitDataSet(
        before = ["RegularRecalculationServiceTest/actualStatus.before.csv"],
        after = ["RegularRecalculationServiceTest/actualStatus.after.csv"]
    )
    fun `actual status`() {
        regularRecalculationService.scheduleCalculation()
    }

    @Test
    @DbUnitDataSet(
        before = ["RegularRecalculationServiceTest/outdatedStatus.before.csv"],
        after = ["RegularRecalculationServiceTest/outdatedStatus.after.csv"]
    )
    fun `outdated status`() {
        regularRecalculationService.scheduleCalculation()
    }
}
