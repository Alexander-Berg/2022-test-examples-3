package ru.yandex.market.notification.simple.util.adapter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import ru.yandex.market.notification.simple.util.adapter.StringMapXmlAdapter.AdaptedMap;
import ru.yandex.market.notification.simple.util.adapter.StringMapXmlAdapter.Entry;
import ru.yandex.market.notification.test.model.AbstractModelTest;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link StringMapXmlAdapter}.
 *
 * @author Vladislav Bauer
 */
public class StringMapXmlAdapterTest extends AbstractModelTest {

    private static final StringMapXmlAdapter ADAPTER = new StringMapXmlAdapter();
    private static final String KEY = "test";
    private static final long VALUE = 5L;


    @Test
    public void testUnmarshal() throws Exception {
        final AdaptedMap adaptedMap = new AdaptedMap();
        adaptedMap.entry.add(Entry.create(KEY, VALUE));

        final Map<String, Object> map = ADAPTER.unmarshal(adaptedMap);

        assertThat(map.size(), equalTo(1));
        assertThat(map.get(KEY), equalTo(VALUE));
    }

    @Test
    public void testMarshal() throws Exception {
        final Map<String, Object> map = Collections.singletonMap(KEY, VALUE);
        final AdaptedMap adaptedMap = ADAPTER.marshal(map);
        final List<Entry> entries = adaptedMap.entry;

        assertThat(entries, hasSize(1));
        assertThat(entries.get(0), equalTo(Entry.create(KEY, VALUE)));
    }

    @Test
    public void testBasicMethods() {
        final Entry data = Entry.create(KEY, VALUE);
        final Entry sameData = Entry.create(KEY, VALUE);
        final Entry otherData = Entry.create("OTHER", "VALUE");

        checkBasicMethods(data, sameData, otherData);
    }

}
