package ru.yandex.market.olap2;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import ru.yandex.market.olap2.util.MapBuilder;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class MapBuilderTest {
    @Test
    public void mustAddNulls() {
        Map<String, Object> expected = new HashMap<>();
        expected.put("k", null);
        expected.put("k2", "val");
        assertThat(new MapBuilder().put("k", null).put("k2", "val").build(),
            is(expected));
    }

    @Test
    public void mustAddNullsStatic() {
        Map<String, Object> expected = new HashMap<>();
        expected.put("k", null);
        expected.put("k2", "val");
        expected.put("k3", null);
        assertThat(MapBuilder.of("k", null, "k2", "val", "k3", null),
            is(expected));
    }
}
