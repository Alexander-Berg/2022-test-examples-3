package ru.yandex.market.vendor.utils;


import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ru.yandex.market.security.core.IStaticDomainAuthoritiesLoader;
import ru.yandex.market.security.model.StaticDomainAuthority;

import static ru.yandex.cs.billing.CsBillingCoreConstants.VENDOR_IDM_DOMAIN;

public class MockedStaticDomainAuthoritiesLoader implements IStaticDomainAuthoritiesLoader {

    private final StaticDomainAuthoritiesProvider staticDomainAuthoritiesProvider;

    public MockedStaticDomainAuthoritiesLoader(StaticDomainAuthoritiesProvider staticDomainAuthoritiesProvider) {
        this.staticDomainAuthoritiesProvider = staticDomainAuthoritiesProvider;
    }

    @Override
    public List<StaticDomainAuthority> load(String domain) {
        return staticDomainAuthoritiesProvider.getRoles().entrySet().stream()
                .flatMap(roleUidsEntry -> roleUidsEntry.getValue().stream()
                        .map(uid -> new StaticDomainAuthority(VENDOR_IDM_DOMAIN, roleUidsEntry.getKey(), uid)))
                .collect(Collectors.toList());
    }

}
