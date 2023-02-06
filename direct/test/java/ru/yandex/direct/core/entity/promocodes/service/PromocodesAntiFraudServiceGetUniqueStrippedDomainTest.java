package ru.yandex.direct.core.entity.promocodes.service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PromocodesAntiFraudServiceGetUniqueStrippedDomainTest {

    @Autowired
    private PromocodesAntiFraudService service;

    @Test
    public void emptyCandidates_returnsNull() {
        assertThat(service.getUniqueStrippedDomain(emptySet()), nullValue());
    }

    @Test
    public void singleDomain_returnsSame() {
        String domain = "yandex.ru";
        assertThat(service.getUniqueStrippedDomain(asSet(domain)), equalTo(domain));
    }

    @Test
    public void singleDomainWithWww_returnsStripped() {
        assertThat(service.getUniqueStrippedDomain(asSet("www.test.cc")), equalTo("test.cc"));
    }

    @Test
    public void singleUppercaseDomain_returnsSameLowercase() {
        assertThat(service.getUniqueStrippedDomain(asSet("CAST.ru")), equalTo("cast.ru"));
    }

    @Test
    public void singleUppercaseDomainWithWww_returnsStrippedLowercase() {
        assertThat(service.getUniqueStrippedDomain(asSet("www.CAST.ru")), equalTo("cast.ru"));
    }

    @Test
    public void singleDomainWithSignificantWww_returnsSame() {
        String domain = "www.yandex";
        assertThat(service.getUniqueStrippedDomain(asSet(domain)), equalTo(domain));
    }

    @Test
    public void twoDomainsWithAndWithoutWww_returnsStripped() {
        Set<String> candidates = asSet("www.test2.com", "test2.com");
        assertThat(service.getUniqueStrippedDomain(candidates), equalTo("test2.com"));
    }

    @Test
    public void twoDomainsWithAndWithoutSignificantWww_returnsNull() {
        Set<String> candidates = asSet("www.tut.by", "tut.by");
        assertThat(service.getUniqueStrippedDomain(candidates), nullValue());
    }

    @Test
    public void twoDifferentDomains_returnsNull() {
        Set<String> candidates = asSet("domain1.com", "domain2.com");
        assertThat(service.getUniqueStrippedDomain(candidates), nullValue());
    }

    @Test
    public void threeDifferentCandidates_returnsNull() {
        Set<String> candidates = asSet("domain1.com", "domain2.com", "domain3.com");
        assertThat(service.getUniqueStrippedDomain(candidates), nullValue());
    }

    @Test
    public void manyDifferentCandidates_returnsNull() {
        Set<String> candidates = IntStream.range(0, 10)
                .mapToObj(ignored -> randomAlphabetic(10) + ".com")
                .collect(toSet());
        assertThat(service.getUniqueStrippedDomain(candidates), nullValue());
    }


    private static Set<String> asSet(String... elements) {
        return new HashSet<>(asList(elements));
    }
}
