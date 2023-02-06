package ru.yandex.market.tanker.client.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

/**
 * @author Vadim Lyalin
 */
public class KeySetTest {

    @Test
    public void testGetKeys() {
        final KeySet keySet = createKeySet();
        final Set<String> keys = keySet.getKeys();

        assertThat(keys, hasItems("key", "ключ"));
        assertNull(keySet.getText("fakeKey"));
    }

    @Test
    public void testStandardSerialization() {
        final KeySet origin = createKeySet();
        final KeySet clone = SerializationUtils.clone(origin);

        assertThat(clone, notNullValue());
    }


    private KeySet createKeySet() {
        final Map<String, Object> keyMap = new HashMap<>();
        keyMap.put("key", null);
        keyMap.put("ключ", "значение");
        return new KeySet(keyMap);
    }

}
