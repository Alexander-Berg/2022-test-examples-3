package ru.yandex.direct.core.entity.trustedredirects.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;

import ru.yandex.direct.core.entity.trustedredirects.model.Opts;
import ru.yandex.direct.core.entity.trustedredirects.model.RedirectType;
import ru.yandex.direct.core.entity.trustedredirects.model.TrustedRedirects;
import ru.yandex.direct.core.entity.trustedredirects.repository.TrustedRedirectsRepository;
import ru.yandex.direct.core.entity.trustedredirects.service.TrustedRedirectsService.Result;
import ru.yandex.direct.dbschema.ppcdict.enums.TrustedRedirectsRedirectType;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.core.entity.trustedredirects.service.TrustedRedirectsService.Result.HTTPS_REQUIRED;
import static ru.yandex.direct.core.entity.trustedredirects.service.TrustedRedirectsService.Result.NOT_TRUSTED;
import static ru.yandex.direct.core.entity.trustedredirects.service.TrustedRedirectsService.Result.TRUSTED;

@RunWith(Parameterized.class)
public class TrustedMobileCounterDomainsTest {

    @Mock
    private TrustedRedirectsRepository repository;

    // under test
    private TrustedRedirectsService dict;

    @Parameter
    public String description;
    @Parameter(1)
    public String domainInDict;
    @Parameter(2)
    public boolean domainIsHttpsOnly;
    @Parameter(3)
    public boolean subDomainsAllowed;
    @Parameter(4)
    public String href;
    @Parameter(5)
    public Result expected;

    @Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(
                new TestCase("href: домен")
                        .domainInDict("ya.ru")
                        .href("https://www.ya.ru").expect(TRUSTED),

                new TestCase("href: длинный домен")
                        .domainInDict("sport.ya.ru")
                        .href("https://www.sport.ya.ru").expect(TRUSTED),

                new TestCase("href в unicode: домен unicode")
                        .domainInDict("новости.рф")
                        .href("https://новости.рф").expect(TRUSTED),

                new TestCase("href в unicode, www: домен unicode")
                        .domainInDict("новости.рф")
                        .href("https://www.новости.рф").expect(TRUSTED),

                new TestCase("href: домен 2-ого уровня")
                        .domainInDict("ya.ru")
                        .href("https://www.sport.ya.ru").expect(TRUSTED),

                new TestCase("href в unicode: домен 2-ого уровня unicode")
                        .domainInDict("новости.рф")
                        .href("https://спорт.новости.рф").expect(TRUSTED),

                new TestCase("href в punycode: домен punycode")
                        .domainInDict("xn--b1amnebsh.xn--p1ai")
                        .href("https://xn--b1amnebsh.xn--p1ai/about").expect(TRUSTED),

                new TestCase("href в punycode: домен 2-ого уровня punycode")
                        .domainInDict("xn--b1amnebsh.xn--p1ai")
                        .href("https://xn--b1amnebsh.xn--b1amnebsh.xn--p1ai/about").expect(TRUSTED),

                new TestCase("href в punycode: домен unicode")
                        .domainInDict("новости.рф")
                        .href("https://xn--b1amnebsh.xn--p1ai/about").expect(TRUSTED),

                new TestCase("href в punycode: домен 2-ого уровня unicode")
                        .domainInDict("новости.рф")
                        .href("https://xn--b1amnebsh.xn--b1amnebsh.xn--p1ai/about").expect(TRUSTED),

                new TestCase("href в unicode, домен punycode")
                        .domainInDict("xn--b1amnebsh.xn--p1ai")
                        .href("https://новости.рф").expect(TRUSTED),

                new TestCase("href в unicode: домен 2-ого уровня punycode")
                        .domainInDict("xn--b1amnebsh.xn--p1ai")
                        .href("https://спорт.новости.рф").expect(TRUSTED),

                new TestCase("href не содержит доверенных доменов")
                        .domainInDict("mail.ru")
                        .href("https://www.mail.ya.ru").expect(NOT_TRUSTED),

                new TestCase("href https, нужен https")
                        .domainInDict("новости.рф").httpsOnly()
                        .href("https://www.новости.рф").expect(TRUSTED),

                new TestCase("href не https, нужен https")
                        .domainInDict("новости.рф").httpsOnly()
                        .href("http://www.новости.рф").expect(HTTPS_REQUIRED),

                new TestCase("href не https, не https допускается")
                        .domainInDict("новости.рф")
                        .href("http://www.новости.рф").expect(TRUSTED),

                new TestCase("href не https, не найден")
                        .domainInDict("news.com")
                        .href("http://www.новости.рф").expect(NOT_TRUSTED),

                new TestCase("href https, с флагом \"разрешить субдомены\", допустимый субдомен 1-го уровня")
                        .domainInDict("some.exampl6.com")
                        .allowWildcard()
                        .href("https://some.exampl6.com").expect(TRUSTED),

                new TestCase("href https, с флагом \"разрешить субдомены\", допустимый субдомен 2-го уровня")
                        .domainInDict("some.exampl6.com")
                        .allowWildcard()
                        .href("https://a.some.exampl6.com").expect(TRUSTED),

                new TestCase("href не https, с флагом httpsOnly и \"разрешить субдомены\"")
                        .domainInDict("some.exampl7.com")
                        .httpsOnly().allowWildcard()
                        .href("http://some.exampl7.com").expect(HTTPS_REQUIRED),

                new TestCase("href не https, с флагом httpsOnly и \"разрешить субдомены\", недопустимый субдомен")
                        .domainInDict("some.exampl7.com")
                        .httpsOnly().allowWildcard()
                        .href("http://a.b.some.exampl7.com").expect(NOT_TRUSTED),

                new TestCase("href https, с флагом httpsOnly и \"разрешить субдомены\", допустимый субдомен")
                        .domainInDict("some.exampl7.com")
                        .httpsOnly().allowWildcard()
                        .href("https://a.some.exampl7.com").expect(TRUSTED)
        );
    }

    @Before
    public void initDict() {
        initMocks(this);

        List<TrustedRedirects> domainAsList = singletonList(new TrustedRedirects()
                .withDomain(domainInDict)
                .withRedirectType(RedirectType.MOBILE_APP_COUNTER)
                .withOpts(new HashSet<Opts>() {{
                    if (domainIsHttpsOnly) {
                        add(Opts.https_only);
                    }
                    if (subDomainsAllowed) {
                        add(Opts.allow_wildcard);
                    }
                }}));
        when(repository.getTrustedDomains(TrustedRedirectsRedirectType.mobile_app_counter)).thenReturn(domainAsList);
        dict = new TrustedRedirectsService(repository);
    }

    @Test
    public void parametrizedTest() {
        assertThat(dict.checkTrackingHref(href), is(expected));
    }

    private static class TestCase {
        private String desc, domain, href;
        private boolean httpsOnly;
        private boolean allowWildcard;

        TestCase(String desc) {
            this.desc = desc;
        }

        TestCase domainInDict(String domain) {
            this.domain = domain;
            return this;
        }

        TestCase httpsOnly() {
            this.httpsOnly = true;
            return this;
        }

        TestCase allowWildcard() {
            this.allowWildcard = true;
            return this;
        }

        TestCase href(String href) {
            this.href = href;
            return this;
        }

        Object[] expect(Result result) {
            return new Object[]{desc, domain, httpsOnly, allowWildcard, href, result};
        }
    }

}
