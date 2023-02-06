package ru.yandex.market.pricingmgmt.service.boundset

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest
import ru.yandex.market.pricingmgmt.service.BoundSetCleaningService
import ru.yandex.market.pricingmgmt.service.TimeService
import java.time.LocalDateTime

@DbUnitDataBaseConfig(
    DbUnitDataBaseConfig.Entry(
        name = "datatypeFactory",
        value = "ru.yandex.market.pricingmgmt.pg.ExtendedPostgresqlDataTypeFactory"
    )
)
class BoundSetCleaningServiceTest(
    @Autowired private val boundSetCleaningService: BoundSetCleaningService,
) : AbstractFunctionalTest() {

    @MockBean
    private lateinit var timeService: TimeService

    @Test
    @DbUnitDataSet(
        before = ["BoundSetCleaningServiceTest.boundSetCleaningTablesTest.before.csv"],
        after = ["BoundSetCleaningServiceTest.boundSetCleaningTablesTest.after.csv"]
    )
    fun testRun() {
        Mockito.`when`(timeService.getNowDateTime())
            .thenReturn(LocalDateTime.of(2001, 1, 8, 1, 1, 1))

        boundSetCleaningService.run()
    }
}
