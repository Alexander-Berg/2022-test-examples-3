package ru.yandex.crypta.lab;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.crypta.lab.formatters.AppFormatter;
import ru.yandex.crypta.lab.formatters.FormattedLine;

@RunWith(Parameterized.class)
public class AppFormatterTest {
    private final String raw;
    private final FormattedLine normalized;

    private static Object[] positive(String raw) {
        return new Object[]{raw, FormattedLine.line(raw)};
    }

    private static Object[] negative(String raw, String error) {
        return new Object[]{raw, FormattedLine.error(raw, error)};
    }

    public AppFormatterTest(String raw, FormattedLine normalized) {
        this.raw = raw;
        this.normalized = normalized;
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> cases() {
        return Arrays.asList(new Object[][]{
                positive(
                        "com.allgoritm.youla"
                ),
                positive(
                        "COM.all-goritm.youla_"
                ),
                negative(
                        "app.1 app.2",
                        "App id can only contain English letters, numbers, dots, hyphens and underscores"
                ),
                negative(
                        "моё.приложение",
                        "App id can only contain English letters, numbers, dots, hyphens and underscores"
                ),
        });
    }

    @Test
    public void test() {
        Assert.assertEquals(normalized, AppFormatter.instance.format(raw));
    }
}
