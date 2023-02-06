package ru.yandex.crypta.lab;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.crypta.lab.formatters.FormattedLine;
import ru.yandex.crypta.lab.formatters.MetricaCounterFormatter;

@RunWith(Parameterized.class)
public class MetricaCounterFormatterTest {
    private final String raw;
    private final FormattedLine normalized;

    private static Object[] positive(String raw) {
        return new Object[]{raw, FormattedLine.line(raw)};
    }

    private static Object[] negative(String raw, String error) {
        return new Object[]{raw, FormattedLine.error(raw, error)};
    }

    public MetricaCounterFormatterTest(String raw, FormattedLine normalized) {
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
                        "12345:38962356"
                ),
                negative(
                        "12345:38962356:1123",
                        "Format is <counter_id>[:<goal_id>]"
                ),
                negative(
                        "12345:asfasdf",
                        "Ids must be numbers"
                ),
                negative(
                        "asfasdf",
                        "Ids must be numbers"
                )
        });
    }

    @Test
    public void test() {
        Assert.assertEquals(normalized, MetricaCounterFormatter.instance.format(raw));
    }
}
