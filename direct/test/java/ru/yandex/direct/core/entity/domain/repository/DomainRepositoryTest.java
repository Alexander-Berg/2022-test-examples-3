package ru.yandex.direct.core.entity.domain.repository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.domain.model.ApiDomainStat;
import ru.yandex.direct.core.entity.domain.model.Domain;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestDomain;
import ru.yandex.direct.core.testing.repository.TestDomainRepository;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DomainRepositoryTest {

    private static final int DEFAULT_SHARD = 1;
    private static final String DEFAULT_BANNER_DOMAIN = "yandex.ru";
    private static final String DEFAULT_MIRROR = "google.com";

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private TestDomainRepository testDomainRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Test
    public void getDomains_ReturnActualDomainByExistDomainInPpc() {
        Domain testDomain = TestDomain.testDomain();

        Long id = testDomainRepository.addToPpcDict(testDomain.getDomain());
        Domain expectedDomain = testDomain.withId(id);
        testDomainRepository.addToPpc(DEFAULT_SHARD, expectedDomain);

        List<Domain> actualDomains = domainRepository.getDomains(DEFAULT_SHARD, singletonList(testDomain.getDomain()));
        assertThat(actualDomains, hasSize(1));

        Domain actualDomain = actualDomains.get(0);
        assertThat(actualDomain, beanDiffer(expectedDomain));
    }

    @Test
    public void getDomains_NotFoundByNonExistDomainInPpc() {
        Domain testDomain = TestDomain.testDomain();

        Long id = testDomainRepository.addToPpcDict(testDomain.getDomain());
        assumeThat(id, notNullValue());

        List<Domain> actualDomains = domainRepository.getDomains(DEFAULT_SHARD, singletonList(testDomain.getDomain()));
        assertThat(actualDomains, empty());
    }

    @Test
    public void addDomains_NonExistsSuccessfulSave() {
        Domain domain = TestDomain.testDomain();
        domainRepository.addDomains(DEFAULT_SHARD, singleton(domain));

        List<Domain> actualDomains = domainRepository.getDomains(DEFAULT_SHARD, singletonList(domain.getDomain()));

        assertThat(actualDomains, hasSize(1));
        assertDomain(actualDomains.get(0), domain.getDomain());
    }

    @Test
    public void addDomains_AlreadyExistsInPpcDict_SuccessSaveWithSameId() {
        Domain testDomain = TestDomain.testDomain();

        Long id = testDomainRepository.addToPpcDict(testDomain.getDomain());
        assumeThat(id, notNullValue());
        Domain expectedDomain = testDomain.withId(id);

        Domain domainToSave = new Domain()
                .withDomain(testDomain.getDomain())
                .withReverseDomain(testDomain.getReverseDomain());
        domainRepository.addDomains(DEFAULT_SHARD, singleton(domainToSave));

        List<Domain> actualDomains =
                domainRepository.getDomains(DEFAULT_SHARD, singletonList(domainToSave.getDomain()));
        assertThat(actualDomains, hasSize(1));
        assertThat(actualDomains.get(0), beanDiffer(expectedDomain));
    }

    @Test
    public void getMainMirror() {
        testDomainRepository.addMainMirror(DEFAULT_BANNER_DOMAIN, DEFAULT_MIRROR);
        Map<String, String> domainMirror = domainRepository.getMainMirrors(singleton(DEFAULT_BANNER_DOMAIN));
        assertThat(domainMirror.keySet(), hasSize(1));
        assertThat(domainMirror.get(DEFAULT_BANNER_DOMAIN), is(DEFAULT_MIRROR));
    }

    @Test
    public void getMirrorCorrection() {
        testDomainRepository.addMirrorCorrection(DEFAULT_BANNER_DOMAIN, DEFAULT_MIRROR);
        Map<String, String> domainMirror = domainRepository.getMainMirrors(singleton(DEFAULT_BANNER_DOMAIN));
        assertThat(domainMirror.keySet(), hasSize(1));
        assertThat(domainMirror.get(DEFAULT_BANNER_DOMAIN), is(DEFAULT_MIRROR));
    }

    @Test
    public void getMirror() {
        testDomainRepository.addMirrorCorrection(DEFAULT_BANNER_DOMAIN, DEFAULT_MIRROR);
        testDomainRepository.addMainMirror(DEFAULT_BANNER_DOMAIN, DEFAULT_BANNER_DOMAIN);
        Map<String, String> domainMirror = domainRepository.getMainMirrors(singleton(DEFAULT_BANNER_DOMAIN));
        assertThat(domainMirror.keySet(), hasSize(1));
        assertThat(domainMirror.get(DEFAULT_BANNER_DOMAIN), is(DEFAULT_MIRROR));
    }

    @Test
    public void addDomainsToPpcDictReturnsAddedAndExistingDomains() {
        List<String> domains = Arrays.asList("thebestdomainever.com", "notthatcooldomain.org");
        Map<String, Long> addDomainsToPpcDict = domainRepository.addDomainsToPpcDict(domains);
        Map<String, Long> addDomainsToPpcDict2 = domainRepository.addDomainsToPpcDict(domains);
        Map<String, Long> domainsToIdsFromPpcDict = domainRepository.getDomainsToIdsFromPpcDict(domains);
        assertThat(addDomainsToPpcDict, equalTo(addDomainsToPpcDict2));
        assertThat(addDomainsToPpcDict, equalTo(domainsToIdsFromPpcDict));
    }

    private void assertDomain(Domain actualDomain, String domainText) {
        assertThat(actualDomain.getId(), greaterThan(0L));
        assertThat(actualDomain.getDomain(), is(domainText));
        assertThat(actualDomain.getReverseDomain(), is(StringUtils.reverse(domainText)));
    }

    @Test
    public void updateDomainsStat_insertsStatForNewDomain() {
        String newDomain = "new.domain.com";
        ApiDomainStat statToCreate = new ApiDomainStat().withFilterDomain(newDomain)
                .withAcceptedItems(1L).withDeclinedItems(2L).withBadReasons(3L).withDeclinedPhrases(4L);

        domainRepository.updateDomainsStat(List.of(statToCreate));

        var createdStat = domainRepository.getDomainsStat(List.of(newDomain));
        assertThat(createdStat, contains(beanDiffer(statToCreate)));
    }

    @Test
    public void updateDomainsStat_updatesStatForExistingDomain() {
        String domain = "domain.com";
        ApiDomainStat statToCreate = new ApiDomainStat().withFilterDomain(domain)
                .withAcceptedItems(1L).withDeclinedItems(2L).withBadReasons(3L).withDeclinedPhrases(4L);
        domainRepository.updateDomainsStat(List.of(statToCreate));

        ApiDomainStat statToUpdate = new ApiDomainStat().withFilterDomain(domain)
                .withAcceptedItems(10L).withDeclinedItems(20L).withBadReasons(30L).withDeclinedPhrases(40L);
        domainRepository.updateDomainsStat(List.of(statToUpdate));

        var expectedStat = new ApiDomainStat().withFilterDomain(domain).withStatDate(LocalDate.now())
                .withAcceptedItems(11L).withDeclinedItems(22L).withBadReasons(33L).withDeclinedPhrases(44L);
        var updatedStat = domainRepository.getDomainsStat(List.of(domain));
        assertThat(updatedStat, contains(beanDiffer(expectedStat)));
    }
}
