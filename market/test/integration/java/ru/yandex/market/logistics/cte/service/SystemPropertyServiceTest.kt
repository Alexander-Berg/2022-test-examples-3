package ru.yandex.market.logistics.cte.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.cte.base.IntegrationTest
import ru.yandex.market.logistics.cte.service.property.SystemPropertyIntKey

class SystemPropertyServiceTest(@Autowired val systemPropertyService: SystemPropertyService)
    : IntegrationTest() {

    @Test
    @DatabaseSetup("classpath:service/system_property_table_before.xml")
    fun shouldCatchDatabaseValueIfOverrideExistingInLiquibase() {
        val result = systemPropertyService.getIntProperty(SystemPropertyIntKey.MONITORING_DAYS_TO_INCLUDE_IN_CHECK)
        Assertions.assertThat(result).isEqualTo(4)
    }

    @Test
    @DatabaseSetup("classpath:service/empty.xml")
    fun shouldCatchDefaultDatabaseActualStateInLiquibaseValue() {
        val result = systemPropertyService.getIntProperty(SystemPropertyIntKey.MONITORING_DAYS_TO_INCLUDE_IN_CHECK)
        Assertions.assertThat(result).isEqualTo(1)
    }


}
