package ru.yandex.market.abo.core.spark.status

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.EmptyTest
import ru.yandex.market.core.application.PartnerApplicationStatus
import ru.yandex.market.core.orginfo.model.OrganizationType

class SparkStatusServiceTest @Autowired constructor(
    val jdbcTemplate: JdbcTemplate,
    val sparkStatusService: SparkStatusService,
) : EmptyTest() {

    @Test
    fun `load shops for check`() {
        jdbcTemplate.update("insert into shop (id, name, is_offline, is_global, age) values (1, 'name', false, false, 1)")
        jdbcTemplate.update("insert into ext_organization_info (datasource_id, ogrn, name, juridical_address) values (1, '123', 'юр имя', 'юр адрес')")
        jdbcTemplate.update("insert into ext_shop_region(datasource_id, country_id) values (1, 225)")

        assertTrue(sparkStatusService.loadShopsForCheckInSpark().isNotEmpty())
    }

    @Test
    fun `load suppliers for check`() {
        jdbcTemplate.update("insert into supplier(id, name, request_id) values (1, 'name', 2)")
        jdbcTemplate.update("insert into ext_organization_info (datasource_id, ogrn, name, juridical_address, type) values (1, '123', 'юр имя', 'юр адрес', ${OrganizationType.IP.id})")
        jdbcTemplate.update("insert into ext_partner_application_status(request_id, status) values (2, '${PartnerApplicationStatus.COMPLETED}')")

        assertTrue(sparkStatusService.loadSuppliersForCheckInSpark().isNotEmpty())
    }
}
