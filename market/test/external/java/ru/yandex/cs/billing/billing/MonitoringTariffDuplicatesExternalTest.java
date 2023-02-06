package ru.yandex.cs.billing.billing;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.cs.billing.AbstractCsBillingCoreExternalFunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

public class MonitoringTariffDuplicatesExternalTest extends AbstractCsBillingCoreExternalFunctionalTest {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/MonitoringTariffDuplicatesExternalTest/testDuplicates/before.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("В тарифах есть дубликаты")
    void testDuplicates() {
        List<String> actual = new ArrayList<>(
                jdbcTemplate.query(
                "SELECT * FROM CS_BILLING.V_MONITOR_TARIFF_DUPLICATES",
                (rs, rn) -> rs.getString("DESCRIPTION")));
        List<String> expected = List.of("Дублирование записей по тарифам для: 1, 2, 3");
        Assertions.assertLinesMatch(expected, actual);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/MonitoringTariffDuplicatesExternalTest/testNoDuplicates/before.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("В тарифах нет дубликатов")
    void testNoDuplicates() {
        List<String> actual = new ArrayList<>(
                jdbcTemplate.query(
                        "SELECT * FROM CS_BILLING.V_MONITOR_TARIFF_DUPLICATES",
                        (rs, rn) -> rs.getString("DESCRIPTION")));
        List<String> expected = List.of();
        Assertions.assertLinesMatch(expected, actual);
    }
}
