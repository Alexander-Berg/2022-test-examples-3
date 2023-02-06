package ru.yandex.market.api.util;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 * Created by apershukov on 14.02.17.
 */
public class ApiCollectorsTest {

    @Test
    public void testIndexEntitiesWithDublicateKey() {
        Map<String, String> map = Stream.of("abc", "dfe", "abc")
                .collect(CommonCollectors.index(x -> x, HashMap::new));

        assertEquals(2, map.size());
        assertEquals("abc", map.get("abc"));
        assertEquals("dfe", map.get("dfe"));
    }
}