package ru.yandex.direct.core.entity.trustedredirects.repository;

import java.util.Collections;
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
import ru.yandex.direct.dbschema.ppcdict.enums.TrustedRedirectsRedirectType;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.dbschema.ppcdict.tables.TrustedRedirects.TRUSTED_REDIRECTS;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TrustedRedirectsRepositoryTest {

    public static final String DOMAIN = "ya.ru";
    @Autowired
    private TrustedRedirectsRepository repository;
    @Autowired
    private DslContextProvider dslContextProvider;


    @Before
    public void before() {
        // clean table
        dslContextProvider.ppcdict().deleteFrom(TRUSTED_REDIRECTS).execute();
    }

    @Test
    public void addDomain_WithNoOptions_SuccessfulAdd() {
        TrustedRedirects trustedRedirects = new TrustedRedirects().withDomain(DOMAIN).
                withRedirectType(RedirectType.COUNTER);
        repository.addTrustedDomain(trustedRedirects);
        List<TrustedRedirects> afterInsertList = repository.getCounterTrustedRedirects();
        trustedRedirects.setOpts(Collections.emptySet());
        assertThat("даже если клали объект без опций, достаем с пустыми опциями",
                afterInsertList, contains(beanDiffer(trustedRedirects)));
    }

    @Test
    public void addDomain_WithOptions_SuccessfulAdd() {
        TrustedRedirects trustedRedirects = new TrustedRedirects().withDomain(DOMAIN)
                .withRedirectType(RedirectType.COUNTER)
                .withOpts(singleton(Opts.allow_wildcard));
        repository.addTrustedDomain(trustedRedirects);
        List<TrustedRedirects> afterInsertList = repository.getCounterTrustedRedirects();
        assertThat("что положили, то и достали", afterInsertList, contains(beanDiffer(trustedRedirects)));

    }


    @Test
    public void addDomain_ShortType_SuccessfulAdd() {
        TrustedRedirects trustedRedirects = new TrustedRedirects().withDomain(DOMAIN)
                .withRedirectType(RedirectType.SHORT_).withOpts(emptySet());
        repository.addTrustedDomain(trustedRedirects);
        List<TrustedRedirects> afterInsertList = repository.getTrustedDomains(TrustedRedirectsRedirectType.short_);
        assertThat("что положили, то и достали", afterInsertList, contains(beanDiffer(trustedRedirects)));

    }

    @Test
    public void deleteDomain() {
        TrustedRedirects trustedRedirects = new TrustedRedirects().withDomain(DOMAIN).
                withRedirectType(RedirectType.COUNTER);
        repository.addTrustedDomain(trustedRedirects);
        boolean result = repository.deleteDomain(trustedRedirects);
        assertTrue(result);
        List<TrustedRedirects> afterDeleteList = repository.getCounterTrustedRedirects();
        assertThat(afterDeleteList, hasSize(0));
    }

}
