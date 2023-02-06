package ru.yandex.market.jmf.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class LinkUtilsTest {

    private final LinkUtils linkUtils = new LinkUtils();

    static Arguments[] externalLinksParameters() {
        return new Arguments[]{
                Arguments.of("Продавец Рога и Копыта: Чтобы получить товар, вам нужно оплатить его по ссылке: " +
                        "blablasite.com", true),
                Arguments.of("Текст с внешеней google.com и яндексовой ссылкой yandex.ru", true),
                Arguments.of("Текст с кириллицей гуггле.ком и яндексовой ссылкой яндекс.рф", true),
                Arguments.of("www.mp3#.com", true),
                Arguments.of("sftp://calendar.google", true),
                Arguments.of("http://www.foufos", true),
                Arguments.of("My little.pony", true),
                Arguments.of("www.t.co", true),
                Arguments.of("https://www.foufos.gr", true),
                Arguments.of("http is a protocol", false),
                Arguments.of("http://www.yandex.ru", false),
                Arguments.of("Http://foo.bar", true),
                Arguments.of("http://foufos.gr", true),
                Arguments.of("ftp://a.ya.ru", false),
                Arguments.of("https://www.t.co", true),
                Arguments.of("HTTPS://foo.bar", true),
                Arguments.of("http://foufos", false),
                Arguments.of("ya . ru", false),
                Arguments.of("ya.ru.narod.ru", true),
                Arguments.of("www.aa.com", true),
                Arguments.of("a.ya.ru", false),
                Arguments.of("www.yandex.ru ya.ru http://ya.ru", false),
                Arguments.of("www.mp3.com", true),
                Arguments.of("", false),
                Arguments.of("calendar.google", true),
                Arguments.of("www.foufos.gr", true),
                Arguments.of("https://ya.ru", false),
                Arguments.of("ftps://a.ya.ru", false),
                Arguments.of("http://www.foufos.gr", true),
                Arguments.of("http://www.aa.com", true),
                Arguments.of("Hello!", false),
                Arguments.of("http://t.co", true),
                Arguments.of("www.-foufos.gr", true),
                Arguments.of("Http://www.ya.ru", false),
                Arguments.of("Yandex.Technology", true),
                Arguments.of("yandex.ru.narod.ru", true),
                Arguments.of("ya.com", false),
                Arguments.of("http://ya.ru", false),
                Arguments.of("www.foufos-.gr", true),
                Arguments.of("My name is Inal", false),
                Arguments.of("a.ya.ru external.link", true),
                Arguments.of("https://www.yandex.ru", false),
                Arguments.of("http://werer.gr", true),
                Arguments.of("https://www.aa.com", true),
                Arguments.of("http://aa.com", true),
                Arguments.of("www.foufos", true),
                Arguments.of("foufos.gr", true),
                Arguments.of("Hello World", false),
                Arguments.of("Hi", false),
                Arguments.of("some.site.com?path=yandex.ru", true),
                Arguments.of("http://www.foufos.gr/kino", true),
                Arguments.of("http://www.t.co", true),
                Arguments.of("m.market.yandex.ru", false),
                Arguments.of("http://m.market.yandex.ru", false)
        };
    }

    @ParameterizedTest
    @MethodSource("externalLinksParameters")
    public void containsExternalLinksTest(String text, boolean containsExternalLink) {
        Assertions.assertEquals(containsExternalLink, linkUtils.containsExternalLinks(text));
    }
}
