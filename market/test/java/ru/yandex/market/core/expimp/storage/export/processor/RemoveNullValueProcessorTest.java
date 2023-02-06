package ru.yandex.market.core.expimp.storage.export.processor;

import java.sql.ResultSetMetaData;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.core.expimp.storage.export.processor.common.RemoveNullValueProcessor;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link RemoveNullValueProcessor}.
 *
 * @author Mikhail Khorkov (atroxaper@yandex-team.ru)
 */
public class RemoveNullValueProcessorTest {

    private RemoveNullValueProcessor instance;
    private ResultSetMetaData meta;

    @Before
    public void setUp() throws Exception {
        instance = new RemoveNullValueProcessor();
        meta = Mockito.mock(ResultSetMetaData.class);
    }

    @Test
    public void calculateRowEmpty() throws Exception {
        assertThat(
                instance.process(new RowProcessorContext(meta, Collections.emptyMap())).getRow(),
                is(Collections.emptyMap()));
    }

    @Test
    public void removeNullValuesPositive() throws Exception {
        final Map<String, Object> origin = new LinkedHashMap<>();
        origin.put("SHOP_ID", null);
        origin.put("DATASOURCE", "datasource");
        origin.put("DATA_SOURCE", null);
        origin.put("HIDE_CPC_LINKS", "hideCpcLinks");
        final Map<String, Object> copy = new LinkedHashMap<>(origin);

        final Map<String, Object> result = instance.process(new RowProcessorContext(meta, copy)).getRow();
        assertThat(copy, equalTo(origin));
        assertThat(result, notNullValue());
        assertThat(result.size(), Matchers.is(2));
        for (Object o : result.values()) {
            assertThat(o, notNullValue());
        }
    }

}
