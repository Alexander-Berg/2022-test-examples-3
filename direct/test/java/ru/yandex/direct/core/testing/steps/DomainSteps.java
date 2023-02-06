package ru.yandex.direct.core.testing.steps;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.direct.core.entity.domain.model.Domain;
import ru.yandex.direct.core.testing.info.DomainInfo;
import ru.yandex.direct.core.testing.repository.TestDomainRepository;

import static ru.yandex.direct.core.testing.data.TestDomain.testDomain;

@Component
public class DomainSteps {

    @Autowired
    private TestDomainRepository domainRepository;

    public DomainInfo createDomain(DomainInfo domainInfo) {
        Long domainId = domainRepository.addToPpcDict(domainInfo.getDomain().getDomain());
        domainInfo.getDomain().withId(domainId);

        domainRepository.addToPpc(domainInfo.getShard(), domainInfo.getDomain());

        return domainInfo;
    }

    public DomainInfo createDomain(int shard) {
        DomainInfo domainInfo = new DomainInfo().withDomain(testDomain()).withShard(shard);
        return createDomain(domainInfo);
    }

    public DomainInfo createDomain(int shard, String postfix) {
        return createDomain(shard, postfix, false);
    }

    public DomainInfo createDomain(int shard, String postfix, boolean lower) {
        DomainInfo domainInfo = new DomainInfo().withDomain(testDomain("test-", postfix, lower)).withShard(shard);
        return createDomain(domainInfo);
    }

    public DomainInfo createDomain(int shard, Domain domain) {
        DomainInfo domainInfo = new DomainInfo().withDomain(domain).withShard(shard);
        return createDomain(domainInfo);
    }

    public Map<String, Long> getDomainIdByDomain(int shard, List<String> domains) {
        return domainRepository.getDomainIdByDomain(shard, domains);
    }

    public void delete(int shard) {
        domainRepository.deleteDomains(shard);
    }

    public void delete(int shard, Collection<String> domains) {
        domainRepository.deleteDomains(shard, domains);
    }
}
