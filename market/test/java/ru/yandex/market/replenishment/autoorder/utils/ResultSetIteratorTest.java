package ru.yandex.market.replenishment.autoorder.utils;

import java.sql.Types;
import java.util.NoSuchElementException;

import org.h2.tools.SimpleResultSet;
import org.junit.Test;
import org.springframework.jdbc.core.RowMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResultSetIteratorTest {
    private static final RowMapper<String> ROW_MAPPER = (rs, i) -> rs.getString(1);

    @Test(expected = NoSuchElementException.class)
    public void testEmptyResultSet() {
        ResultSetIterator<? extends String> itr = new ResultSetIterator<>(new SimpleResultSet(), ROW_MAPPER);

        assertFalse(itr.hasNext());
        itr.next();
    }

    @Test(expected = NoSuchElementException.class)
    public void testSingleItemResultSet() {
        SimpleResultSet resultSet = new SimpleResultSet();
        resultSet.addColumn("col", Types.VARCHAR, 255, 0);
        resultSet.addRow("val");

        ResultSetIterator<?> itr = new ResultSetIterator<>(resultSet, ROW_MAPPER);

        assertTrue(itr.hasNext());
        assertEquals("val", itr.next());
        assertFalse(itr.hasNext());
        itr.next();
    }
}
