package ru.yandex.market.core.expimp.storage.export.processor;

import java.sql.ResultSetMetaData;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.core.expimp.storage.export.processor.common.FieldChangeProcessor;

import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link FieldChangeProcessor}.
 *
 * @author Mikhail Khorkov (atroxaper@yandex-team.ru)
 */
public class FieldChangeProcessorTest {

    @Test
    public void processSimple1Positive() throws Exception {
        final FieldChangeProcessor processor = new FieldChangeProcessor(symptom(), change());
        Map<String, Object> row = processor.process(argument()).getRow();
        checkChange(row);
    }

    @Test
    public void processSimple2Positive() throws Exception {
        Map<String, String> symptom = symptom();
        symptom.remove("key1");
        symptom.remove("key2");
        symptom.remove("key3");
        final FieldChangeProcessor processor = new FieldChangeProcessor(symptom, change());
        Map<String, Object> row = processor.process(argument()).getRow();
        checkChange(row);
    }

    @Test
    public void processNotChangePositive() throws Exception {
        Map<String, String> symptom = new HashMap<>();
        symptom.put("key4", "key-for");
        final FieldChangeProcessor processor = new FieldChangeProcessor(symptom, change());
        Map<String, Object> row = processor.process(argument()).getRow();
        assertThat(row, Matchers.notNullValue());
        Assert.assertThat(row.get("key4"), Matchers.is("key-four"));
    }

    @Test
    public void processSimpleAndRemovePositive() throws Exception {
        final FieldChangeProcessor processor = new FieldChangeProcessor(symptom(), change(),
                Arrays.asList("key1", "key2", "key3"));
        Map<String, Object> row = processor.process(argument()).getRow();
        assertThat(row, Matchers.notNullValue());
        assertThat(row.size(), Matchers.is(1));
        assertThat(row.get("key4"), Matchers.is("key-three"));
    }


    private RowProcessorContext argument() {
        final Map<String, Object> row = new LinkedHashMap<>();
        row.put("key1", true);
        row.put("key2", null);
        row.put("key3", 3);
        row.put("key4", "key-four");
        return new RowProcessorContext(Mockito.mock(ResultSetMetaData.class), row);
    }

    private Map<String, String> symptom() {
        final Map<String, String> symptom = new HashMap<>();
        symptom.put("key1", "true");
        symptom.put("key2", "null");
        symptom.put("key3", "3");
        symptom.put("key4", "key-four");
        return symptom;
    }

    private Map<String, Object> change() {
        final Map<String, Object> change = new HashMap<>();
        change.put("key1", false);
        change.put("key4", "key-three");
        return change;
    }

    private void checkChange(final Map<String, Object> row) {
        assertThat(row, Matchers.notNullValue());
        for (Map.Entry<String, Object> cEntry : change().entrySet()) {
            assertThat(cEntry.getValue(), Matchers.is(row.get(cEntry.getKey())));
        }
    }

}
