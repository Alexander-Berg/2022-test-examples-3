package ru.yandex.direct.core.entity.promocodes.service;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.After;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.testing.repository.TestDomainRepository;

@ParametersAreNonnullByDefault
public class PromocodeTestBase {

    @Autowired
    private TestDomainRepository testDomainRepository;

    @After
    public void cleanup() {
        addedMirrors.forEach(testDomainRepository::deleteMainMirror);
        addedStat.forEach(testDomainRepository::deleteDomainStat);
        addedMirrors.clear();
        addedStat.clear();
    }

    private final Set<String> addedMirrors = new HashSet<>();
    private final Set<String> addedStat = new HashSet<>();

    void addMainMirror(String domain, String domainMirror) {
        testDomainRepository.addMainMirror(domain, domainMirror);
        addedMirrors.add(domain);
    }

    void addDomainStat(String domain, Long showsApprox) {
        testDomainRepository.addDomainStat(domain, showsApprox);
        addedStat.add(domain);
    }
}
