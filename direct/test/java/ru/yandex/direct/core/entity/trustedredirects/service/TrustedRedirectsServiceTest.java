package ru.yandex.direct.core.entity.trustedredirects.service;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.trustedredirects.model.Opts;
import ru.yandex.direct.core.entity.trustedredirects.model.RedirectType;
import ru.yandex.direct.core.entity.trustedredirects.model.TrustedRedirects;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.dbschema.ppcdict.tables.TrustedRedirects.TRUSTED_REDIRECTS;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TrustedRedirectsServiceTest {

    @Autowired
    private TrustedRedirectsService trustedRedirectsService;
    @Autowired
    private DslContextProvider dslContextProvider;

    @Before
    public void before() {
        // clean table
        dslContextProvider.ppcdict().deleteFrom(TRUSTED_REDIRECTS).execute();
    }

    @Test
    public void addTrustedDomain_FromHrefNoOptions_Successful() {
        String href = "http://planeta.ru/about";
        TrustedRedirects trustedRedirects = new TrustedRedirects().withDomain(href).
                withRedirectType(RedirectType.COUNTER);
        boolean result = trustedRedirectsService.addTrustedDomain(trustedRedirects);
        assertTrue(result);
        List<TrustedRedirects> selected = trustedRedirectsService.getCounterTrustedRedirects();
        assertThat(selected, hasSize(1));

        trustedRedirects.setOpts(Collections.emptySet());
        assertThat("даже если клали объект без опций, достаем с пустыми опциями",
                selected, contains(beanDiffer(trustedRedirects)));
    }

    @Test
    public void addTrustedDomain_InvalidDomain_NotAdded() {
        TrustedRedirects trustedRedirects = new TrustedRedirects().withDomain("ya..ru").
                withRedirectType(RedirectType.COUNTER);
        boolean result = trustedRedirectsService.addTrustedDomain(trustedRedirects);
        assertFalse(result);
        List<TrustedRedirects> selected = trustedRedirectsService.getCounterTrustedRedirects();
        assertThat(selected, hasSize(0));

    }

    @Test
    public void addTrustedDomain_ShortDomainWithOptions_Successful() {
        String domain = "ya.ru";
        TrustedRedirects trustedRedirects = new TrustedRedirects().withDomain(domain).
                withRedirectType(RedirectType.COUNTER).
                withOpts(EnumSet.of(Opts.https_only, Opts.allow_wildcard));
        boolean result = trustedRedirectsService.addTrustedDomain(trustedRedirects);
        assertTrue(result);
        List<TrustedRedirects> selected = trustedRedirectsService.getCounterTrustedRedirects();
        assertThat(selected, hasSize(1));
        assertThat("что положили, то и достали", selected, contains(beanDiffer(trustedRedirects)));
    }

    @Test
    public void deleteDomain_InvalidDomain_NotDeleted() {
        TrustedRedirects trustedRedirects = new TrustedRedirects().withDomain("https://ya.ru").
                withRedirectType(RedirectType.COUNTER);
        trustedRedirectsService.addTrustedDomain(trustedRedirects);


        TrustedRedirects toDelete = new TrustedRedirects().withDomain("https://ya.ru").
                withRedirectType(RedirectType.COUNTER);
        boolean result = trustedRedirectsService.deleteDomain(toDelete);
        assertFalse(result);
        List<TrustedRedirects> selected = trustedRedirectsService.getCounterTrustedRedirects();
        assertThat(selected, hasSize(1));
    }

    @Test
    public void deleteDomain_ValidDomain_Successful() {
        TrustedRedirects trustedRedirects = new TrustedRedirects().withDomain("ya.ru").
                withRedirectType(RedirectType.COUNTER);
        trustedRedirectsService.addTrustedDomain(trustedRedirects);

        boolean result = trustedRedirectsService.deleteDomain(trustedRedirects);
        assertTrue(result);
        List<TrustedRedirects> selected = trustedRedirectsService.getCounterTrustedRedirects();
        assertThat(selected, hasSize(0));
    }
}
