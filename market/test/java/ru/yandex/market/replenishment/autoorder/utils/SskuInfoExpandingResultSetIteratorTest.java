package ru.yandex.market.replenishment.autoorder.utils;

import java.sql.Types;

import org.h2.tools.SimpleResultSet;
import org.junit.Test;
import org.springframework.jdbc.core.RowMapper;

import ru.yandex.market.replenishment.autoorder.model.entity.postgres.SskuInfo;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.Supplier;
import ru.yandex.market.replenishment.autoorder.utils.data_expansion.ExpandingResultSetIterator;
import ru.yandex.market.replenishment.autoorder.utils.data_expansion.SskuInfoExpandingResultSetIterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SskuInfoExpandingResultSetIteratorTest {
    private static final RowMapper<SskuInfo> ROW_MAPPER = (rs, i) -> {
        SskuInfo sskuInfo = new SskuInfo();
        sskuInfo.setSsku(rs.getString(1));
        sskuInfo.setSupplier(new Supplier());
        return sskuInfo;
    };

    @Test()
    public void testExpandingItemResultSet() {
        SimpleResultSet resultSet = new SimpleResultSet();
        resultSet.addColumn("col", Types.VARCHAR, 255, 0);
        int rows = 20_000;
        String[] shopSkus = new String[]{"val", "val."};

        for (int shopSkuId = 0; shopSkuId < shopSkus.length; shopSkuId++) {
            for (int i = 0; i < rows; i++) {
                resultSet.addRow("000" + (i + 1) + "." + shopSkus[shopSkuId]);
            }
        }

        int ratio = 2;
        long magicNumber = 1_000_000_000_000L;
        ExpandingResultSetIterator<SskuInfo> itr =
                new SskuInfoExpandingResultSetIterator(resultSet, ROW_MAPPER, ratio);

        assertTrue(itr.hasNext());
        for (int shopSkuId = 0; shopSkuId < shopSkus.length; shopSkuId++) {
            for (int row = 1; row <= rows; row++) {
                for (int i = ratio - 1; i > 0; i--) {
                    String prefix = Long.toString(magicNumber * i + row);
                    String ssku = prefix + "." + shopSkus[shopSkuId];
                    assertEquals(ssku, itr.next().getSsku());
                }
                assertEquals("000" + row + "." + shopSkus[shopSkuId], itr.next().getSsku());
            }
        }

        assertFalse(itr.hasNext());
    }


    @Test()
    public void testExpandingItemWithWhitespacesResultSet() {
        SimpleResultSet resultSet = new SimpleResultSet();
        resultSet.addColumn("col", Types.VARCHAR, 255, 0);

        resultSet.addRow("3p.00001.val ");

        int ratio = 2;
        long magicNumber = 1_000_000_000_000L;
        ExpandingResultSetIterator<SskuInfo> itr =
                new SskuInfoExpandingResultSetIterator(resultSet, ROW_MAPPER, ratio);

        assertTrue(itr.hasNext());

        String ssku = "3p.00001.val ";
        String sskuExpanded = "3p.1000000000001.val ";
        assertEquals(sskuExpanded, itr.next().getSsku());
        assertEquals(ssku, itr.next().getSsku());

        assertFalse(itr.hasNext());
    }
}
