package ru.yandex.direct.core.entity.domain.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jooq.DSLContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.domain.model.Domain;
import ru.yandex.direct.core.entity.domain.repository.DomainRepository;
import ru.yandex.direct.core.entity.domain.repository.MarketRatingRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestDomain;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DomainServiceTest {
    private static final int DEFAULT_SHARD = 1;

    @Autowired
    private MarketRatingRepository marketRatingRepository;
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private ShardHelper shardSupport;
    @Autowired
    private DslContextProvider dslContextProvider;

    private DomainService domainService;

    @Before
    public void before() {
        domainService = new DomainService(marketRatingRepository, domainRepository, shardSupport,
                dslContextProvider);
    }

    @Test
    public void testUpdateRatings() throws Exception {

        domainService.updateMarketDomainRatings(new HashMap<String, Long>() {{
            put("a", 1L);
            put("b", 2L);
        }});
        domainService.updateMarketDomainRatings(new HashMap<String, Long>() {{
            put("b", 3L);
            put("c", 4L);
        }});

        Map<String, Long> map = marketRatingRepository.getAllByName();
        assertEquals((long) map.get("a"), -1L);
        assertEquals((long) map.get("b"), 3L);
        assertEquals((long) map.get("c"), 4L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getOrCreate_emptyDomain() {
        DSLContext dslContext = dslContextProvider.ppc(DEFAULT_SHARD);
        domainService.getOrCreate(dslContext, singletonList(" "));
    }

    @Test
    public void getOrCreate() {
        Domain domain1 = TestDomain.testDomain();
        Domain domain2 = TestDomain.testDomain();
        domainRepository.addDomains(DEFAULT_SHARD, singleton(domain1));

        List<Long> ids = domainService.getOrCreate(
                dslContextProvider.ppc(DEFAULT_SHARD),
                Arrays.asList(domain1.getDomain(), domain2.getDomain()));
        assertThat(ids, hasSize(2));
        assertThat(ids.get(0), not(equalTo(ids.get(1))));
    }

    @Test
    public void getOrCreate_repeatedWithSpaces() {
        Domain domain = TestDomain.testDomain();
        List<Long> ids = domainService.getOrCreate(
                dslContextProvider.ppc(DEFAULT_SHARD),
                Arrays.asList(domain.getDomain() + " ", " " + domain.getDomain()));
        assertThat(ids, hasSize(2));
        assertThat(ids.get(0), equalTo(ids.get(1)));
    }

}
