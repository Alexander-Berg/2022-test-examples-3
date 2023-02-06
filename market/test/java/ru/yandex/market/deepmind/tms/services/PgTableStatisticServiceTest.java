package ru.yandex.market.deepmind.tms.services;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class PgTableStatisticServiceTest extends DeepmindBaseDbTestClass {
    private PgTableStatisticService pgTableStatisticService;
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        pgTableStatisticService = new PgTableStatisticService(jdbcTemplate);
    }

    @Test
    public void testSelectByTableName() {
        var table = pgTableStatisticService.getTableInformation("msku", "offer").get();
        assertEquals("msku", table.getSchema());
        assertEquals("offer", table.getTableName());
        assertFalse(table.getMetrics().isEmpty());
    }

    @Test
    public void testSelectBySchemaName() {
        var allSize = pgTableStatisticService.getTablesInformation().size();
        var bySchema = pgTableStatisticService.getTablesInformationFromSchema("msku");
        assertFalse(bySchema.isEmpty());
        assertNotEquals(allSize, bySchema.size());
        bySchema.forEach(currentTable -> {
            assertEquals("msku", currentTable.getSchema());
        });
    }

    @Test
    public void testSelectAll() {
        var tables = pgTableStatisticService.getTablesInformation();
        tables.forEach(table -> {
            assertFalse(table.getMetrics().isEmpty());
        });
    }

    @Test
    public void testConvertMetrics() {
        var someTable = pgTableStatisticService.getTablesInformation().get(0);
        var converted = pgTableStatisticService.convertMetric(someTable);
        converted.forEach(metric -> {
            var labels = metric.labels;
            assertTrue(labels.containsKey("schema"));
            assertTrue(labels.containsKey("table"));
        });
    }
}
