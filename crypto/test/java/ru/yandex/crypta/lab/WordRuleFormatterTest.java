package ru.yandex.crypta.lab;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.crypta.lab.formatters.FormattedLine;
import ru.yandex.crypta.lab.formatters.WordRuleFormatter;

@RunWith(Parameterized.class)
public class WordRuleFormatterTest {
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

    public WordRuleFormatterTest(String raw, FormattedLine normalized) {
        this.raw = raw;
        this.normalized = normalized;
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> cases() {
        return Arrays.asList(new Object[][]{
                positive(
                    "грыжа AND межпозвонкового AND диска",
                    "грыжа AND межпозвонковый AND диск"
                ),
                positive(
                        "мама AND мыла AND раму",
                        "мама AND (мыло OR мыть) AND рама",
                        List.of(WordRuleFormatter.AMBIGUOUS_TAG)
                ),
                positive(
                        "знакомство OR познакомиться OR (находить AND (женщина OR девушка)) AND NOT бесплатно",
                        "знакомство OR познакомиться OR (находить AND (женщина OR девушка) AND NOT бесплатно)"
                ),
                positive(
                        "АМНП Солнышко",
                        "амнп AND солнышко"
                ),
                negative(
                        "купить AND",
                        "position 12: no viable alternative at input 'AND'"
                ),
                positive(
                        "АМНП-01",
                        "амнп AND 01"
                ),
                positive(
                        "metro AND and",
                        "metro AND and"
                ),
                positive(
                        "орехово-зуево OR слово",
                        "(орехово AND зуево) OR слово"
                )
        });
    }

    @Test
    public void test() {
        Assert.assertEquals(normalized, WordRuleFormatter.instance.format(raw));
    }
}
