package ru.yandex.market.pricingmgmt.postgres

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig
import ru.yandex.market.pricingmgmt.api.ControllerTest

@DbUnitDataBaseConfig(
    DbUnitDataBaseConfig.Entry(
        name = "datatypeFactory",
        value = "ru.yandex.market.pricingmgmt.pg.ExtendedPostgresqlDataTypeFactory"
    )
)
open class AbstractDatabaseTest : ControllerTest() {
    @Autowired
    protected lateinit var jdbcTemplate: JdbcTemplate
}
