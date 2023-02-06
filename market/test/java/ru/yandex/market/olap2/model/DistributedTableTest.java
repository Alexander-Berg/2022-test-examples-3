package ru.yandex.market.olap2.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.market.olap2.util.DdlUtils;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class DistributedTableTest {


    private final String inputEngine;
    private final boolean isDistributed;

    public DistributedTableTest(String inputEngine, boolean isDistributed) {
        this.inputEngine = inputEngine;
        this.isDistributed = isDistributed;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // usual case
                {"Distributed('cubes', 'input_table', rand())", true},
                {"Distributed('cubes', 'input_table', any_other_function())", true},
                {"Merge('cubes', 'input_table', any_other_function())", false}
        });
    }

    @Test
    public void testIsTableDistributed() {
        DistributedTable distributedTable = new DistributedTable(
                "tabe_distributed",
                "table_source",
                inputEngine
        );
        assertEquals(isDistributed, distributedTable.isTableDistributed());
    }
}
