package ru.yandex.cs.placement.tms.util;

import net.javacrumbs.jsonunit.JsonAssert;
import org.mockito.ArgumentMatcher;

import static net.javacrumbs.jsonunit.JsonAssert.when;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;

/**
 * @author antipov93.
 */
public class JsonArgMatcher implements ArgumentMatcher<String> {

    private final String expected;

    public JsonArgMatcher(String expected) {
        this.expected = expected;
    }

    @Override
    public boolean matches(String actual) {
        JsonAssert.assertJsonEquals(
                expected,
                actual,
                when(IGNORING_ARRAY_ORDER)
        );
        return true;
    }
}
