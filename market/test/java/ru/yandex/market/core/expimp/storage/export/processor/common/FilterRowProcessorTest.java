package ru.yandex.market.core.expimp.storage.export.processor.common;

import java.sql.ResultSetMetaData;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.core.expimp.storage.export.processor.RowProcessorContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FilterRowProcessorTest {

    private static final String KEY1 = "key1";
    private static final String KEY2 = "key2";

    @Test
    public void testPassData() {
        FilterRowProcessor<Boolean> processor = new FilterRowProcessor<>(KEY1, Boolean.class, Boolean.TRUE::equals);

        RowProcessorContext data = argument();
        Map<String, Object> result = processor.process(data).getRow();

        assertEquals(data.getRow(), result);
    }

    @Test
    public void testFilterOutData() {
        FilterRowProcessor<Boolean> processor = new FilterRowProcessor<>(KEY1, Boolean.class, Boolean.FALSE::equals);

        RowProcessorContext data = argument();
        Map<String, Object> result = processor.process(data).getRow();

        assertNull(result);
    }

    @Test
    public void testSkipNullValue() {
        FilterRowProcessor<Boolean> processor = new FilterRowProcessor<>(KEY2, Boolean.class, Boolean.FALSE::equals);

        RowProcessorContext data = argument();
        Map<String, Object> result = processor.process(data).getRow();

        assertNull(result);
    }

    private RowProcessorContext argument() {
        final Map<String, Object> row = new LinkedHashMap<>();
        row.put(KEY1, true);
        row.put(KEY2, null);
        row.put("key3", 3);
        row.put("key4", "key-four");
        return new RowProcessorContext(Mockito.mock(ResultSetMetaData.class), row);
    }
}
