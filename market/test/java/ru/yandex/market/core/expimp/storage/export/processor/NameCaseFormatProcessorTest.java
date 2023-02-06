package ru.yandex.market.core.expimp.storage.export.processor;

import java.sql.ResultSetMetaData;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.base.CaseFormat;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.core.expimp.storage.export.processor.common.NameCaseFormatProcessor;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link NameCaseFormatProcessor}.
 *
 * @author Mikhail Khorkov (atroxaper@yandex-team.ru)
 */
public class NameCaseFormatProcessorTest {

    private NameCaseFormatProcessor instance;
    private ResultSetMetaData meta;

    @Before
    public void setUp() throws Exception {
        instance = new NameCaseFormatProcessor(CaseFormat.LOWER_CAMEL);
        meta = Mockito.mock(ResultSetMetaData.class);
    }

    @Test
    public void calculateRowEmpty() throws Exception {
        assertThat(
                instance.process(new RowProcessorContext(meta, Collections.emptyMap())).getRow(),
                is(Collections.emptyMap()));
    }

    @Test
    public void changeColumnNamePositive() throws Exception {
        final Map<String, Object> origin = new LinkedHashMap<>();
        origin.put("SHOP_ID", "shopId");
        origin.put("DATASOURCE", "datasource");
        origin.put("DATA_SOURCE", "dataSource");
        origin.put("HIDE_CPC_LINKS", "hideCpcLinks");
        final Map<String, Object> copy = new LinkedHashMap<>(origin);

        final Map<String, Object> result = instance.process(new RowProcessorContext(meta, copy)).getRow();
        assertThat(copy, equalTo(origin));
        assertThat(result, notNullValue());
        assertThat(result.size(), Matchers.is(origin.size()));
        for (Map.Entry<String, Object> originEntry : origin.entrySet()) {
            final String value = (String) originEntry.getValue();
            assertThat(value, equalTo(result.get(value)));
        }
    }

}
