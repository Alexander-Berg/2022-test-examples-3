package ru.yandex.market.core.expimp.storage.export.processor;

import java.sql.ResultSetMetaData;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.core.expimp.storage.export.processor.common.TrimStringValueProcessor;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class TrimStringValueProcessorTest {

    private TrimStringValueProcessor instance;
    private ResultSetMetaData meta;

    @Before
    public void setUp() {
        instance = new TrimStringValueProcessor();
        meta = Mockito.mock(ResultSetMetaData.class);
    }

    @Test
    public void testOnEmptyMap() {
        final RowProcessorContext argument = new RowProcessorContext(meta, Collections.emptyMap());
        assertThat(instance.process(argument).getRow(), is(Collections.emptyMap()));
    }

    @Test
    public void testOnNonEmptyMap() {
        Map<String, Object> result = instance.process(new RowProcessorContext(meta, getTestData()))
                .getRow();

        assertEquals(getExpected(), result);
    }

    private Map<String, Object> getExpected() {
        Map<String, Object> expected = new LinkedHashMap<>();

        expected.put("SHOP_ID", null);
        expected.put("DATASOURCE", "datasource");
        expected.put("DATA_SOURCE", null);
        expected.put("HIDE_CPC_LINKS", "hideCpcLinks");
        expected.put("FIELD", 1234);

        return expected;
    }

    private Map<String, Object> getTestData() {
        Map<String, Object> testData = new LinkedHashMap<>();

        testData.put("SHOP_ID", null);
        testData.put("DATASOURCE", "    datasource   ");
        testData.put("DATA_SOURCE", "\n");
        testData.put("HIDE_CPC_LINKS", "hideCpcLinks\n\n");
        testData.put("FIELD", 1234);

        return testData;
    }
}
