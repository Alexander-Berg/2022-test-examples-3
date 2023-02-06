package ru.yandex.market.jmf.security.test;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.metaclass.Metaclass;
import ru.yandex.market.jmf.security.impl.action.domain.SecurityDomain;
import ru.yandex.market.jmf.security.impl.action.domain.SecurityDomains;
import ru.yandex.market.jmf.security.impl.action.domain.TestSecurityDomain;
import ru.yandex.market.jmf.security.impl.action.domain.TestSecurityDomains;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SecurityDomainsTest {
    @Test
    public void testGetsMostSpecificSecurityDomain() {
        Metaclass parent = mock(Metaclass.class);
        Metaclass child = mock(Metaclass.class);
        Metaclass anotherParent = mock(Metaclass.class);

        when(parent.getFqn()).thenReturn(Fqn.parse("parent"));

        when(child.getFqn()).thenReturn(Fqn.parse("child"));
        when(child.getParent()).thenReturn(parent);

        when(anotherParent.getFqn()).thenReturn(Fqn.parse("anotherParent"));

        SecurityDomain parentDomain = new TestSecurityDomain(parent, SecurityDomain.DEFAULT);
        SecurityDomain childDomain = new TestSecurityDomain(child, parentDomain);
        SecurityDomain anotherParentDomain = new TestSecurityDomain(anotherParent, SecurityDomain.DEFAULT);

        Map<Fqn, SecurityDomain> securityDomainMap = new HashMap<>();
        securityDomainMap.put(parent.getFqn(), parentDomain);
        securityDomainMap.put(child.getFqn(), childDomain);
        securityDomainMap.put(anotherParent.getFqn(), anotherParentDomain);
        SecurityDomains securityDomains = new TestSecurityDomains(securityDomainMap);

        Assertions.assertEquals(childDomain, securityDomains.getSecurityDomain(child));
        Assertions.assertEquals(parentDomain, securityDomains.getSecurityDomain(parent));
    }

    @Test
    public void testConstructsDefaultDomainIfNotDefined() {
        Metaclass parent = mock(Metaclass.class);

        when(parent.getFqn()).thenReturn(Fqn.parse("parent"));

        Map<Fqn, SecurityDomain> securityDomainMap = new HashMap<>();
        SecurityDomains securityDomains = new TestSecurityDomains(securityDomainMap);

        var securityDomain = securityDomains.getSecurityDomain(parent);
        Assertions.assertNotNull(securityDomain);
        Assertions.assertNotEquals(SecurityDomain.DEFAULT, securityDomain);
    }
}
