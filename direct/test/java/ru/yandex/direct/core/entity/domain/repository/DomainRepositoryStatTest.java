package ru.yandex.direct.core.entity.domain.repository;

import java.util.Collections;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.domain.model.Domain;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestDomain;
import ru.yandex.direct.core.testing.repository.TestDomainRepository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DomainRepositoryStatTest {

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private TestDomainRepository testDomainRepository;

    private Domain domain;

    @Before
    public void before() {
        domain = TestDomain.testDomain();
    }

    @After
    public void cleanup() {
        testDomainRepository.deleteDomainStat(domain.getDomain());
    }

    @Test
    public void getDomainsWithStat_nonZeroStat() {
        testDomainRepository.addDomainStat(domain.getDomain(), 10L);

        Set<String> domainsWithStat = domainRepository.getDomainsWithStat(Collections.singleton(domain.getDomain()));

        assertThat(domainsWithStat, hasSize(1));
        assertThat(domainsWithStat, contains(domain.getDomain()));
    }

    @Test
    public void getDomainsWithStat_zeroStat() {
        testDomainRepository.addDomainStat(domain.getDomain(), 0L);

        Set<String> domainsWithStat = domainRepository.getDomainsWithStat(Collections.singleton(domain.getDomain()));

        assertThat(domainsWithStat, empty());
    }

    @Test
    public void getDomainsWithStat_empty() {
        Set<String> domainsWithStat = domainRepository.getDomainsWithStat(Collections.singleton(domain.getDomain()));

        assertThat(domainsWithStat, empty());
    }
}
