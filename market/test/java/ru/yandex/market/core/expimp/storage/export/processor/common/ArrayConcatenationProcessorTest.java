package ru.yandex.market.core.expimp.storage.export.processor.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sql.rowset.RowSetMetaDataImpl;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.core.expimp.storage.export.processor.RowProcessorContext;
import ru.yandex.market.core.expimp.storage.export.processor.RowProcessorResult;

/**
 * Unit-тесты для {@link ArrayConcatenationProcessor}.
 *
 * @author Mikhail Khorkov (atroxaper@yandex-team.ru)
 */
public class ArrayConcatenationProcessorTest {

    private ArrayConcatenationProcessor instance;


    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void concatFullPositive() {
        instance = new ArrayConcatenationProcessor(Collections.singletonList("row1"), ";", true);

        final RowProcessorResult result =
                instance.process(new RowProcessorContext(new RowSetMetaDataImpl(), createRow()));

        final Map<String, Object> row = result.getRow();
        Assert.assertThat(row, Matchers.notNullValue());
        Assert.assertThat(row.get("row1"), Matchers.is("1;2;3;4;"));
        Assert.assertThat(row.get("row3"), Matchers.instanceOf((new Object[]{}).getClass()));
    }

    @Test
    public void concatNotFullPositive() {
        instance = new ArrayConcatenationProcessor(Collections.singletonList("row1"), ";", false);

        final RowProcessorResult result =
                instance.process(new RowProcessorContext(new RowSetMetaDataImpl(), createRow()));

        final Map<String, Object> row = result.getRow();
        Assert.assertThat(row, Matchers.notNullValue());
        Assert.assertThat(row.get("row1"), Matchers.is("1;2;3;4"));
        Assert.assertThat(row.get("row3"), Matchers.instanceOf((new Object[]{}).getClass()));
    }

    @Test
    public void concatBothPositive() {
        instance = new ArrayConcatenationProcessor(Arrays.asList("row1", "row3"), ";", true);

        final RowProcessorResult result =
                instance.process(new RowProcessorContext(new RowSetMetaDataImpl(), createRow()));

        final Map<String, Object> row = result.getRow();
        Assert.assertThat(row, Matchers.notNullValue());
        Assert.assertThat(row.get("row1"), Matchers.is("1;2;3;4;"));
        Assert.assertThat(row.get("row3"), Matchers.is("1;2;3;4;5;"));
    }

    private Map<String, Object> createRow() {
        final Map<String, Object> row = new HashMap<>();
        row.put("row0", 5);
        row.put("row1", new Object[] {"1", "2", "3", "4"});
        row.put("row2", null);
        row.put("row3", new Object[] {1, 2, 3, 4, 5});

        return row;
    }

}
