package ru.yandex.crypta.lab;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.crypta.lab.formatters.ExpressionFormatter;
import ru.yandex.crypta.lab.formatters.FormattedLine;

@RunWith(Parameterized.class)
public class ExpressionFormatterTest {
    private final String raw;
    private final FormattedLine normalized;

    private static Object[] positive(String raw, String normalized, List<String> tags) {
        return new Object[]{raw, FormattedLine.line(raw, normalized, tags)};
    }

    private static Object[] positive(String raw, String normalized) {
        return positive(raw, normalized, List.of());
    }

    private static Object[] negative(String raw, String error) {
        return new Object[]{raw, FormattedLine.error(raw, error)};
    }

    public ExpressionFormatterTest(String raw, FormattedLine normalized) {
        this.raw = raw;
        this.normalized = normalized;
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> cases() {
        return Arrays.asList(new Object[][]{
                positive(
                        "export-1 AND export-2 AND export-3",
                        "export-1 AND export-2 AND export-3"
                ),
                positive(
                        "export-1 OR export-2 OR (export-3 AND (export-4 OR export-5)) AND NOT export-6",
                        "export-1 OR export-2 OR (export-3 AND (export-4 OR export-5) AND NOT export-6)"
                ),
                negative(
                        "export-1 export-2",
                        "position 9: mismatched input 'export-2' expecting <EOF>"
                ),
                negative(
                        "export-1 AND",
                        "position 12: no viable alternative at input '<EOF>'"
                ),
                negative(
                        "export-1 AND export-10",
                        "Unknown exports: export-10"
                ),
        });
    }

    private static final Set<String> UNKNOWN_EXPORTS = Cf.set(
        "export-10"
    );

    public static Set<String> getUnknownExports(Set<String> exports) {
        return exports.stream()
                .filter(UNKNOWN_EXPORTS::contains)
                .collect(Collectors.toSet());
    }

    @Test
    public void test() {
        Assert.assertEquals(normalized, new ExpressionFormatter(ExpressionFormatterTest::getUnknownExports).format(raw));
    }
}
