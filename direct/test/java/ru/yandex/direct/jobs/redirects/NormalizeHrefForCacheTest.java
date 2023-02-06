package ru.yandex.direct.jobs.redirects;

import java.util.Collection;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.jobs.redirects.RedirectCacheService.normalizeHref;

class NormalizeHrefForCacheTest {

    static Collection<Object[]> params() {
        return asList(new Object[][]{
                {"", ""},
                {"http://www.ya.ru", "http://www.ya.ru"},
                {"http://www.ya.ru/?afsd=eqwr", "http://www.ya.ru/?afsd=eqwr"},
                {"www.ya.ru/?afsd=eqwr", "www.ya.ru/?afsd=eqwr"},
                {"www.ya.ru/?asdf=wqer&utm_term=eqwr&utm_campaign=fadsf&wer=avzx", "www.ya.ru/?asdf=wqer&wer=avzx"},
                {"www.ya.ru/qqq?utm_term=eqwr&utm_campaign=fadsf", "www.ya.ru/qqq"},
                {"www.ya.ru/?utm_term=eqwr&utm_campaign=fadsf&wer=avzx", "www.ya.ru/?wer=avzx"},
                {"www.ya.ru/?asdf=234&utm_term=eqwr&utm_campaign=fadsf", "www.ya.ru/?asdf=234"},
                {"www.ya.ru/?openstat=123", "www.ya.ru/"},
                {"www.ya.ru?match_type=qqq", "www.ya.ru"},
                {"www.ya.ru/?matched_keyword=test", "www.ya.ru/"},
                {"www.ya.ru?utm_term=eqwr&p=1;match_type=88;title=qq;matched_keyword=test", "www.ya.ru?p=1&title=qq"},
                {"www.ya.ru/?neopenstat=123", "www.ya.ru/?neopenstat=123"},
        });
    }

    @ParameterizedTest(name = "href: {0}")
    @MethodSource("params")
    void test(String href, String expected) {
        String actual = normalizeHref(href);
        assertThat(actual).isEqualTo(expected);
    }
}
