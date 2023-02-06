package ru.yandex.market.clickphite.metric;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 25.07.2018
 */
public class StringTemplateTest {
    @Test
    public void empty() {
        assertEquals(
            "",
            render("",ImmutableMap.of())
        );
    }

    @Test
    public void noVariables() {
        assertEquals(
            "qwerty",
            render("qwerty",ImmutableMap.of())
        );
    }

    @Test
    public void oneVariableWithoutConstantParts() {
        assertEquals(
            "FOO",
            render("${foo}",ImmutableMap.of("foo", "FOO"))
        );
    }

    @Test
    public void oneVariableWithConstantPartBeforeIt() {
        assertEquals(
            "foo.FOO",
            render("foo.${foo}",ImmutableMap.of("foo", "FOO"))
        );
    }

    @Test
    public void oneVariableWithConstantPartAfterIt() {
        assertEquals(
            "FOO.foo",
            render("${foo}.foo",ImmutableMap.of("foo", "FOO"))
        );
    }

    @Test
    public void oneVariableWithConstantPartsBeforeAndAfterIt() {
        assertEquals(
            "foo.FOO.bar",
            render("foo.${foo}.bar",ImmutableMap.of("foo", "FOO"))
        );
    }

    @Test
    public void oneVariableUsedTwice() {
        assertEquals(
            "FOOFOO",
            render("${foo}${foo}",ImmutableMap.of("foo", "FOO"))
        );
    }

    @Test
    public void twoAdjacentVariables() {
        assertEquals(
            "FOOBAR",
            render("${foo}${bar}", ImmutableMap.of("foo", "FOO", "bar", "BAR"))
        );
    }

    @Test
    public void twoVariablesWithConstantPartBetweenThem() {
        assertEquals(
            "FOO.bar.BAR",
            render("${foo}.bar.${bar}",ImmutableMap.of("foo", "FOO", "bar", "BAR"))
        );
    }

    @Test
    public void extraVariables() {
        assertEquals(
            "foo.FOO",
            render("foo.${foo}",ImmutableMap.of("foo", "FOO", "bar", "BAR"))
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingVariable() {
        render("foo.${foo}",ImmutableMap.of());
    }

    @Test
    public void emptyVariable() {
        assertEquals(
            "foo..bar",
            render("foo.${foo}.bar",ImmutableMap.of("foo", ""))
        );
    }

    @Test
    public void dollarSignWithoutCurlyBrackets() {
        assertEquals(
            "$qwerty",
            render("$qwerty", ImmutableMap.of())
        );
    }

    @Test
    public void noClosingCurlyBracket() {
        assertEquals(
            "${qwerty",
            render("${qwerty", ImmutableMap.of())
        );
    }

    private static String render(String template, Map<String, String> variables) {
        return new StringTemplate(template, true)
            .render(variables::get)
            .toString();
    }
}
