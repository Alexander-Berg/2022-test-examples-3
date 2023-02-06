package ru.yandex.market.core.expimp.storage.export.processor;

import java.math.BigDecimal;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.core.expimp.storage.export.processor.common.NumberToBooleanProcessor;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link NumberToBooleanProcessor}.
 *
 * @author Mikhail Khorkov (atroxaper@yandex-team.ru)
 */
public class NumberToBooleanProcessorTest {

    private NumberToBooleanProcessor instance;
    private ResultSetMetaData meta;

    @Before
    public void setUp() throws Exception {
        instance = new NumberToBooleanProcessor();
        meta = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(meta.getPrecision(Mockito.eq(1))).thenReturn(1);
        Mockito.when(meta.getColumnType(Mockito.eq(1))).thenReturn(Types.NUMERIC);
        Mockito.when(meta.getPrecision(Mockito.eq(2))).thenReturn(1);
        Mockito.when(meta.getColumnType(Mockito.eq(2))).thenReturn(Types.TIMESTAMP);
        Mockito.when(meta.getPrecision(Mockito.eq(3))).thenReturn(10);
        Mockito.when(meta.getColumnType(Mockito.eq(3))).thenReturn(Types.NUMERIC);
        Mockito.when(meta.getPrecision(Mockito.eq(4))).thenReturn(1);
        Mockito.when(meta.getColumnType(Mockito.eq(4))).thenReturn(Types.NUMERIC);
        Mockito.when(meta.getPrecision(Mockito.eq(5))).thenThrow(new SQLException("ex"));
        Mockito.when(meta.getColumnType(Mockito.eq(5))).thenThrow(new SQLException("ex"));
    }

    @Test
    public void calculateRowEmpty() throws Exception {
        assertThat(
                instance.process(new RowProcessorContext(meta, Collections.emptyMap())).getRow(),
                is(Collections.emptyMap()));
    }

    @Test
    public void removeConvertBooleanPositive() throws Exception {
        final Map<String, Object> origin = createMap();
        final Map<String, Object> copy = new LinkedHashMap<>(origin);

        final Map<String, Object> result = instance.process(new RowProcessorContext(meta, copy)).getRow();
        assertThat(copy, equalTo(origin));
        assertThat(result, notNullValue());
        assertThat(result.size(), is(origin.size()));
        assertThat(result.get("booleanTrue"), is(true));
        assertThat(result.get("booleanFalse"), is(false));
        assertThat(result.get("timestamp"), equalTo(origin.get("timestamp")));
        assertThat(result.get("anyNumber"), equalTo(origin.get("anyNumber")));
    }


    private Map<String, Object> createMap() {
        final Map<String, Object> origin = new LinkedHashMap<>();
        origin.put("booleanTrue", new BigDecimal(1));
        origin.put("timestamp", 50);
        origin.put("anyNumber", new BigDecimal(49));
        origin.put("booleanFalse", new BigDecimal(0));
        return origin;
    }

}
