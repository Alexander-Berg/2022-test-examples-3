package ru.yandex.crypta.lab;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.crypta.lab.formatters.FormattedLine;
import ru.yandex.crypta.lab.formatters.IntWithCommentFormatter;

@RunWith(Parameterized.class)
public class IntWithCommentFormatterTest {
    private final String raw;
    private final FormattedLine normalized;

    private static Object[] positive(String raw, String normalized) {
        return new Object[]{raw, FormattedLine.line(raw, normalized)};
    }

    private static Object[] positive(String raw) {
        return positive(raw, raw);
    }

    private static Object[] negative(String raw, String error) {
        return new Object[]{raw, FormattedLine.error(raw, error)};
    }

    public IntWithCommentFormatterTest(String raw, FormattedLine normalized) {
        this.raw = raw;
        this.normalized = normalized;
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> cases() {
        return Arrays.asList(new Object[][]{
                positive(
                        "12345"
                ),
                positive(
                        "54789 # blabla",
                        "54789"
                ),
                negative(
                        "not an id",
                        "Not a number"
                )
        });
    }

    @Test
    public void test() {
        Assert.assertEquals(normalized, IntWithCommentFormatter.instance.format(raw));
    }
}
