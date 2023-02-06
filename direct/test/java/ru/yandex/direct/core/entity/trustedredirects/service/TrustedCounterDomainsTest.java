package ru.yandex.direct.core.entity.trustedredirects.service;

import java.util.Collection;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.trustedredirects.model.RedirectType;
import ru.yandex.direct.core.entity.trustedredirects.model.TrustedRedirects;
import ru.yandex.direct.core.entity.trustedredirects.repository.TrustedRedirectsRepository;
import ru.yandex.direct.dbschema.ppcdict.enums.TrustedRedirectsRedirectType;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class TrustedCounterDomainsTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"href: домен", "ya.ru", "https://www.ya.ru", true},
                {"href: длинный домен", "sport.ya.ru", "https://www.sport.ya.ru", true},
                {"href в unicode: домен unicode", "новости.рф", "https://новости.рф", true},
                {"href в unicode, www: домен unicode", "новости.рф", "https://www.новости.рф", true},

                {"href: домен 2-ого уровня", "ya.ru", "https://www.sport.ya.ru", true},
                {"href в unicode: домен 2-ого уровня unicode", "новости.рф", "https://спорт.новости.рф",
                        true},

                {"href в punycode: домен punycode", "xn--b1amnebsh.xn--p1ai",
                        "https://xn--b1amnebsh.xn--p1ai/about", true},

                {"href в punycode: домен 2-ого уровня punycode", "xn--b1amnebsh.xn--p1ai",
                        "https://xn--b1amnebsh.xn--b1amnebsh.xn--p1ai/about", true},


                {"href в punycode: домен unicode", "новости.рф",
                        "https://xn--b1amnebsh.xn--p1ai/about", true},

                {"href в punycode: домен 2-ого уровня unicode", "новости.рф",
                        "https://xn--b1amnebsh.xn--b1amnebsh.xn--p1ai/about", true},


                {"href в unicode, домен punycode", "xn--b1amnebsh.xn--p1ai",
                        "https://новости.рф", true},

                {"href в unicode: домен 2-ого уровня punycode", "xn--b1amnebsh.xn--p1ai",
                        "https://спорт.новости.рф", true},

                {"href не содержит доверенных доменов", "mail.ru", "https://www.mail.ya.ru", false},
        });
    }

    private String domain;
    private String href;
    private boolean contains;

    @SuppressWarnings("UnusedParameters")
    public TrustedCounterDomainsTest(String testName, String domain, String href, boolean contains) {
        this.domain = domain;
        this.href = href;
        this.contains = contains;
    }

    @Test
    public void parametrizedTest() {
        TrustedRedirectsService dict = buildDict(domain);
        assertThat(dict.isTrusted(href), is(contains));
    }

    private TrustedRedirectsService buildDict(String domain) {
        TrustedRedirectsRepository trustedDomainsRepository = mock(TrustedRedirectsRepository.class);
        when(trustedDomainsRepository.getTrustedDomains(TrustedRedirectsRedirectType.counter)).thenReturn(
                Collections.singletonList(new TrustedRedirects().withDomain(domain)
                        .withRedirectType(RedirectType.COUNTER)
                        .withOpts(Collections.emptySet())));
        return new TrustedRedirectsService(trustedDomainsRepository);
    }

}
