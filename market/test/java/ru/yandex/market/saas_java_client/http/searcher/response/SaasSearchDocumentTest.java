package ru.yandex.market.saas_java_client.http.searcher.response;

import org.junit.Test;
import ru.yandex.market.saas_java_client.http.common.SaasAttr;
import ru.yandex.market.saas_java_client.http.common.SaasAttr.DoubleKind;
import ru.yandex.market.saas_java_client.http.common.SaasAttr.IntKind;
import ru.yandex.market.saas_java_client.http.common.SaasAttr.IsProperty;
import ru.yandex.market.saas_java_client.http.common.SaasAttr.NoGroup;
import ru.yandex.market.saas_java_client.http.common.SaasAttr.NotSearchable;
import ru.yandex.market.saas_java_client.http.common.SaasAttr.StringKind;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;


@SuppressWarnings("checkstyle:MagicNumber")
public class SaasSearchDocumentTest {

    private static final SaasAttr<IntKind, NotSearchable, NoGroup, IsProperty> INT_FIELD
            = SaasAttr.intAttr("i_field").property();

    private static final SaasAttr<StringKind, NotSearchable, NoGroup, IsProperty> STR_FIELD
            = SaasAttr.stringAttr("s_field").property();

    private static final SaasAttr<DoubleKind, NotSearchable, NoGroup, IsProperty> DOUBLE_FIELD
            = SaasAttr.doubleAttr("d_field").property();

    @Test
    public void testConversion() {
        SaasSearchDocument document = new SaasSearchDocument();
        document.properties = map(
                "i_field", "1",
                "s_field", "Some string",
                "d_field", "10.0");

        assertEquals(1, document.getPropertyAsInt(INT_FIELD));
        assertEquals("Some string", document.getPropertyAsStr(STR_FIELD).get());
        assertEquals(10.0, document.getPropertyAsDouble(DOUBLE_FIELD), 0.001);
    }

    @Test
    public void testSimpleListConversion() {
        SaasSearchDocument document = new SaasSearchDocument();
        document.properties = map(
                "i_field", "1",
                "s_field", "Some string",
                "d_field", "10.0");

        assertEquals(Collections.singletonList(1), document.getPropertyAsIntList(INT_FIELD));
        assertEquals(Collections.singletonList("Some string"), document.getPropertyAsStrList(STR_FIELD));
        assertEquals(Collections.singletonList(10.0), document.getPropertyAsDoubleList(DOUBLE_FIELD));
    }

    @Test
    public void testLongerListConversion() {
        SaasSearchDocument document = new SaasSearchDocument();
        document.properties = map(
                "i_field", Arrays.asList("1", "2"),
                "s_field", Arrays.asList("a", "b"),
                "d_field", Arrays.asList("10.0", "20.0"));

        assertEquals(Arrays.asList(1, 2), document.getPropertyAsIntList(INT_FIELD));
        assertEquals(Arrays.asList("a", "b"), document.getPropertyAsStrList(STR_FIELD));
        assertEquals(Arrays.asList(10.0, 20.0), document.getPropertyAsDoubleList(DOUBLE_FIELD));
    }

    @Test
    public void testAbsent() {
        SaasSearchDocument document = new SaasSearchDocument();
        document.properties = map();

        try {
            document.getPropertyAsInt(INT_FIELD);
            fail("Must throw NoSuchElementException");
        } catch (NoSuchElementException ignored) {
        }

        try {
            document.getPropertyAsDouble(DOUBLE_FIELD);
            fail("Must throw NoSuchElementException");
        } catch (NoSuchElementException ignored) {
        }

        // Строки немного особенные, т.к. SaaS возвращает null-ы на пустые строки,
        // и можно внезапно получить NoSuchElement, если не проверить.
        assertFalse(document.getPropertyAsStr(STR_FIELD).isPresent());

        assertEquals(Collections.emptyList(), document.getPropertyAsDoubleList(DOUBLE_FIELD));
        assertEquals(Collections.emptyList(), document.getPropertyAsIntList(INT_FIELD));
        assertEquals(Collections.emptyList(), document.getPropertyAsStrList(STR_FIELD));
    }

    private Map<String, Object> map(Object... items) {
        HashMap<String, Object> result = new HashMap<>();
        for (int i = 0; i < items.length; i += 2) {
            result.put((String) items[i], items[i + 1]);
        }
        return result;
    }
}
