package ru.yandex.crypta.lab;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.crypta.lab.formatters.FormattedLine;
import ru.yandex.crypta.lab.formatters.UrlFormatter;

@RunWith(Parameterized.class)
public class UrlFormatterTest {
    private final String raw;
    private final FormattedLine withPath;
    private final FormattedLine withoutPath;

    private static Object[] testCase(String raw, FormattedLine withPath, FormattedLine withoutPath) {
        return new Object[]{raw, withPath, withoutPath};
    }

    private static Object[] testCase(String raw, FormattedLine withoutPath) {
        return testCase(raw, withoutPath, withoutPath);
    }

    public UrlFormatterTest(String raw, FormattedLine withPath, FormattedLine withoutPath) {
        this.raw = raw;
        this.withPath = withPath;
        this.withoutPath = withoutPath;
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> cases() {
        return Arrays.asList(new Object[][]{
                testCase(
                        "https://lab.crypta.yandex-team.ru/",
                        FormattedLine.line(
                                "https://lab.crypta.yandex-team.ru/",
                                "lab.crypta.yandex-team.ru"
                        ),
                        FormattedLine.line(
                                "https://lab.crypta.yandex-team.ru/",
                                "lab.crypta.yandex-team.ru",
                                List.of(UrlFormatter.HAS_PATH_TAG)
                        )
                ),
                testCase(
                        "www.rbc.ru",
                        FormattedLine.line(
                                "www.rbc.ru",
                                "rbc.ru"
                        )
                ),
                testCase(
                        "ololo://www.rbc.ru",
                        FormattedLine.error(
                                "ololo://www.rbc.ru",
                                "unknown protocol: ololo"
                        )
                ),
                testCase(
                        "ru.wikipedia.org/wiki/URI#Структура_URI",
                        FormattedLine.line(
                                "ru.wikipedia.org/wiki/URI#Структура_URI",
                                "ru.wikipedia.org/wiki/URI",
                                List.of(UrlFormatter.HAS_FRAGMENT_TAG)
                        ),
                        FormattedLine.line(
                                "ru.wikipedia.org/wiki/URI#Структура_URI",
                                "ru.wikipedia.org",
                                List.of(UrlFormatter.HAS_FRAGMENT_TAG, UrlFormatter.HAS_PATH_TAG)
                        )
                ),
                testCase(
                        "market.yandex.ru/catalog--smartfony/16814639/list?hid=91491&onstock=1&local-offers-first=0",
                        FormattedLine.line(
                                "market.yandex.ru/catalog--smartfony/16814639/list?hid=91491&onstock=1&local-offers" +
                                        "-first=0",
                                "market.yandex.ru/catalog--smartfony/16814639/list",
                                List.of(UrlFormatter.HAS_QUERY_TAG)
                        ),
                        FormattedLine.line(
                                "market.yandex.ru/catalog--smartfony/16814639/list?hid=91491&onstock=1&local-offers" +
                                        "-first=0",
                                "market.yandex.ru",
                                List.of(UrlFormatter.HAS_QUERY_TAG, UrlFormatter.HAS_PATH_TAG)
                        )
                ),
                testCase(
                        "regexp:market.yandex.ru/catalog--smartfony/\\d+/list",
                        FormattedLine.line(
                                "regexp:market.yandex.ru/catalog--smartfony/\\d+/list"
                        )
                ),
                testCase(
                        "regexp:invalid[",
                        FormattedLine.error(
                                "regexp:invalid[",
                                "Unclosed character class near index 7\n" +
                                        "invalid[\n" +
                                        "       ^"
                        )
                ),
                testCase(
                        "blablabla/path",
                        FormattedLine.error(
                                "blablabla/path",
                                "Hostname is invalid: blablabla"
                        )
                ),
                testCase(
                        "word AND stuff",
                        FormattedLine.error(
                                "word AND stuff",
                                "Url contains spaces: word AND stuff"
                        )
                ),
                testCase(
                        "http://globalsources.com/, https://russian.alibaba.com/, https://www.1688.com/",
                        FormattedLine.error(
                                "http://globalsources.com/, https://russian.alibaba.com/, https://www.1688.com/",
                                "Url contains spaces: http://globalsources.com/, https://russian.alibaba.com/, https://www.1688.com/"
                        )
                ),
                testCase(
                        "санатории-кмв.рф/города/ессентуки",
                        FormattedLine.line(
                                "санатории-кмв.рф/города/ессентуки"
                        ),
                        FormattedLine.line(
                                "санатории-кмв.рф/города/ессентуки",
                                "санатории-кмв.рф",
                                List.of(UrlFormatter.HAS_PATH_TAG)
                        )
                ),
                testCase(
                        "ftp://www.rbc.ru",
                        FormattedLine.error(
                                "ftp://www.rbc.ru",
                                "unsupported protocol: ftp"
                        )
                ),
        });
    }

    @Test
    public void test() {
        Assert.assertEquals(withPath, UrlFormatter.withPath.format(raw));
        Assert.assertEquals(withoutPath, UrlFormatter.withoutPath.format(raw));
    }
}
