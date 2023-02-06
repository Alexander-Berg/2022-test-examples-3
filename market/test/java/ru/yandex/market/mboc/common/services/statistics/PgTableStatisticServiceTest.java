package ru.yandex.market.mboc.common.services.statistics;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author apluhin
 * @created 10/26/20
 */
public class PgTableStatisticServiceTest extends BaseDbTestClass {
    private PgTableStatisticService pgTableStatisticService;
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        pgTableStatisticService = new PgTableStatisticService(jdbcTemplate);
    }

    @Test
    public void testSelectByTableName() {
        var table = pgTableStatisticService.getTableInformation("offer").get();
        Assert.assertEquals("mbo_category", table.getSchema());
        Assert.assertEquals("offer", table.getTableName());
        Assert.assertFalse(table.getMetrics().isEmpty());
    }

    @Test
    public void testSelectBySchemaName() {
        var allSize = pgTableStatisticService.getTablesInformation().size();
        var bySchema = pgTableStatisticService.getTablesInformationFromSchema("mbo_category");
        assertFalse(bySchema.isEmpty());
        assertNotEquals(allSize, bySchema.size());
        bySchema.forEach(currentTable -> {
            assertEquals("mbo_category", currentTable.getSchema());
        });
    }

    @Test
    public void testSelectAll() {
        var tables = pgTableStatisticService.getTablesInformation();
        tables.forEach(table -> {
            Assert.assertFalse(table.getMetrics().isEmpty());
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
