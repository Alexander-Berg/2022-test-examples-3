package ru.yandex.direct.core.entity.redirectcheckqueue.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.redirectcheckqueue.model.RedirectCheckQueueDomainStat;
import ru.yandex.direct.core.entity.redirectcheckqueue.repository.RedirectCheckQueueRepository;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

public class RedirectCheckQueueServiceTest {
    private static final String DOMAIN_ONE = "domainone.ru";
    private static final String DOMAIN_TWO = "domaintwo.by";
    private static final String DOMAIN_THREE = "domainthree.ua";
    private static final LocalDateTime BASE_TS = LocalDateTime.now();

    @Mock
    private RedirectCheckQueueRepository redirectCheckQueueRepository;
    @Mock
    private ShardHelper shardHelper;

    private RedirectCheckQueueService service;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        service = new RedirectCheckQueueService(redirectCheckQueueRepository, null, shardHelper);
    }

    @Test
    public void testDomainCheckStat() {
        doReturn(Arrays.asList(1, 2, 3))
                .when(shardHelper).dbShards();

        RedirectCheckQueueDomainStat statOneSOne = new RedirectCheckQueueDomainStat()
                .withDomain(DOMAIN_ONE)
                .withCampaignsNum(1)
                .withBannersNum(3)
                .withOldestEntryAge(BASE_TS.plusHours(20));
        RedirectCheckQueueDomainStat statThreeSOne = new RedirectCheckQueueDomainStat()
                .withDomain(DOMAIN_THREE)
                .withCampaignsNum(5)
                .withBannersNum(167)
                .withOldestEntryAge(BASE_TS.minusDays(5));

        RedirectCheckQueueDomainStat statOneSThree = new RedirectCheckQueueDomainStat()
                .withDomain(DOMAIN_ONE)
                .withCampaignsNum(2)
                .withBannersNum(2)
                .withOldestEntryAge(BASE_TS.plusHours(5));
        RedirectCheckQueueDomainStat statTwoSThree = new RedirectCheckQueueDomainStat()
                .withDomain(DOMAIN_TWO)
                .withCampaignsNum(60)
                .withBannersNum(180)
                .withOldestEntryAge(BASE_TS.minusDays(16));
        RedirectCheckQueueDomainStat statThreeSThree = new RedirectCheckQueueDomainStat()
                .withDomain(DOMAIN_THREE)
                .withCampaignsNum(1)
                .withBannersNum(1)
                .withOldestEntryAge(BASE_TS);

        doReturn(Arrays.asList(
                statOneSOne,
                statThreeSOne
        )).when(redirectCheckQueueRepository).getDomainCheckStat(eq(1));
        doReturn(Collections.emptyList()).when(redirectCheckQueueRepository).getDomainCheckStat(eq(2));
        doReturn(Arrays.asList(
                statOneSThree,
                statTwoSThree,
                statThreeSThree
        )).when(redirectCheckQueueRepository).getDomainCheckStat(eq(3));

        List<RedirectCheckQueueDomainStat> domainCheckStat = service.getDomainCheckStat();
        assertThat(domainCheckStat)
                .containsExactlyInAnyOrder(
                        new RedirectCheckQueueDomainStat()
                                .withDomain(DOMAIN_ONE)
                                .withCampaignsNum(statOneSOne.getCampaignsNum() + statOneSThree.getCampaignsNum())
                                .withBannersNum(statOneSOne.getBannersNum() + statOneSThree.getBannersNum())
                                .withOldestEntryAge(statOneSThree.getOldestEntryAge()),
                        new RedirectCheckQueueDomainStat()
                                .withDomain(DOMAIN_TWO)
                                .withCampaignsNum(statTwoSThree.getCampaignsNum())
                                .withBannersNum(statTwoSThree.getBannersNum())
                                .withOldestEntryAge(statTwoSThree.getOldestEntryAge()),
                        new RedirectCheckQueueDomainStat()
                                .withDomain(DOMAIN_THREE)
                                .withCampaignsNum(statThreeSOne.getCampaignsNum() + statThreeSThree.getCampaignsNum())
                                .withBannersNum(statThreeSOne.getBannersNum() + statThreeSThree.getBannersNum())
                                .withOldestEntryAge(statThreeSOne.getOldestEntryAge())
                );
    }

    @Test
    public void testDomainCheckStatEmpty() {
        doReturn(Arrays.asList(1, 2, 3))
                .when(shardHelper).dbShards();
        doReturn(Collections.emptyList()).when(redirectCheckQueueRepository).getDomainCheckStat(anyInt());

        List<RedirectCheckQueueDomainStat> domainCheckStat = service.getDomainCheckStat();
        assertThat(domainCheckStat)
                .isEmpty();
    }
}
