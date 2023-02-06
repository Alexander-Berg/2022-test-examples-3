package ru.yandex.market.mbi.partner_stat.repository;

import org.dbunit.database.DatabaseConfig;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.partner_stat.FunctionalTest;
import ru.yandex.market.mbi.partner_stat.config.ClickHouseTestConfig;

/**
 * Класс для тестирования на ClickHouse.
 */
@ExtendWith(SpringExtension.class)
@SpringJUnitConfig(
        classes = ClickHouseTestConfig.class
)
@ActiveProfiles("clickHouseTest")
@DbUnitDataBaseConfig(
        @DbUnitDataBaseConfig.Entry(
                name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS,
                value = "true"))
@DbUnitDataSet(dataSource = ClickHouseTestConfig.DATA_SOURCE,
        nonTruncatedTables = {
                "mbi.tmp_vendors",
                "mbi.vendors",
                "mbi.tmp_categories",
                "mbi.categories",
                "mbi.v_filters",
                "mbi.sales_filter"
        })
public class ClickhouseFunctionalTest extends FunctionalTest {

    @Autowired
    private NamedParameterJdbcTemplate clickHouseJdbcTemplate;
}
