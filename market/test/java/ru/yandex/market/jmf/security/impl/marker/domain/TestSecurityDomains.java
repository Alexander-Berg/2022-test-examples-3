package ru.yandex.market.jmf.security.impl.marker.domain;

import java.util.Map;

import ru.yandex.market.jmf.metadata.Fqn;

public class TestSecurityDomains extends SecurityDomains {
    public TestSecurityDomains(Map<Fqn, SecurityDomain> map) {
        super(map);
    }
}
