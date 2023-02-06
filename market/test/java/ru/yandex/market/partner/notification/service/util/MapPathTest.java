package ru.yandex.market.partner.notification.service.util;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MapPathTest {
    private static final String VALUE = "value";
    private static final Map<String, Object> CORRECT_NAVIGATED_MAP =
            Map.of("a", Map.of("b", Map.of("c", VALUE)));

    private static final Map<String, Object> INCORRECT_NAVIGATED_MAP =
            Map.of("a", Map.of(123, Map.of("c", VALUE)));

    @Test
    public void navigateMap_correctPath_returnsCorrectValue() {
        String path = "a.b.c";

        Optional<Object> value = MapPath.getValue(CORRECT_NAVIGATED_MAP, path);
        Assertions.assertEquals(VALUE, value.orElse(null));
    }

    @Test
    public void navigateMap_incorrectPath_returnsEmptyOptional() {
        String path = "a.not_b.c";
        Optional<Object> value = MapPath.getValue(CORRECT_NAVIGATED_MAP, path);

        Assertions.assertTrue(value.isEmpty());
    }

    @Test
    public void navigateMap_correctPathWithSlashes_returnsCorrectValue() {
        String path = "a/b/c";

        Optional<Object> value = MapPath.getValue(CORRECT_NAVIGATED_MAP, path);
        Assertions.assertEquals(VALUE, value.orElse(null));
    }

    @Test
    public void navigateMap_incorrectMapType_returnsEmptyOptional() {
        String path = "a.b.c";

        Optional<Object> value = MapPath.getValue(INCORRECT_NAVIGATED_MAP, path);
        Assertions.assertTrue(value.isEmpty());
    }

    @Test
    public void navigateMap_pathLongerThanMapHierarchy_returnsEmptyOptional() {
        String path = "a.b.c.d.e.f";

        Optional<Object> value = MapPath.getValue(CORRECT_NAVIGATED_MAP, path);
        Assertions.assertTrue(value.isEmpty());
    }

    @Test
    public void navigateMap_emptyPath_returnsRootMap() {
        String path = "";

        Optional<Object> value = MapPath.getValue(CORRECT_NAVIGATED_MAP, path);
        Assertions.assertEquals(CORRECT_NAVIGATED_MAP, value.get());
    }
}
