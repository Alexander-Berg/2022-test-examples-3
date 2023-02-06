package ru.yandex.market.crm.campaign.test.utils;

import java.util.Map;

import com.hubspot.jinjava.Jinjava;
import org.junit.Test;

import ru.yandex.market.mcrm.utils.Maps;

import static org.junit.Assert.assertEquals;

/**
 * @author zloddey
 */
public class JinjaSenderTest {
    private final Jinjava jinjava = new JinjavaSender();

    @Test
    public void singleValue() {
        String template = "hello, {{name}}";
        Map<String, Object> context = map("name", "Keanu");
        assertEquals("hello, Keanu", jinjava.render(template, context));
    }

    @Test
    public void nestedValue() {
        String template = "hello, {{user.name}}";
        Map<String, Object> context = map("user", map("name", "Keanu"));
        assertEquals("hello, Keanu", jinjava.render(template, context));
    }

    @Test
    public void macroBlock() {
        String template = "{% macro hello(x) %}hello, {{ x }}{% endmacro %}{{ hello(user.name) }}";
        Map<String, Object> context = map("user", map("name", "Keanu"));
        assertEquals("hello, Keanu", jinjava.render(template, context));
    }

    @Test
    public void wrapTagIsTransparent() {
        String template = "{% wrap \"1\" %}hello, {{x}}{% endwrap %}";
        Map<String, Object> context = map("x", "Keanu");
        assertEquals("hello, Keanu", jinjava.render(template, context));
    }

    @Test
    public void opensCounterTagIsIgnored() {
        String template = "{% opens_counter %}hello, {{x}}";
        Map<String, Object> context = map("x", "Keanu");
        assertEquals("hello, Keanu", jinjava.render(template, context));
    }

    private Map<String, Object> map(Object... values) {
        return Maps.of(values);
    }
}
