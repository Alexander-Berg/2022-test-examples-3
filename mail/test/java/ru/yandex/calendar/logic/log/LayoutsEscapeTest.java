package ru.yandex.calendar.logic.log;

import org.junit.Test;

import ru.yandex.bolts.function.Function;
import ru.yandex.misc.test.Assert;

public class LayoutsEscapeTest {
    @Test
    public void escapeJson() {
        Function<String, String> escape = JsonLogPatternLayout.ESCAPE_JSON::translate;

        Assert.equals("/\\\\/\\\"", escape.apply("/\\/\""));
        Assert.equals("Привет", escape.apply("Привет"));
    }
}
