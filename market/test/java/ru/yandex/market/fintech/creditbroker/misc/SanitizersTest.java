package ru.yandex.market.fintech.creditbroker.misc;

import java.util.List;
import java.util.Optional;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SanitizersTest {
    @Test
    void sanitize() {
        assertEquals("s********g", Sanitizers.sanitize("some-sting"));
        assertEquals("null", Sanitizers.sanitize("null"));
        assertEquals("", Sanitizers.sanitize(""));
        assertEquals("    ", Sanitizers.sanitize("    "));
        assertNull(Sanitizers.sanitize((String) null));
    }

    @Test
    void sanitizeOptional() {
        assertEquals("s********g", Sanitizers.sanitize(Optional.of("some-sting")));
        assertEquals("null", Sanitizers.sanitize(Optional.of("null")));
        assertNull(Sanitizers.sanitize(Optional.empty()));
        assertNull(Sanitizers.sanitize((Optional<String>) null));
    }

    @Test
    void sanitizeList() {
        assertEquals("[v*****1, v*****2, v*****3]", Sanitizers.sanitize(List.of("value-1", "value-2", "value-3")));
        assertEquals("[v*****1, null, v*****3]", Sanitizers.sanitize(Lists.newArrayList("value-1", null, "value-3")));
        assertEquals("[]", Sanitizers.sanitize(List.of()));
        assertNull(Sanitizers.sanitize((List<String>) null));
    }

    @Test
    void sanitizeObject() {
        assertEquals("*****", Sanitizers.sanitize(12345));
        assertNull(Sanitizers.sanitize((Object) null));
    }
}
