package ru.yandex.direct.core.testing.steps;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppcdict.tables.EcomDomains.ECOM_DOMAINS;

@Component
public class EcomDomainsSteps {
    private final DslContextProvider dslContextProvider;

    @Autowired
    public EcomDomainsSteps(DslContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
    }

    public void addEcomDomain(String domain, Long offersCount) {
        dslContextProvider.ppcdict()
                .insertInto(ECOM_DOMAINS)
                .columns(ECOM_DOMAINS.DOMAIN, ECOM_DOMAINS.OFFERS_COUNT)
                .values(domain, offersCount)
                .onDuplicateKeyIgnore()
                .execute();
    }

    public void addEcomDomain(String domain, Long offersCount, LocalDateTime lastModifiedPreview) {
        dslContextProvider.ppcdict()
                .insertInto(ECOM_DOMAINS)
                .columns(ECOM_DOMAINS.DOMAIN, ECOM_DOMAINS.OFFERS_COUNT, ECOM_DOMAINS.LAST_MODIFIED_PREVIEW)
                .values(domain, offersCount, lastModifiedPreview)
                .onDuplicateKeyIgnore()
                .execute();
    }

    public void addEcomDomain(String domain) {
        addEcomDomain(domain, 10L);
    }
}
