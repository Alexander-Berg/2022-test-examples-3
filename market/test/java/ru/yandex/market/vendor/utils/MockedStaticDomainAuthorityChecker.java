package ru.yandex.market.vendor.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ru.yandex.market.security.checker.StaticDomainAuthorityChecker;
import ru.yandex.market.security.model.Authority;
import ru.yandex.market.security.model.Uidable;

public class MockedStaticDomainAuthorityChecker extends StaticDomainAuthorityChecker {

    private final StaticDomainAuthoritiesProvider staticDomainAuthoritiesProvider;

    public MockedStaticDomainAuthorityChecker(StaticDomainAuthoritiesProvider staticDomainAuthoritiesProvider) {
        super(null);
        this.staticDomainAuthoritiesProvider = staticDomainAuthoritiesProvider;
    }


    @Override
    protected boolean checkTyped(Uidable data, Authority authority) {
        final long uid = data.getUid();
        final String name = authority.getName();
        if (name == null) {
            return false;
        }
        final String domain = authority.getDomain();
        if (domain == null) {
            return false;
        }
        Map<String, Set<Long>> rolesToUids = staticDomainAuthoritiesProvider.getRoles();
        return rolesToUids.containsKey(name) && rolesToUids.get(name).contains(uid);

    }
}
