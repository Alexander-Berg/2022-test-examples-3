package ru.yandex.direct.core.testing.steps;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.trustedredirects.model.Opts;
import ru.yandex.direct.core.entity.trustedredirects.model.RedirectType;
import ru.yandex.direct.core.entity.trustedredirects.model.TrustedRedirects;
import ru.yandex.direct.core.entity.trustedredirects.repository.TrustedRedirectsRepository;

import static java.util.Collections.singleton;

public class TrustedRedirectSteps {

    public static final String DOMAIN = "trusted1.com";

    private final TrustedRedirectsRepository trustedRedirectsRepository;

    @Autowired
    public TrustedRedirectSteps(TrustedRedirectsRepository trustedRedirectsRepository) {
        this.trustedRedirectsRepository = trustedRedirectsRepository;
    }

    public void addValidCounter() {
        TrustedRedirects trustedRedirects = new TrustedRedirects()
                .withDomain("yandex.ru")
                .withRedirectType(RedirectType.COUNTER)
                .withOpts(singleton(Opts.allow_wildcard));
        trustedRedirectsRepository.addTrustedDomain(trustedRedirects);
    }


    public void addValidMobileCounter() {
        TrustedRedirects trustedRedirects = new TrustedRedirects()
                .withDomain(DOMAIN).withRedirectType(RedirectType.MOBILE_APP_COUNTER)
                .withOpts(singleton(Opts.allow_wildcard));
        trustedRedirectsRepository.addTrustedDomain(trustedRedirects);
        List<TrustedRedirects> trustedRedirectList = Arrays.asList(
                new TrustedRedirects()
                        .withDomain(DOMAIN).withRedirectType(RedirectType.MOBILE_APP_COUNTER)
                        .withOpts(singleton(Opts.allow_wildcard)),
                new TrustedRedirects()
                        .withDomain("redirect.appmetrica.yandex.com")
                        .withRedirectType(RedirectType.MOBILE_APP_COUNTER)
                        .withOpts(singleton(Opts.allow_wildcard)),
                new TrustedRedirects()
                        .withDomain("adjust.com")
                        .withRedirectType(RedirectType.MOBILE_APP_COUNTER)
                        .withOpts(singleton(Opts.allow_wildcard)),
                new TrustedRedirects()
                        .withDomain("app.appsflyer.com")
                        .withRedirectType(RedirectType.MOBILE_APP_COUNTER)
                        .withOpts(singleton(Opts.allow_wildcard)),
                new TrustedRedirects()
                        .withDomain("trusted.impression.com").withRedirectType(RedirectType.MOBILE_APP_IMPRESSION_COUNTER)
                        .withOpts(singleton(Opts.allow_wildcard)),
                new TrustedRedirects()
                        .withDomain("view.adjust.com")
                        .withRedirectType(RedirectType.MOBILE_APP_IMPRESSION_COUNTER)
                        .withOpts(singleton(Opts.allow_wildcard)),
                new TrustedRedirects()
                        .withDomain("impression.appsflyer.com")
                        .withRedirectType(RedirectType.MOBILE_APP_IMPRESSION_COUNTER)
                        .withOpts(singleton(Opts.allow_wildcard))
        );
        trustedRedirectList.forEach(trustedRedirectsRepository::addTrustedDomain);
    }

    public void addValidCounters() {
        addValidMobileCounter();
        addValidCounter();
    }

    public void deleteTrusted() {
        TrustedRedirects trustedRedirects = new TrustedRedirects()
                .withDomain(DOMAIN)
                .withRedirectType(RedirectType.MOBILE_APP_COUNTER);
        trustedRedirectsRepository.deleteDomain(trustedRedirects);
        trustedRedirects = new TrustedRedirects()
                .withDomain("yandex.ru")
                .withRedirectType(RedirectType.COUNTER);
        trustedRedirectsRepository.deleteDomain(trustedRedirects);

    }
}
