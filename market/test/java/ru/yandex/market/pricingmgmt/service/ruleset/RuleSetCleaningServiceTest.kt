package ru.yandex.market.pricingmgmt.service.ruleset

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest
import ru.yandex.market.pricingmgmt.service.RuleSetCleaningService
import ru.yandex.market.pricingmgmt.service.TimeService
import java.time.LocalDateTime

@DbUnitDataBaseConfig(
    DbUnitDataBaseConfig.Entry(
        name = "datatypeFactory",
        value = "ru.yandex.market.pricingmgmt.pg.ExtendedPostgresqlDataTypeFactory"
    )
)
class RuleSetCleaningServiceTest : AbstractFunctionalTest() {

    @MockBean
    private lateinit var timeService: TimeService

    @Autowired
    private lateinit var ruleSetCleaningService: RuleSetCleaningService

    @Test
    @DbUnitDataSet(
        before = ["RuleSetCleaningServiceTest.ruleSetCleaningTablesTest.before.csv"],
        after = ["RuleSetCleaningServiceTest.ruleSetCleaningTablesTest.after.csv"]
    )
    fun testRun() {
        Mockito.`when`(timeService.getNowDateTime()).thenReturn(LocalDateTime.of(2001, 1, 8, 1, 1, 1))
        ruleSetCleaningService.run()
    }
}
